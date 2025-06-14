/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv.model;

import javafx.beans.property.SimpleStringProperty;
import java.io.File;
import java.util.Objects;

/**
 * Wraps a File with a JavaFX property for display in table views.
 */
public class FileItem {
    private final File file;
    private final SimpleStringProperty fileName;

    /**
     * Creates a FileItem for the specified file.
     *
     * @param file the File to wrap (must not be null)
     */
    public FileItem(File file) {
        this.file = file;
        this.fileName = new SimpleStringProperty(file.getName());
    }

    /**
     * Returns the underlying File object.
     *
     * @return the wrapped File
     */
    public File getFile() {
        return file;
    }

    /**
     * Gets the file name for display purposes.
     *
     * @return the file name as a String
     */
    public String getFileName() {
        return fileName.get();
    }

    /**
     * Provides the file name as a JavaFX property for table binding.
     *
     * @return a SimpleStringProperty containing the file name
     */
    public SimpleStringProperty fileNameProperty() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileItem)) return false;
        return Objects.equals(file, ((FileItem)o).file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}