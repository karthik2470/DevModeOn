package com.cfdeploytool.service;

import com.cfdeploytool.model.DeploymentHistory;
import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.persistence.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages deployment history persistence and retrieval.
 */
public class HistoryService {

    private final FileStore fileStore;

    public HistoryService(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    /**
     * Saves a deployment history record.
     */
    public void saveDeployment(DeploymentHistory history) {
        try {
            String json = JsonUtil.historyToJson(history);
            fileStore.saveHistory(history.getId(), json);
        } catch (IOException e) {
            System.err.println("Failed to save deployment history: " + e.getMessage());
        }
    }

    /**
     * Returns all deployment history records, sorted newest first.
     */
    public List<DeploymentHistory> getHistory() {
        List<DeploymentHistory> historyList = new ArrayList<>();
        try {
            List<Path> files = fileStore.listHistoryFiles();
            for (Path file : files) {
                try {
                    String json = fileStore.readJson(file);
                    if (json != null && !json.isBlank()) {
                        DeploymentHistory history = JsonUtil.jsonToHistory(json);
                        historyList.add(history);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse history file " + file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to list history files: " + e.getMessage());
        }

        // Sort newest first
        historyList.sort(Comparator.comparing(DeploymentHistory::getDeployedAt).reversed());
        return historyList;
    }

    /**
     * Loads a specific deployment history by ID.
     */
    public DeploymentHistory getHistoryById(String id) {
        try {
            String json = fileStore.loadHistory(id);
            if (json != null && !json.isBlank()) {
                return JsonUtil.jsonToHistory(json);
            }
        } catch (IOException e) {
            System.err.println("Failed to load history: " + e.getMessage());
        }
        return null;
    }

    /**
     * Deletes a specific history record.
     */
    public boolean deleteHistory(String id) {
        try {
            return fileStore.deleteHistory(id);
        } catch (IOException e) {
            System.err.println("Failed to delete history: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears all deployment history.
     */
    public void clearHistory() {
        try {
            fileStore.clearHistory();
        } catch (IOException e) {
            System.err.println("Failed to clear history: " + e.getMessage());
        }
    }

    /**
     * Returns the total number of history records.
     */
    public int getHistoryCount() {
        try {
            return fileStore.listHistoryFiles().size();
        } catch (IOException e) {
            return 0;
        }
    }
}
