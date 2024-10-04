package com.corundumstudio.socketio;

public class HttpRequestDecoderConfiguration {

    private int maxInitialLineLength;
    private int maxHeaderSize;
    private int maxChunkSize;

    public HttpRequestDecoderConfiguration(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
        this.maxChunkSize = maxChunkSize;
    }

    public HttpRequestDecoderConfiguration() {
        this(4096, 8192, 8192);
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public void setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }
}
