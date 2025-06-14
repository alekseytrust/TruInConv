/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv.converters;

import test.truinconv.model.ConversionCategory;
import java.io.File;

/**
 * Routes file conversions to the appropriate converter based on category.
 * Supports IMAGE, AUDIO, and VIDEO conversions.
 */
public final class ConverterRouter {

    // Prevent instantiation of utility class
    private ConverterRouter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Dispatches conversion to the specific converter.
     *
     * @param inputFile          source file to convert
     * @param outputFile         destination file for converted content
     * @param targetFormat       desired output file format
     * @param conversionCategory category determining which converter to use
     * @throws Exception if the underlying conversion fails
     * @throws IllegalArgumentException if any parameter is invalid or category unsupported
     */
    public static void convert(File inputFile, File outputFile,
                               String targetFormat,
                               ConversionCategory conversionCategory) throws Exception {
        validateParameters(inputFile, outputFile, targetFormat, conversionCategory);

        switch (conversionCategory) {
            case IMAGE -> ImageConverter.convert(inputFile, outputFile, targetFormat);
            case AUDIO -> AudioConverter.convert(inputFile, outputFile, targetFormat);
            case VIDEO -> VideoConverter.convert(inputFile, outputFile, targetFormat);
            default -> throw new IllegalArgumentException(
                    "Unsupported conversion category: " + conversionCategory);
        }
    }

    /**
     * Ensures all inputs are non-null and targetFormat is not empty.
     *
     * @param inputFile          source file
     * @param outputFile         destination file
     * @param targetFormat       format string
     * @param conversionCategory conversion type
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateParameters(File inputFile,
                                           File outputFile,
                                           String targetFormat,
                                           ConversionCategory conversionCategory) {
        if (inputFile == null) {
            throw new IllegalArgumentException("Input file cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        if (targetFormat == null || targetFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Target format cannot be null or empty");
        }
        if (conversionCategory == null) {
            throw new IllegalArgumentException("Conversion category cannot be null");
        }
    }
}
