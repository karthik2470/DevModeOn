package com.cfdeployagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lists and backs up files in a deploy directory on the agent machine.
 */
public class DirectoryService {

    private final BackupStorageService backupStorage = new BackupStorageService();

    public DirectoryResult listFiles(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return DirectoryResult.failure("path is required");
        }
        Path dir = Path.of(directoryPath.trim()).normalize();
        if (!Files.exists(dir)) {
            return DirectoryResult.failure("Directory does not exist: " + dir);
        }
        if (!Files.isDirectory(dir)) {
            return DirectoryResult.failure("Path is not a directory: " + dir);
        }
        try {
            List<FileEntry> files = new ArrayList<>();
            try (Stream<Path> stream = Files.list(dir)) {
                for (Path p : stream.filter(Files::isRegularFile).toList()) {
                    files.add(new FileEntry(
                            p.getFileName().toString(),
                            Files.size(p),
                            p.toString()));
                }
            }
            files.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            return DirectoryResult.success(dir.toString(), files);
        } catch (IOException e) {
            return DirectoryResult.failure("Failed to list directory: " + e.getMessage());
        }
    }

    public BackupFilesResult backupFiles(String sourcePath, String backupDirPath, List<String> fileNames) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return BackupFilesResult.failure("sourcePath is required");
        }
        if (backupDirPath == null || backupDirPath.isBlank()) {
            return BackupFilesResult.failure("backupDir is required");
        }
        if (fileNames == null || fileNames.isEmpty()) {
            return BackupFilesResult.failure("No files selected");
        }

        Path sourceDir = Path.of(sourcePath.trim()).normalize();

        try {
            if (!Files.isDirectory(sourceDir)) {
                return BackupFilesResult.failure("Source is not a directory: " + sourceDir);
            }
            Path backupRun = backupStorage.resolveDateFolder(backupDirPath);

            int ok = 0;
            List<String> errors = new ArrayList<>();
            for (String name : fileNames) {
                String safe = sanitizeFileName(name);
                if (safe.isBlank()) {
                    continue;
                }
                Path src = sourceDir.resolve(safe).normalize();
                if (!src.startsWith(sourceDir)) {
                    errors.add(safe + ": invalid path");
                    continue;
                }
                if (!Files.isRegularFile(src)) {
                    errors.add(safe + ": not found");
                    continue;
                }
                try {
                    backupStorage.copyWithRotation(src, backupRun);
                    ok++;
                } catch (IOException e) {
                    errors.add(safe + ": " + e.getMessage());
                }
            }

            if (ok == 0) {
                return BackupFilesResult.failure(
                        errors.isEmpty() ? "No files backed up" : String.join("; ", errors));
            }
            String msg = "Backed up " + ok + " file(s) to " + backupRun;
            if (!errors.isEmpty()) {
                msg += " (" + errors.size() + " skipped)";
            }
            return BackupFilesResult.success(backupRun.toString(), ok, errors, msg);
        } catch (IOException e) {
            return BackupFilesResult.failure("Backup failed: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record FileEntry(String name, long sizeBytes, String fullPath) {
    }

    public static final class DirectoryResult {
        private final boolean success;
        private final String path;
        private final List<FileEntry> files;
        private final String message;

        private DirectoryResult(boolean success, String path, List<FileEntry> files, String message) {
            this.success = success;
            this.path = path;
            this.files = files != null ? files : List.of();
            this.message = message;
        }

        public static DirectoryResult success(String path, List<FileEntry> files) {
            return new DirectoryResult(true, path, files, "OK");
        }

        public static DirectoryResult failure(String message) {
            return new DirectoryResult(false, null, List.of(), message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPath() {
            return path;
        }

        public List<FileEntry> getFiles() {
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
        private final List<String> errors;
        private final String message;

        private BackupFilesResult(boolean success, String backupPath, int fileCount,
                                  List<String> errors, String message) {
            this.success = success;
            this.backupPath = backupPath;
            this.fileCount = fileCount;
            this.errors = errors;
            this.message = message;
        }

        public static BackupFilesResult success(String backupPath, int count,
                                                List<String> errors, String message) {
            return new BackupFilesResult(true, backupPath, count, errors, message);
        }

        public static BackupFilesResult failure(String message) {
            return new BackupFilesResult(false, null, 0, List.of(), message);
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
