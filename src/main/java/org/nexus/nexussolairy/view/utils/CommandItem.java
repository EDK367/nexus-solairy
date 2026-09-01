package org.nexus.nexussolairy.view.utils;

public class CommandItem {
    private final String title;
    private final String category;
    private final String shortcut;
    private final Runnable action;

    public CommandItem(String title, String category, String shortcut, Runnable action) {
        this.title = title;
        this.category = category;
        this.shortcut = shortcut;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getShortcut() {
        return shortcut;
    }

    public Runnable getAction() {
        return action;
    }

    @Override
    public String toString() {
        return title;
    }
}
