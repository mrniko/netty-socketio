package com.corundumstudio.socketio;

import java.util.Arrays;
import java.util.List;

/**
 * Basic configuration class, contains only primitive, String and enum types
 * as fields. Used as a base class for Configuration
 * and for extends context configuration in other modules like spring-boot, etc.
 */
public abstract class BasicConfiguration {
    protected String context = "/socket.io";

    protected List<Transport> transports = Arrays.asList(Transport.WEBSOCKET, Transport.POLLING);

    protected int bossThreads = 0; // 0 = current_processors_amount * 2
    protected int workerThreads = 0; // 0 = current_processors_amount * 2
    protected boolean useLinuxNativeEpoll;

    protected boolean allowCustomRequests = false;

    protected int upgradeTimeout = 10000;
    protected int pingTimeout = 60000;
    protected int pingInterval = 25000;
    protected int firstDataTimeout = 5000;

    protected int maxHttpContentLength = 64 * 1024;
    protected int maxFramePayloadLength = 64 * 1024;

    protected String packagePrefix;
    protected String hostname;
    protected int port = -1;

    protected String allowHeaders;

    protected boolean preferDirectBuffer = true;

    protected AckMode ackMode = AckMode.AUTO_SUCCESS_ONLY;

    protected boolean addVersionHeader = true;

    protected String origin;

    protected boolean enableCors = true;

    protected boolean httpCompression = true;

    protected boolean websocketCompression = true;

    protected boolean randomSession = false;

    protected boolean needClientAuth = false;

    protected BasicConfiguration() {
    }

    protected BasicConfiguration(BasicConfiguration conf) {
        setBossThreads(conf.getBossThreads());
        setWorkerThreads(conf.getWorkerThreads());
        setUseLinuxNativeEpoll(conf.isUseLinuxNativeEpoll());

        setPingInterval(conf.getPingInterval());
        setPingTimeout(conf.getPingTimeout());
        setFirstDataTimeout(conf.getFirstDataTimeout());

        setHostname(conf.getHostname());
        setPort(conf.getPort());

        setContext(conf.getContext());
        setAllowCustomRequests(conf.isAllowCustomRequests());

        setTransports(conf.getTransports().toArray(new Transport[0]));
        setMaxHttpContentLength(conf.getMaxHttpContentLength());
        setPackagePrefix(conf.getPackagePrefix());

        setPreferDirectBuffer(conf.isPreferDirectBuffer());
        setAckMode(conf.getAckMode());
        setMaxFramePayloadLength(conf.getMaxFramePayloadLength());
        setUpgradeTimeout(conf.getUpgradeTimeout());

        setAddVersionHeader(conf.isAddVersionHeader());
        setOrigin(conf.getOrigin());
        setEnableCors(conf.isEnableCors());
        setAllowHeaders(conf.getAllowHeaders());

        setHttpCompression(conf.isHttpCompression());
        setWebsocketCompression(conf.isWebsocketCompression());
        setRandomSession(conf.randomSession);
        setNeedClientAuth(conf.isNeedClientAuth());
    }

    public String getHostname() {
        return hostname;
    }

    /**
     * Optional parameter. If not set then bind address
     * will be 0.0.0.0 or ::0
     *
     * @param hostname - name of host
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    /**
     * Ping interval
     *
     * @param heartbeatIntervalSecs - time in milliseconds
     */
    public void setPingInterval(int heartbeatIntervalSecs) {
        this.pingInterval = heartbeatIntervalSecs;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    /**
     * Ping timeout
     * Use <code>0</code> to disable it
     *
     * @param heartbeatTimeoutSecs - time in milliseconds
     */
    public void setPingTimeout(int heartbeatTimeoutSecs) {
        this.pingTimeout = heartbeatTimeoutSecs;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public boolean isHeartbeatsEnabled() {
        return pingTimeout > 0;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isAllowCustomRequests() {
        return allowCustomRequests;
    }

    /**
     * Allow to service custom requests differs from socket.io protocol.
     * In this case it's necessary to add own handler which handle them
     * to avoid hang connections.
     * Default is {@code false}
     *
     * @param allowCustomRequests - {@code true} to allow
     */
    public void setAllowCustomRequests(boolean allowCustomRequests) {
        this.allowCustomRequests = allowCustomRequests;
    }

    /**
     * Set maximum http content length limit
     *
     * @param value the maximum length of the aggregated http content.
     */
    public void setMaxHttpContentLength(int value) {
        this.maxHttpContentLength = value;
    }

    public int getMaxHttpContentLength() {
        return maxHttpContentLength;
    }

    /**
     * Transports supported by server
     *
     * @param transports - list of transports
     */
    public void setTransports(Transport... transports) {
        if (transports.length == 0) {
            throw new IllegalArgumentException("Transports list can't be empty");
        }
        this.transports = Arrays.asList(transports);
    }

    public List<Transport> getTransports() {
        return transports;
    }

    /**
     * Package prefix for sending json-object from client
     * without full class name.
     * <p>
     * With defined package prefix socket.io client
     * just need to define '@class: 'SomeType'' in json object
     * instead of '@class: 'com.full.package.name.SomeType''
     *
     * @param packagePrefix - prefix string
     *
     */
    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    /**
     * Buffer allocation method used during packet encoding.
     * Default is {@code true}
     *
     * @param preferDirectBuffer {@code true} if a direct buffer should be tried to be used as target for
     *                           the encoded messages. If {@code false} is used it will allocate a heap
     *                           buffer, which is backed by an byte array.
     */
    public void setPreferDirectBuffer(boolean preferDirectBuffer) {
        this.preferDirectBuffer = preferDirectBuffer;
    }

    public boolean isPreferDirectBuffer() {
        return preferDirectBuffer;
    }

    /**
     * Auto ack-response mode
     * Default is {@code AckMode.AUTO_SUCCESS_ONLY}
     *
     * @param ackMode - ack mode
     * @see AckMode
     */
    public void setAckMode(AckMode ackMode) {
        this.ackMode = ackMode;
    }

    public AckMode getAckMode() {
        return ackMode;
    }

    /**
     * Set maximum websocket frame content length limit
     *
     * @param maxFramePayloadLength - length
     */
    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public int getMaxFramePayloadLength() {
        return maxFramePayloadLength;
    }

    /**
     * Transport upgrade timeout in milliseconds
     *
     * @param upgradeTimeout - upgrade timeout
     */
    public void setUpgradeTimeout(int upgradeTimeout) {
        this.upgradeTimeout = upgradeTimeout;
    }

    public int getUpgradeTimeout() {
        return upgradeTimeout;
    }

    /**
     * Adds <b>Server</b> header with lib version to http response.
     * <p>
     * Default is <code>true</code>
     *
     * @param addVersionHeader - <code>true</code> to add header
     */
    public void setAddVersionHeader(boolean addVersionHeader) {
        this.addVersionHeader = addVersionHeader;
    }

    public boolean isAddVersionHeader() {
        return addVersionHeader;
    }

    /**
     * Set <b>Access-Control-Allow-Origin</b> header value for http each
     * response.
     * Default is <code>null</code>
     * <p>
     * If value is <code>null</code> then request <b>ORIGIN</b> header value used.
     *
     * @param origin - origin
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    /**
     * cors dispose
     * <p>
     * Default is <code>true</code>
     *
     * @param enableCors enableCors
     */
    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }

    public boolean isEnableCors() {
        return enableCors;
    }

    public boolean isUseLinuxNativeEpoll() {
        return useLinuxNativeEpoll;
    }

    public void setUseLinuxNativeEpoll(boolean useLinuxNativeEpoll) {
        this.useLinuxNativeEpoll = useLinuxNativeEpoll;
    }

    /**
     * Set the response Access-Control-Allow-Headers
     *
     * @param allowHeaders - allow headers
     *
     */
    public void setAllowHeaders(String allowHeaders) {
        this.allowHeaders = allowHeaders;
    }

    public String getAllowHeaders() {
        return allowHeaders;
    }

    /**
     * Timeout between channel opening and first data transfer
     * Helps to avoid 'silent channel' attack and prevents
     * 'Too many open files' problem in this case
     *
     * @param firstDataTimeout - timeout value
     */
    public void setFirstDataTimeout(int firstDataTimeout) {
        this.firstDataTimeout = firstDataTimeout;
    }

    public int getFirstDataTimeout() {
        return firstDataTimeout;
    }

    /**
     * Activate http protocol compression. Uses {@code gzip} or
     * {@code deflate} encoding choice depends on the {@code "Accept-Encoding"} header value.
     * <p>
     * Default is <code>true</code>
     *
     * @param httpCompression - <code>true</code> to use http compression
     */
    public void setHttpCompression(boolean httpCompression) {
        this.httpCompression = httpCompression;
    }

    public boolean isHttpCompression() {
        return httpCompression;
    }

    /**
     * Activate websocket protocol compression.
     * Uses {@code permessage-deflate} encoding only.
     * <p>
     * Default is <code>true</code>
     *
     * @param websocketCompression - <code>true</code> to use websocket compression
     */
    public void setWebsocketCompression(boolean websocketCompression) {
        this.websocketCompression = websocketCompression;
    }

    public boolean isWebsocketCompression() {
        return websocketCompression;
    }

    public boolean isRandomSession() {
        return randomSession;
    }

    public void setRandomSession(boolean randomSession) {
        this.randomSession = randomSession;
    }

    /**
     * Enable/disable client authentication.
     * Has no effect unless a trust store has been provided.
     * <p>
     * Default is <code>false</code>
     *
     * @param needClientAuth - <code>true</code> to use client authentication
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }
}
