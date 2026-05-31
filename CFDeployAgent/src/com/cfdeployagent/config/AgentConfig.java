package com.cfdeployagent.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads agent configuration from agent.properties.
 */
public class AgentConfig {

    private static final int DEFAULT_PORT = 8585;

    private final int port;
    private final Map<DeploymentCategory, Map<DeploymentTier, Path>> categoryPaths;
    private final boolean backupEnabled;
    private final Path backupDir;

    public AgentConfig(int port,
                       Map<DeploymentCategory, Map<DeploymentTier, Path>> categoryPaths,
                       boolean backupEnabled, Path backupDir) {
        this.port = port;
        this.categoryPaths = categoryPaths;
        this.backupEnabled = backupEnabled;
        this.backupDir = backupDir;
    }

    public static AgentConfig load() throws IOException {
        Properties props = new Properties();
        Path configPath = Path.of("agent.properties");

        if (Files.isRegularFile(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }
        }

        Path baseDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        int port = parseInt(props.getProperty("port"), DEFAULT_PORT);

        Map<DeploymentCategory, Map<DeploymentTier, Path>> paths = new EnumMap<>(DeploymentCategory.class);
        Map<DeploymentTier, Path> customFunction = new EnumMap<>(DeploymentTier.class);
        customFunction.put(DeploymentTier.T2, resolvePath(baseDir, props,
                "path.customfunction.t2", "D:\\Temp\\Siemens\\CustomFunctions\\bin"));
        customFunction.put(DeploymentTier.T4, resolvePath(baseDir, props,
                "path.customfunction.t4", "D:\\Temp\\Siemens\\customsolutions\\CF"));
        paths.put(DeploymentCategory.CUSTOM_FUNCTION, Map.copyOf(customFunction));

        Map<DeploymentTier, Path> plugin = new EnumMap<>(DeploymentTier.class);
        plugin.put(DeploymentTier.T2, resolvePath(baseDir, props,
                "path.plugin.t2", "D:\\Temp\\Siemens\\tcroot\\portal\\plugins"));
        plugin.put(DeploymentTier.T4, resolvePath(baseDir, props,
                "path.plugin.t4", "D:\\Temp\\Siemens\\customsolutions\\jar"));
        paths.put(DeploymentCategory.PLUGIN, Map.copyOf(plugin));

        boolean backupEnabled = Boolean.parseBoolean(props.getProperty("backup.enabled", "true"));
        Path backupDir = resolvePath(baseDir, props, "backup.dir", "backups");

        return new AgentConfig(port, paths, backupEnabled, backupDir);
    }

    private static Path resolvePath(Path baseDir, Properties props, String key, String defaultValue) {
        String value = props.getProperty(key, defaultValue);
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            path = baseDir.resolve(path);
        }
        return path.normalize();
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getPort() {
        return port;
    }

    public Path getDeployPath(DeploymentCategory category, DeploymentTier tier) {
        Map<DeploymentTier, Path> tierPaths = categoryPaths.get(category);
        if (tierPaths == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        Path path = tierPaths.get(tier);
        if (path == null) {
            throw new IllegalArgumentException("Unknown tier: " + tier);
        }
        return path;
    }

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public Iterable<Path> getAllDeployPaths() {
        java.util.List<Path> all = new java.util.ArrayList<>();
        for (Map<DeploymentTier, Path> tierPaths : categoryPaths.values()) {
            all.addAll(tierPaths.values());
        }
        return all;
    }
}
