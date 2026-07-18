package com.cfdeployagent.handler;

import com.cfdeployagent.service.TaskService;
import com.cfdeployagent.service.TaskService.CommandResult;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles POST /api/task/control requests.
 * Triggers start, stop, restart, or kill operations.
 */
public class TaskControlHandler implements HttpHandler {

    private final TaskService taskService;

    public TaskControlHandler(TaskService taskService) {
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
            String action = JsonUtil.extractField(body, "action");
            String type = JsonUtil.extractField(body, "type");
            String target = JsonUtil.extractField(body, "target");

            if (action == null || type == null || target == null) {
                HealthHandler.sendResponse(exchange, 400, JsonUtil.errorResponse("Missing action, type, or target"));
                return;
            }

            CommandResult result;
            if ("service".equalsIgnoreCase(type)) {
                result = taskService.controlService(target, action);
            } else if ("program".equalsIgnoreCase(type)) {
                if ("start".equalsIgnoreCase(action) || "restart".equalsIgnoreCase(action)) {
                    if ("restart".equalsIgnoreCase(action)) {
                        java.io.File file = new java.io.File(target);
                        taskService.killProcess(file.getName(), false);
                    }
                    result = taskService.startProgram(target);
                } else if ("stop".equalsIgnoreCase(action)) {
                    java.io.File file = new java.io.File(target);
                    result = taskService.killProcess(file.getName(), false);
                } else {
                    result = new CommandResult(-1, "", "Unknown program action: " + action);
                }
            } else if ("batch".equalsIgnoreCase(type)) {
                if ("start".equalsIgnoreCase(action)) {
                    result = taskService.startBatch(target);
                } else if ("stop".equalsIgnoreCase(action)) {
                    result = taskService.stopBatch(target);
                } else if ("restart".equalsIgnoreCase(action)) {
                    taskService.stopBatch(target);
                    result = taskService.startBatch(target);
                } else {
                    result = new CommandResult(-1, "", "Unknown batch action: " + action);
                }
            } else if ("process".equalsIgnoreCase(type)) {
                if ("kill".equalsIgnoreCase(action)) {
                    boolean byPid = false;
                    try {
                        Integer.parseInt(target);
                        byPid = true;
                    } catch (NumberFormatException ignored) {}
                    result = taskService.killProcess(target, byPid);
                } else {
                    result = new CommandResult(-1, "", "Unknown process action: " + action);
                }
            } else {
                result = new CommandResult(-1, "", "Unknown type: " + type);
            }

            int status = result.isSuccess() ? 200 : 500;
            String message = result.getMessage();
            HealthHandler.sendResponse(exchange, status, 
                    "{\"success\":" + result.isSuccess() + ",\"message\":" + JsonUtil.quote(message) + "}");
        } catch (Exception e) {
            HealthHandler.sendResponse(exchange, 500, JsonUtil.errorResponse("Failed to control task: " + e.getMessage()));
        }
    }
}
