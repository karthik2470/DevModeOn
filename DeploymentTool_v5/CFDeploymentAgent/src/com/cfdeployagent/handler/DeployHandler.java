package com.cfdeployagent.handler;

import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.config.DeploymentTier;
import com.cfdeployagent.service.FileDeploymentService;
import com.cfdeployagent.service.FileDeploymentService.DeployResult;
import com.cfdeployagent.util.JsonUtil;
import com.cfdeployagent.util.MultipartParser;
import com.cfdeployagent.util.MultipartParser.ParsedMultipart;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles POST /api/deploy multipart upload requests from the deployment client.
 */
public class DeployHandler implements HttpHandler {

    private final FileDeploymentService deploymentService;

    public DeployHandler(FileDeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HealthHandler.sendResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse("Expected multipart/form-data"));
            return;
        }

        try {
            ParsedMultipart multipart = MultipartParser.parse(exchange.getRequestBody(), contentType);

            String fileName = multipart.getField("fileName");
            String categoryRaw = multipart.getField("deploymentCategory");
            if (categoryRaw == null || categoryRaw.isBlank()) {
                categoryRaw = multipart.getField("category");
            }
            String tierRaw = multipart.getField("deploymentTier");
            byte[] fileBytes = multipart.getFileBytes();

            if (fileName == null || fileName.isBlank()) {
                HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse("Missing fileName field"));
                return;
            }
            if (fileBytes == null) {
                HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse("Missing file upload"));
                return;
            }

            DeploymentCategory category = DeploymentCategory.parse(categoryRaw);
            DeploymentTier tier = DeploymentTier.parse(tierRaw);
            boolean allowCreate = parseAllowCreateDirectories(multipart);
            boolean performBackup = parsePerformBackup(multipart);
            String backupDir = multipart.getField("backupDir");
            String deployBackupDir = multipart.getField("deployBackupDir");
            String targetPath = multipart.getField("targetPath");
            String lastModifiedRaw = multipart.getField("lastModified");
            Long lastModified = null;
            if (lastModifiedRaw != null && !lastModifiedRaw.isBlank()) {
                try {
                    lastModified = Long.parseLong(lastModifiedRaw.trim());
                } catch (NumberFormatException ignored) {}
            }
            DeployResult result = deploymentService.deploy(
                    fileName, category, tier, fileBytes, allowCreate, performBackup,
                    backupDir, deployBackupDir, targetPath, lastModified);
            int statusCode = result.isSuccess() ? 200 : 500;
            HealthHandler.sendResponse(exchange, statusCode,
                    JsonUtil.deployResponse(result.isSuccess(), result.getMessage()));
        } catch (IllegalArgumentException e) {
            HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse(e.getMessage()));
        } catch (IOException e) {
            HealthHandler.sendResponse(exchange, 500,
                    JsonUtil.errorResponse("Failed to process upload: " + e.getMessage()));
        }
    }

    private static boolean parsePerformBackup(MultipartParser.ParsedMultipart multipart) {
        String raw = multipart.getField("performBackup");
        return raw != null && Boolean.parseBoolean(raw.trim());
    }

    private static boolean parseAllowCreateDirectories(MultipartParser.ParsedMultipart multipart) {
        String raw = multipart.getField("allowCreateDirectories");
        if (raw == null || raw.isBlank()) {
            String serverType = multipart.getField("serverType");
            if (serverType != null && serverType.toUpperCase().contains("CORPORATE")) {
                return false;
            }
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
