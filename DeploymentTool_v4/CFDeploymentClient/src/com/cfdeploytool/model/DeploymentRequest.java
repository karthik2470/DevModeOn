package com.cfdeploytool.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a deployment request — files, servers, category, and tier scope.
 */
public class DeploymentRequest {

    private String id;
    private List<DeploymentFile> files;
    private List<Server> targetServers;
    private DeploymentCategory category;
    private DeploymentScope scope;
    private String ticketNumber;
    private LocalDateTime createdAt;

    public DeploymentRequest() {
        this.id = UUID.randomUUID().toString();
        this.files = new ArrayList<>();
        this.targetServers = new ArrayList<>();
        this.category = DeploymentCategory.CUSTOM_FUNCTION;
        this.scope = DeploymentScope.BOTH;
        this.createdAt = LocalDateTime.now();
    }

    public DeploymentRequest(List<DeploymentFile> files, List<Server> targetServers,
                             DeploymentCategory category, DeploymentScope scope) {
        this();
        this.files = new ArrayList<>(files);
        this.targetServers = new ArrayList<>(targetServers);
        this.category = category != null ? category : DeploymentCategory.CUSTOM_FUNCTION;
        this.scope = scope != null ? scope : DeploymentScope.BOTH;
    }

    public String getId() {
        return id;
    }

    public List<DeploymentFile> getFiles() {
        return files;
    }

    public List<Server> getTargetServers() {
        return targetServers;
    }

    public DeploymentCategory getCategory() {
        return category;
    }

    public DeploymentScope getScope() {
        return scope;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public List<DeploymentTier> getTargetTiers() {
        return scope.getTiers();
    }

    /**
     * Tiers per server type: Other → 2T only; Corporate → both 2T and 4T paths.
     */
    public List<DeploymentTier> getTiersForServer(Server server) {
        if (server != null && server.isCorporate()) {
            return List.of(DeploymentTier.T2, DeploymentTier.T4);
        }
        return List.of(DeploymentTier.T2);
    }

    /**
     * Total operations = sum over servers of files × tiers for that server.
     */
    public int getTotalOperations() {
        int total = 0;
        for (Server server : targetServers) {
            total += files.size() * getTiersForServer(server).size();
        }
        return total;
    }

    @Override
    public String toString() {
        return "DeploymentRequest{files=" + files.size()
                + ", servers=" + targetServers.size()
                + ", scope=" + scope + "}";
    }
}
