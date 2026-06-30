package com.cfdeployagent.handler;

import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.service.VersionService;
import com.cfdeployagent.service.VersionService.VersionResult;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles POST /api/version/read — reads the current 4T version file.
 */
public class VersionReadHandler implements HttpHandler {

    private final VersionService versionService;

    public VersionReadHandler(VersionService versionService) {
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

            DeploymentCategory category = DeploymentCategory.parse(categoryRaw);
            VersionResult result = versionService.readVersion(category);
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
}
