package org.nexus.nexussolairy.model.enums;

public enum LanguageType {
    PIG_LATIN("Pig Latin", ".pig", "#00D9FF"),
    Y_LANG("Y?", ".y", "#8B5CF6"),
    ZETARIANO("Zetariano", ".z", "#38BDF8"),
    C_LANG("C", ".c", "#22C55E"),
    UNKNOWN("Plain Text", ".txt", "#8B98A7");

    private final String displayName;
    private final String extension;
    private final String colorHex;

    LanguageType(String displayName, String extension, String colorHex) {
        this.displayName = displayName;
        this.extension = extension;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtension() {
        return extension;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static LanguageType fromFileName(String fileName) {
        if (fileName == null) return UNKNOWN;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pig")) return PIG_LATIN;
        if (lower.endsWith(".y")) return Y_LANG;
        if (lower.endsWith(".z")) return ZETARIANO;
        if (lower.endsWith(".c") || lower.endsWith(".h")) return C_LANG;
        return UNKNOWN;
    }
}
