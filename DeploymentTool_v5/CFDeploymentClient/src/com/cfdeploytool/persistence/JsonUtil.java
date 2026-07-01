package com.cfdeploytool.persistence;

import com.cfdeploytool.model.*;
import com.cfdeploytool.model.BackupRecord.BackupStatus;
import com.cfdeploytool.model.DeploymentFile.FileType;
import com.cfdeploytool.model.DeploymentResult.ResultStatus;
import com.cfdeploytool.model.Server.ServerStatus;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON serializer/deserializer using only JDK classes.
 * Handles the simple flat models needed by the application.
 */
public class JsonUtil {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ==================== SERIALIZATION ====================

    /**
     * Serializes a Server to a JSON string.
     */
    public static String serverToJson(Server server) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": ").append(quote(server.getId())).append(",\n");
        sb.append("  \"name\": ").append(quote(server.getName())).append(",\n");
        sb.append("  \"host\": ").append(quote(server.getHost())).append(",\n");
        sb.append("  \"port\": ").append(server.getPort()).append(",\n");
        sb.append("  \"description\": ").append(quote(server.getDescription())).append(",\n");
        sb.append("  \"status\": ").append(quote(server.getStatus().name())).append(",\n");
        sb.append("  \"serverType\": ").append(quote(server.getServerTypesString())).append(",\n");
        sb.append("  \"lastChecked\": ").append(server.getLastChecked() != null
                ? quote(server.getLastChecked().format(DT_FORMAT)) : "null").append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a list of Servers to a JSON array string.
     */
    public static String serversToJson(List<Server> servers) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < servers.size(); i++) {
            sb.append(indent(serverToJson(servers.get(i)), "  "));
            if (i < servers.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializes a DeploymentResult to a JSON string.
     */
    public static String resultToJson(DeploymentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"serverName\": ").append(quote(result.getServerName())).append(",\n");
        sb.append("  \"serverHost\": ").append(quote(result.getServerHost())).append(",\n");
        sb.append("  \"fileName\": ").append(quote(result.getFileName())).append(",\n");
        sb.append("  \"fileType\": ").append(quote(result.getFileType())).append(",\n");
        sb.append("  \"status\": ").append(quote(result.getStatus().name())).append(",\n");
        sb.append("  \"message\": ").append(quote(result.getMessage())).append(",\n");
        sb.append("  \"timestamp\": ").append(quote(result.getTimestamp().format(DT_FORMAT))).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a DeploymentHistory to a JSON string.
     */
    public static String historyToJson(DeploymentHistory history) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": ").append(quote(history.getId())).append(",\n");
        sb.append("  \"ticketNumber\": ").append(quote(history.getTicketNumber())).append(",\n");
        sb.append("  \"deployedAt\": ").append(quote(history.getDeployedAt().format(DT_FORMAT))).append(",\n");
        sb.append("  \"totalFiles\": ").append(history.getTotalFiles()).append(",\n");
        sb.append("  \"totalServers\": ").append(history.getTotalServers()).append(",\n");
        sb.append("  \"successCount\": ").append(history.getSuccessCount()).append(",\n");
        sb.append("  \"failedCount\": ").append(history.getFailedCount()).append(",\n");
        sb.append("  \"skippedCount\": ").append(history.getSkippedCount()).append(",\n");
        sb.append("  \"fileNames\": ").append(stringListToJson(history.getFileNames())).append(",\n");
        sb.append("  \"serverNames\": ").append(stringListToJson(history.getServerNames())).append(",\n");
        sb.append("  \"results\": [\n");
        List<DeploymentResult> results = history.getResults();
        for (int i = 0; i < results.size(); i++) {
            sb.append(indent(resultToJson(results.get(i)), "    "));
            if (i < results.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a BackupRecord to a JSON string.
     */
    public static String backupRecordToJson(BackupRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": ").append(quote(record.getId())).append(",\n");
        sb.append("  \"serverName\": ").append(quote(record.getServerName())).append(",\n");
        sb.append("  \"serverHost\": ").append(quote(record.getServerHost())).append(",\n");
        sb.append("  \"backupPath\": ").append(quote(record.getBackupPath())).append(",\n");
        sb.append("  \"createdAt\": ").append(quote(record.getCreatedAt().format(DT_FORMAT))).append(",\n");
        sb.append("  \"totalSizeBytes\": ").append(record.getTotalSizeBytes()).append(",\n");
        sb.append("  \"fileCount\": ").append(record.getFileCount()).append(",\n");
        sb.append("  \"status\": ").append(quote(record.getStatus().name())).append(",\n");
        sb.append("  \"notes\": ").append(quote(record.getNotes())).append(",\n");
        sb.append("  \"fileNames\": ").append(stringListToJson(record.getFileNames())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    // ==================== ENVIRONMENT SETTINGS ====================

    public record EnvironmentSettingsData(String activeEnvironmentId, List<EnvironmentConfig> environments) {
    }

    public static String environmentConfigToJson(EnvironmentConfig env) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": ").append(quote(env.getId())).append(",\n");
        sb.append("  \"name\": ").append(quote(env.getName())).append(",\n");
        sb.append("  \"port\": ").append(env.getPort()).append(",\n");
        sb.append("  \"pathCustomFunctionT2\": ").append(quote(env.getPathCustomFunctionT2())).append(",\n");
        sb.append("  \"pathCustomFunctionT4\": ").append(quote(env.getPathCustomFunctionT4())).append(",\n");
        sb.append("  \"pathPluginT2\": ").append(quote(env.getPathPluginT2())).append(",\n");
        sb.append("  \"pathPluginT4\": ").append(quote(env.getPathPluginT4())).append(",\n");
        sb.append("  \"pathPluginDllT2\": ").append(quote(env.getPathPluginDllT2())).append(",\n");
        sb.append("  \"pathPluginDllT4\": ").append(quote(env.getPathPluginDllT4())).append(",\n");
        sb.append("  \"backupDir\": ").append(quote(env.getBackupDir())).append(",\n");
        sb.append("  \"deployBackupDir\": ").append(quote(env.getDeployBackupDir())).append(",\n");
        sb.append("  \"tcServicesAndPrograms\": ").append(quote(env.getTcServicesAndPrograms())).append(",\n");
        sb.append("  \"runningTcPrograms\": ").append(quote(env.getRunningTcPrograms())).append(",\n");
        sb.append("  \"allowedServerIds\": ").append(stringListToJson(env.getAllowedServerIds())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public static String environmentSettingsToJson(String activeId, List<EnvironmentConfig> environments) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"activeEnvironmentId\": ").append(quote(activeId)).append(",\n");
        sb.append("  \"environments\": [\n");
        for (int i = 0; i < environments.size(); i++) {
            sb.append(indent(environmentConfigToJson(environments.get(i)), "    "));
            if (i < environments.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    public static EnvironmentConfig jsonToEnvironmentConfig(String json) {
        Map<String, String> map = parseJsonObject(json);
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId(unquote(map.getOrDefault("id", env.getId())));
        env.setName(unquote(map.getOrDefault("name", "Environment")));
        env.setPort(Integer.parseInt(map.getOrDefault("port", "8585").trim()));
        env.setPathCustomFunctionT2(unquote(map.getOrDefault("pathCustomFunctionT2",
                "D:\\Temp\\Siemens\\CustomFunctions\\bin")));
        env.setPathCustomFunctionT4(unquote(map.getOrDefault("pathCustomFunctionT4",
                "D:\\Temp\\Siemens\\customsolutions\\CF")));
        env.setPathPluginT2(unquote(map.getOrDefault("pathPluginT2",
                "D:\\Temp\\Siemens\\tcroot\\portal\\plugins")));
        env.setPathPluginT4(unquote(map.getOrDefault("pathPluginT4",
                "D:\\Temp\\Siemens\\customsolutions\\jar")));
        env.setPathPluginDllT2(unquote(map.getOrDefault("pathPluginDllT2",
                "D:\\Temp\\Siemens\\tcroot\\portal\\plugins")));
        env.setPathPluginDllT4(unquote(map.getOrDefault("pathPluginDllT4",
                "D:\\Temp\\Siemens\\customsolutions\\dll")));
        env.setBackupDir(unquote(map.getOrDefault("backupDir", "D:\\backups\\")));
        String deployBackup = unquote(map.getOrDefault("deployBackupDir", ""));
        if (deployBackup == null || deployBackup.isBlank()) {
            deployBackup = env.getBackupDir();
        }
        env.setDeployBackupDir(deployBackup);
        env.setTcServicesAndPrograms(unquote(map.getOrDefault("tcServicesAndPrograms",
                "SERVICE:Spooler:Print Spooler\nSERVICE:Themes:Windows Themes")));
        env.setRunningTcPrograms(unquote(map.getOrDefault("runningTcPrograms",
                "tcserver.exe, teamcenter.exe, fcc.exe")));
        int idsStart = json.indexOf("\"allowedServerIds\"");
        if (idsStart >= 0) {
            int arrStart = json.indexOf('[', idsStart);
            int arrEnd = findMatchingBracket(json, arrStart);
            env.setAllowedServerIds(parseStringArray(json.substring(arrStart, arrEnd + 1)));
        }
        return env;
    }

    public static EnvironmentSettingsData jsonToEnvironmentSettings(String json) {
        Map<String, String> top = parseJsonObject(json);
        String activeId = unquote(top.getOrDefault("activeEnvironmentId", "default"));

        List<EnvironmentConfig> envs = new ArrayList<>();
        int envStart = json.indexOf("\"environments\"");
        if (envStart >= 0) {
            int arrStart = json.indexOf('[', envStart);
            int arrEnd = findMatchingBracket(json, arrStart);
            List<String> objects = parseJsonArray(json.substring(arrStart, arrEnd + 1));
            for (String obj : objects) {
                envs.add(jsonToEnvironmentConfig(obj));
            }
        }
        return new EnvironmentSettingsData(activeId, envs);
    }

    // ==================== DESERIALIZATION ====================

    /**
     * Deserializes a JSON string to a Server.
     */
    public static Server jsonToServer(String json) {
        Map<String, String> map = parseJsonObject(json);
        Server server = new Server();
        server.setId(unquote(map.getOrDefault("id", "")));
        server.setName(unquote(map.getOrDefault("name", "")));
        server.setHost(unquote(map.getOrDefault("host", "")));
        server.setPort(Integer.parseInt(map.getOrDefault("port", "8585").trim()));
        server.setDescription(unquote(map.getOrDefault("description", "")));
        String statusStr = unquote(map.getOrDefault("status", "UNKNOWN"));
        try {
            server.setStatus(ServerStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            server.setStatus(ServerStatus.UNKNOWN);
        }
        server.setServerTypes(Server.ServerType.fromMultipleString(unquote(map.get("serverType"))));
        String lastChecked = unquote(map.getOrDefault("lastChecked", "null"));
        if (lastChecked != null && !lastChecked.equals("null") && !lastChecked.isEmpty()) {
            server.setLastChecked(LocalDateTime.parse(lastChecked, DT_FORMAT));
        }
        return server;
    }

    /**
     * Deserializes a JSON array string to a list of Servers.
     */
    public static List<Server> jsonToServers(String json) {
        List<Server> servers = new ArrayList<>();
        List<String> objects = parseJsonArray(json);
        for (String obj : objects) {
            servers.add(jsonToServer(obj));
        }
        return servers;
    }

    /**
     * Deserializes a JSON string to a DeploymentResult.
     */
    public static DeploymentResult jsonToResult(String json) {
        Map<String, String> map = parseJsonObject(json);
        DeploymentResult result = new DeploymentResult();
        result.setServerName(unquote(map.getOrDefault("serverName", "")));
        result.setServerHost(unquote(map.getOrDefault("serverHost", "")));
        result.setFileName(unquote(map.getOrDefault("fileName", "")));
        result.setFileType(unquote(map.getOrDefault("fileType", "")));
        String statusStr = unquote(map.getOrDefault("status", "FAILED"));
        try {
            result.setStatus(ResultStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            result.setStatus(ResultStatus.FAILED);
        }
        result.setMessage(unquote(map.getOrDefault("message", "")));
        String ts = unquote(map.getOrDefault("timestamp", ""));
        if (ts != null && !ts.isEmpty()) {
            result.setTimestamp(LocalDateTime.parse(ts, DT_FORMAT));
        }
        return result;
    }

    /**
     * Deserializes a JSON string to a DeploymentHistory.
     */
    public static DeploymentHistory jsonToHistory(String json) {
        // Extract the results array first, then parse the flat fields
        DeploymentHistory history = new DeploymentHistory();

        // Find and extract the results array
        int resultsStart = json.indexOf("\"results\"");
        String flatPart;
        List<DeploymentResult> results = new ArrayList<>();

        if (resultsStart >= 0) {
            int arrayStart = json.indexOf('[', resultsStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            String resultsJson = json.substring(arrayStart, arrayEnd + 1);
            List<String> resultObjects = parseJsonArray(resultsJson);
            for (String obj : resultObjects) {
                results.add(jsonToResult(obj));
            }
            // Remove results array for flat parsing
            flatPart = json.substring(0, resultsStart) + json.substring(arrayEnd + 1);
        } else {
            flatPart = json;
        }

        Map<String, String> map = parseJsonObject(flatPart);

        history.setId(unquote(map.getOrDefault("id", "")));
        history.setTicketNumber(unquote(map.getOrDefault("ticketNumber", "")));
        String deployedAt = unquote(map.getOrDefault("deployedAt", ""));
        if (deployedAt != null && !deployedAt.isEmpty()) {
            history.setDeployedAt(LocalDateTime.parse(deployedAt, DT_FORMAT));
        }
        history.setTotalFiles(Integer.parseInt(map.getOrDefault("totalFiles", "0").trim()));
        history.setTotalServers(Integer.parseInt(map.getOrDefault("totalServers", "0").trim()));
        history.setSuccessCount(Integer.parseInt(map.getOrDefault("successCount", "0").trim()));
        history.setFailedCount(Integer.parseInt(map.getOrDefault("failedCount", "0").trim()));
        history.setSkippedCount(Integer.parseInt(map.getOrDefault("skippedCount", "0").trim()));

        // Parse file names array
        int fileNamesStart = json.indexOf("\"fileNames\"");
        if (fileNamesStart >= 0) {
            int arrStart = json.indexOf('[', fileNamesStart);
            int arrEnd = findMatchingBracket(json, arrStart);
            history.setFileNames(parseStringArray(json.substring(arrStart, arrEnd + 1)));
        }

        // Parse server names array
        int serverNamesStart = json.indexOf("\"serverNames\"");
        if (serverNamesStart >= 0) {
            int arrStart = json.indexOf('[', serverNamesStart);
            int arrEnd = findMatchingBracket(json, arrStart);
            history.setServerNames(parseStringArray(json.substring(arrStart, arrEnd + 1)));
        }

        history.setResults(results);
        return history;
    }

    /**
     * Deserializes a JSON string to a BackupRecord.
     */
    public static BackupRecord jsonToBackupRecord(String json) {
        BackupRecord record = new BackupRecord();

        // Parse file names array first
        int fileNamesStart = json.indexOf("\"fileNames\"");
        if (fileNamesStart >= 0) {
            int arrStart = json.indexOf('[', fileNamesStart);
            int arrEnd = findMatchingBracket(json, arrStart);
            record.setFileNames(parseStringArray(json.substring(arrStart, arrEnd + 1)));
        }

        Map<String, String> map = parseJsonObject(json);

        record.setId(unquote(map.getOrDefault("id", "")));
        record.setServerName(unquote(map.getOrDefault("serverName", "")));
        record.setServerHost(unquote(map.getOrDefault("serverHost", "")));
        record.setBackupPath(unquote(map.getOrDefault("backupPath", "")));

        String createdAt = unquote(map.getOrDefault("createdAt", ""));
        if (createdAt != null && !createdAt.isEmpty()) {
            record.setCreatedAt(LocalDateTime.parse(createdAt, DT_FORMAT));
        }

        String sizeStr = map.getOrDefault("totalSizeBytes", "0").trim();
        try {
            record.setTotalSizeBytes(Long.parseLong(sizeStr));
        } catch (NumberFormatException e) {
            record.setTotalSizeBytes(0);
        }

        record.setFileCount(Integer.parseInt(map.getOrDefault("fileCount", "0").trim()));

        String statusStr = unquote(map.getOrDefault("status", "FAILED"));
        try {
            record.setStatus(BackupStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            record.setStatus(BackupStatus.FAILED);
        }

        record.setNotes(unquote(map.getOrDefault("notes", "")));

        return record;
    }

    public static String usersToJson(List<Map<String, String>> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < users.size(); i++) {
            Map<String, String> user = users.get(i);
            sb.append("  {\n");
            sb.append("    \"username\": ").append(quote(user.get("username"))).append(",\n");
            sb.append("    \"password\": ").append(quote(user.get("password"))).append("\n");
            sb.append("  }");
            if (i < users.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<Map<String, String>> jsonToUsers(String json) {
        List<Map<String, String>> users = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return users;
        }
        List<String> items = parseJsonArray(json);
        for (String item : items) {
            Map<String, String> map = parseJsonObject(item);
            Map<String, String> clean = new LinkedHashMap<>();
            clean.put("username", unquote(map.get("username")));
            clean.put("password", unquote(map.get("password")));
            users.add(clean);
        }
        return users;
    }

    // ==================== UTILITY METHODS ====================

    public static String quote(String value) {
        if (value == null) return "null";
        return "\"" + escapeJson(value) + "\"";
    }

    private static String unquote(String value) {
        if (value == null || value.equals("null")) return null;
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return unescapeJson(value);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private static String indent(String text, String prefix) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(prefix).append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static String stringListToJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(quote(list.get(i)));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parses a flat JSON object into key-value pairs (values are raw strings).
     * Does NOT handle nested objects/arrays — those should be extracted first.
     */
    public static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            // Find key
            int keyStart = json.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = json.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart + 1, keyEnd);

            // Find colon
            int colonIdx = json.indexOf(':', keyEnd + 1);
            if (colonIdx < 0) break;

            // Find value
            int valueStart = colonIdx + 1;
            while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

            String value;
            if (valueStart >= json.length()) break;

            char firstChar = json.charAt(valueStart);
            if (firstChar == '"') {
                // String value — find closing quote (handle escapes)
                int valueEnd = findClosingQuote(json, valueStart);
                value = json.substring(valueStart, valueEnd + 1);
                i = valueEnd + 1;
            } else if (firstChar == '[') {
                // Array — skip for flat parsing
                int bracketEnd = findMatchingBracket(json, valueStart);
                value = json.substring(valueStart, bracketEnd + 1);
                i = bracketEnd + 1;
            } else if (firstChar == '{') {
                // Nested object — skip for flat parsing
                int braceEnd = findMatchingBrace(json, valueStart);
                value = json.substring(valueStart, braceEnd + 1);
                i = braceEnd + 1;
            } else {
                // Number, boolean, null
                int valueEnd = json.indexOf(',', valueStart);
                int valueEnd2 = json.indexOf('}', valueStart);
                int valueEnd3 = json.indexOf('\n', valueStart);
                int end = json.length();
                if (valueEnd >= 0) end = Math.min(end, valueEnd);
                if (valueEnd2 >= 0) end = Math.min(end, valueEnd2);
                if (valueEnd3 >= 0) end = Math.min(end, valueEnd3);
                value = json.substring(valueStart, end).trim();
                i = end;
            }

            map.put(key, value.trim());
            i = Math.max(i, valueStart + 1);
        }
        return map;
    }

    /**
     * Parses a JSON array of objects into a list of JSON object strings.
     */
    public static List<String> parseJsonArray(String json) {
        List<String> items = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return items;

        int i = 1; // skip opening [
        while (i < json.length() - 1) {
            // Skip whitespace and commas
            while (i < json.length() - 1 && (json.charAt(i) == ' ' || json.charAt(i) == ','
                    || json.charAt(i) == '\n' || json.charAt(i) == '\r' || json.charAt(i) == '\t')) {
                i++;
            }
            if (i >= json.length() - 1) break;

            if (json.charAt(i) == '{') {
                int end = findMatchingBrace(json, i);
                items.add(json.substring(i, end + 1));
                i = end + 1;
            } else {
                i++;
            }
        }
        return items;
    }

    /**
     * Parses a JSON array of strings.
     */
    private static List<String> parseStringArray(String json) {
        List<String> items = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return items;

        int i = 1;
        while (i < json.length() - 1) {
            int quoteStart = json.indexOf('"', i);
            if (quoteStart < 0 || quoteStart >= json.length() - 1) break;
            int quoteEnd = findClosingQuote(json, quoteStart);
            items.add(json.substring(quoteStart + 1, quoteEnd));
            i = quoteEnd + 1;
        }
        return items;
    }

    private static int findClosingQuote(String json, int openQuote) {
        int i = openQuote + 1;
        while (i < json.length()) {
            if (json.charAt(i) == '\\') {
                i += 2; // skip escaped character
            } else if (json.charAt(i) == '"') {
                return i;
            } else {
                i++;
            }
        }
        return json.length() - 1;
    }

    static int findMatchingBrace(String json, int openBrace) {
        int depth = 0;
        boolean inString = false;
        for (int i = openBrace; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return json.length() - 1;
    }

    static int findMatchingBracket(String json, int openBracket) {
        int depth = 0;
        boolean inString = false;
        for (int i = openBracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return json.length() - 1;
    }
}
