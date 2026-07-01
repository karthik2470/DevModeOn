package com.cfdeploytool.model;

import com.cfdeploytool.service.EnvironmentService;

/**
 * Resolves deployment paths from the active environment profile.
 */
public final class DeployPathResolver {

    private static EnvironmentService environmentService;

    private DeployPathResolver() {
    }

    public static void init(EnvironmentService service) {
        environmentService = service;
    }

    public static String resolve(DeploymentCategory category, DeploymentTier tier) {
        if (environmentService != null) {
            return environmentService.resolvePath(category, tier);
        }
        return fallback(category, tier);
    }

    private static String fallback(DeploymentCategory category, DeploymentTier tier) {
        EnvironmentConfig def = EnvironmentConfig.createDefault();
        return def.getDeployPath(category, tier);
    }
}
