package com.cfdeployagent.service;

import com.cfdeployagent.config.AgentConfig;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to query and control Windows services, programs, batch scripts, and processes
 * on the agent machine.
 */
public class TaskService {

    private static final int OUTPUT_MAX_LINES = 2000;

    private final AgentConfig config;

    /** Live process handles for running batch scripts, keyed by canonical bat path. */
    private final Map<String, Process> batchProcesses = new ConcurrentHashMap<>();

    /** Ring-buffer output for each batch script (last OUTPUT_MAX_LINES lines). */
    private final Map<String, LinkedList<String>> batchOutput = new ConcurrentHashMap<>();

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

    /** Standalone processes running for services with custom Logon As users. */
    private final Map<String, Process> serviceProcesses = new ConcurrentHashMap<>();

    public String getServiceStatus(String serviceName) {
        String key = serviceName.trim().toLowerCase();
        Process customProc = serviceProcesses.get(key);
        if (customProc != null) {
            if (customProc.isAlive()) {
                return "RUNNING";
            } else {
                serviceProcesses.remove(key);
            }
        }

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sc", "query", serviceName});
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
            return "STOPPED";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public CommandResult controlService(String serviceName, String action) {
        String key = serviceName.trim().toLowerCase();

        // 1. Check if the service has a custom Logon As user configuration
        String binaryPath = null;
        String startName = null;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sc", "qc", serviceName});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trim = line.trim();
                    int colonIdx = trim.indexOf(':');
                    if (colonIdx >= 0) {
                        String namePart = trim.substring(0, colonIdx).trim().toUpperCase();
                        String valPart = trim.substring(colonIdx + 1).trim();
                        if (namePart.startsWith("BINARY_PATH_NAME")) {
                            binaryPath = valPart;
                        } else if (namePart.startsWith("SERVICE_START_NAME")) {
                            startName = valPart;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        boolean isCustomUser = false;
        if (startName != null && !startName.isBlank()) {
            String startUpper = startName.toUpperCase();
            if (!startUpper.equals("LOCALSYSTEM") &&
                !startUpper.equals("NT AUTHORITY\\LOCALSERVICE") &&
                !startUpper.equals("NT AUTHORITY\\NETWORKSERVICE") &&
                !startUpper.equals("LOCAL SERVICE") &&
                !startUpper.equals("NETWORK SERVICE")) {
                isCustomUser = true;
            }
        }

        // 2. Intercept and run as current user if Logon As account is custom
        if (isCustomUser && binaryPath != null && !binaryPath.isBlank()) {
            if ("start".equalsIgnoreCase(action) || "restart".equalsIgnoreCase(action)) {
                if ("restart".equalsIgnoreCase(action)) {
                    stopCustomServiceProcess(key);
                }

                // If already running standalone, don't restart
                Process existing = serviceProcesses.get(key);
                if (existing != null && existing.isAlive()) {
                    return new CommandResult(0, "Service is already running as custom process.", "");
                }

                try {
                    List<String> cmdArgs = parseCommandLine(binaryPath);
                    if (cmdArgs.isEmpty()) {
                        return new CommandResult(-1, "", "Failed to parse BINARY_PATH_NAME: " + binaryPath);
                    }

                    File exeFile = new File(cmdArgs.get(0));
                    ProcessBuilder pb = new ProcessBuilder(cmdArgs);
                    if (exeFile.getParentFile() != null && exeFile.getParentFile().exists()) {
                        pb.directory(exeFile.getParentFile());
                    }
                    pb.redirectErrorStream(true);

                    Process proc = pb.start();
                    serviceProcesses.put(key, proc);

                    // Wait a short duration to verify if it crashes immediately
                    Thread.sleep(600);
                    if (!proc.isAlive()) {
                        StringBuilder output = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                            String line;
                            int lineCount = 0;
                            while ((line = reader.readLine()) != null && lineCount < 50) {
                                output.append(line).append("\n");
                                lineCount++;
                            }
                        } catch (IOException ignored) {}
                        
                        int exitValue = proc.exitValue();
                        serviceProcesses.remove(key);
                        return new CommandResult(exitValue, "", 
                            "Service executable exited immediately with code " + exitValue + ".\n"
                            + "Console Output:\n" + output.toString().trim());
                    }

                    return new CommandResult(0, "Started service as standalone process under current user context (PID " + proc.pid() + ").", "");
                } catch (Exception e) {
                    return new CommandResult(-1, "", "Failed to run custom service executable: " + e.getMessage());
                }
            } else if ("stop".equalsIgnoreCase(action)) {
                boolean stopped = stopCustomServiceProcess(key);
                if (stopped) {
                    return new CommandResult(0, "Service standalone process stopped successfully.", "");
                } else {
                    return new CommandResult(0, "Service was not running as a custom process.", "");
                }
            } else {
                return new CommandResult(-1, "", "Unknown action: " + action);
            }
        }

        // 3. Fallback to standard Windows SCM controls for default system accounts
        if ("start".equalsIgnoreCase(action)) {
            return runCommand("net", "start", serviceName);
        } else if ("stop".equalsIgnoreCase(action)) {
            return runCommand("net", "stop", serviceName);
        } else if ("restart".equalsIgnoreCase(action)) {
            runCommand("net", "stop", serviceName);
            return runCommand("net", "start", serviceName);
        } else {
            return new CommandResult(-1, "", "Unknown action: " + action);
        }
    }

    private boolean stopCustomServiceProcess(String key) {
        Process proc = serviceProcesses.remove(key);
        if (proc != null && proc.isAlive()) {
            try {
                long pid = proc.pid();
                runCommand("taskkill", "/F", "/T", "/PID", String.valueOf(pid));
                try {
                    proc.descendants().forEach(ProcessHandle::destroyForcibly);
                    proc.destroyForcibly();
                } catch (Exception ignored) {}
                proc.waitFor();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static List<String> parseCommandLine(String commandLine) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    list.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            list.add(current.toString());
        }
        return list;
    }

    // ==================== PROGRAMS AND PROCESSES ====================

    public List<Integer> getProcessPids(String imageName) {
        List<Integer> pids = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"tasklist", "/FO", "CSV", "/NH", "/FI", "IMAGENAME eq " + imageName});
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
        if (byPid) {
            return runCommand("taskkill", "/F", "/PID", target);
        } else {
            return runCommand("taskkill", "/F", "/IM", target);
        }
    }

    // ==================== BATCH SCRIPT EXECUTION ====================

    /**
     * Starts a .bat file and captures its stdout+stderr into an in-memory ring buffer.
     * If a process for this bat is already running, returns success without starting again.
     */
    public CommandResult startBatch(String batPath) {
        String key = normalizeBatKey(batPath);
        Process existing = batchProcesses.get(key);
        if (existing != null && existing.isAlive()) {
            return new CommandResult(0, "Batch script is already running.", "");
        }

        try {
            File batFile = new File(batPath);
            if (!batFile.exists()) {
                return new CommandResult(-1, "", "Batch file not found: " + batPath);
            }

            // Create shims directory for external commands that break with redirected stdin (e.g. timeout.exe)
            File shimsDir = new File("shims");
            if (!shimsDir.exists()) {
                shimsDir.mkdirs();
            }
            File timeoutExe = new File(shimsDir, "timeout.exe");
            if (!timeoutExe.exists()) {
                File csFile = new File(shimsDir, "timeout.cs");
                try {
                    try (FileWriter writer = new FileWriter(csFile)) {
                        writer.write("using System;\n"
                                + "using System.Threading;\n"
                                + "class TimeoutShim {\n"
                                + "    static void Main(string[] args) {\n"
                                + "        int secs = 1;\n"
                                + "        for (int i = 0; i < args.Length; i++) {\n"
                                + "            if ((args[i] == \"/t\" || args[i] == \"/T\") && i + 1 < args.Length) {\n"
                                + "                int.TryParse(args[i+1], out secs);\n"
                                + "            }\n"
                                + "        }\n"
                                + "        Thread.Sleep(secs * 1000);\n"
                                + "    }\n"
                                + "}\n");
                    }
                    // Compile via system csc.exe
                    Process compileProc = Runtime.getRuntime().exec(new String[]{
                        "C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe",
                        "/out:" + timeoutExe.getAbsolutePath(),
                        csFile.getAbsolutePath()
                    });
                    compileProc.waitFor();
                } catch (Exception ignored) {
                } finally {
                    if (csFile.exists()) {
                        csFile.delete();
                    }
                }
            }

            // Clean up any old timeout.bat that might interfere
            File oldTimeoutBat = new File(shimsDir, "timeout.bat");
            if (oldTimeoutBat.exists()) {
                oldTimeoutBat.delete();
            }

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", batPath);
            pb.directory(batFile.getParentFile());
            pb.redirectErrorStream(true); // merge stderr into stdout

            // Prepend shims directory to environment PATH
            Map<String, String> env = pb.environment();
            String pathKey = "PATH";
            for (String keyStr : env.keySet()) {
                if ("path".equalsIgnoreCase(keyStr)) {
                    pathKey = keyStr;
                    break;
                }
            }
            String existingPath = env.get(pathKey);
            env.put(pathKey, shimsDir.getAbsolutePath() + File.pathSeparator + (existingPath != null ? existingPath : ""));

            Process process = pb.start();
            batchProcesses.put(key, process);

            // Keep stdin open as a pipe, but feed newlines periodically.
            // This means 'pause' receives a simulated keypress (newline) within 250ms
            // instead of getting a closed/redirected stdin which causes:
            //   "Input redirection is not supported, exiting the process immediately"
            final OutputStream processStdin = process.getOutputStream();
            Thread stdinFeeder = new Thread(() -> {
                try {
                    while (process.isAlive()) {
                        processStdin.write('\n');
                        processStdin.flush();
                        Thread.sleep(250);
                    }
                } catch (Exception ignored) {
                } finally {
                    try { processStdin.close(); } catch (IOException ignored) {}
                }
            }, "batch-stdin-feeder-" + batFile.getName());
            stdinFeeder.setDaemon(true);
            stdinFeeder.start();

            // Reset output buffer
            LinkedList<String> outputBuffer = new LinkedList<>();
            batchOutput.put(key, outputBuffer);

            // Capture output asynchronously
            Thread captureThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (outputBuffer) {
                            outputBuffer.addLast(line);
                            if (outputBuffer.size() > OUTPUT_MAX_LINES) {
                                outputBuffer.removeFirst();
                            }
                        }
                    }
                } catch (IOException ignored) {}

                // Append a completion marker once stdout is fully drained
                int exitCode = 0;
                try { exitCode = process.waitFor(); } catch (InterruptedException ignored) {}
                String marker = "\n─────────────────────────────────────\n"
                        + "Process finished with exit code " + exitCode
                        + "\n─────────────────────────────────────";
                synchronized (outputBuffer) {
                    for (String markerLine : marker.split("\n")) {
                        outputBuffer.addLast(markerLine);
                    }
                }
            }, "batch-capture-" + batFile.getName());
            captureThread.setDaemon(true);
            captureThread.start();

            return new CommandResult(0, "Batch script started: " + batFile.getName(), "");
        } catch (Exception e) {
            return new CommandResult(-1, "", "Failed to start batch: " + e.getMessage());
        }
    }

    /**
     * Stops a running batch process by killing the entire Windows process tree.
     * Uses {@code taskkill /F /T /PID} to ensure cmd.exe and all its children
     * (programs spawned by the bat script) are all terminated.
     */
    public CommandResult stopBatch(String batPath) {
        String key = normalizeBatKey(batPath);
        Process process = batchProcesses.remove(key);
        if (process == null || !process.isAlive()) {
            return new CommandResult(0, "Batch script was not running.", "");
        }
        try {
            long pid = process.pid();

            // /F = force, /T = kill entire tree (all descendants)
            CommandResult killResult = runCommand("taskkill", "/F", "/T", "/PID", String.valueOf(pid));

            // Also destroy via Java as a fallback
            try {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            } catch (Exception ignored) {}

            return new CommandResult(0, "Batch script stopped (PID " + pid + ").", "");
        } catch (Exception e) {
            return new CommandResult(-1, "", "Failed to stop batch: " + e.getMessage());
        }
    }

    /** Returns "RUNNING" if the batch process is alive, "STOPPED" otherwise. */
    public String getBatchStatus(String batPath) {
        String key = normalizeBatKey(batPath);
        Process process = batchProcesses.get(key);
        return (process != null && process.isAlive()) ? "RUNNING" : "STOPPED";
    }

    /**
     * Returns all buffered output lines for the given batch script joined by newlines.
     */
    public String getBatchOutput(String batPath) {
        String key = normalizeBatKey(batPath);
        LinkedList<String> buffer = batchOutput.get(key);
        if (buffer == null) return "";
        synchronized (buffer) {
            return String.join("\n", buffer);
        }
    }

    private String normalizeBatKey(String batPath) {
        return batPath.trim().toLowerCase().replace('/', '\\');
    }

    // ==================== HELPERS ====================

    private CommandResult runCommand(String... command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            try (BufferedReader outReader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
                 BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream(), "UTF-8"))) {
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
