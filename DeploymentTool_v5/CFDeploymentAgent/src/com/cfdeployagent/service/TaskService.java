package com.cfdeployagent.service;

import com.cfdeployagent.config.AgentConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to query and control Windows services, programs, and processes on the agent machine.
 */
public class TaskService {

    private final AgentConfig config;

    public TaskService(AgentConfig config) {
        this.config = config;
    }

    public static class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String getMessage() {
            if (isSuccess()) {
                return stdout.isBlank() ? "Success" : stdout;
            }
            return stderr.isBlank() ? "Exit code: " + exitCode : stderr;
        }
    }

    // ==================== WINDOWS SERVICES ====================

    public String getServiceStatus(String serviceName) {
        try {
            Process p = Runtime.getRuntime().exec("sc query \"" + serviceName + "\"");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toUpperCase().contains("STATE")) {
                        String upper = line.toUpperCase();
                        if (upper.contains("RUNNING")) {
                            return "RUNNING";
                        } else if (upper.contains("STOPPED")) {
                            return "STOPPED";
                        } else if (upper.contains("PAUSED")) {
                            return "PAUSED";
                        } else if (upper.contains("START_PENDING")) {
                            return "STARTING";
                        } else if (upper.contains("STOP_PENDING")) {
                            return "STOPPING";
                        }
                    }
                }
            }
            return "STOPPED"; // sc query output missing STATE usually means service stopped or not found
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public CommandResult controlService(String serviceName, String action) {
        String command;
        if ("start".equalsIgnoreCase(action)) {
            command = "net start \"" + serviceName + "\"";
        } else if ("stop".equalsIgnoreCase(action)) {
            command = "net stop \"" + serviceName + "\"";
        } else if ("restart".equalsIgnoreCase(action)) {
            CommandResult stopRes = runCommand("net stop \"" + serviceName + "\"");
            // Ignore failure if service was already stopped
            CommandResult startRes = runCommand("net start \"" + serviceName + "\"");
            return startRes;
        } else {
            return new CommandResult(-1, "", "Unknown action: " + action);
        }
        return runCommand(command);
    }

    // ==================== PROGRAMS AND PROCESSES ====================

    public List<Integer> getProcessPids(String imageName) {
        List<Integer> pids = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec("tasklist /FO CSV /NH /FI \"IMAGENAME eq " + imageName + "\"");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("\"")) {
                        String[] parts = line.split("\",\"");
                        if (parts.length > 1) {
                            String pidStr = parts[1].replace("\"", "").trim();
                            try {
                                pids.add(Integer.parseInt(pidStr));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return pids;
    }

    public CommandResult startProgram(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return new CommandResult(-1, "", "Executable file not found: " + path);
            }
            ProcessBuilder pb = new ProcessBuilder(path);
            pb.directory(file.getParentFile());
            pb.start();
            return new CommandResult(0, "Program started successfully in background", "");
        } catch (Exception e) {
            return new CommandResult(-1, "", "Failed to start program: " + e.getMessage());
        }
    }

    public CommandResult killProcess(String target, boolean byPid) {
        String command;
        if (byPid) {
            command = "taskkill /F /PID " + target;
        } else {
            command = "taskkill /F /IM " + target;
        }
        return runCommand(command);
    }

    // ==================== HELPERS ====================

    private CommandResult runCommand(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            try (BufferedReader outReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                 BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = outReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
                while ((line = errReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }
            int exitCode = p.waitFor();
            return new CommandResult(exitCode, stdout.toString().trim(), stderr.toString().trim());
        } catch (Exception e) {
            return new CommandResult(-1, "", e.getMessage());
        }
    }
}
