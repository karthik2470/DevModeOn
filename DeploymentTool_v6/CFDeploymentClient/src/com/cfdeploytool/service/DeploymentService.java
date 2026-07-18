package com.cfdeploytool.service;

import com.cfdeploytool.model.*;
import com.cfdeploytool.model.DeploymentResult.ResultStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates validation, deployment, and version updates across servers and tiers.
 */
public class DeploymentService {

    public interface ProgressCallback {
        void onValidationResult(HttpDeploymentClient.PathValidationResult result);

        void onProgress(DeploymentResult result, int completed, int total);

        void onComplete(List<DeploymentResult> results);

        void onServerStart(Server server);

        void onFileStart(Server server, DeploymentFile file, DeploymentTier tier);

        void onLog(Server server, String message, boolean success);
    }

    private final HttpDeploymentClient httpClient;
    private final HistoryService historyService;
    private final EnvironmentService environmentService;
    private final ExecutorService executor;
    private volatile boolean cancelled = false;

    public DeploymentService(HistoryService historyService, EnvironmentService environmentService) {
        this.httpClient = new HttpDeploymentClient();
        this.historyService = historyService;
        this.environmentService = environmentService;
        this.executor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors(), 8));
    }

    public DeploymentHistory deploy(DeploymentRequest request, ProgressCallback callback) {
        cancelled = false;
        DeploymentCategory category = request.getCategory();
        int totalOps = request.getTotalOperations();
        List<DeploymentResult> allResults = new CopyOnWriteArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);

        List<HttpDeploymentClient.PathValidationResult> validationResults =
                validateServers(request, category, callback);
        if (validationResults.stream().anyMatch(r -> !r.isValid())) {
            if (callback != null) {
                callback.onComplete(new ArrayList<>(allResults));
            }
            DeploymentHistory failedHistory = new DeploymentHistory(request, allResults);
            historyService.saveDeployment(failedHistory);
            return failedHistory;
        }

        List<Future<?>> futures = new ArrayList<>();
        for (Server server : request.getTargetServers()) {
            Future<?> future = executor.submit(() -> {
                if (callback != null) {
                    callback.onServerStart(server);
                }

                List<DeploymentTier> serverTiers = request.getTiersForServer(server);
                for (DeploymentTier tier : serverTiers) {
                    for (DeploymentFile file : request.getFiles()) {
                        if (cancelled) {
                            DeploymentResult skipped = new DeploymentResult(
                                    server, file, ResultStatus.SKIPPED,
                                    "[" + tier.getDisplayName() + "] Deployment cancelled");
                            allResults.add(skipped);
                            int done = completed.incrementAndGet();
                            if (callback != null) {
                                callback.onProgress(skipped, done, totalOps);
                            }
                            continue;
                        }

                        if (callback != null) {
                            callback.onFileStart(server, file, tier);
                        }

                        boolean enableDeployBackup = request.isAutoBackup() && shouldPerformDeployBackup(server, tier);
                        String existingBackupDir = enableDeployBackup
                                ? resolveCorporateBackupDir(server) : null;
                        String deployBackupDir = enableDeployBackup
                                ? resolveCorporateDeployBackupDir(server) : null;
                        DeploymentResult result = httpClient.sendFile(
                                server, file, category, tier, existingBackupDir, deployBackupDir);
                        allResults.add(result);
                        int done = completed.incrementAndGet();

                        if (callback != null) {
                            callback.onProgress(result, done, totalOps);
                            callback.onLog(server, tier.getDisplayName() + " - "
                                    + result.getFileName() + ": " + result.getMessage(), result.isSuccess());
                        }
                    }
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("Deployment task failed: " + e.getCause().getMessage());
            }
        }

        DeploymentHistory history = new DeploymentHistory(request, new ArrayList<>(allResults));
        historyService.saveDeployment(history);

        if (callback != null) {
            callback.onComplete(new ArrayList<>(allResults));
        }

        return history;
    }

    private List<HttpDeploymentClient.PathValidationResult> validateServers(
            DeploymentRequest request, DeploymentCategory category, ProgressCallback callback) {
        List<HttpDeploymentClient.PathValidationResult> results = new ArrayList<>();

        for (Server server : request.getTargetServers()) {
            for (DeploymentTier tier : request.getTiersForServer(server)) {
                HttpDeploymentClient.PathValidationResult result =
                        httpClient.validatePath(server, category, tier);
                results.add(result);
                if (callback != null) {
                    callback.onValidationResult(result);
                    callback.onLog(server, tier.getDisplayName() + " - Path validation: "
                            + result.getMessage(), result.isValid());
                }
            }
        }
        return results;
    }

    public List<HttpDeploymentClient.VersionReadResult> readVersions(
            List<Server> servers, DeploymentCategory category) {
        List<HttpDeploymentClient.VersionReadResult> results = new ArrayList<>();
        for (Server server : servers) {
            results.add(httpClient.readVersion(server, category));
        }
        return results;
    }

    public List<HttpDeploymentClient.VersionUpdateResult> updateVersions(
            List<Server> servers, DeploymentCategory category, String newVersion) {
        List<HttpDeploymentClient.VersionUpdateResult> results = new ArrayList<>();
        for (Server server : servers) {
            results.add(httpClient.updateVersion(server, category, newVersion));
        }
        return results;
    }

    /**
     * Corporate servers deploy to 2T and 4T; backups run once on 2T only to avoid duplicate copies.
     */
    private boolean shouldPerformDeployBackup(Server server, DeploymentTier tier) {
        return server != null && server.isCorporate() && tier == DeploymentTier.T2;
    }

    /** Existing file backup (corporate.backup.path) before overwrite — corporate only. */
    private String resolveCorporateBackupDir(Server server) {
        if (server == null || !server.isCorporate()) {
            return null;
        }
        String dir = environmentService.getActiveEnvironment().getBackupDir();
        return (dir != null && !dir.isBlank()) ? dir.trim() : null;
    }

    /** Deployed file backup (corporate.Dbackup.path) after deploy — corporate only. */
    private String resolveCorporateDeployBackupDir(Server server) {
        if (server == null || !server.isCorporate()) {
            return null;
        }
        String dir = environmentService.getActiveEnvironment().getDeployBackupDir();
        return (dir != null && !dir.isBlank()) ? dir.trim() : null;
    }

    public void cancel() {
        cancelled = true;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
