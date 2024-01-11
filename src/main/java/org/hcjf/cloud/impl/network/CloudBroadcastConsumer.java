package org.hcjf.cloud.impl.network;

import org.hcjf.io.net.InetPortProvider;
import org.hcjf.io.net.broadcast.BroadcastConsumer;
import org.hcjf.io.net.broadcast.BroadcastService;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author javaito
 */
public class CloudBroadcastConsumer implements BroadcastConsumer {

    private final Map<String, Object> parameters;

    public CloudBroadcastConsumer() {
        parameters = new HashMap<>();
        parameters.put(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_ADDRESS,
                SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_ADDRESS));
        parameters.put(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT,
                SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT));
    }

    @Override
    public String getTaskName() {
        return SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Broadcast.TASK_NAME);
    }

    @Override
    public String getPrivateKey() {
        return SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.CLUSTER_NAME);
    }

    @Override
    public String getIpVersion() {
        return SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Broadcast.IP_VERSION);
    }

    @Override
    public String getNetInterfaceName() {
        return SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Broadcast.INTERFACE_NAME);
    }

    @Override
    public Integer getPort() {
        return InetPortProvider.getUdpPort(SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.Broadcast.PORT));
    }

    @Override
    public Map<String, Object> getPingParameters() {
        return parameters;
    }

    @Override
    public void onPing(BroadcastService.PingMessage pingMessage) {
        String remoteHost = (String) pingMessage.getCustomParameters().get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_ADDRESS);
        Integer remotePort = (Integer) pingMessage.getCustomParameters().get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT);
        Node node = new Node();
        node.setLanAddress(remoteHost);
        node.setLanPort(remotePort);
        CloudOrchestrator.getInstance().registerConsumer(node);
    }

    @Override
    public void onPong(BroadcastService.PongMessage pongMessage) {
        String remoteHost = (String) pongMessage.getCustomParameters().get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_ADDRESS);
        Integer remotePort = (Integer) pongMessage.getCustomParameters().get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT);
        Node node = new Node();
        node.setLanAddress(remoteHost);
        node.setLanPort(remotePort);
        CloudOrchestrator.getInstance().registerConsumer(node);
    }

    @Override
    public void onShutdown(BroadcastService.ShutdownMessage shutdownMessage) {
    }
}
