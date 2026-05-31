package com.cfdeploytool.model;

/**
 * Deployment target category — determines server destination paths.
 */
public enum DeploymentCategory {
    CUSTOM_FUNCTION("CustomFunction", "versiondll.txt"),
    PLUGIN("Plugin", "versionjar.txt");

    private final String displayName;
    private final String versionFileName;

    DeploymentCategory(String displayName, String versionFileName) {
        this.displayName = displayName;
        this.versionFileName = versionFileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersionFileName() {
        return versionFileName;
    }

    public String getPath(DeploymentTier tier) {
        return DeployPathResolver.resolve(this, tier);
    }

    /** Version file lives on the 4T deploy path only. */
    public String getVersionPath() {
        return getPath(DeploymentTier.T4);
    }
}
