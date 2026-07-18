package com.cfdeployagent.handler;

import com.cfdeployagent.service.DirectoryService;
import com.cfdeployagent.service.DirectoryService.DirectoryResult;
import com.cfdeployagent.service.DirectoryService.FileEntry;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ListDirectoryHandler implements HttpHandler {

    private final DirectoryService directoryService;

    public ListDirectoryHandler(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HealthHandler.sendResponse(exchange, 405, JsonUtil.errorResponse("Method not allowed"));
            return;
        }
        try {
            String body = JsonUtil.readBody(exchange.getRequestBody());
            String path = JsonUtil.extractField(body, "path");
            DirectoryResult result = directoryService.listFiles(path);
            int code = result.isSuccess() ? 200 : 400;
            HealthHandler.sendResponse(exchange, code, toJson(result));
        } catch (Exception e) {
            HealthHandler.sendResponse(exchange, 500, JsonUtil.errorResponse(e.getMessage()));
        }
    }

    private static String toJson(DirectoryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":").append(result.isSuccess());
        sb.append(",\"path\":").append(JsonUtil.quote(result.getPath()));
        sb.append(",\"message\":").append(JsonUtil.quote(result.getMessage()));
        sb.append(",\"files\":[");
        for (int i = 0; i < result.getFiles().size(); i++) {
            FileEntry f = result.getFiles().get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":").append(JsonUtil.quote(f.name()));
            sb.append(",\"size\":").append(f.sizeBytes());
            sb.append(",\"path\":").append(JsonUtil.quote(f.fullPath()));
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }
}
