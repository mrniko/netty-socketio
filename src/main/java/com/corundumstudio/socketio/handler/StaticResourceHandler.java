/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler;

import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.activation.MimetypesFileTypeMap;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class StaticResourceHandler extends SimpleChannelUpstreamHandler {
	
	private static Logger log = LoggerFactory.getLogger(StaticResourceHandler.class);

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    
    private static File defaultTempDir;

	private String webappDir;

	public StaticResourceHandler(String webappDir) {
		this.webappDir = webappDir;
	}
	
	/**
	 * Sets the temporary directory where the static content will be extracted. <br/>
	 * Should only be used when you are hosting your files inside the JAR.
	 * By default it will be the temporary folder of the OS. In android it is necessary 
	 * to manually set the value: context.getCacheDir ();
	 */
	public static void setDefaultTempDir(File defaultTempDir) {
		StaticResourceHandler.defaultTempDir = defaultTempDir;
	}

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
        	
        	HttpRequest req = (HttpRequest) msg;
        	
    		if(req.getUri().startsWith("/socket.io")){
    			ctx.sendUpstream(e);
    			return;
    		}
        	
            // QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            
    		String filePath = getFilePathToUri(req.getUri());
    		
            if (filePath != null && filePath.trim().length() > 0) {
            	
            	File resource = new File(filePath);
            	
            	log.debug("serving: "+ resource + " ("+resource.exists()+")");
            	
            	
                HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

                if (isNotModified(req, resource)) {
                    sendNotModified(ctx);
                    return;
                }

                RandomAccessFile raf;
                try {
                    raf = new RandomAccessFile(resource, "r");
                } catch (FileNotFoundException fnfe) {
                    sendError(ctx, NOT_FOUND);
                    return;
                }
                long fileLength = raf.length();

                setContentLength(res, fileLength);
                setContentTypeHeader(res, resource);
                setDateAndCacheHeaders(res, resource);
                writeContent(raf, fileLength, e.getChannel());
                return;
            }else{
                sendError(ctx, NOT_FOUND);
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private boolean isNotModified(HttpRequest request, File file) throws ParseException {
        String ifModifiedSince = request.getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client does
            // not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            return ifModifiedSinceDateSeconds == fileLastModifiedSeconds;
        }
        return false;
    }

    private void sendNotModified(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    private void setDateHeader(HttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
    }

    private void writeContent(RandomAccessFile raf, long fileLength, Channel ch) throws IOException {
        ChannelFuture writeFuture;
        if (ch.getPipeline().get(SslHandler.class) != null) {
            // Cannot use zero-copy with HTTPS.
            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
        } else {
            // No encryption - use zero-copy.
            final FileRegion region =
                new DefaultFileRegion(raf.getChannel(), 0, fileLength);
            writeFuture = ch.write(region);
            writeFuture.addListener(new ChannelFutureProgressListener() {
                public void operationComplete(ChannelFuture future) {
                    region.releaseExternalResources();
                }

                @Override
                public void operationProgressed(ChannelFuture future, long amount, long current, long total)
                        throws Exception {
                }

            });
        }

        writeFuture.addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n",
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(
                HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    private void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }
    
	private String getFilePathToUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".")
				|| uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Serve index file
		if (uri.endsWith("/")) {
			uri = "index.html";
		}
		
		StringBuffer path = new StringBuffer();
		
		if(webappDir != null && webappDir.trim().length() != 0){
			
			// Absolute file path, like /var/www/mywebapp or c:\webapp
			if(webappDir.startsWith("/") || isWindowsPath(webappDir)){
				path.append(webappDir).append(File.separator).append(uri);
			
			// Embedded into jar	
			}else if(webappDir.startsWith("jar:")){
				
				String basedir = webappDir.replaceAll("jar:", ""); // remove 'jar:' prefix
				String jarPath = basedir + (!uri.startsWith("/") ? "/" : "") + uri; // mount relative path
				
				ClassLoader loader = this.getClass().getClassLoader();
				URL resource = loader.getResource(jarPath);
				
				if(resource != null){
						
					try {
						
						String name = new File(resource.getFile()).getName();
						File tempFile = null;
						
						// Static temp folder to extract files..
						if(defaultTempDir != null){
							
							tempFile = new File(defaultTempDir, uri);
							
							tempFile.getParentFile().mkdirs();
							
							if(!tempFile.exists()) {
								log.debug("Extracting file: " + tempFile);
								copyFromJarToFile(resource, tempFile);
							}
						// Use temp folder of S.O
						// NOTE: use the same logic as above ??
						}else{
							tempFile = File.createTempFile("webapp_static_", "_"+name);
							
							if(!tempFile.exists())
								copyFromJarToFile(resource, tempFile);						
						}
						
						path.append(tempFile.getAbsolutePath());
						
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						return null;
					}

				}else{
					return null;
				}
				
			// Relative to current dir.	
			}else{
				String current = System.getProperty("user.dir");
				path.append(current).append(File.separator).append(webappDir).append(File.separator).append(uri);
			}
		
		}else{
			String current = System.getProperty("user.dir");
			path.append(current).append(File.separator).append(uri);
		}
		
		// Convert to absolute path.
		return path.toString();
	}
	
	private File copyFromJarToFile(URL url, File f) throws Exception {
	    byte[] buffer = new byte[1024];
	    int bytesRead;
	 
	    BufferedInputStream inputStream = null;
	    BufferedOutputStream outputStream = null;
	    URLConnection connection = url.openConnection();
	    // If you need to use a proxy for your connection, the URL class has another openConnection method.
	    // For example, to connect to my local SOCKS proxy I can use:
	    // url.openConnection(new Proxy(Proxy.Type.SOCKS, newInetSocketAddress("localhost", 5555)));
	    inputStream = new BufferedInputStream(connection.getInputStream());
	    outputStream = new BufferedOutputStream(new FileOutputStream(f));
	    while ((bytesRead = inputStream.read(buffer)) != -1) {
	      outputStream.write(buffer, 0, bytesRead);
	    }
	    inputStream.close();
	    outputStream.close();
	    return f;
	  }
	
	private static boolean isWindowsPath(String text)
	{
	    return text.matches("^\\D:\\\\.*?");
	}
}
