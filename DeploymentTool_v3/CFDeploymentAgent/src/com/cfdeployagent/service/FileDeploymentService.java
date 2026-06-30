package com.cfdeployagent.service;

import com.cfdeployagent.config.AgentConfig;
import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.config.DeploymentTier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes uploaded files to category and tier-specific target directories.
 * Corporate deploy: old file -> backupDir, new file -> deployBackupDir (client-supplied).
 */
public class FileDeploymentService {

    private final AgentConfig config;
    private final BackupStorageService backupStorage = new BackupStorageService();

    public FileDeploymentService(AgentConfig config) {
        this.config = config;
    }

    public DeployResult deploy(String fileName, DeploymentCategory category,
                               DeploymentTier tier, byte[] fileBytes) {
        return deploy(fileName, category, tier, fileBytes, true, false, null, null);
    }

    public DeployResult deploy(String fileName, DeploymentCategory category,
                               DeploymentTier tier, byte[] fileBytes,
                               boolean allowCreateDirectories,
                               boolean performBackup,
                               String existingFileBackupDir,
                               String deployingFileBackupDir) {
        return deploy(fileName, category, tier, fileBytes, allowCreateDirectories, performBackup,
                existingFileBackupDir, deployingFileBackupDir, null);
    }

    public DeployResult deploy(String fileName, DeploymentCategory category,
                               DeploymentTier tier, byte[] fileBytes,
                               boolean allowCreateDirectories,
                               boolean performBackup,
                               String existingFileBackupDir,
                               String deployingFileBackupDir,
                               String targetPathRaw) {
        if (fileName == null || fileName.isBlank()) {
            return DeployResult.failure("fileName is required");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            return DeployResult.failure("Uploaded file is empty");
        }

        String safeName = sanitizeFileName(fileName);
        if (safeName.isBlank()) {
            return DeployResult.failure("Invalid fileName");
        }

        Path targetDir;
        if (targetPathRaw != null && !targetPathRaw.isBlank()) {
            targetDir = Path.of(targetPathRaw);
        } else {
            targetDir = config.getDeployPath(category, tier);
        }
        Path targetFile = targetDir.resolve(safeName).normalize();

        if (!targetFile.startsWith(targetDir)) {
            return DeployResult.failure("Invalid file path");
        }

        try {
            if (!Files.exists(targetDir)) {
                if (!allowCreateDirectories) {
                    return DeployResult.failure("Target path does not exist: " + targetDir);
                }
                Files.createDirectories(targetDir);
            } else if (!Files.isDirectory(targetDir)) {
                return DeployResult.failure("Target path is not a directory: " + targetDir);
            }

            if (performBackup && Files.exists(targetFile)
                    && existingFileBackupDir != null && !existingFileBackupDir.isBlank()) {
                backupFileToDateFolder(targetFile, existingFileBackupDir);
            }

            Files.write(targetFile, fileBytes);

            if (performBackup && deployingFileBackupDir != null && !deployingFileBackupDir.isBlank()) {
                backupFileToDateFolder(targetFile, deployingFileBackupDir);
            }

            return DeployResult.success("Deployed to " + targetFile);
        } catch (IOException e) {
            return DeployResult.failure("Failed to write file: " + e.getMessage());
        }
    }

    /** {@code backupRoot}/yyyyMMdd/filename (rotate existing copy in that folder). */
    private void backupFileToDateFolder(Path sourceFile, String backupRootPath) throws IOException {
        Path dateFolder = backupStorage.resolveDateFolder(backupRootPath);
        backupStorage.copyWithRotation(sourceFile, dateFolder);
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static final class DeployResult {
        private final boolean success;
        private final String message;

        private DeployResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static DeployResult success(String message) {
            return new DeployResult(true, message);
        }

        public static DeployResult failure(String message) {
            return new DeployResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
