package com.cfdeployagent.handler;

import com.cfdeployagent.service.TaskService;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles GET /api/task/batch/output?target=<bat_path>
 * Returns JSON: {"running": true|false, "output": "...captured lines..."}
 */
public class BatchOutputHandler implements HttpHandler {

    private final TaskService taskService;

    public BatchOutputHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HealthHandler.sendResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            return;
        }

        try {
            // Parse target from query string
            String query = exchange.getRequestURI().getRawQuery();
            String target = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "target".equalsIgnoreCase(kv[0])) {
                        target = java.net.URLDecoder.decode(kv[1], "UTF-8");
                        break;
                    }
                }
            }

            if (target == null || target.isBlank()) {
                HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse("Missing 'target' parameter"));
                return;
            }

            boolean running = "RUNNING".equals(taskService.getBatchStatus(target));
            String output = taskService.getBatchOutput(target);

            // Escape the output for JSON
            String escapedOutput = output
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r\n", "\\n")
                    .replace("\n", "\\n")
                    .replace("\r", "\\n")
                    .replace("\t", "\\t");

            String json = "{\"success\":true,\"running\":" + running
                    + ",\"output\":\"" + escapedOutput + "\"}";

            HealthHandler.sendResponse(exchange, 200, json);
        } catch (Exception e) {
            HealthHandler.sendResponse(exchange, 500, JsonUtil.errorResponse("Error reading batch output: " + e.getMessage()));
        }
    }
}
