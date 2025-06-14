/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv.constants;

import test.truinconv.model.ConversionCategory;
import java.util.Map;
import java.util.Set;

/**
 * Provides supported format conversion mappings per category.
 * Maps each ConversionCategory to a map of source format extensions
 * and their allowed target format extensions.
 */
public class ConversionMappings {

    /**
     * Conversion lookup table:
     * <ul>
     *   <li>Key: ConversionCategory (IMAGE, AUDIO, VIDEO)</li>
     *   <li>Value: Map of source extension to a Set of permitted target extensions</li>
     * </ul>
     */
    public static final Map<ConversionCategory, Map<String, Set<String>>> MAPPINGS =
            Map.of(
                    ConversionCategory.IMAGE, Map.of(
                            "JPG",  Set.of("PNG", "BMP", "GIF", "TIFF"),
                            "JPEG", Set.of("PNG", "BMP", "GIF", "TIFF"),
                            "PNG",  Set.of("JPG", "JPEG", "BMP", "GIF", "TIFF"),
                            "BMP",  Set.of("JPG", "JPEG", "PNG", "GIF", "TIFF"),
                            "GIF",  Set.of("JPG", "JPEG", "PNG", "BMP", "TIFF"),
                            "TIFF", Set.of("JPG", "JPEG", "PNG", "BMP", "GIF")
                    ),
                    ConversionCategory.AUDIO, Map.of(
                            "MP3", Set.of("WAV", "AAC", "FLAC", "OGG"),
                            "WAV", Set.of("MP3", "AAC", "FLAC", "OGG"),
                            "AAC", Set.of("MP3", "WAV", "FLAC", "OGG"),
                            "FLAC",Set.of("MP3", "WAV", "AAC", "OGG"),
                            "OGG", Set.of("MP3", "WAV", "AAC", "FLAC")
                    ),
                    ConversionCategory.VIDEO, Map.of(
                            "MP4", Set.of("AVI", "MKV", "MOV", "WEBM"),
                            "AVI", Set.of("MP4", "MKV", "MOV", "WEBM"),
                            "MKV", Set.of("MP4", "AVI", "MOV", "WEBM"),
                            "MOV", Set.of("MP4", "AVI", "MKV", "WEBM"),
                            "WEBM",Set.of("MP4", "AVI", "MKV", "MOV")
                    )
            );
}