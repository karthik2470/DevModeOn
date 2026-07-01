package com.cfdeployagent.handler;

import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.config.DeploymentTier;
import com.cfdeployagent.service.PathValidationService;
import com.cfdeployagent.service.PathValidationService.ValidationResult;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles POST /api/validate-path — validates target path and write permissions.
 */
public class ValidatePathHandler implements HttpHandler {

    private final PathValidationService validationService;

    public ValidatePathHandler(PathValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HealthHandler.sendResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            return;
        }

        try {
            String body = JsonUtil.readBody(exchange.getRequestBody());
            String categoryRaw = JsonUtil.extractField(body, "category");
            if (categoryRaw == null || categoryRaw.isBlank()) {
                categoryRaw = JsonUtil.extractField(body, "deploymentCategory");
            }
            String tierRaw = JsonUtil.extractField(body, "deploymentTier");
            String targetPath = JsonUtil.extractField(body, "targetPath");

            DeploymentCategory category = DeploymentCategory.parse(categoryRaw);
            DeploymentTier tier = DeploymentTier.parse(tierRaw);
            boolean allowCreate = parseAllowCreateDirectories(body);
            ValidationResult result = validationService.validate(category, tier, allowCreate, targetPath);
            int statusCode = result.isValid() ? 200 : 400;
            HealthHandler.sendResponse(exchange, statusCode, JsonUtil.validationResponse(
                    result.isValid(),
                    result.getPath() != null ? result.getPath().toString() : "",
                    result.isWritable(),
                    result.getMessage()));
        } catch (IllegalArgumentException e) {
            HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse(e.getMessage()));
        } catch (IOException e) {
            HealthHandler.sendResponse(exchange, 500,
                    JsonUtil.errorResponse("Failed to validate path: " + e.getMessage()));
        }
    }

    private static boolean parseAllowCreateDirectories(String body) {
        String raw = JsonUtil.extractField(body, "allowCreateDirectories");
        if (raw == null || raw.isBlank()) {
            String serverType = JsonUtil.extractField(body, "serverType");
            if (serverType != null && serverType.toUpperCase().contains("CORPORATE")) {
                return false;
            }
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
