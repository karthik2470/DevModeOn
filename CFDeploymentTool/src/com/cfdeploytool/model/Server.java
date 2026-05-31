package com.cfdeploytool.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered target server where deployments can be sent.
 */
public class Server {

    public enum ServerStatus {
        ONLINE, OFFLINE, UNKNOWN
    }

    /**
     * Corporate = 2T + 4T deploy paths (folders not auto-created).
     * Other = 2T deploy path only.
     */
    public enum ServerType {
        OTHER("Other server"),
        CORPORATE("Corporate server");

        private final String displayName;

        ServerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ServerType fromString(String value) {
            if (value == null || value.isBlank()) {
                return OTHER;
            }
            String normalized = value.trim().toUpperCase().replace(' ', '_');
            if ("POOL".equals(normalized)) {
                return OTHER;
            }
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException e) {
                if (value.toLowerCase().contains("corporate")) {
                    return CORPORATE;
                }
                return OTHER;
            }
        }
    }

    private String id;
    private String name;
    private String host;
    private int port;
    private String description;
    private ServerStatus status;
    private ServerType serverType;
    private LocalDateTime lastChecked;

    public Server() {
        this.id = UUID.randomUUID().toString();
        this.port = 8585;
        this.status = ServerStatus.UNKNOWN;
        this.description = "";
        this.serverType = ServerType.OTHER;
    }

    public Server(String name, String host, int port, String description) {
        this();
        this.name = name;
        this.host = host;
        this.port = port;
        this.description = description != null ? description : "";
    }

    // --- Getters and Setters ---

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }

    public ServerType getServerType() {
        return serverType != null ? serverType : ServerType.OTHER;
    }

    public void setServerType(ServerType serverType) {
        this.serverType = serverType != null ? serverType : ServerType.OTHER;
    }

    public boolean isCorporate() {
        return getServerType() == ServerType.CORPORATE;
    }

    public boolean isOther() {
        return !isCorporate();
    }

    /** Corporate servers never auto-create missing deploy folders on the agent. */
    public boolean isAllowCreateDirectories() {
        return !isCorporate();
    }

    public String getBaseUrl() {
        return "http://" + host + ":" + port;
    }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return id.equals(server.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
