package com.cfdeploytool.service;

import com.cfdeploytool.model.DeploymentCategory;
import com.cfdeploytool.model.DeploymentTier;
import com.cfdeploytool.model.EnvironmentConfig;
import com.cfdeploytool.persistence.FileStore;
import com.cfdeploytool.persistence.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages named environment profiles (agent.properties-style) and the active environment.
 */
public class EnvironmentService {

    public interface ChangeListener {
        void onEnvironmentsChanged();
    }

    private final FileStore fileStore;
    private final List<EnvironmentConfig> environments = new ArrayList<>();
    private final List<ChangeListener> listeners = new CopyOnWriteArrayList<>();
    private String activeEnvironmentId = "default";

    public EnvironmentService(FileStore fileStore) {
        this.fileStore = fileStore;
        load();
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        for (ChangeListener l : listeners) {
            l.onEnvironmentsChanged();
        }
    }

    public List<EnvironmentConfig> getEnvironments() {
        return List.copyOf(environments);
    }

    public EnvironmentConfig getActiveEnvironment() {
        return findById(activeEnvironmentId)
                .orElse(environments.isEmpty() ? EnvironmentConfig.createDefault() : environments.get(0));
    }

    public String getActiveEnvironmentId() {
        return activeEnvironmentId;
    }

    public String getActiveEnvironmentName() {
        return getActiveEnvironment().getName();
    }

    public void setActiveEnvironmentId(String id) {
        if (findById(id).isEmpty()) {
            return;
        }
        activeEnvironmentId = id;
        save();
        notifyChanged();
    }

    public Optional<EnvironmentConfig> findById(String id) {
        return environments.stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    public void addEnvironment(EnvironmentConfig config) {
        environments.add(config);
        save();
        notifyChanged();
    }

    public void updateEnvironment(EnvironmentConfig config) {
        for (int i = 0; i < environments.size(); i++) {
            if (environments.get(i).getId().equals(config.getId())) {
                environments.set(i, config);
                save();
                notifyChanged();
                return;
            }
        }
        environments.add(config);
        save();
        notifyChanged();
    }

    public boolean deleteEnvironment(String id) {
        if (environments.size() <= 1) {
            return false;
        }
        boolean removed = environments.removeIf(e -> e.getId().equals(id));
        if (!removed) {
            return false;
        }
        if (activeEnvironmentId.equals(id)) {
            activeEnvironmentId = environments.get(0).getId();
        }
        save();
        notifyChanged();
        return true;
    }

    public String resolvePath(DeploymentCategory category, DeploymentTier tier) {
        return getActiveEnvironment().getDeployPath(category, tier);
    }

    private void load() {
        try {
            String json = fileStore.loadEnvironmentSettings();
            if (json == null || json.isBlank()) {
                seedDefault();
                return;
            }
            JsonUtil.EnvironmentSettingsData data = JsonUtil.jsonToEnvironmentSettings(json);
            environments.clear();
            environments.addAll(data.environments());
            activeEnvironmentId = data.activeEnvironmentId();
            if (environments.isEmpty()) {
                seedDefault();
            } else if (findById(activeEnvironmentId).isEmpty()) {
                activeEnvironmentId = environments.get(0).getId();
            }
        } catch (Exception e) {
            System.err.println("Failed to load environments: " + e.getMessage());
            seedDefault();
        }
    }

    private void seedDefault() {
        environments.clear();
        EnvironmentConfig def = EnvironmentConfig.createDefault();
        environments.add(def);
        activeEnvironmentId = def.getId();
        save();
    }

    public void save() {
        try {
            fileStore.saveEnvironmentSettings(JsonUtil.environmentSettingsToJson(
                    activeEnvironmentId, environments));
        } catch (IOException e) {
            System.err.println("Failed to save environments: " + e.getMessage());
        }
    }
}
