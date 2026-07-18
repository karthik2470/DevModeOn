package com.cfdeployagent;

import com.cfdeployagent.config.AgentConfig;
import com.cfdeployagent.handler.BackupFilesHandler;
import com.cfdeployagent.handler.BatchOutputHandler;
import com.cfdeployagent.handler.DeployHandler;
import com.cfdeployagent.handler.HealthHandler;
import com.cfdeployagent.handler.ListDirectoryHandler;
import com.cfdeployagent.handler.ValidatePathHandler;
import com.cfdeployagent.handler.VersionIncrementHandler;
import com.cfdeployagent.handler.VersionReadHandler;
import com.cfdeployagent.handler.VersionUpdateHandler;
import com.cfdeployagent.handler.TaskControlHandler;
import com.cfdeployagent.handler.TaskStatusHandler;
import com.cfdeployagent.service.TaskService;
import com.cfdeployagent.service.DirectoryService;
import com.cfdeployagent.service.FileDeploymentService;
import com.cfdeployagent.service.PathValidationService;
import com.cfdeployagent.service.VersionService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * CF Deploy Agent — server-side REST API for receiving file deployments.
 */
public class Main {

    public static void main(String[] args) {
        try {
            AgentConfig config = AgentConfig.load();
            int port = resolvePort(args, config.getPort());

            ensureDirectories(config);

            FileDeploymentService deploymentService = new FileDeploymentService(config);
            PathValidationService validationService = new PathValidationService(config);
            VersionService versionService = new VersionService(config);
            DirectoryService directoryService = new DirectoryService();
            TaskService taskService = new TaskService(config);
 
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/validate-path", new ValidatePathHandler(validationService));
            server.createContext("/api/deploy", new DeployHandler(deploymentService));
            server.createContext("/api/list-directory", new ListDirectoryHandler(directoryService));
            server.createContext("/api/backup-files", new BackupFilesHandler(directoryService));
            server.createContext("/api/version/read", new VersionReadHandler(versionService));
            server.createContext("/api/version/update", new VersionUpdateHandler(versionService));
            server.createContext("/api/version/increment", new VersionIncrementHandler(versionService));
            server.createContext("/api/task/status", new TaskStatusHandler(taskService));
            server.createContext("/api/task/control", new TaskControlHandler(taskService));
            server.createContext("/api/task/batch/output", new BatchOutputHandler(taskService));
            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();

            System.out.println("CF Deploy Agent started on port " + port);
            System.out.println("  Health:  http://localhost:" + port + "/api/health");
            System.out.println("  Deploy:  http://localhost:" + port + "/api/deploy");
            System.out.println("Press Ctrl+C to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        } catch (IOException e) {
            System.err.println("Failed to start CF Deploy Agent: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int resolvePort(String[] args, int defaultPort) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {
                    return defaultPort;
                }
            }
        }
        return defaultPort;
    }

    private static void ensureDirectories(AgentConfig config) throws IOException {
        Set<Path> created = new LinkedHashSet<>();
        for (Path path : config.getAllDeployPaths()) {
            if (created.add(path)) {
                Files.createDirectories(path);
            }
        }
    }
}
