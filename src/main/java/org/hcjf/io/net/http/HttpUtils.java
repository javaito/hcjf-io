package org.hcjf.io.net.http;

import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;

import java.util.Map;
import java.util.regex.Pattern;

public class HttpUtils {

    /**
     * This method is called for getting accessControl from a map.
     * @param host host information.
     * @param accessControlMap All the accessControlMap information.
     * @return Return an instance from the map of accessControlMap or null.
     */
    public static HttpServer.AccessControl getAccessControl(String host, Map<String, HttpServer.AccessControl> accessControlMap) {
        String startChar = SystemProperties.get(IOSystemProperties.Net.Http.HOST_ACCESS_CONTROL_REGEX_START_CHAR);
        for(String accessHost : accessControlMap.keySet()) {
            if(accessHost.startsWith(startChar)) {
                if(Pattern.matches(accessHost.substring(startChar.length()),host)) {
                    return accessControlMap.get(accessHost);
                }
            } else if (accessHost.equals(host)) {
                return accessControlMap.get(accessHost);
            }
        }
        return null;
    }
}
