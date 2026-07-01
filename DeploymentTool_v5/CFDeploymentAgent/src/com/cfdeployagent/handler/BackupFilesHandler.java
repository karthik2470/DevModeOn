package com.cfdeployagent.handler;

import com.cfdeployagent.service.DirectoryService;
import com.cfdeployagent.service.DirectoryService.BackupFilesResult;
import com.cfdeployagent.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BackupFilesHandler implements HttpHandler {

    private final DirectoryService directoryService;

    public BackupFilesHandler(DirectoryService directoryService) {
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
            String sourcePath = JsonUtil.extractField(body, "sourcePath");
            String backupDir = JsonUtil.extractField(body, "backupDir");
            List<String> fileNames = parseFileNamesArray(body);
            BackupFilesResult result = directoryService.backupFiles(sourcePath, backupDir, fileNames);
            int code = result.isSuccess() ? 200 : 500;
            String json = "{\"success\":" + result.isSuccess()
                    + ",\"message\":" + JsonUtil.quote(result.getMessage())
                    + ",\"backupPath\":" + JsonUtil.quote(result.getBackupPath())
                    + ",\"fileCount\":" + result.getFileCount() + "}";
            HealthHandler.sendResponse(exchange, code, json);
        } catch (Exception e) {
            HealthHandler.sendResponse(exchange, 500, JsonUtil.errorResponse(e.getMessage()));
        }
    }

    private static List<String> parseFileNamesArray(String json) {
        List<String> names = new ArrayList<>();
        int key = json.indexOf("\"fileNames\"");
        if (key < 0) {
            return names;
        }
        int start = json.indexOf('[', key);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) {
            return names;
        }
        String arr = json.substring(start + 1, end);
        int i = 0;
        while (i < arr.length()) {
            int q1 = arr.indexOf('"', i);
            if (q1 < 0) {
                break;
            }
            int q2 = arr.indexOf('"', q1 + 1);
            if (q2 < 0) {
                break;
            }
            names.add(arr.substring(q1 + 1, q2));
            i = q2 + 1;
        }
        return names;
    }
}
