package test.truinconv.converters;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;
import java.util.Set;

/**
 * High-quality video converter that preserves original quality.
 * <p>
 * Supported video formats: MP4, AVI, MKV, MOV, WEBM
 */
public final class VideoConverter {
    private static final int FALLBACK_AUDIO_BITRATE      = 192000;
    private static final int FALLBACK_AUDIO_SAMPLE_RATE  = 48000;
    private static final int FALLBACK_AUDIO_CHANNELS     = 2;
    private static final int FALLBACK_VIDEO_BITRATE      = 3000000;
    private static final int FALLBACK_VIDEO_FRAME_RATE   = 30;
    private static final int MIN_VIDEO_BITRATE           = 1500000;
    private static final int MAX_VIDEO_BITRATE           = 20000000;
    private static final int MIN_AUDIO_BITRATE           = 128000;
    private static final int WEBM_MIN_BITRATE            = 1000000;
    private static final int WEBM_MAX_BITRATE            = 8000000;
    private static final Set<String> VIDEO_FORMATS = Set.of("MP4", "AVI", "MKV", "MOV", "WEBM");

    // Prevent instantiation
    private VideoConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts a video file to the specified format while preserving original quality.
     *
     * @param inputFile    source video file
     * @param outputFile   destination file for converted video
     * @param targetFormat desired output format (case-insensitive)
     * @throws EncoderException if conversion fails
     */
    public static void convert(File inputFile, File outputFile, String targetFormat) throws EncoderException {
        if (inputFile == null || outputFile == null || targetFormat == null) {
            throw new IllegalArgumentException("Input file, output file, and target format cannot be null");
        }
        String normalizedFormat = targetFormat.toUpperCase().trim();
        if (!VIDEO_FORMATS.contains(normalizedFormat)) {
            throw new IllegalArgumentException("Unsupported video format: " + targetFormat);
        }

        MultimediaObject sourceMedia = new MultimediaObject(inputFile);
        EncodingAttributes encodingSettings = createQualityPreservingSettings(sourceMedia, normalizedFormat);

        Encoder encoder = new Encoder();
        encoder.encode(sourceMedia, outputFile, encodingSettings);
    }

    /**
     * Builds encoding settings that preserve the original file's quality.
     *
     * @param sourceMedia   multimedia object of the source file
     * @param targetFormat  normalized format string
     * @return configured encoding attributes
     * @throws EncoderException if media info retrieval fails
     */
    private static EncodingAttributes createQualityPreservingSettings(
            MultimediaObject sourceMedia, String targetFormat) throws EncoderException {
        EncodingAttributes encodingSettings = new EncodingAttributes();
        MultimediaInfo originalInfo = sourceMedia.getInfo();

        VideoAttributes videoSettings = createQualityPreservingVideoSettings(originalInfo, targetFormat);
        encodingSettings.setVideoAttributes(videoSettings);

        if (originalInfo.getAudio() != null) {
            AudioAttributes audioSettings = createQualityPreservingAudioSettings(originalInfo);
            encodingSettings.setAudioAttributes(audioSettings);
        }

        encodingSettings.setOutputFormat(targetFormat.toLowerCase());
        return encodingSettings;
    }

    /**
     * Configures video encoding settings based on the original video info.
     *
     * @param originalInfo multimedia info of the source file
     * @param videoFormat  target video format
     * @return video attributes configured for quality preservation
     */
    private static VideoAttributes createQualityPreservingVideoSettings(
            MultimediaInfo originalInfo, String videoFormat) {
        VideoAttributes videoSettings = new VideoAttributes();
        videoSettings.setCodec(getVideoCodec(videoFormat));

        VideoInfo originalVideo = originalInfo.getVideo();
        if (originalVideo != null) {
            VideoSize originalSize = originalVideo.getSize();
            if (originalSize != null) {
                videoSettings.setSize(originalSize);
            }
            int safeBitrate = calculateSafeBitrate(originalVideo, videoFormat);
            videoSettings.setBitRate(safeBitrate);

            Float originalFrameRate = originalVideo.getFrameRate();
            if (originalFrameRate > 0 && originalFrameRate <= 60) {
                videoSettings.setFrameRate(Math.round(originalFrameRate));
            } else {
                videoSettings.setFrameRate(FALLBACK_VIDEO_FRAME_RATE);
            }
        } else {
            videoSettings.setBitRate(FALLBACK_VIDEO_BITRATE);
            videoSettings.setFrameRate(FALLBACK_VIDEO_FRAME_RATE);
        }

        return videoSettings;
    }

    /**
     * Configures audio encoding settings based on the original audio info.
     *
     * @param originalInfo multimedia info of the source file
     * @return audio attributes configured for quality preservation
     */
    private static AudioAttributes createQualityPreservingAudioSettings(MultimediaInfo originalInfo) {
        AudioAttributes audioSettings = new AudioAttributes();
        audioSettings.setCodec("aac");

        AudioInfo originalAudio = originalInfo.getAudio();
        if (originalAudio != null) {
            Integer originalBitrate = originalAudio.getBitRate();
            audioSettings.setBitRate(
                    originalBitrate >= MIN_AUDIO_BITRATE ? originalBitrate : FALLBACK_AUDIO_BITRATE
            );
            audioSettings.setSamplingRate(originalAudio.getSamplingRate());
            audioSettings.setChannels(originalAudio.getChannels());
        } else {
            audioSettings.setBitRate(FALLBACK_AUDIO_BITRATE);
            audioSettings.setSamplingRate(FALLBACK_AUDIO_SAMPLE_RATE);
            audioSettings.setChannels(FALLBACK_AUDIO_CHANNELS);
        }

        return audioSettings;
    }

    /**
     * Determines a safe video bitrate within defined thresholds.
     *
     * @param originalVideo info about the original video track
     * @param videoFormat   target format (e.g., WEBM applies special limits)
     * @return clamped bitrate value
     */
    private static int calculateSafeBitrate(VideoInfo originalVideo, String videoFormat) {
        int originalBitrate = originalVideo.getBitRate();
        if (originalBitrate <= 0) {
            return FALLBACK_VIDEO_BITRATE;
        }

        int minBitrate = MIN_VIDEO_BITRATE;
        int maxBitrate = MAX_VIDEO_BITRATE;
        if ("WEBM".equalsIgnoreCase(videoFormat)) {
            minBitrate = WEBM_MIN_BITRATE;
            maxBitrate = WEBM_MAX_BITRATE;
        }
        return Math.max(minBitrate, Math.min(maxBitrate, originalBitrate));
    }

    /**
     * Selects an appropriate codec for the given format.
     *
     * @param videoFormat target format identifier
     * @return codec name for encoding
     */
    private static String getVideoCodec(String videoFormat) {
        return switch (videoFormat.toUpperCase()) {
            case "MP4", "AVI", "MKV", "MOV" -> "libx264";
            case "WEBM" -> "libvpx";
            default -> throw new IllegalArgumentException("Unsupported video format: " + videoFormat);
        };
    }
}