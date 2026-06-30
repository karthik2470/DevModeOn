package com.cfdeploytool.service;

import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.persistence.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to manage client-side authentication and users file seeding.
 */
public class AuthService {

    private final FileStore fileStore;
    private final List<Map<String, String>> users = new ArrayList<>();

    public AuthService(FileStore fileStore) {
        this.fileStore = fileStore;
        try {
            loadOrSeedUsers();
        } catch (IOException e) {
            System.err.println("Failed to load or seed users: " + e.getMessage());
        }
    }

    private void loadOrSeedUsers() throws IOException {
        String json = fileStore.loadUsers();
        if (json == null || json.trim().isEmpty()) {
            // Seed default admin user
            Map<String, String> defaultUser = new LinkedHashMap<>();
            defaultUser.put("username", "admin");
            defaultUser.put("password", "admin");
            users.add(defaultUser);
            fileStore.saveUsers(JsonUtil.usersToJson(users));
        } else {
            users.addAll(JsonUtil.jsonToUsers(json));
        }
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        for (Map<String, String> user : users) {
            if (username.equalsIgnoreCase(user.get("username")) && password.equals(user.get("password"))) {
                return true;
            }
        }
        return false;
    }
}
