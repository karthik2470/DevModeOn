package com.cfdeploytool.model;

import java.time.LocalDateTime;

/**
 * Captures the result of deploying one file to one server.
 */
public class DeploymentResult {

    public enum ResultStatus {
        SUCCESS("Success"),
        FAILED("Failed"),
        SKIPPED("Skipped"),
        IN_PROGRESS("In Progress");

        private final String displayName;

        ResultStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String serverName;
    private String serverHost;
    private String fileName;
    private String fileType;
    private ResultStatus status;
    private String message;
    private LocalDateTime timestamp;

    public DeploymentResult() {
        this.timestamp = LocalDateTime.now();
    }

    public DeploymentResult(Server server, DeploymentFile file, ResultStatus status, String message) {
        this();
        this.serverName = server.getName();
        this.serverHost = server.getHost() + ":" + server.getPort();
        this.fileName = file.getFileName();
        this.fileType = file.getFileType().name();
        this.status = status;
        this.message = message;
    }

    // --- Getters and Setters ---

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return status == ResultStatus.SUCCESS;
    }

    @Override
    public String toString() {
        return fileName + " -> " + serverName + ": " + status.getDisplayName() +
                (message != null && !message.isEmpty() ? " (" + message + ")" : "");
    }
}
