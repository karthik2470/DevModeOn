package com.cfdeployagent.handler;

import com.cfdeployagent.service.TaskService;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Handles POST /api/task/status requests.
 * Accepts list of services, programs, and processes to check.
 */
public class TaskStatusHandler implements HttpHandler {

    private final TaskService taskService;

    public TaskStatusHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HealthHandler.sendResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            return;
        }

        try {
            String body = JsonUtil.readBody(exchange.getRequestBody());
            String servicesRaw = JsonUtil.extractField(body, "services");
            String programsRaw = JsonUtil.extractField(body, "programs");
            String runningProcessesRaw = JsonUtil.extractField(body, "runningProcesses");

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"success\":true,");

            // Query Services
            json.append("\"services\":{");
            if (servicesRaw != null && !servicesRaw.isBlank()) {
                String[] services = servicesRaw.split(",");
                for (int i = 0; i < services.length; i++) {
                    String service = services[i].trim();
                    if (i > 0) json.append(",");
                    json.append(JsonUtil.quote(service)).append(":").append(JsonUtil.quote(taskService.getServiceStatus(service)));
                }
            }
            json.append("},");

            // Query Programs
            json.append("\"programs\":{");
            if (programsRaw != null && !programsRaw.isBlank()) {
                String[] programs = programsRaw.split(",");
                for (int i = 0; i < programs.length; i++) {
                    String p = programs[i].trim();
                    String[] parts = p.split("\\|");
                    String progName = parts[0].trim();
                    if (i > 0) json.append(",");
                    
                    List<Integer> pids = taskService.getProcessPids(progName);
                    String status = pids.isEmpty() ? "STOPPED" : "RUNNING";
                    json.append(JsonUtil.quote(progName)).append(":").append(JsonUtil.quote(status));
                }
            }
            json.append("},");

            // Query Running Processes
            json.append("\"runningProcesses\":{");
            if (runningProcessesRaw != null && !runningProcessesRaw.isBlank()) {
                String[] processes = runningProcessesRaw.split(",");
                for (int i = 0; i < processes.length; i++) {
                    String process = processes[i].trim();
                    if (i > 0) json.append(",");
                    List<Integer> pids = taskService.getProcessPids(process);
                    json.append(JsonUtil.quote(process)).append(":{");
                    json.append("\"running\":").append(!pids.isEmpty()).append(",");
                    json.append("\"pids\":[");
                    for (int j = 0; j < pids.size(); j++) {
                        if (j > 0) json.append(",");
                        json.append(pids.get(j));
                    }
                    json.append("]}");
                }
            }
            json.append("}");
            json.append("}");

            HealthHandler.sendResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            HealthHandler.sendResponse(exchange, 500, JsonUtil.errorResponse("Failed to query status: " + e.getMessage()));
        }
    }
}
