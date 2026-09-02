package org.nexus.nexussolairy.service.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class FileService {

    public String readFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            return "";
        }
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    public void writeFile(File file, String content) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Files.writeString(file.toPath(), content != null ? content : "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public File createFile(File parent, String name, String defaultContent) throws IOException {
        File newFile = new File(parent, name);
        if (newFile.exists()) {
            throw new IOException("File already exists: " + name);
        }
        writeFile(newFile, defaultContent);
        return newFile;
    }

    public File createFolder(File parent, String name) throws IOException {
        File newFolder = new File(parent, name);
        if (newFolder.exists()) {
            throw new IOException("Folder already exists: " + name);
        }
        if (!newFolder.mkdirs()) {
            throw new IOException("Failed to create folder: " + name);
        }
        return newFolder;
    }

    public boolean delete(File file) {
        if (file == null || !file.exists()) return false;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        return file.delete();
    }
}
