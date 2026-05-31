package com.cfdeploytool.persistence;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles reading and writing JSON files to disk.
 * Manages the application's data directory structure.
 */
public class FileStore {

    private final Path dataDir;
    private final Path historyDir;
    private final Path backupDir;
    private final Path serversFile;
    private final Path environmentSettingsFile;

    /**
     * Creates a FileStore rooted at the given base directory.
     * Typically the application's working directory.
     */
    public FileStore(Path baseDir) {
        this.dataDir = baseDir.resolve("data");
        this.historyDir = dataDir.resolve("history");
        this.backupDir = dataDir.resolve("backups_meta");
        this.serversFile = dataDir.resolve("servers.json");
        this.environmentSettingsFile = dataDir.resolve("environment_settings.json");
        initDirectories();
    }

    /**
     * Creates the data directory structure if it doesn't exist.
     */
    private void initDirectories() {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(historyDir);
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            System.err.println("Failed to create data directories: " + e.getMessage());
        }
    }

    // ==================== GENERIC FILE OPERATIONS ====================

    /**
     * Writes a JSON string to a file, creating parent directories if needed.
     */
    public void writeJson(Path filePath, String json) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Reads a JSON string from a file.
     * Returns null if the file doesn't exist.
     */
    public String readJson(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readString(filePath);
    }

    /**
     * Lists all files in a directory matching the given extension.
     */
    public List<Path> listFiles(Path directory, String extension) throws IOException {
        if (!Files.exists(directory)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().endsWith(extension))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Deletes a file if it exists.
     */
    public boolean deleteFile(Path filePath) throws IOException {
        return Files.deleteIfExists(filePath);
    }

    // ==================== SERVER STORAGE ====================

    /**
     * Saves the servers JSON to the servers file.
     */
    public void saveServers(String json) throws IOException {
        writeJson(serversFile, json);
    }

    /**
     * Loads the servers JSON from the servers file.
     */
    public String loadServers() throws IOException {
        return readJson(serversFile);
    }

    // ==================== ENVIRONMENT SETTINGS ====================

    public void saveEnvironmentSettings(String json) throws IOException {
        writeJson(environmentSettingsFile, json);
    }

    public String loadEnvironmentSettings() throws IOException {
        return readJson(environmentSettingsFile);
    }

    // ==================== HISTORY STORAGE ====================

    /**
     * Saves a deployment history JSON to a file in the history directory.
     */
    public void saveHistory(String id, String json) throws IOException {
        Path historyFile = historyDir.resolve(id + ".json");
        writeJson(historyFile, json);
    }

    /**
     * Loads a deployment history JSON by ID.
     */
    public String loadHistory(String id) throws IOException {
        Path historyFile = historyDir.resolve(id + ".json");
        return readJson(historyFile);
    }

    /**
     * Lists all history file paths.
     */
    public List<Path> listHistoryFiles() throws IOException {
        return listFiles(historyDir, ".json");
    }

    /**
     * Deletes a specific history record.
     */
    public boolean deleteHistory(String id) throws IOException {
        Path historyFile = historyDir.resolve(id + ".json");
        return deleteFile(historyFile);
    }

    /**
     * Deletes all history records.
     */
    public void clearHistory() throws IOException {
        List<Path> files = listHistoryFiles();
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    // ==================== BACKUP STORAGE ====================

    /**
     * Saves a backup record JSON to a file in the backup metadata directory.
     */
    public void saveBackup(String id, String json) throws IOException {
        Path backupFile = backupDir.resolve(id + ".json");
        writeJson(backupFile, json);
    }

    /**
     * Loads a backup record JSON by ID.
     */
    public String loadBackup(String id) throws IOException {
        Path backupFile = backupDir.resolve(id + ".json");
        return readJson(backupFile);
    }

    /**
     * Lists all backup metadata file paths.
     */
    public List<Path> listBackupFiles() throws IOException {
        return listFiles(backupDir, ".json");
    }

    /**
     * Deletes a specific backup record.
     */
    public boolean deleteBackup(String id) throws IOException {
        Path backupFile = backupDir.resolve(id + ".json");
        return deleteFile(backupFile);
    }

    /**
     * Deletes all backup records.
     */
    public void clearBackups() throws IOException {
        List<Path> files = listBackupFiles();
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    // ==================== ACCESSORS ====================

    public Path getDataDir() {
        return dataDir;
    }

    public Path getHistoryDir() {
        return historyDir;
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public Path getServersFile() {
        return serversFile;
    }
}
