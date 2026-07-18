package com.cfdeploytool.service;

import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.model.Server;
import com.cfdeploytool.model.Server.ServerStatus;
import com.cfdeploytool.model.Server.ServerType;
import com.cfdeploytool.persistence.FileStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the registry of target deployment servers.
 * Delegates storage and CRUD operations to the active environment profile.
 */
public class ServerManager {

    private final FileStore fileStore;
    private final EnvironmentService environmentService;
    private final HttpClient httpClient;

    public ServerManager(FileStore fileStore, EnvironmentService environmentService) {
        this.fileStore = fileStore;
        this.environmentService = environmentService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Registers a new server in the active environment.
     */
    public void addServer(Server server) {
        environmentService.getActiveEnvironment().getServers().add(server);
        environmentService.save();
    }

    /**
     * Updates an existing server's details in the active environment.
     */
    public void updateServer(Server updated) {
        List<Server> list = environmentService.getActiveEnvironment().getServers();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(updated.getId())) {
                list.set(i, updated);
                environmentService.save();
                return;
            }
        }
    }

    /**
     * Removes a server by ID from the active environment.
     */
    public void removeServer(String id) {
        environmentService.getActiveEnvironment().getServers().removeIf(s -> s.getId().equals(id));
        environmentService.save();
    }

    /**
     * Returns all registered servers for the active environment.
     */
    public List<Server> getServers() {
        return new ArrayList<>(environmentService.getActiveEnvironment().getServers());
    }

    /**
     * Finds a server by ID in the active environment.
     */
    public Optional<Server> getServerById(String id) {
        return environmentService.getActiveEnvironment().getServers().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    /**
     * Checks the health of a server by pinging its /api/health endpoint.
     * Updates the server's status and lastChecked timestamp.
     */
    public ServerStatus checkServerHealth(Server server) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            server.setLastChecked(LocalDateTime.now());
            if (response.statusCode() == 200) {
                server.setStatus(ServerStatus.ONLINE);
            } else {
                server.setStatus(ServerStatus.OFFLINE);
            }
        } catch (Exception e) {
            server.setLastChecked(LocalDateTime.now());
            server.setStatus(ServerStatus.OFFLINE);
        }

        updateServer(server);
        return server.getStatus();
    }

    /**
     * Checks health of all registered servers in the active environment.
     */
    public void checkAllServersHealth() {
        for (Server server : environmentService.getActiveEnvironment().getServers()) {
            checkServerHealth(server);
        }
    }

    /**
     * Returns the count of registered servers in the active environment.
     */
    public int getServerCount() {
        return environmentService.getActiveEnvironment().getServers().size();
    }

    public boolean hasCorporateServer() {
        return environmentService.getActiveEnvironment().getServers().stream()
                .anyMatch(Server::isCorporate);
    }

    public List<Server> getCorporateServers() {
        return environmentService.getActiveEnvironment().getServers().stream()
                .filter(Server::isCorporate)
                .toList();
    }

    public Optional<Server> getCorporateServer() {
        return environmentService.getActiveEnvironment().getServers().stream()
                .filter(Server::isCorporate)
                .findFirst();
    }

    /**
     * Ensures at most one corporate server; returns false if a second would be added.
     */
    public boolean canRegisterCorporateServer(Server candidate) {
        if (candidate == null || !candidate.isCorporate()) {
            return true;
        }
        return environmentService.getActiveEnvironment().getServers().stream()
                .filter(Server::isCorporate)
                .allMatch(s -> s.getId().equals(candidate.getId()));
    }
}

