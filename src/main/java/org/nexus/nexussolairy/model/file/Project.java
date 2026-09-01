package org.nexus.nexussolairy.model.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private File rootDirectory;
    private List<File> openFiles;
    private File activeFile;

    public Project(String name, File rootDirectory) {
        this.name = name;
        this.rootDirectory = rootDirectory;
        this.openFiles = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public List<File> getOpenFiles() {
        return openFiles;
    }

    public void setOpenFiles(List<File> openFiles) {
        this.openFiles = openFiles;
    }

    public File getActiveFile() {
        return activeFile;
    }

    public void setActiveFile(File activeFile) {
        this.activeFile = activeFile;
    }
}
