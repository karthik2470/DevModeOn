package com.cfdeploytool.model;

import java.util.List;

/**
 * Which deployment tiers to target for a deployment job.
 */
public enum DeploymentScope {
    BOTH("2T + 4T"),
    T2_ONLY("2T only"),
    T4_ONLY("4T only");

    private final String displayName;

    DeploymentScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<DeploymentTier> getTiers() {
        return switch (this) {
            case BOTH -> List.of(DeploymentTier.T2, DeploymentTier.T4);
            case T2_ONLY -> List.of(DeploymentTier.T2);
            case T4_ONLY -> List.of(DeploymentTier.T4);
        };
    }
}
