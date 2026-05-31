package com.cfdeploytool.service;

import com.cfdeploytool.model.Server;
import com.cfdeploytool.service.HttpDeploymentClient.BackupFilesResult;
import com.cfdeploytool.service.HttpDeploymentClient.ListDirectoryResult;
import com.cfdeploytool.service.HttpDeploymentClient.RemoteFileEntry;

import java.util.List;

/**
 * Remote backup operations via the deployment agent.
 */
public class BackupService {

    private final HttpDeploymentClient httpClient;

    public BackupService() {
        this.httpClient = new HttpDeploymentClient();
    }

    public ListDirectoryResult listFiles(Server server, String directoryPath) {
        return httpClient.listDirectory(server, directoryPath);
    }

    public BackupFilesResult backupSelected(Server server, String sourcePath, String backupDir,
                                            List<String> fileNames) {
        return httpClient.backupFiles(server, sourcePath, backupDir, fileNames);
    }

    public List<RemoteFileEntry> getLastListedFiles(ListDirectoryResult result) {
        return result.getFiles();
    }
}
