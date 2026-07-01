package com.cfdeployagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stores backups under {@code backupRoot/yyyyMMdd/} using the original file name.
 * If a file already exists, the existing copy is renamed with {@code _1}, {@code _2}, … suffix.
 */
public class BackupStorageService {

    private static final DateTimeFormatter DATE_FOLDER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public Path resolveDateFolder(String backupDirPath) throws IOException {
        Path backupRoot = normalizeBackupRoot(backupDirPath);
        Path dateFolder = backupRoot.resolve(LocalDateTime.now().format(DATE_FOLDER));
        Files.createDirectories(dateFolder);
        return dateFolder;
    }

    /**
     * Copies {@code sourceFile} into {@code dateFolder} under its file name, rotating any existing file.
     */
    public void copyWithRotation(Path sourceFile, Path dateFolder) throws IOException {
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("Source is not a file: " + sourceFile);
        }
        String fileName = sourceFile.getFileName().toString();
        Path dest = dateFolder.resolve(fileName).normalize();
        if (!dest.startsWith(dateFolder.normalize())) {
            throw new IOException("Invalid backup file name: " + fileName);
        }
        if (Files.exists(dest)) {
            Path rotated = nextRotatedPath(dateFolder, fileName);
            Files.move(dest, rotated, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.copy(sourceFile, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private Path nextRotatedPath(Path dateFolder, String fileName) {
        String baseName;
        String extension;
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        } else {
            baseName = fileName;
            extension = "";
        }
        int n = 1;
        Path candidate;
        do {
            candidate = dateFolder.resolve(baseName + "_" + n + extension);
            n++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private Path normalizeBackupRoot(String backupDirPath) {
        Path backupRoot = Path.of(backupDirPath.trim()).normalize();
        if (!backupRoot.isAbsolute()) {
            backupRoot = Path.of(System.getProperty("user.dir")).resolve(backupRoot).normalize();
        }
        return backupRoot;
    }
}
