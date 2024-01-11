package org.hcjf.io.net.http.proxy;

import org.hcjf.errors.Errors;
import org.hcjf.io.net.http.Context;
import org.hcjf.io.net.http.HttpRequest;
import org.hcjf.io.net.http.HttpResponse;
import org.hcjf.io.net.http.HttpServer;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Http proxy implementation
 * @author javaito
 */
public class HttpProxy extends HttpServer {

    private static final String DEFAULT_CONTEXT_REGEX = ".*";

    private final List<HttpProxyRule> rules;
    private Context defaultContext;

    public HttpProxy(Integer port) {
        super(port);
        this.rules = new ArrayList<>();

        defaultContext = new Context(DEFAULT_CONTEXT_REGEX) {

            @Override
            public HttpResponse onContext(HttpRequest request) {
                HttpResponse response = null;
                for(HttpProxyRule rule : rules) {
                    if(rule.evaluate(request)) {
                        ProxyTask task = rule.getTask();
                        response = task.execute(request);
                        break;
                    }
                }
                return response;
            }

        };
        super.addContext(defaultContext);
    }

    public HttpProxy() {
        this(SystemProperties.getInteger(IOSystemProperties.Net.Http.DEFAULT_SERVER_PORT));
    }

    /**
     * Adds a rule to redirect incoming requests.
     * @param rule Http proxy rule.
     * @throws NullPointerException if the rule parameter is null.
     */
    public final void addRule(HttpProxyRule rule) {
        if(rule == null) {
            throw new NullPointerException("Null http proxy rule");
        }
        rules.add(rule);
    }

    /**
     * This kind of http server not support custom context because there are only
     * one context to take the incoming request and redirect the traffic using the storage rules.
     * @param context Http context.
     * @throws UnsupportedOperationException all calls
     */
    @Override
    public final synchronized void addContext(Context context) {
        throw new UnsupportedOperationException("This kind of http server not support add custom context");
    }

    /**
     * All the times return the same internal context.
     * @param contextName This parameter is ignored.
     * @return Always returns the unique internal context
     */
    @Override
    protected final ContextMatcher findContext(String contextName) {
        return new ContextMatcher(defaultContext, null);
    }

}
