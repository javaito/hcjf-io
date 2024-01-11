package org.hcjf.io.net.http;

import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;

/**
 * This class represents a http response package.
 * @author javaito
 *
 */
public class HttpResponse extends HttpPackage {

    private static final int VERSION_INDEX = 0;
    private static final int RESPONSE_CODE_INDEX = 1;

    private Integer responseCode;
    private String reasonPhrase;

    public HttpResponse() {
        this.responseCode = HttpResponseCode.OK;
        this.reasonPhrase = HttpResponseCode.DefaultPhrase.getDefaultPhrase(HttpResponseCode.OK);
    }

    protected HttpResponse(HttpResponse httpResponse) {
        super(httpResponse);
        this.responseCode = httpResponse.responseCode;
        this.reasonPhrase = httpResponse.reasonPhrase;
    }

    /**
     * Return the numeric code that represents the status of the http request.
     * @return Response code.
     */
    public Integer getResponseCode() {
        return responseCode;
    }

    /**
     * Set the numeric code that represents the status of the http request.
     * @param responseCode Response code.
     */
    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
        this.reasonPhrase = HttpResponseCode.DefaultPhrase.getDefaultPhrase(responseCode);
    }

    /**
     * Return a phrase that represents why the server response with this package.
     * @return Reason phrase.
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Set a phrase that represents why the server response with this package.
     * @param reasonPhrase Reason phrase.
     */
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * This kind of http package never process his body.
     */
    @Override
    protected void processBody() {}

    /**
     * Set the values of the first line of the package.
     * @param firstLine String representation of the firs line.
     */
    @Override
    protected void processFirstLine(String firstLine) {
        String[] parts = firstLine.split(LINE_FIELD_SEPARATOR);

        if(parts.length == 2) {
            setResponseCode(Integer.parseInt(parts[RESPONSE_CODE_INDEX].trim()));
            setHttpVersion(parts[VERSION_INDEX]);
        } if(parts.length >= 3) {
            setResponseCode(Integer.parseInt(parts[RESPONSE_CODE_INDEX].trim()));
            setHttpVersion(parts[VERSION_INDEX]);
            StringBuilder reasonPhraseBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                reasonPhraseBuilder.append(parts[i]);
            }
            setReasonPhrase(reasonPhraseBuilder.toString());
        }
    }

    /**
     * Return the string representation of the package header.
     * @return String representation of the package header.
     */
    private String toStringProtocolHeader() {
        StringBuilder builder = new StringBuilder();

        builder.append(getHttpVersion()).append(LINE_FIELD_SEPARATOR);
        builder.append(getResponseCode()).append(LINE_FIELD_SEPARATOR);
        builder.append(getReasonPhrase() == null ? "" : getReasonPhrase()).append(STRING_LINE_SEPARATOR);
        for(HttpHeader header : getHeaders()) {
            builder.append(header).append(STRING_LINE_SEPARATOR);
        }
        for(Cookie cookie : getCookies()) {
            builder.append(cookie instanceof Cookie2 ? HttpHeader.SET_COOKIE2.toString() : HttpHeader.SET_COOKIE.toString());
            builder.append(": ").append(cookie).append(STRING_LINE_SEPARATOR);
        }
        builder.append(STRING_LINE_SEPARATOR);
        return builder.toString();
    }

    /**
     * Return the byte array that represents the http package header.
     * @return Byte array.
     */
    @Override
    public byte[] getProtocolHeader() {
        return toStringProtocolHeader().getBytes();
    }

    /**
     * Create the standard representation of the http response package.
     * @return String representation of the package
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(toStringProtocolHeader());
        if(getBody() != null) {
            int maxLength = SystemProperties.getInteger(IOSystemProperties.Net.Http.OUTPUT_LOG_BODY_MAX_LENGTH);
            if(maxLength > 0) {
                if (getBody().length > maxLength) {
                    builder.append(new String(getBody(), 0, maxLength));
                    builder.append(" ... [").append(getBody().length - maxLength).append(" more]");
                } else {
                    String s = new String(getBody());
                    builder.append(new String(getBody()));
                }
            }
        }

        return builder.toString();
    }
}
