package test.truinconv.converters;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Set;

/**
 * High-quality audio converter that preserves original quality.
 * <p>
 * Supported audio formats: MP3, WAV, AAC, FLAC, OGG
 */
public final class AudioConverter {
    private static final int FALLBACK_AUDIO_BITRATE     = 192000; // 192 kbps
    private static final int FALLBACK_AUDIO_SAMPLE_RATE = 48000;  // 48 kHz
    private static final int FALLBACK_AUDIO_CHANNELS    = 2;
    private static final int MIN_AUDIO_BITRATE          = 128000; // 128 kbps minimum
    private static final Set<String> AUDIO_FORMATS      = Set.of("MP3", "WAV", "AAC", "FLAC", "OGG");

    // Prevent instantiation
    private AudioConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts an audio file to the specified format while preserving its original quality.
     *
     * @param inputFile    source audio file
     * @param outputFile   destination file for converted audio
     * @param targetFormat desired output format (case-insensitive)
     * @throws EncoderException if conversion fails
     * @throws IllegalArgumentException if any argument is null or format unsupported
     */
    public static void convert(File inputFile, File outputFile, String targetFormat) throws EncoderException {
        if (inputFile == null || outputFile == null || targetFormat == null) {
            throw new IllegalArgumentException("Input file, output file, and target format cannot be null");
        }
        String normalizedFormat = targetFormat.toUpperCase().trim();
        if (!AUDIO_FORMATS.contains(normalizedFormat)) {
            throw new IllegalArgumentException("Unsupported audio format: " + targetFormat);
        }

        MultimediaObject sourceMedia = new MultimediaObject(inputFile);
        EncodingAttributes encodingSettings = createQualityPreservingSettings(sourceMedia, normalizedFormat);

        Encoder encoder = new Encoder();
        encoder.encode(sourceMedia, outputFile, encodingSettings);
    }

    /**
     * Builds encoding settings that preserve the original audio quality.
     *
     * @param sourceMedia  multimedia object of the source file
     * @param targetFormat normalized target format string
     * @return configured encoding attributes
     * @throws EncoderException if retrieving media info fails
     */
    private static EncodingAttributes createQualityPreservingSettings(
            MultimediaObject sourceMedia, String targetFormat) throws EncoderException {
        EncodingAttributes encodingSettings = new EncodingAttributes();
        MultimediaInfo originalInfo = sourceMedia.getInfo();

        AudioAttributes audioSettings = createQualityPreservingAudioSettings(originalInfo, targetFormat);
        encodingSettings.setAudioAttributes(audioSettings);
        encodingSettings.setOutputFormat(targetFormat.toLowerCase());
        return encodingSettings;
    }

    /**
     * Configures audio encoding settings to match or improve the original file's quality.
     *
     * @param originalInfo multimedia info of the source file
     * @param audioFormat  normalized target audio format
     * @return audio attributes configured for quality preservation
     */
    private static AudioAttributes createQualityPreservingAudioSettings(
            MultimediaInfo originalInfo, String audioFormat) {
        AudioAttributes audioSettings = new AudioAttributes();
        audioSettings.setCodec(getAudioCodec(audioFormat));

        AudioInfo originalAudio = originalInfo.getAudio();
        if (originalAudio != null) {
            Integer originalBitrate = originalAudio.getBitRate();
            audioSettings.setBitRate(originalBitrate >= MIN_AUDIO_BITRATE ? originalBitrate : FALLBACK_AUDIO_BITRATE);

            Integer sampleRate = originalAudio.getSamplingRate();
            audioSettings.setSamplingRate(sampleRate);

            Integer channels = originalAudio.getChannels();
            audioSettings.setChannels(channels);
        } else {
            audioSettings.setBitRate(FALLBACK_AUDIO_BITRATE);
            audioSettings.setSamplingRate(FALLBACK_AUDIO_SAMPLE_RATE);
            audioSettings.setChannels(FALLBACK_AUDIO_CHANNELS);
        }
        return audioSettings;
    }

    /**
     * Returns a suitable codec name for the given audio format.
     *
     * @param audioFormat normalized audio format string
     * @return codec identifier for encoding
     * @throws IllegalArgumentException if format is unsupported
     */
    private static String getAudioCodec(String audioFormat) {
        return switch (audioFormat.toUpperCase()) {
            case "MP3" -> "libmp3lame";
            case "AAC" -> "aac";
            case "FLAC" -> "flac";
            case "OGG" -> "libvorbis";
            case "WAV" -> "pcm_s16le";
            default -> throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
        };
    }
}