package com.cfdeploytool.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A persisted record of an entire deployment job including all results.
 */
public class DeploymentHistory {

    private String id;
    private String ticketNumber;
    private LocalDateTime deployedAt;
    private int totalFiles;
    private int totalServers;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private List<String> fileNames;
    private List<String> serverNames;
    private List<DeploymentResult> results;

    public DeploymentHistory() {
        this.id = UUID.randomUUID().toString();
        this.deployedAt = LocalDateTime.now();
        this.fileNames = new ArrayList<>();
        this.serverNames = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    public DeploymentHistory(DeploymentRequest request, List<DeploymentResult> results) {
        this();
        this.ticketNumber = request.getTicketNumber();
        this.totalFiles = request.getFiles().size();
        this.totalServers = request.getTargetServers().size();
        this.results = new ArrayList<>(results);

        // Collect file names
        for (DeploymentFile f : request.getFiles()) {
            fileNames.add(f.getFileName());
        }

        // Collect server names
        for (Server s : request.getTargetServers()) {
            serverNames.add(s.getName());
        }

        // Count results
        this.successCount = 0;
        this.failedCount = 0;
        this.skippedCount = 0;
        for (DeploymentResult r : results) {
            switch (r.getStatus()) {
                case SUCCESS -> successCount++;
                case FAILED -> failedCount++;
                case SKIPPED -> skippedCount++;
                default -> {}
            }
        }
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(LocalDateTime deployedAt) {
        this.deployedAt = deployedAt;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalServers() {
        return totalServers;
    }

    public void setTotalServers(int totalServers) {
        this.totalServers = totalServers;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public List<String> getServerNames() {
        return serverNames;
    }

    public void setServerNames(List<String> serverNames) {
        this.serverNames = serverNames;
    }

    public List<DeploymentResult> getResults() {
        return results;
    }

    public void setResults(List<DeploymentResult> results) {
        this.results = results;
    }

    public int getTotalOperations() {
        return totalFiles * totalServers;
    }

    public boolean isFullySuccessful() {
        return failedCount == 0 && skippedCount == 0;
    }

    public String getOverallStatus() {
        if (failedCount == 0 && skippedCount == 0) {
            return "SUCCESS";
        } else if (successCount == 0) {
            return "FAILED";
        } else {
            return "PARTIAL";
        }
    }

    @Override
    public String toString() {
        return "Deployment " + id.substring(0, 8) + " at " + deployedAt +
                " [" + successCount + "/" + getTotalOperations() + " succeeded]";
    }
}
