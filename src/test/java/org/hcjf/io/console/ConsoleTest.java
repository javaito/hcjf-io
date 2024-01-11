package org.hcjf.io.console;

import org.hcjf.layers.Layer;
import org.hcjf.layers.Layers;
import org.hcjf.layers.query.JoinableMap;
import org.hcjf.layers.query.Query;
import org.hcjf.layers.query.Queryable;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;
import org.hcjf.service.ServiceSession;
import org.hcjf.utils.Cryptography;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author javaito
 * @email javaito@gmail.com
 */
public class ConsoleTest {

    public static void main(String[] args) {

        System.setProperty(SystemProperties.Log.SYSTEM_OUT_ENABLED, "true");
        System.setProperty(IOSystemProperties.Cloud.Orchestrator.ThisNode.ID, UUID.randomUUID().toString());
        System.setProperty(IOSystemProperties.Cloud.Orchestrator.ThisNode.NAME, "Test");
        System.setProperty(IOSystemProperties.Cloud.Orchestrator.ThisNode.VERSION, "1.0");
        System.setProperty(IOSystemProperties.Cloud.Orchestrator.ThisNode.CLUSTER_NAME, "Test");

        Layers.publishLayer(TestLayer.class);

        Cryptography cryptography = new Cryptography();
        ConsoleServer consoleServer = new ConsoleServer(5900, cryptography) {

            @Override
            protected ServerMetadata getMetadata() {
                ServerMetadata metadata = new ServerMetadata();
                metadata.setInstanceId(SystemProperties.getUUID(IOSystemProperties.Cloud.Orchestrator.ThisNode.ID));
                metadata.setClusterName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.CLUSTER_NAME));
                metadata.setServerName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.NAME));
                metadata.setServerVersion(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.VERSION));
                metadata.setLoginRequired(true);
                metadata.setLoginFields(List.of("user"));
                metadata.setLoginSecretFields(List.of("password"));
                return metadata;
            }

            @Override
            protected ServiceSession login(Map<String, Object> parameters) {
                ServiceSession serviceSession = new ServiceSession(ServiceSession.getGuestSession().getId());
                serviceSession.setSessionName(parameters.get("user").toString());
                return serviceSession;
            }

            @Override
            protected Collection<JoinableMap> evaluate(Queryable queryable) {
                return Query.evaluate(queryable);
            }
        };
        consoleServer.start();

    }

    public static class TestLayer extends Layer implements ConsoleCommandLayerInterface {

        public TestLayer() {
            super("test");
        }

        @Override
        public Object execute(List<Object> parameters) {

            System.out.println(parameters);

            return "Todo bien!!!";
        }
    }

}
