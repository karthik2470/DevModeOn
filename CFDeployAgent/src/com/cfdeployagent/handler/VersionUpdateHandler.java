package com.cfdeployagent.handler;

import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.service.VersionService;
import com.cfdeployagent.service.VersionService.VersionResult;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles POST /api/version/update — writes a new version to the 4T version file.
 */
public class VersionUpdateHandler implements HttpHandler {

    private final VersionService versionService;

    public VersionUpdateHandler(VersionService versionService) {
        this.versionService = versionService;
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
            String version = JsonUtil.extractField(body, "version");

            DeploymentCategory category = DeploymentCategory.parse(categoryRaw);
            boolean allowCreate = parseAllowCreateDirectories(body);
            VersionResult result = versionService.updateVersion(category, version, allowCreate);
            int statusCode = result.isSuccess() ? 200 : 500;
            HealthHandler.sendResponse(exchange, statusCode, JsonUtil.versionResponse(
                    result.isSuccess(),
                    result.getPreviousVersion(),
                    result.getNewVersion(),
                    result.getPath(),
                    result.getMessage()));
        } catch (IllegalArgumentException e) {
            HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse(e.getMessage()));
        }
    }

    private static boolean parseAllowCreateDirectories(String body) {
        String raw = JsonUtil.extractField(body, "allowCreateDirectories");
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
