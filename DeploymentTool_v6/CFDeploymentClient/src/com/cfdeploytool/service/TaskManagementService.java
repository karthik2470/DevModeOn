package com.cfdeploytool.service;

import com.cfdeploytool.model.Server;
import com.cfdeploytool.persistence.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Service client to query and control tasks (services, programs, processes) on remote servers.
 */
public class TaskManagementService {

    private final HttpClient httpClient;

    public TaskManagementService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static class TaskStatusResult {
        private final boolean success;
        private final String message;
        private final Map<String, String> servicesStatus = new HashMap<>();
        private final Map<String, String> programsStatus = new HashMap<>();
        private final Map<String, ProcessStatus> processesStatus = new HashMap<>();
        private final Map<String, String> batchesStatus = new HashMap<>();

        public TaskStatusResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getServicesStatus() {
            return servicesStatus;
        }

        public Map<String, String> getProgramsStatus() {
            return programsStatus;
        }

        public Map<String, ProcessStatus> getProcessesStatus() {
            return processesStatus;
        }

        public Map<String, String> getBatchesStatus() {
            return batchesStatus;
        }
    }

    public static class ProcessStatus {
        private final boolean running;
        private final List<Integer> pids;

        public ProcessStatus(boolean running, List<Integer> pids) {
            this.running = running;
            this.pids = pids;
        }

        public boolean isRunning() {
            return running;
        }

        public List<Integer> getPids() {
            return pids;
        }
    }

    public static class TaskControlResult {
        private final boolean success;
        private final String message;

        public TaskControlResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public TaskStatusResult queryStatus(Server server, String services, String programs, String runningProcesses, String batches) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"services\":").append(JsonUtil.quote(services != null ? services : "")).append(",");
            body.append("\"programs\":").append(JsonUtil.quote(programs != null ? programs : "")).append(",");
            body.append("\"runningProcesses\":").append(JsonUtil.quote(runningProcesses != null ? runningProcesses : "")).append(",");
            body.append("\"batches\":").append(JsonUtil.quote(batches != null ? batches : ""));
            body.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/task/status"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new TaskStatusResult(false, "HTTP " + response.statusCode() + ": " + response.body());
            }

            String respBody = response.body();
            Map<String, String> top = JsonUtil.parseJsonObject(respBody);
            boolean success = "true".equalsIgnoreCase(top.get("success"));
            if (!success) {
                return new TaskStatusResult(false, top.getOrDefault("message", "Agent returned failure"));
            }

            TaskStatusResult result = new TaskStatusResult(true, "OK");
            
            // Parse services
            String servicesJson = top.get("services");
            if (servicesJson != null && !servicesJson.isBlank() && !servicesJson.equals("{}")) {
                Map<String, String> sMap = JsonUtil.parseJsonObject(servicesJson);
                for (var entry : sMap.entrySet()) {
                    result.servicesStatus.put(entry.getKey(), unescapeJsonString(entry.getValue()));
                }
            }

            // Parse programs
            String programsJson = top.get("programs");
            if (programsJson != null && !programsJson.isBlank() && !programsJson.equals("{}")) {
                Map<String, String> pMap = JsonUtil.parseJsonObject(programsJson);
                for (var entry : pMap.entrySet()) {
                    result.programsStatus.put(entry.getKey(), unescapeJsonString(entry.getValue()));
                }
            }

            // Parse running processes
            String procJson = top.get("runningProcesses");
            if (procJson != null && !procJson.isBlank() && !procJson.equals("{}")) {
                Map<String, String> procMap = JsonUtil.parseJsonObject(procJson);
                for (var entry : procMap.entrySet()) {
                    String val = entry.getValue();
                    Map<String, String> inner = JsonUtil.parseJsonObject(val);
                    boolean running = "true".equalsIgnoreCase(inner.get("running"));
                    String pidsRaw = inner.get("pids");
                    List<Integer> pidsList = new ArrayList<>();
                    if (pidsRaw != null && pidsRaw.startsWith("[") && pidsRaw.endsWith("]")) {
                        String innerPids = pidsRaw.substring(1, pidsRaw.length() - 1).trim();
                        if (!innerPids.isBlank()) {
                            for (String token : innerPids.split(",")) {
                                try {
                                    pidsList.add(Integer.parseInt(token.trim()));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    result.processesStatus.put(entry.getKey(), new ProcessStatus(running, pidsList));
                }
            }

            // Parse batch statuses
            // Keys are stored normalized (lowercase, single backslashes) to match client lookups
            String batchesJson = top.get("batches");
            if (batchesJson != null && !batchesJson.isBlank() && !batchesJson.equals("{}")) {
                Map<String, String> bMap = JsonUtil.parseJsonObject(batchesJson);
                for (var entry : bMap.entrySet()) {
                    // The JSON key has escaped backslashes (e.g. "c:\\users\\..."),
                    // unescape then normalize to lowercase so client lookups always match.
                    String rawKey = entry.getKey().replace("\\\\", "\\").replace("/", "\\").toLowerCase();
                    result.batchesStatus.put(rawKey, unescapeJsonString(entry.getValue()));
                }
            }

            return result;
        } catch (java.net.ConnectException e) {
            return new TaskStatusResult(false, "Connection refused - agent may not be running");
        } catch (Exception e) {
            return new TaskStatusResult(false, "Query failed: " + e.getMessage());
        }
    }

    public TaskControlResult controlTask(Server server, String action, String type, String target) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"action\":").append(JsonUtil.quote(action)).append(",");
            body.append("\"type\":").append(JsonUtil.quote(type)).append(",");
            body.append("\"target\":").append(JsonUtil.quote(target));
            body.append("}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/task/control"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String respBody = response.body();
            Map<String, String> top = JsonUtil.parseJsonObject(respBody);
            boolean success = response.statusCode() == 200 && "true".equalsIgnoreCase(top.get("success"));
            String message = top.getOrDefault("message", respBody);
            return new TaskControlResult(success, unescapeJsonString(message));
        } catch (java.net.ConnectException e) {
            return new TaskControlResult(false, "Connection refused - agent may not be running");
        } catch (Exception e) {
            return new TaskControlResult(false, "Control action failed: " + e.getMessage());
        }
    }

    public static class BatchOutputResult {
        private final boolean success;
        private final boolean running;
        private final String output;

        public BatchOutputResult(boolean success, boolean running, String output) {
            this.success = success;
            this.running = running;
            this.output = output;
        }

        public boolean isSuccess() { return success; }
        public boolean isRunning() { return running; }
        public String getOutput() { return output; }
    }

    public BatchOutputResult queryBatchOutput(Server server, String batPath) {
        try {
            String encodedPath = java.net.URLEncoder.encode(batPath, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(server.getBaseUrl() + "/api/task/batch/output?target=" + encodedPath))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new BatchOutputResult(false, false, "HTTP " + response.statusCode());
            }

            Map<String, String> top = JsonUtil.parseJsonObject(response.body());
            boolean running = "true".equalsIgnoreCase(top.get("running"));
            String output = unescapeJsonString(top.getOrDefault("output", ""));
            return new BatchOutputResult(true, running, output);
        } catch (java.net.ConnectException e) {
            return new BatchOutputResult(false, false, "Connection refused - agent may not be running");
        } catch (Exception e) {
            return new BatchOutputResult(false, false, "Failed to query batch output: " + e.getMessage());
        }
    }

    private static String unescapeJsonString(String value) {
        if (value == null) return null;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
