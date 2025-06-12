package test.truinconv.converters;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Simple image format converter supporting common image formats.
 * <p>
 * Supported formats: JPEG, PNG, BMP, GIF, TIFF
 * Transparently handles formats without alpha by compositing onto white.
 */
public final class ImageConverter {

    // Formats that do not support transparency
    private static final Set<String> FORMATS_WITHOUT_TRANSPARENCY = Set.of("JPG", "JPEG");

    // Prevent instantiation
    private ImageConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts an image file to the specified format, handling transparency as needed.
     *
     * @param inputFile    source image file
     * @param outputFile   destination file for converted image
     * @param targetFormat desired output format (case-insensitive)
     * @throws IOException              if reading or writing fails
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static void convert(File inputFile, File outputFile, String targetFormat) throws IOException {
        validateParameters(inputFile, outputFile, targetFormat);

        BufferedImage sourceImage = readImage(inputFile);
        BufferedImage processedImage = prepareImageForFormat(sourceImage, targetFormat);
        writeImage(processedImage, outputFile, targetFormat);
    }

    /**
     * Ensures inputFile, outputFile, and targetFormat are non-null (and non-empty).
     *
     * @param inputFile    source file
     * @param outputFile   destination file
     * @param targetFormat format string
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateParameters(File inputFile, File outputFile, String targetFormat) {
        if (inputFile == null) {
            throw new IllegalArgumentException("Input file cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        if (targetFormat == null || targetFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Target format cannot be null or empty");
        }
    }

    /**
     * Reads an image from the given file.
     *
     * @param imageFile file containing the source image
     * @return BufferedImage read from file
     * @throws IOException if reading fails or file unsupported
     */
    private static BufferedImage readImage(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Cannot read image file: " + imageFile.getName() +
                    ". File may be corrupted or unsupported.");
        }
        return image;
    }

    /**
     * Prepares the image for the target format, removing transparency if needed.
     *
     * @param originalImage source BufferedImage
     * @param targetFormat  desired output format
     * @return image ready for writing
     */
    private static BufferedImage prepareImageForFormat(BufferedImage originalImage, String targetFormat) {
        if (needsTransparencyRemoval(targetFormat)) {
            return removeTransparency(originalImage);
        }
        return originalImage;
    }

    /**
     * Determines if the target format lacks transparency support.
     *
     * @param format format string to check
     * @return true if transparency must be removed
     */
    private static boolean needsTransparencyRemoval(String format) {
        return FORMATS_WITHOUT_TRANSPARENCY.contains(format.toUpperCase().trim());
    }

    /**
     * Composites the image onto a white background to drop alpha channel.
     *
     * @param imageWithTransparency image potentially containing alpha
     * @return new BufferedImage without alpha
     */
    private static BufferedImage removeTransparency(BufferedImage imageWithTransparency) {
        int width = imageWithTransparency.getWidth();
        int height = imageWithTransparency.getHeight();

        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(imageWithTransparency, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    /**
     * Writes the processed image to the output file in the specified format.
     *
     * @param image        image to write
     * @param outputFile   destination file
     * @param targetFormat desired output format
     * @throws IOException if writing fails or format unsupported
     */
    private static void writeImage(BufferedImage image, File outputFile, String targetFormat) throws IOException {
        String formatName = normalizeFormatName(targetFormat);
        boolean success = ImageIO.write(image, formatName, outputFile);
        if (!success) {
            throw new IOException("Failed to write image in format: " + targetFormat +
                    ". Format may not be supported.");
        }
    }

    /**
     * Normalizes the format name for ImageIO, mapping "jpg" to "jpeg".
     *
     * @param format input format string
     * @return normalized format name acceptable to ImageIO
     */
    private static String normalizeFormatName(String format) {
        String normalized = format.toLowerCase().trim();
        return "jpg".equals(normalized) ? "jpeg" : normalized;
    }
}