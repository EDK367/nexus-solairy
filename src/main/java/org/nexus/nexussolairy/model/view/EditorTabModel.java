package org.nexus.nexussolairy.model.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nexus.nexussolairy.model.enums.LanguageType;

import java.io.File;

public class EditorTabModel {
    private final File file;
    private final StringProperty title;
    private final BooleanProperty dirty;
    private final LanguageType languageType;
    private String savedContent;

    public EditorTabModel(File file, String initialContent) {
        this.file = file;
        this.savedContent = initialContent != null ? initialContent : "";
        this.title = new SimpleStringProperty(file != null ? file.getName() : "Untitled");
        this.dirty = new SimpleBooleanProperty(false);
        this.languageType = file != null ? LanguageType.fromFileName(file.getName()) : LanguageType.PIG_LATIN;
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }

    public LanguageType getLanguageType() {
        return languageType;
    }

    public String getSavedContent() {
        return savedContent;
    }

    public void setSavedContent(String savedContent) {
        this.savedContent = savedContent;
        setDirty(false);
    }
}
