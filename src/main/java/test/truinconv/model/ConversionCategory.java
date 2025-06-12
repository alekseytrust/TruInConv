package test.truinconv.model;

/**
 * Supported conversion categories, each with a human-readable display name.
 */
public enum ConversionCategory {
    /** Image conversion category. */
    IMAGE("Images"),
    /** Audio conversion category. */
    AUDIO("Audio"),
    /** Video conversion category. */
    VIDEO("Video");

    private final String displayName;

    /**
     * Associates a display name with the category.
     *
     * @param displayName human-readable name used in the UI
     */
    ConversionCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the display name for this category.
     *
     * @return the UI-friendly name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a string to its corresponding ConversionCategory by display name.
     *
     * @param str case-insensitive display name
     * @return matching ConversionCategory
     * @throws IllegalArgumentException if no matching category is found
     */
    public static ConversionCategory fromString(String str) {
        for (ConversionCategory c : values()) {
            if (c.displayName.equalsIgnoreCase(str)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + str);
    }
}