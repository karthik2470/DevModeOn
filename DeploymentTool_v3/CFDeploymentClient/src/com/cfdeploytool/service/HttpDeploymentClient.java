package com.cfdeploytool.service;

import com.cfdeploytool.model.DeploymentCategory;
import com.cfdeploytool.model.DeploymentFile;
import com.cfdeploytool.model.DeploymentResult;
import com.cfdeploytool.model.DeploymentResult.ResultStatus;
import com.cfdeploytool.model.DeploymentTier;
import com.cfdeploytool.model.Server;
import com.cfdeploytool.persistence.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client for communicating with remote deployment agents.
 */
public class HttpDeploymentClient {

    private final HttpClient httpClient;
    private static final int TIMEOUT_SECONDS = 120;

    public HttpDeploymentClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public PathValidationResult validatePath(Server server, DeploymentCategory category,
                                             DeploymentTier tier) {
        try {
            String body = "{\"category\":\"" + category.name()
                    + "\",\"deploymentTier\":\"" + tier.name()
                    + "\",\"targetPath\":" + JsonUtil.quote(category.getPath(tier))
                    + ",\"serverType\":\"" + server.getServerTypesString()
                    + "\",\"allowCreateDirectories\":" + server.isAllowCreateDirectories() + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/validate-path"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            boolean valid = responseBody.contains("\"valid\":true") || responseBody.contains("\"valid\": true");
            String path = extractJsonField(responseBody, "path");
            String message = extractJsonField(responseBody, "message");
            return new PathValidationResult(server, tier, valid, path,
                    message != null ? message : responseBody);
        } catch (java.net.ConnectException e) {
            return new PathValidationResult(server, tier, false, category.getPath(tier),
                    "Connection refused - agent may not be running");
        } catch (Exception e) {
            return new PathValidationResult(server, tier, false, category.getPath(tier),
                    "Validation error: " + e.getMessage());
        }
    }

    public DeploymentResult sendFile(Server server, DeploymentFile deploymentFile,
                                     DeploymentCategory category, DeploymentTier tier) {
        return sendFile(server, deploymentFile, category, tier, null, null);
    }

    public DeploymentResult sendFile(Server server, DeploymentFile deploymentFile,
                                     DeploymentCategory category, DeploymentTier tier,
                                     String existingFileBackupDir, String deployingFileBackupDir) {
        try {
            String boundary = "----CFDeployBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] fileBytes = Files.readAllBytes(deploymentFile.getFile().toPath());
            boolean hasExistingBackup = existingFileBackupDir != null && !existingFileBackupDir.isBlank();
            boolean hasDeployBackup = deployingFileBackupDir != null && !deployingFileBackupDir.isBlank();
            boolean performBackup = server.isCorporate() && (hasExistingBackup || hasDeployBackup);
            byte[] body = buildMultipartBody(boundary, server, deploymentFile, category, tier,
                    fileBytes, performBackup, existingFileBackupDir, deployingFileBackupDir);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/deploy"))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"success\":true") || responseBody.contains("\"success\": true")) {
                    String message = extractJsonField(responseBody, "message");
                    String detail = "[" + tier.getDisplayName() + "] "
                            + (message != null ? message : "Deployed successfully");
                    return new DeploymentResult(server, deploymentFile, ResultStatus.SUCCESS, detail);
                } else {
                    String message = extractJsonField(responseBody, "message");
                    return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                            "[" + tier.getDisplayName() + "] "
                                    + (message != null ? message : "Agent returned failure"));
                }
            } else {
                return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                        "[" + tier.getDisplayName() + "] HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (java.net.ConnectException e) {
            return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                    "[" + tier.getDisplayName() + "] Connection refused");
        } catch (java.net.http.HttpTimeoutException e) {
            return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                    "[" + tier.getDisplayName() + "] Request timed out");
        } catch (IOException e) {
            return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                    "[" + tier.getDisplayName() + "] I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                    "[" + tier.getDisplayName() + "] Deployment interrupted");
        } catch (Exception e) {
            return new DeploymentResult(server, deploymentFile, ResultStatus.FAILED,
                    "[" + tier.getDisplayName() + "] " + e.getMessage());
        }
    }

    public VersionReadResult readVersion(Server server, DeploymentCategory category) {
        try {
            String body = "{\"category\":\"" + category.name()
                    + "\",\"targetPath\":" + JsonUtil.quote(category.getVersionPath())
                    + ",\"allowCreateDirectories\":" + server.isAllowCreateDirectories() + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/version/read"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() == 404) {
                return new VersionReadResult(server, false, null, null,
                        "Agent endpoint not found - restart CFDeployAgent with latest build");
            }
            if (response.statusCode() >= 400) {
                String message = extractJsonField(responseBody, "message");
                return new VersionReadResult(server, false, null, null,
                        message != null ? message : "HTTP " + response.statusCode());
            }

            return parseVersionReadResult(server, responseBody);
        } catch (java.net.ConnectException e) {
            return new VersionReadResult(server, false, null, null,
                    "Connection refused - is the agent running on " + server.getHost() + ":" + server.getPort() + "?");
        } catch (Exception e) {
            return new VersionReadResult(server, false, null, null,
                    "Failed to read version: " + e.getMessage());
        }
    }

    public VersionUpdateResult updateVersion(Server server, DeploymentCategory category, String version) {
        try {
            String body = "{\"category\":\"" + category.name()
                    + "\",\"version\":\"" + escapeJson(version)
                    + "\",\"targetPath\":" + JsonUtil.quote(category.getVersionPath())
                    + ",\"allowCreateDirectories\":" + server.isAllowCreateDirectories() + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/version/update"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseVersionUpdateResult(server, response.body());
        } catch (Exception e) {
            return new VersionUpdateResult(server, false, null, null,
                    "Version update failed: " + e.getMessage());
        }
    }

    private VersionReadResult parseVersionReadResult(Server server, String responseBody) {
        boolean success = responseBody.contains("\"success\":true") || responseBody.contains("\"success\": true");
        String version = extractJsonField(responseBody, "newVersion");
        if (version == null) {
            version = extractJsonField(responseBody, "previousVersion");
        }
        String path = extractJsonField(responseBody, "path");
        String message = extractJsonField(responseBody, "message");
        if (!success && message == null) {
            message = responseBody;
        }
        return new VersionReadResult(server, success, version, path, message);
    }

    private VersionUpdateResult parseVersionUpdateResult(Server server, String responseBody) {
        boolean success = responseBody.contains("\"success\":true") || responseBody.contains("\"success\": true");
        String previous = extractJsonField(responseBody, "previousVersion");
        String next = extractJsonField(responseBody, "newVersion");
        String message = extractJsonField(responseBody, "message");
        return new VersionUpdateResult(server, success, previous, next,
                message != null ? message : responseBody);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private byte[] buildMultipartBody(String boundary, Server server, DeploymentFile deploymentFile,
                                      DeploymentCategory category, DeploymentTier tier,
                                      byte[] fileBytes, boolean performBackup,
                                      String existingFileBackupDir,
                                      String deployingFileBackupDir) throws IOException {
        String fileName = deploymentFile.getFileName();
        StringBuilder sb = new StringBuilder();

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"fileName\"\r\n\r\n");
        sb.append(fileName).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"deploymentCategory\"\r\n\r\n");
        sb.append(category.name()).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"deploymentTier\"\r\n\r\n");
        sb.append(tier.name()).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"serverType\"\r\n\r\n");
        sb.append(server.getServerTypesString()).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"allowCreateDirectories\"\r\n\r\n");
        sb.append(server.isAllowCreateDirectories()).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"targetPath\"\r\n\r\n");
        sb.append(category.getPath(tier)).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"performBackup\"\r\n\r\n");
        sb.append(performBackup).append("\r\n");

        if (existingFileBackupDir != null && !existingFileBackupDir.isBlank()) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"backupDir\"\r\n\r\n");
            sb.append(existingFileBackupDir).append("\r\n");
        }

        if (deployingFileBackupDir != null && !deployingFileBackupDir.isBlank()) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"deployBackupDir\"\r\n\r\n");
            sb.append(deployingFileBackupDir).append("\r\n");
        }

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"fileType\"\r\n\r\n");
        sb.append(deploymentFile.getFileType().name()).append("\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(fileName).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
        return body;
    }

    public ListDirectoryResult listDirectory(Server server, String directoryPath) {
        try {
            String body = "{\"path\":\"" + escapeJson(directoryPath) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/list-directory"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseListDirectoryResult(response.statusCode(), response.body());
        } catch (java.net.ConnectException e) {
            return ListDirectoryResult.failure("Connection refused - is the agent running?");
        } catch (Exception e) {
            return ListDirectoryResult.failure("Failed to list files: " + e.getMessage());
        }
    }

    public BackupFilesResult backupFiles(Server server, String sourcePath, String backupDir,
                                         List<String> fileNames) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("{\"sourcePath\":\"").append(escapeJson(sourcePath));
            body.append("\",\"backupDir\":\"").append(escapeJson(backupDir));
            body.append("\",\"fileNames\":[");
            for (int i = 0; i < fileNames.size(); i++) {
                if (i > 0) {
                    body.append(',');
                }
                body.append('"').append(escapeJson(fileNames.get(i))).append('"');
            }
            body.append("]}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/backup-files"))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            boolean success = responseBody.contains("\"success\":true") || responseBody.contains("\"success\": true");
            String message = extractJsonField(responseBody, "message");
            String backupPath = extractJsonField(responseBody, "backupPath");
            int fileCount = extractJsonInt(responseBody, "fileCount");
            if (!success) {
                return BackupFilesResult.failure(message != null ? message : responseBody);
            }
            return BackupFilesResult.success(backupPath, fileCount,
                    message != null ? message : "Backup complete");
        } catch (java.net.ConnectException e) {
            return BackupFilesResult.failure("Connection refused - is the agent running?");
        } catch (Exception e) {
            return BackupFilesResult.failure("Backup failed: " + e.getMessage());
        }
    }

    private ListDirectoryResult parseListDirectoryResult(int statusCode, String responseBody) {
        if (statusCode == 404) {
            return ListDirectoryResult.failure(
                    "Agent endpoint not found — restart CFDeployAgent with latest build");
        }
        boolean success = responseBody.contains("\"success\":true") || responseBody.contains("\"success\": true");
        String path = extractJsonField(responseBody, "path");
        String message = extractJsonField(responseBody, "message");
        if (!success) {
            return ListDirectoryResult.failure(message != null ? message : responseBody);
        }
        List<RemoteFileEntry> files = parseFileEntries(responseBody);
        return ListDirectoryResult.success(path, files, message != null ? message : "OK");
    }

    private List<RemoteFileEntry> parseFileEntries(String json) {
        List<RemoteFileEntry> files = new ArrayList<>();
        int filesKey = json.indexOf("\"files\"");
        if (filesKey < 0) {
            return files;
        }
        int arrStart = json.indexOf('[', filesKey);
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd <= arrStart) {
            return files;
        }
        String arr = json.substring(arrStart + 1, arrEnd);
        int i = 0;
        while (i < arr.length()) {
            int objStart = arr.indexOf('{', i);
            if (objStart < 0) {
                break;
            }
            int objEnd = arr.indexOf('}', objStart);
            if (objEnd < 0) {
                break;
            }
            String obj = arr.substring(objStart, objEnd + 1);
            String name = extractJsonField(obj, "name");
            long size = extractJsonLong(obj, "size");
            if (name != null) {
                files.add(new RemoteFileEntry(name, size));
            }
            i = objEnd + 1;
        }
        return files;
    }

    private long extractJsonLong(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return 0;
        }
        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx < 0) {
            return 0;
        }
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int extractJsonInt(String json, String field) {
        return (int) extractJsonLong(json, field);
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    public static final class PathValidationResult {
        private final Server server;
        private final DeploymentTier tier;
        private final boolean valid;
        private final String path;
        private final String message;

        public PathValidationResult(Server server, DeploymentTier tier, boolean valid,
                                    String path, String message) {
            this.server = server;
            this.tier = tier;
            this.valid = valid;
            this.path = path;
            this.message = message;
        }

        public Server getServer() {
            return server;
        }

        public DeploymentTier getTier() {
            return tier;
        }

        public boolean isValid() {
            return valid;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class VersionReadResult {
        private final Server server;
        private final boolean success;
        private final String version;
        private final String path;
        private final String message;

        public VersionReadResult(Server server, boolean success, String version,
                                 String path, String message) {
            this.server = server;
            this.success = success;
            this.version = version;
            this.path = path;
            this.message = message;
        }

        public Server getServer() {
            return server;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getVersion() {
            return version;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class VersionUpdateResult {
        private final Server server;
        private final boolean success;
        private final String previousVersion;
        private final String newVersion;
        private final String message;

        public VersionUpdateResult(Server server, boolean success,
                                   String previousVersion, String newVersion, String message) {
            this.server = server;
            this.success = success;
            this.previousVersion = previousVersion;
            this.newVersion = newVersion;
            this.message = message;
        }

        public Server getServer() {
            return server;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPreviousVersion() {
            return previousVersion;
        }

        public String getNewVersion() {
            return newVersion;
        }

        public String getMessage() {
            return message;
        }
    }

    public record RemoteFileEntry(String name, long sizeBytes) {
        public String formattedSize() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            }
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }

    public static final class ListDirectoryResult {
        private final boolean success;
        private final String path;
        private final List<RemoteFileEntry> files;
        private final String message;

        private ListDirectoryResult(boolean success, String path, List<RemoteFileEntry> files, String message) {
            this.success = success;
            this.path = path;
            this.files = files != null ? files : List.of();
            this.message = message;
        }

        public static ListDirectoryResult success(String path, List<RemoteFileEntry> files, String message) {
            return new ListDirectoryResult(true, path, files, message);
        }

        public static ListDirectoryResult failure(String message) {
            return new ListDirectoryResult(false, null, List.of(), message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPath() {
            return path;
        }

        public List<RemoteFileEntry> getFiles() {
            return files;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class BackupFilesResult {
        private final boolean success;
        private final String backupPath;
        private final int fileCount;
        private final String message;

        private BackupFilesResult(boolean success, String backupPath, int fileCount, String message) {
            this.success = success;
            this.backupPath = backupPath;
            this.fileCount = fileCount;
            this.message = message;
        }

        public static BackupFilesResult success(String backupPath, int fileCount, String message) {
            return new BackupFilesResult(true, backupPath, fileCount, message);
        }

        public static BackupFilesResult failure(String message) {
            return new BackupFilesResult(false, null, 0, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getBackupPath() {
            return backupPath;
        }

        public int getFileCount() {
            return fileCount;
        }

        public String getMessage() {
            return message;
        }
    }
}
