package com.cfdeployagent.service;

import com.cfdeployagent.config.AgentConfig;
import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.config.DeploymentTier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and updates version files on the 4T deploy path (versiondll.txt / versionjar.txt).
 */
public class VersionService {

    private final AgentConfig config;

    public VersionService(AgentConfig config) {
        this.config = config;
    }

    public VersionResult readVersion(DeploymentCategory category) {
        Path versionFile = getVersionFilePath(category);
        try {
            if (!Files.exists(versionFile)) {
                return VersionResult.success("0", "0", versionFile.toString(),
                        "Version file not found — using 0");
            }
            String current = Files.readString(versionFile).replace("\uFEFF", "").trim();
            if (current.isEmpty()) {
                current = "0";
            }
            return VersionResult.success(current, current, versionFile.toString(), "Current version read");
        } catch (IOException e) {
            return VersionResult.failure(versionFile.toString(),
                    "Failed to read version: " + e.getMessage());
        }
    }

    public VersionResult updateVersion(DeploymentCategory category, String newVersion) {
        return updateVersion(category, newVersion, true);
    }

    public VersionResult updateVersion(DeploymentCategory category, String newVersion,
                                       boolean allowCreateDirectories) {
        if (newVersion == null || newVersion.isBlank()) {
            return VersionResult.failure("version is required");
        }
        String trimmed = newVersion.trim();
        Path versionFile = getVersionFilePath(category);

        try {
            Path deployDir = config.getDeployPath(category, DeploymentTier.T4);
            if (!Files.exists(deployDir)) {
                if (!allowCreateDirectories) {
                    return VersionResult.failure(deployDir.toString(),
                            "4T deploy path does not exist (corporate server — folder will not be created)");
                }
                Files.createDirectories(deployDir);
            }

            String previous = "0";
            if (Files.exists(versionFile)) {
                previous = Files.readString(versionFile).trim();
                if (previous.isEmpty()) {
                    previous = "0";
                }
            }

            Files.writeString(versionFile, trimmed);
            return VersionResult.success(previous, trimmed, versionFile.toString(),
                    "Version updated to " + trimmed);
        } catch (IOException e) {
            return VersionResult.failure(versionFile.toString(),
                    "Failed to update version: " + e.getMessage());
        }
    }

    public VersionResult incrementVersion(DeploymentCategory category) {
        VersionResult current = readVersion(category);
        if (!current.isSuccess()) {
            return current;
        }
        String next = increment(current.getNewVersion());
        return updateVersion(category, next);
    }

    private Path getVersionFilePath(DeploymentCategory category) {
        return config.getDeployPath(category, DeploymentTier.T4)
                .resolve(category.getVersionFileName());
    }

    static String increment(String version) {
        if (version.matches("\\d+")) {
            return String.valueOf(Long.parseLong(version) + 1);
        }
        if (version.matches("\\d+(\\.\\d+)+")) {
            String[] parts = version.split("\\.");
            int last = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(last + 1);
            return String.join(".", parts);
        }
        return version + ".1";
    }

    public static final class VersionResult {
        private final boolean success;
        private final String previousVersion;
        private final String newVersion;
        private final String path;
        private final String message;

        private VersionResult(boolean success, String previousVersion, String newVersion,
                              String path, String message) {
            this.success = success;
            this.previousVersion = previousVersion;
            this.newVersion = newVersion;
            this.path = path;
            this.message = message;
        }

        public static VersionResult success(String previous, String next, String path, String message) {
            return new VersionResult(true, previous, next, path, message);
        }

        public static VersionResult failure(String path, String message) {
            return new VersionResult(false, null, null, path, message);
        }

        public static VersionResult failure(String message) {
            return failure("", message);
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

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }
    }
}
