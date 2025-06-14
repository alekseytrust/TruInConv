/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv;

import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized theme management for the TruInConv application.
 * Handles theme state persistence, icon updates, and scene styling.
 */
public class ThemeManager {

    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    private static final String THEME_PREF_KEY       = "darkMode";
    private static final String DARK_MODE_CSS_CLASS  = "dark-mode";

    /**
     * Icon and color configuration for theme toggle buttons.
     */
    private static final class IconConfig {
        static final String LIGHT_ICON  = "mdi2m-moon-waning-crescent";
        static final String DARK_ICON   = "mdi2w-white-balance-sunny";
        static final String LIGHT_COLOR = "#888";
        static final String DARK_COLOR  = "#FFA500";
    }

    private static ThemeManager instance;
    private final Preferences preferences;
    private boolean darkMode;

    // Prevent direct instantiation
    private ThemeManager() {
        this.preferences = Preferences.userNodeForPackage(ThemeManager.class);
        this.darkMode    = preferences.getBoolean(THEME_PREF_KEY, false);
    }

    /**
     * Returns the singleton instance of ThemeManager.
     * @return the ThemeManager instance
     */
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Indicates whether dark mode is currently enabled.
     * @return true if dark mode is active, false otherwise
     */
    public boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Enables or disables dark mode and persists the preference.
     * @param darkMode true to enable dark mode, false for light mode
     */
    public void setDarkMode(boolean darkMode) {
        if (this.darkMode != darkMode) {
            this.darkMode = darkMode;
            preferences.putBoolean(THEME_PREF_KEY, darkMode);
            try {
                preferences.flush();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to persist theme preference", e);
            }
        }
    }

    /**
     * Toggles between light and dark mode.
     * @return the new dark mode state
     */
    public boolean toggleTheme() {
        setDarkMode(!darkMode);
        return darkMode;
    }

    /**
     * Applies the current theme (CSS class) to the given scene.
     * @param scene the scene on which to apply the theme
     */
    public void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        try {
            if (darkMode) {
                if (!scene.getRoot().getStyleClass().contains(DARK_MODE_CSS_CLASS)) {
                    scene.getRoot().getStyleClass().add(DARK_MODE_CSS_CLASS);
                }
            } else {
                scene.getRoot().getStyleClass().remove(DARK_MODE_CSS_CLASS);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying theme to scene", e);
        }
    }

    /**
     * Configures a ToggleButton to reflect and control the theme state.
     * Sets its icon and click handler to update all relevant scenes.
     * @param toggleButton the ToggleButton to update
     */
    public void updateToggleButton(ToggleButton toggleButton) {
        if (toggleButton == null) {
            return;
        }
        try {
            toggleButton.setSelected(darkMode);
            updateToggleButtonIcon(toggleButton);
            toggleButton.setOnAction(e -> {
                boolean newState = toggleButton.isSelected();
                setDarkMode(newState);
                updateToggleButtonIcon(toggleButton);
                if (toggleButton.getScene() != null) {
                    applyTheme(toggleButton.getScene());
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating toggle button", e);
        }
    }

    /**
     * Updates the icon of a theme ToggleButton based on the current theme.
     * @param toggleButton the ToggleButton whose icon to update
     */
    private void updateToggleButtonIcon(ToggleButton toggleButton) {
        try {
            FontIcon icon = (FontIcon) toggleButton.getGraphic();
            if (icon != null) {
                if (darkMode) {
                    icon.setIconLiteral(IconConfig.DARK_ICON);
                    icon.setIconColor(Paint.valueOf(IconConfig.DARK_COLOR));
                } else {
                    icon.setIconLiteral(IconConfig.LIGHT_ICON);
                    icon.setIconColor(Paint.valueOf(IconConfig.LIGHT_COLOR));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating toggle button icon", e);
        }
    }

    /**
     * Sets up a ToggleButton with a fixed icon size and click handler for theme toggling.
     * @param toggleButton the ToggleButton to configure
     * @param iconSize the desired icon size in pixels
     */
    public void updateToggleButtonWithSize(ToggleButton toggleButton, double iconSize) {
        if (toggleButton == null) {
            return;
        }
        try {
            toggleButton.setSelected(darkMode);
            updateToggleButtonIconWithSize(toggleButton, iconSize);
            toggleButton.setOnAction(e -> {
                boolean newState = toggleButton.isSelected();
                setDarkMode(newState);
                updateToggleButtonIconWithSize(toggleButton, iconSize);
                if (toggleButton.getScene() != null) {
                    applyTheme(toggleButton.getScene());
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating toggle button with size", e);
        }
    }

    /**
     * Updates the icon and size of a theme ToggleButton based on the current theme.
     * @param toggleButton the ToggleButton whose icon to update
     * @param iconSize the size to set on the icon
     */
    private void updateToggleButtonIconWithSize(ToggleButton toggleButton, double iconSize) {
        try {
            FontIcon icon = (FontIcon) toggleButton.getGraphic();
            if (icon != null) {
                if (darkMode) {
                    icon.setIconLiteral(IconConfig.DARK_ICON);
                    icon.setIconColor(Paint.valueOf(IconConfig.DARK_COLOR));
                } else {
                    icon.setIconLiteral(IconConfig.LIGHT_ICON);
                    icon.setIconColor(Paint.valueOf(IconConfig.LIGHT_COLOR));
                }
                icon.setIconSize((int) Math.round(iconSize));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating toggle button icon with size", e);
        }
    }

    /**
     * Initializes theme management for a scene and its associated ToggleButton.
     * @param scene the Scene to apply theme to
     * @param toggleButton the ToggleButton controlling theme toggling
     */
    public void initializeTheme(Scene scene, ToggleButton toggleButton) {
        applyTheme(scene);
        updateToggleButton(toggleButton);
        if (scene != null) {
            scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
                if (newRoot != null) {
                    applyTheme(scene);
                }
            });
        }
    }

    /**
     * Provides a Runnable that reapplies the current theme to the specified scene.
     * Useful for attaching listeners elsewhere.
     * @param scene the Scene to update
     * @return a Runnable that applies the current theme
     */
    public Runnable createThemeChangeListener(Scene scene) {
        return () -> applyTheme(scene);
    }

    /**
     * Returns the CSS class name used for dark mode.
     * @return the dark mode CSS class identifier
     */
    public String getDarkModeClass() {
        return DARK_MODE_CSS_CLASS;
    }
}