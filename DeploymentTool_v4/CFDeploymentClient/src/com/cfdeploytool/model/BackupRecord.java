package com.cfdeploytool.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a backup record capturing metadata about a backup operation.
 */
public class BackupRecord {

    public enum BackupStatus {
        SUCCESS, FAILED, PARTIAL, IN_PROGRESS
    }

    private String id;
    private String serverName;
    private String serverHost;
    private String backupPath;
    private LocalDateTime createdAt;
    private long totalSizeBytes;
    private int fileCount;
    private BackupStatus status;
    private String notes;
    private List<String> fileNames;

    public BackupRecord() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = BackupStatus.IN_PROGRESS;
        this.notes = "";
        this.fileNames = new ArrayList<>();
    }

    public BackupRecord(String serverName, String serverHost, String backupPath) {
        this();
        this.serverName = serverName;
        this.serverHost = serverHost;
        this.backupPath = backupPath;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    /**
     * Returns a human-readable formatted size string.
     */
    public String getFormattedSize() {
        if (totalSizeBytes < 1024) return totalSizeBytes + " B";
        if (totalSizeBytes < 1024 * 1024) return String.format("%.1f KB", totalSizeBytes / 1024.0);
        if (totalSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024));
        return String.format("%.2f GB", totalSizeBytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public String toString() {
        return serverName + " — " + createdAt + " (" + status + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupRecord that = (BackupRecord) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
