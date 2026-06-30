package com.cfdeploytool.service;

import com.cfdeploytool.model.Server;
import com.cfdeploytool.model.Server.ServerStatus;
import com.cfdeploytool.model.Server.ServerType;
import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.persistence.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the registry of target deployment servers.
 * Handles CRUD operations and persistence to JSON.
 */
public class ServerManager {

    private final List<Server> servers;
    private final FileStore fileStore;
    private final HttpClient httpClient;

    public ServerManager(FileStore fileStore) {
        this.fileStore = fileStore;
        this.servers = new CopyOnWriteArrayList<>();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        loadServers();
    }

    /**
     * Registers a new server.
     */
    public void addServer(Server server) {
        servers.add(server);
        saveServers();
    }

    /**
     * Updates an existing server's details.
     */
    public void updateServer(Server updated) {
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getId().equals(updated.getId())) {
                servers.set(i, updated);
                saveServers();
                return;
            }
        }
    }

    /**
     * Removes a server by ID.
     */
    public void removeServer(String id) {
        servers.removeIf(s -> s.getId().equals(id));
        saveServers();
    }

    /**
     * Returns an unmodifiable view of all registered servers.
     */
    public List<Server> getServers() {
        return new ArrayList<>(servers);
    }

    /**
     * Finds a server by ID.
     */
    public Optional<Server> getServerById(String id) {
        return servers.stream().filter(s -> s.getId().equals(id)).findFirst();
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
     * Checks health of all registered servers.
     */
    public void checkAllServersHealth() {
        for (Server server : servers) {
            checkServerHealth(server);
        }
    }

    /**
     * Returns the count of registered servers.
     */
    public int getServerCount() {
        return servers.size();
    }

    public boolean hasCorporateServer() {
        return servers.stream().anyMatch(Server::isCorporate);
    }

    public List<Server> getCorporateServers() {
        return servers.stream().filter(Server::isCorporate).toList();
    }

    public Optional<Server> getCorporateServer() {
        return servers.stream().filter(Server::isCorporate).findFirst();
    }

    /**
     * Ensures at most one corporate server; returns false if a second would be added.
     */
    public boolean canRegisterCorporateServer(Server candidate) {
        if (candidate == null || !candidate.isCorporate()) {
            return true;
        }
        return servers.stream()
                .filter(Server::isCorporate)
                .allMatch(s -> s.getId().equals(candidate.getId()));
    }

    // ==================== PERSISTENCE ====================

    private void loadServers() {
        try {
            String json = fileStore.loadServers();
            if (json != null && !json.isBlank()) {
                List<Server> loaded = JsonUtil.jsonToServers(json);
                servers.clear();
                servers.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load servers: " + e.getMessage());
        }
    }

    private void saveServers() {
        try {
            String json = JsonUtil.serversToJson(new ArrayList<>(servers));
            fileStore.saveServers(json);
        } catch (IOException e) {
            System.err.println("Failed to save servers: " + e.getMessage());
        }
    }
}
