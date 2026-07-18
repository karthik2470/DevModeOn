package com.cfdeployagent.service;

import com.cfdeployagent.config.AgentConfig;
import com.cfdeployagent.config.DeploymentCategory;
import com.cfdeployagent.config.DeploymentTier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates deploy target paths and write permissions on the server.
 */
public class PathValidationService {

    private final AgentConfig config;

    public PathValidationService(AgentConfig config) {
        this.config = config;
    }

    public ValidationResult validate(DeploymentCategory category, DeploymentTier tier) {
        return validate(category, tier, true, null);
    }

    public ValidationResult validate(DeploymentCategory category, DeploymentTier tier,
                                     boolean allowCreateDirectories) {
        return validate(category, tier, allowCreateDirectories, null);
    }

    public ValidationResult validate(DeploymentCategory category, DeploymentTier tier,
                                     boolean allowCreateDirectories, String targetPathRaw) {
        Path targetDir;
        if (targetPathRaw != null && !targetPathRaw.isBlank()) {
            targetDir = Path.of(targetPathRaw);
        } else {
            targetDir = config.getDeployPath(category, tier);
        }

        try {
            if (!Files.exists(targetDir)) {
                if (!allowCreateDirectories) {
                    return ValidationResult.invalid(targetDir,
                            "Target path does not exist (corporate server — folder will not be created)");
                }
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    return ValidationResult.invalid(targetDir,
                            "Target path does not exist and could not be created: " + e.getMessage());
                }
            }

            if (!Files.isDirectory(targetDir)) {
                return ValidationResult.invalid(targetDir, "Target path is not a directory");
            }

            Path probe = targetDir.resolve(".cfdeploy_probe_" + System.currentTimeMillis());
            try {
                Files.writeString(probe, "probe");
                Files.deleteIfExists(probe);
            } catch (IOException e) {
                return ValidationResult.invalid(targetDir,
                        "Target path is not writable: " + e.getMessage());
            }

            return ValidationResult.valid(targetDir, "Path validated successfully");
        } catch (Exception e) {
            return ValidationResult.invalid(targetDir, "Validation failed: " + e.getMessage());
        }
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final Path path;
        private final boolean writable;
        private final String message;

        private ValidationResult(boolean valid, Path path, boolean writable, String message) {
            this.valid = valid;
            this.path = path;
            this.writable = writable;
            this.message = message;
        }

        public static ValidationResult valid(Path path, String message) {
            return new ValidationResult(true, path, true, message);
        }

        public static ValidationResult invalid(Path path, String message) {
            return new ValidationResult(false, path, false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public Path getPath() {
            return path;
        }

        public boolean isWritable() {
            return writable;
        }

        public String getMessage() {
            return message;
        }
    }
}
