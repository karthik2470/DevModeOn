package com.cfdeploytool.model;

import java.io.File;

/**
 * Represents a file selected for deployment.
 */
public class DeploymentFile {

    public enum FileType {
        EXE("Executable (.exe)"),
        JAR("JAR Archive (.jar)"),
        DLL("Dynamic Library (.dll)"),
        BAT("Batch Script (.bat)"),
        PLUGIN_JAR("Plugin JAR (.jar)"),
        CONFIG("Configuration File");

        private final String displayName;

        FileType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private File file;
    private FileType fileType;
    private long sizeBytes;

    public DeploymentFile(File file, FileType fileType) {
        this.file = file;
        this.fileType = fileType;
        this.sizeBytes = file.length();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.sizeBytes = file.length();
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getFileName() {
        return file.getName();
    }

    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", sizeBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Infers file type from extension. Defaults to CONFIG for unknown types.
     */
    public static FileType inferFileType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".exe")) {
            return FileType.EXE;
        } else if (name.endsWith(".dll")) {
            return FileType.DLL;
        } else if (name.endsWith(".bat")) {
            return FileType.BAT;
        } else if (name.endsWith(".jar")) {
            return FileType.JAR; // User can manually change to PLUGIN_JAR
        } else {
            return FileType.CONFIG;
        }
    }

    @Override
    public String toString() {
        return file.getName() + " [" + fileType.getDisplayName() + "] (" + getFormattedSize() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeploymentFile that = (DeploymentFile) o;
        return file.getAbsolutePath().equals(that.file.getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return file.getAbsolutePath().hashCode();
    }
}
