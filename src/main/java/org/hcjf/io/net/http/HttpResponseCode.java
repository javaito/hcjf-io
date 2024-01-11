package org.hcjf.io.net.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains commons http response codes.
 * @author javaito
 */
public interface HttpResponseCode {

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.1.1
     */
    Integer CONTINUE = 100;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.1.2
     */
    Integer SWITCHING_PROTOCOLS = 101;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.1
     */
    Integer OK = 200;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.2
     */
    Integer CREATED = 201;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.3
     */
    Integer ACCEPTED = 202;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.4
     */
    Integer NON_AUTHORITATIVE_INFOTMATION = 203;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.5
     */
    Integer NO_CONTENT = 204;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.6
     */
    Integer RESET_CONTENT = 205;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.2.7
     */
    Integer PARTIAL_CONTENT = 206;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.1
     */
    Integer MULTIPLE_CHOICES = 300;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.2
     */
    Integer MOVED_PERMANENTLY = 301;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.3
     */
    Integer FOUND = 302;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.4
     */
    Integer SEE_OTHER = 303;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.5
     */
    Integer NOT_MODIFIED = 304;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.6
     */
    Integer USE_PROXY = 305;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.3.8
     */
    Integer TEMPORARY_REDIRECT = 307;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.1
     */
    Integer BAD_REQUEST = 400;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.2
     */
    Integer UNAUTHORIZED = 401;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.3
     */
    Integer PAYMENT_REQUIRED = 402;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.4
     */
    Integer FORBIDDEN = 403;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.5
     */
    Integer NOT_FOUND = 404;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.6
     */
    Integer METHOD_NOT_ALLOWED = 405;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.7
     */
    Integer NOT_ACCEPTABLE = 406;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.9
     */
    Integer REQUEST_TIMEOUT = 408;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.10
     */
    Integer CONFLICT = 409;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.11
     */
    Integer GONE = 410;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.12
     */
    Integer LENGTH_REQUIRED = 411;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.13
     */
    Integer PRECONDITION_FAILED = 412;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.14
     */
    Integer REQUEST_ENTITY_TOO_LARGE = 413;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.15
     */
    Integer REQUEST_URI_TOO_LARGE = 414;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.16
     */
    Integer UNSUPPORTED_MEDIA_TYPE = 415;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.17
     */
    Integer REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.4.18
     */
    Integer EXPECTATION_FAILED = 417;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.1
     */
    Integer INTERNAL_SERVER_ERROR = 500;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.2
     */
    Integer NOT_IMPLEMENTED = 501;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.3
     */
    Integer BAD_GATEWAY = 502;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.4
     */
    Integer SERVICE_UNAVAILABLE = 503;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.5
     */
    Integer GATEWAY_TIMEOUT = 504;

    /**
     * href=https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.1.110.5.6
     */
    Integer HTTP_VERSION_NOT_SUPPORTED = 505;

    final class DefaultPhrase {

        private static final Map<Integer, String> phrases;

        static {
            phrases = new HashMap<>();
            phrases.put(CONTINUE, "Continue");
            phrases.put(SWITCHING_PROTOCOLS, "Switching protocols");
            phrases.put(OK, "Ok");
            phrases.put(CREATED, "Created");
            phrases.put(ACCEPTED, "Accepted");
            phrases.put(NON_AUTHORITATIVE_INFOTMATION, "Non authoritative information");
            phrases.put(NO_CONTENT, "No content");
            phrases.put(RESET_CONTENT, "Reset content");
            phrases.put(PARTIAL_CONTENT, "Partial content");
            phrases.put(MULTIPLE_CHOICES, "Multiple choices");
            phrases.put(MOVED_PERMANENTLY, "Moved permanently");
            phrases.put(FOUND, "Found");
            phrases.put(SEE_OTHER, "See other");
            phrases.put(NOT_MODIFIED, "Not modified");
            phrases.put(USE_PROXY, "Use proxy");
            phrases.put(TEMPORARY_REDIRECT, "Temporary redirect");
            phrases.put(BAD_REQUEST, "Bad request");
            phrases.put(UNAUTHORIZED, "Unauthorized");
            phrases.put(PAYMENT_REQUIRED, "Payment required");
            phrases.put(FORBIDDEN, "Forbidden");
            phrases.put(NOT_FOUND, "Not found");
            phrases.put(METHOD_NOT_ALLOWED, "Method not allowed");
            phrases.put(NOT_ACCEPTABLE, "Not acceptable");
            phrases.put(REQUEST_TIMEOUT, "Request timeout");
            phrases.put(CONFLICT, "Conflict");
            phrases.put(GONE, "Gone");
            phrases.put(LENGTH_REQUIRED, "Length required");
            phrases.put(PRECONDITION_FAILED, "Precondition failed");
            phrases.put(REQUEST_ENTITY_TOO_LARGE, "Request entity too large");
            phrases.put(REQUEST_URI_TOO_LARGE, "Request uri too large");
            phrases.put(UNSUPPORTED_MEDIA_TYPE, "Unsupported media type.");
            phrases.put(REQUESTED_RANGE_NOT_SATISFIABLE, "Requested range not satisfiable");
            phrases.put(EXPECTATION_FAILED, "Expectation failed");
            phrases.put(INTERNAL_SERVER_ERROR, "Internal server error");
            phrases.put(NOT_IMPLEMENTED, "Not implemented");
            phrases.put(BAD_GATEWAY, "Bad gateway");
            phrases.put(SERVICE_UNAVAILABLE, "Service unavailable");
            phrases.put(GATEWAY_TIMEOUT, "Gateway timeout");
            phrases.put(HTTP_VERSION_NOT_SUPPORTED, "Http version not supported");
        }

        /**
         * Returns the default phrase for the specific response code.
         * @param httpResponseCode Http response code.
         * @return Response phrase.
         */
        public static String getDefaultPhrase(Integer httpResponseCode) {
            return DefaultPhrase.phrases.get(httpResponseCode);
        }
    }
}
