package org.hcjf.io.net.http;

import org.hcjf.errors.HCJFRuntimeException;
import org.hcjf.io.net.NetClient;
import org.hcjf.io.net.NetPackage;
import org.hcjf.io.net.NetService;
import org.hcjf.io.net.NetSession;
import org.hcjf.io.net.ssl.SslClient;
import org.hcjf.io.net.ssl.SslPeer;
import org.hcjf.log.Log;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.UUID;

/**
 * Client to invoke some http server.
 * @author javaito
 */
public class HttpClient extends NetClient<HttpSession, HttpPackage> {

    public static final String HTTP_CLIENT_LOG_TAG = "HTTP_CLIENT";

    private static final String CONNECTION_TIMEOUT_MESSAGE = "Connection timeout";
    private static final String READ_TIMEOUT_MESSAGE = "Read timeout";
    private static final String DISCONNECTION_MESSAGE = "Http client request end";
    private static final String SESSION_NAME = "Http client session";

    private final URL url;
    private Status status;
    private HttpResponse response;
    private HttpRequest request;
    private Long connectTimeout;
    private Long writeTimeout;
    private Long readTimeout;
    private HttpSession session;
    private HttpPackage.HttpProtocol httpProtocol;
    private Boolean httpsInsecureConnection;
    private HttpResponseHandler responseHandler;
    private final Object connectionMonitor = new Object();
    private final Object readMonitor = new Object();
    private SslClient sslClient;

    public HttpClient(URL url) {
        super(url.getHost(), url.getPort() != -1 ? url.getPort() :
                    url.getProtocol().equalsIgnoreCase(HttpPackage.HttpProtocol.HTTPS.toString()) ?
                            SystemProperties.getInteger(IOSystemProperties.Net.Https.DEFAULT_CLIENT_PORT) :
                            SystemProperties.getInteger(IOSystemProperties.Net.Http.DEFAULT_CLIENT_PORT),
                    url.getProtocol().equalsIgnoreCase(HttpPackage.HttpProtocol.HTTPS.toString()) ?
                            NetService.TransportLayerProtocol.TCP_SSL :
                            NetService.TransportLayerProtocol.TCP);
        this.url = url;
        this.connectTimeout = SystemProperties.getLong(IOSystemProperties.Net.Http.DEFAULT_CLIENT_CONNECT_TIMEOUT);
        this.writeTimeout = SystemProperties.getLong(IOSystemProperties.Net.Http.DEFAULT_CLIENT_WRITE_TIMEOUT);
        this.readTimeout = SystemProperties.getLong(IOSystemProperties.Net.Http.DEFAULT_CLIENT_READ_TIMEOUT);
        this.httpProtocol = url.getProtocol().equalsIgnoreCase(HttpPackage.HttpProtocol.HTTPS.toString()) ?
                HttpPackage.HttpProtocol.HTTPS : HttpPackage.HttpProtocol.HTTP;
        this.httpsInsecureConnection = false;
        init();
    }

    private void init() {
        //Init defaults
        this.response = null;
        this.status = Status.INACTIVE;

        //Create default request
        request = new HttpRequest();
        request.setProtocol(httpProtocol);
        request.setHttpVersion(HttpVersion.VERSION_1_1);
        request.setContext(url.getFile());
        request.setMethod(HttpMethod.GET);
        request.setBody(new byte[0]);
        request.addHeader(new HttpHeader(HttpHeader.HOST, url.getHost()));
        request.addHeader(new HttpHeader(HttpHeader.USER_AGENT, HttpHeader.DEFAULT_USER_AGENT));
    }

    public HttpResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(HttpResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Return the connection timeout value.
     * @return Connection timeout vaue.
     */
    public final Long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the connection timeout value.
     * @param connectTimeout Connection timeout value.
     */
    public final void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Return the write timeout value.
     * @return Write timeout value.
     */
    public final Long getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Set the write timeout value.
     * @param writeTimeout Write timeout value.
     */
    public final void setWriteTimeout(Long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    /**
     * Return the read timeout value.
     * @return Read timeout value.
     */
    public final Long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set the read timeout value.
     * @param readTimeout Read timeout value.
     */
    public final void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Add path over base context of the constructor URI.
     * @param paths Adding paths.
     */
    public void addPath(String... paths) {
        StringBuilder newPath = new StringBuilder();
        newPath.append(this.request.getContext());
        String separator = this.request.getContext().endsWith(
                HttpPackage.HTTP_CONTEXT_SEPARATOR) ? "" : HttpPackage.HTTP_CONTEXT_SEPARATOR;
        for(String path : paths) {
            newPath.append(separator).append(path);
            separator = HttpPackage.HTTP_CONTEXT_SEPARATOR;
        }
        this.request.setContext(newPath.toString());
    }

    /**
     * Return all the internal fields to the default values
     * in order to reuse this client.
     */
    public final void reset() {
        init();
    }

    /**
     * Set the request body.
     * @param body Request body.
     */
    public void setBody(byte[] body) {
        request.setBody(body);
    }

    /**
     * Set the http method to request.
     * @param method Http method.
     */
    public final void setHttpMethod(HttpMethod method) {
        request.setMethod(method);
    }

    /**
     * Add header to request.
     * @param header Http header.
     */
    public final void addHttpHeader(String header) {
        request.addHeader(new HttpHeader(header));
    }

    /**
     * Add header to request.
     * @param header Http header.
     */
    public final void addHttpHeader(HttpHeader header) {
        request.addHeader(header);
    }

    /**
     * Returns if the https connection is insecure or not.
     * @return Secure https connection
     */
    public Boolean isHttpsInsecureConnection() {
        return httpsInsecureConnection;
    }

    /**
     * Set if the https connection is insecure of not.
     * @param httpsInsecureConnection Secure https connection.
     */
    public void setHttpsInsecureConnection(Boolean httpsInsecureConnection) {
        this.httpsInsecureConnection = httpsInsecureConnection;
    }

    /**
     * Creates the SSL engine.
     * @return SSL engine instance.
     */
    @Override
    protected synchronized SslPeer getSslPeer() {
        if (sslClient == null) {
            try {
                sslClient = new SslClient("TLSv1.2", url.getHost(), url.getPort());
            } catch (Exception ex) {
                throw new HCJFRuntimeException("Ssl client fail", ex);
            }
        }
        return sslClient;
    }

    /**
     * This method return the object that represent the
     * client's session.
     * @return Client's session.
     */
    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public HttpSession checkSession(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        return session;
    }

    /**
     * This method decode the implementation data.
     * @param payLoad Implementation data.
     * @return Implementation data encoded.
     */
    @Override
    protected byte[] encode(HttpPackage payLoad) {
        byte[] result = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(payLoad.getProtocolHeader());
            if(payLoad.getBody() != null) {
                out.write(payLoad.getBody());
            }
            out.flush();
            result = out.toByteArray();
        } catch (Exception ex){}
        return result;
    }

    /**
     * This method decode the net package to obtain the implementation data
     * @param netPackage Net package.
     * @return Return the implementation data.
     */
    @Override
    protected HttpPackage decode(NetPackage netPackage) {
        if(response == null) {
            response = new HttpResponse();
            response.setProtocol(httpProtocol);
            response.setTransferDecodingLayer(responseHandler);
        }
        response.addData(netPackage.getPayload());
        return response;
    }

    /**
     * Destroy the session.
     * @param session Net session to be destroyed
     */
    @Override
    public void destroySession(NetSession session) {
        session = null;
    }

    /**
     *
     * @param session Connected session.
     * @param payLoad Decoded package payload.
     * @param netPackage Original package.
     */
    @Override
    protected void onConnect(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        synchronized (connectionMonitor) {
            if(status == Status.CONNECTING) {
                status = Status.CONNECTED;
                connectionMonitor.notifyAll();
            }
        }
    }

    @Override
    protected void onDisconnect(HttpSession session, NetPackage netPackage) {
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     *
     * @param session Net session.
     * @param payLoad Net package decoded
     * @param netPackage Net package.
     */
    @Override
    protected final void onRead(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        if(getResponseHandler() == null) {
            if (response.isComplete()) {
                synchronized (readMonitor) {
                    status = Status.DONE;
                    readMonitor.notifyAll();
                }
            }
        }
    }

    /**
     * This method execute all the steps to do a http request. Creates the connection,
     * sends the request package and reads the response, then this response
     * is returned as a method response.
     * @return Http response package.
     */
    public final HttpResponse request() {
        long time = System.currentTimeMillis();
        Integer errorCode = null;

        //Connection block
        status = Status.CONNECTING;
        synchronized (connectionMonitor) {
            for (int i = 0; i < 3; i++) {
                session = new HttpSession(UUID.randomUUID(), this);
                session.setRequest(request);
                session.setSessionName(SESSION_NAME);
                connect();
                if (status == Status.CONNECTING) {
                    try {
                        connectionMonitor.wait(getConnectTimeout());
                    } catch (InterruptedException e) {
                    }
                }

                if (status == Status.CONNECTING) {
                    try {
                        disconnect(getSession(), DISCONNECTION_MESSAGE);
                        destroySession(getSession());
                    } catch (Exception ex){}
                } else {
                    break;
                }
            }

            if(status == Status.CONNECTING) {
                status = Status.ERROR;
                errorCode = HttpResponseCode.REQUEST_TIMEOUT;
            }
        }

        //Request writing request and read response block
        if(status != Status.ERROR) {
            status = Status.WRITING;

            Log.out(HTTP_CLIENT_LOG_TAG, "Request\r\n%s", request.toString());
            try {
                write(getSession(), request, true);
            } catch (Exception ex) {
                status = Status.ERROR;
                errorCode = HttpResponseCode.BAD_REQUEST;
            }

            if(getResponseHandler() == null) {
                synchronized (readMonitor) {
                    if (status == Status.WRITING) {
                        try {
                            readMonitor.wait(getWriteTimeout());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (status == Status.WRITING) {
                        status = Status.ERROR;
                        errorCode = HttpResponseCode.REQUEST_TIMEOUT;
                    }
                }
            }
        }

        HttpResponse response;
        if(status == Status.ERROR) {
            response = new HttpResponse();
            response.setHttpVersion(HttpVersion.VERSION_1_1);
            response.setResponseCode(errorCode);
        } else {
            response = this.response;
        }

        if(getResponseHandler() == null) {
            disconnect(getSession(), DISCONNECTION_MESSAGE);
        }

        Log.in(HTTP_CLIENT_LOG_TAG, "Response -> [Time: %d ms]\r\n%s",
                (System.currentTimeMillis() - time), response.toString());
        return response;
    }

    public final HttpResponse asyncRequest(HttpResponseHandler responseHandler) {
        setResponseHandler(responseHandler);

        return request();
    }

    /**
     * Clietn request status.
     */
    private enum Status {

        INACTIVE,

        CONNECTING,

        CONNECTED,

        WRITING,

        DONE,

        ERROR;

    }
}
