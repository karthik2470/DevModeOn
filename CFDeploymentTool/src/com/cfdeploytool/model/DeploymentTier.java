package com.cfdeploytool.model;

/**
 * Deployment target tier — 2T or 4T destination paths on each server.
 */
public enum DeploymentTier {
    T2("2T"),
    T4("4T");

    private final String displayName;

    DeploymentTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
