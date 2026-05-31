package com.cfdeploytool.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Client-side environment profile mirroring {@code agent.properties} keys.
 */
public class EnvironmentConfig {

    private String id;
    private String name;

    private int port = 8585;
    private String pathCustomFunctionT2 = "D:\\Temp\\Siemens\\CustomFunctions\\bin";
    private String pathCustomFunctionT4 = "D:\\Temp\\Siemens\\customsolutions\\CF";
    private String pathPluginT2 = "D:\\Temp\\Siemens\\tcroot\\portal\\plugins";
    private String pathPluginT4 = "D:\\Temp\\Siemens\\customsolutions\\jar";
    /** Manual backup root on agent (Settings: corporate.backup.path). */
    private String backupDir = "D:\\backups\\";
    /** Pre-deploy backup when deploying files (Settings: corporate.Dbackup.path). */
    private String deployBackupDir = "D:\\backups\\deploy\\";

    public EnvironmentConfig() {
        this.id = UUID.randomUUID().toString();
        this.name = "New Environment";
    }

    public EnvironmentConfig(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static EnvironmentConfig createDefault() {
        return new EnvironmentConfig("default", "Default");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPathCustomFunctionT2() {
        return pathCustomFunctionT2;
    }

    public void setPathCustomFunctionT2(String pathCustomFunctionT2) {
        this.pathCustomFunctionT2 = pathCustomFunctionT2;
    }

    public String getPathCustomFunctionT4() {
        return pathCustomFunctionT4;
    }

    public void setPathCustomFunctionT4(String pathCustomFunctionT4) {
        this.pathCustomFunctionT4 = pathCustomFunctionT4;
    }

    public String getPathPluginT2() {
        return pathPluginT2;
    }

    public void setPathPluginT2(String pathPluginT2) {
        this.pathPluginT2 = pathPluginT2;
    }

    public String getPathPluginT4() {
        return pathPluginT4;
    }

    public void setPathPluginT4(String pathPluginT4) {
        this.pathPluginT4 = pathPluginT4;
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public String getDeployBackupDir() {
        return deployBackupDir;
    }

    public void setDeployBackupDir(String deployBackupDir) {
        this.deployBackupDir = deployBackupDir;
    }

    public String getDeployPath(DeploymentCategory category, DeploymentTier tier) {
        return switch (category) {
            case CUSTOM_FUNCTION -> switch (tier) {
                case T2 -> pathCustomFunctionT2;
                case T4 -> pathCustomFunctionT4;
            };
            case PLUGIN -> switch (tier) {
                case T2 -> pathPluginT2;
                case T4 -> pathPluginT4;
            };
        };
    }

    public EnvironmentConfig copy() {
        EnvironmentConfig c = new EnvironmentConfig(UUID.randomUUID().toString(), name + " (copy)");
        c.port = port;
        c.pathCustomFunctionT2 = pathCustomFunctionT2;
        c.pathCustomFunctionT4 = pathCustomFunctionT4;
        c.pathPluginT2 = pathPluginT2;
        c.pathPluginT4 = pathPluginT4;
        c.backupDir = backupDir;
        c.deployBackupDir = deployBackupDir;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnvironmentConfig that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
