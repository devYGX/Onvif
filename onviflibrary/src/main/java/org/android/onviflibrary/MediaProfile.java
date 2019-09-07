package org.android.onviflibrary;

public class MediaProfile {
    public static final int UNKNOWN = 0;
    public static final int SUBSTREAM = 1;
    public static final int MAINSTREAM = 2;

    private int streamType;
    private String token;
    private String name;
    private String mediaStreamUri;
    private String onvifMediaStreamUri;
    private VideoEncoderConfiguration videoEncoderConfiguration;

    public String getOnvifMediaStreamUri() {
        return onvifMediaStreamUri;
    }

    public void setOnvifMediaStreamUri(String onvifMediaStreamUri) {
        this.onvifMediaStreamUri = onvifMediaStreamUri;
    }

    public int getStreamType() {
        return streamType;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    public static class VideoEncoderConfiguration{
        private String token;
        private String name;
        private String enoding;
        private VideoResolution resolution;
        private VideoRateControl videoRateControl;

        public VideoEncoderConfiguration() {

        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEnoding() {
            return enoding;
        }

        public void setEnoding(String enoding) {
            this.enoding = enoding;
        }

        public VideoResolution getResolution() {
            return resolution;
        }

        public void setResolution(VideoResolution resolution) {
            this.resolution = resolution;
        }

        public VideoRateControl getVideoRateControl() {
            return videoRateControl;
        }

        public void setVideoRateControl(VideoRateControl videoRateControl) {
            this.videoRateControl = videoRateControl;
        }

        @Override
        public String toString() {
            return "VideoEncoderConfiguration{" +
                    "token='" + token + '\'' +
                    ", name='" + name + '\'' +
                    ", enoding='" + enoding + '\'' +
                    ", resolution=" + resolution +
                    ", videoRateControl=" + videoRateControl +
                    '}';
        }
    }

    public static class VideoResolution{
        private int width;
        private int height;


        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "VideoResolution{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    public static class VideoRateControl{
        private int frameRateLimit;
        private int encodingInterval;
        private int bitrateLimit;

        public int getFrameRateLimit() {
            return frameRateLimit;
        }

        public void setFrameRateLimit(int frameRateLimit) {
            this.frameRateLimit = frameRateLimit;
        }

        public int getEncodingInterval() {
            return encodingInterval;
        }

        public void setEncodingInterval(int encodingInterval) {
            this.encodingInterval = encodingInterval;
        }

        public int getBitrateLimit() {
            return bitrateLimit;
        }

        public void setBitrateLimit(int bitrateLimit) {
            this.bitrateLimit = bitrateLimit;
        }

        @Override
        public String toString() {
            return "VideoRateControl{" +
                    "frameRateLimit=" + frameRateLimit +
                    ", encodingInterval=" + encodingInterval +
                    ", bitrateLimit=" + bitrateLimit +
                    '}';
        }
    }


    public VideoEncoderConfiguration getVideoEncoderConfiguration() {
        return videoEncoderConfiguration;
    }

    public void setVideoEncoderConfiguration(VideoEncoderConfiguration videoEncoderConfiguration) {
        this.videoEncoderConfiguration = videoEncoderConfiguration;
    }

    public String getMediaStreamUri() {
        return mediaStreamUri;
    }

    public void setMediaStreamUri(String mediaStreamUri) {
        this.mediaStreamUri = mediaStreamUri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MediaProfile{" +
                "streamType=" + streamType +
                ", token='" + token + '\'' +
                ", name='" + name + '\'' +
                ", mediaStreamUri='" + mediaStreamUri + '\'' +
                ", onvifMediaStreamUri='" + onvifMediaStreamUri + '\'' +
                ", videoEncoderConfiguration=" + videoEncoderConfiguration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaProfile that = (MediaProfile) o;
        return mediaStreamUri.equals(that.mediaStreamUri);
    }
}
