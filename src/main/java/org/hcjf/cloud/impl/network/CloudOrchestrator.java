package org.hcjf.cloud.impl.network;

import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import org.hcjf.cloud.Cloud;
import org.hcjf.cloud.impl.LockImpl;
import org.hcjf.cloud.impl.messages.*;
import org.hcjf.cloud.impl.objects.*;
import org.hcjf.errors.HCJFRemoteException;
import org.hcjf.errors.HCJFRemoteInvocationTimeoutException;
import org.hcjf.errors.HCJFRuntimeException;
import org.hcjf.events.DistributedEvent;
import org.hcjf.events.Events;
import org.hcjf.events.RemoteEvent;
import org.hcjf.events.StoreStrategyLayerInterface;
import org.hcjf.io.net.NetService;
import org.hcjf.io.net.NetServiceConsumer;
import org.hcjf.io.net.broadcast.BroadcastService;
import org.hcjf.io.net.kubernetes.KubernetesSpy;
import org.hcjf.io.net.kubernetes.KubernetesSpyConsumer;
import org.hcjf.io.net.messages.Message;
import org.hcjf.io.net.messages.MessageCollection;
import org.hcjf.io.net.messages.NetUtils;
import org.hcjf.io.net.messages.ResponseMessage;
import org.hcjf.layers.Layer;
import org.hcjf.layers.LayerInterface;
import org.hcjf.layers.Layers;
import org.hcjf.layers.crud.ReadRowsLayerInterface;
import org.hcjf.layers.query.JoinableMap;
import org.hcjf.layers.query.Queryable;
import org.hcjf.log.Log;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;
import org.hcjf.service.Service;
import org.hcjf.service.ServiceSession;
import org.hcjf.utils.Introspection;
import org.hcjf.utils.JsonUtils;
import org.hcjf.utils.Strings;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * This class implements a orchestrator in order to maintains the connection
 * between nodes, synchronize all the time the node information and admin the
 * communication between nodes using messages.
 * @author javaito.
 */
public final class CloudOrchestrator extends Service<NetworkComponent> {

    private static final class OriginDataFields {
        private static final String NODE = "node";
        private static final String SERVICE = "service";
        private static final String UNKNOWN = "unknown";
    }

    public static final CloudOrchestrator instance;

    static {
        instance = new CloudOrchestrator();

        Layers.publishLayer(SystemCloudNodeReadableImplementation.class);
        Layers.publishLayer(SystemCloudServiceReadableImplementation.class);
    }

    private Node thisNode;
    private Map<String,Object> thisNodeMap;
    private Map<UUID,Node> nodes;
    private Map<String, Node> nodesByLanId;
    private Map<String, Node> nodesByWanId;
    private Set<Node> sortedNodes;
    private Map<UUID, Node> waitingAck;
    private Map<UUID, ResponseListener> responseListeners;

    private ServiceEndPoint thisServiceEndPoint;
    private Map<String,Object> thisServiceEndPointMap;
    private Map<UUID, ServiceEndPoint> endPoints;
    private Map<String, ServiceEndPoint> endPointsByGatewayId;
    private Object publishMeMonitor;

    private Object wagonMonitor;
    private Long lastVisit;
    private Long lastServicePublication;
    private Map<String,List<Message>> wagonLoad;

    private CloudServer server;
    private DistributedTree sharedStore;
    private Random random;

    private CloudOrchestrator() {
        super(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.SERVICE_NAME),
                SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.SERVICE_PRIORITY));
    }

    public static CloudOrchestrator getInstance() {
        return instance;
    }

    @Override
    protected void init() {
        nodes = new HashMap<>();
        nodesByLanId = new HashMap<>();
        nodesByWanId = new HashMap<>();
        sortedNodes = new TreeSet<>();
        waitingAck = new HashMap<>();
        responseListeners = new HashMap<>();

        thisNode = new Node();
        UUID thisNodeId = SystemProperties.getUUID(IOSystemProperties.Cloud.Orchestrator.ThisNode.ID);
        if(thisNodeId == null) {
            thisNodeId = UUID.randomUUID();
        }
        thisNode.setId(thisNodeId);
        thisNode.setDataCenterName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.DATA_CENTER_NAME));
        thisNode.setClusterName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.CLUSTER_NAME));
        thisNode.setName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.NAME));
        thisNode.setVersion(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.VERSION));
        thisNode.setLanAddress(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_ADDRESS));
        thisNode.setLanPort(SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT));
        if(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.WAN_ADDRESS) != null) {
            thisNode.setWanAddress(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.WAN_ADDRESS));
            thisNode.setWanPort(SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.ThisNode.WAN_PORT));
        }
        thisNode.setStartupDate(new Date());
        thisNode.setStatus(Node.Status.CONNECTED);
        thisNode.setLocalNode(true);
        sortedNodes.add(thisNode);

        thisServiceEndPoint = new ServiceEndPoint();
        UUID thisServiceEndPointId = SystemProperties.getUUID(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.ID);
        if(thisServiceEndPointId == null) {
            thisServiceEndPointId = UUID.randomUUID();
        }
        thisServiceEndPoint.setId(thisServiceEndPointId);
        thisServiceEndPoint.setName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.NAME));
        thisServiceEndPoint.setGatewayAddress(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.GATEWAY_ADDRESS));
        thisServiceEndPoint.setGatewayPort(SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.GATEWAY_PORT));
        thisServiceEndPoint.setDistributedEventListener(SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.DISTRIBUTED_EVENT_LISTENER));

        endPoints = new HashMap<>();
        endPointsByGatewayId = new HashMap<>();

        publishMeMonitor = new Object();

        wagonMonitor = new Object();
        lastVisit = System.currentTimeMillis();
        lastServicePublication = System.currentTimeMillis() - SystemProperties.getLong(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.PUBLICATION_TIMEOUT);
        wagonLoad = new HashMap<>();

        random = new Random();
        sharedStore = new DistributedTree(Strings.EMPTY_STRING);
        server = new CloudServer();
        server.start();

        try {
            for (Node node : SystemProperties.getObjects(IOSystemProperties.Cloud.Orchestrator.NODES, Node.class)) {
                registerConsumer(node);
            }
        } catch (Exception ex) {
            Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Load nodes from properties fail", ex);
        }

        try {
            for (ServiceEndPoint serviceEndPoint : SystemProperties.getObjects(IOSystemProperties.Cloud.Orchestrator.SERVICE_END_POINTS, ServiceEndPoint.class)) {
                registerConsumer(serviceEndPoint);
            }
        } catch (Exception ex) {
            Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Load service end points from properties fail", ex);
        }

        if(SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.Broadcast.ENABLED)) {
            CloudBroadcastConsumer broadcastConsumer = new CloudBroadcastConsumer();
            BroadcastService.getInstance().registerConsumer(broadcastConsumer);
        }

        if(SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.Kubernetes.ENABLED)) {
            thisNode.setId(new UUID((NetUtils.getLocalIp() + Node.class.getName()).hashCode(), KubernetesSpy.getHostName().hashCode()));
            thisServiceEndPoint.setId(new UUID(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Kubernetes.NAMESPACE).hashCode(),
                    SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Kubernetes.SERVICE_NAME).hashCode()));
            thisServiceEndPoint.setName(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Kubernetes.SERVICE_NAME));
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes service id %s", thisServiceEndPoint.getId());
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes node id %s", thisNode.getId());
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Local IP %s", NetUtils.getLocalIp());
            thisNode.setLanAddress(NetUtils.getLocalIp());
            thisServiceEndPoint.setGatewayAddress(NetUtils.getLocalIp());
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes consumer starting");

            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes pod labels: %s",
                    SystemProperties.getMap(IOSystemProperties.Cloud.Orchestrator.Kubernetes.POD_LABELS).toString());
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes service labels: %s",
                    SystemProperties.getMap(IOSystemProperties.Cloud.Orchestrator.Kubernetes.SERVICE_LABELS).toString());
            KubernetesSpy.getInstance().registerConsumer(new KubernetesSpyConsumer(
                    pod -> {
                        Map<String,String> expectedLabels = SystemProperties.getMap(IOSystemProperties.Cloud.Orchestrator.Kubernetes.POD_LABELS);
                        Map<String,String> labels = pod.getMetadata().getLabels();
                        return SystemProperties.getList(IOSystemProperties.Cloud.Orchestrator.Kubernetes.ALLOW_PHASES).contains(pod.getStatus().getPhase()) &&
                                verifyLabels(expectedLabels, labels);
                    },
                    service -> {
                        Map<String,String> expectedLabels = SystemProperties.getMap(IOSystemProperties.Cloud.Orchestrator.Kubernetes.SERVICE_LABELS);
                        Map<String,String> labels = service.getMetadata().getLabels();
                        return verifyLabels(expectedLabels, labels);
                    }) {

                private final Map<String,Node> nodesByPodId = new HashMap<>();

                @Override
                protected void onPodDiscovery(V1Pod pod) {
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes pod discovery: %s", pod.getMetadata().getUid());
                    Node node = new Node();
                    node.setLanAddress(pod.getStatus().getPodIP());
                    //Use the same port because supposed that the node is a replica of this node.
                    node.setLanPort(SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.ThisNode.LAN_PORT));
                    registerConsumer(node);
                    nodesByPodId.put(pod.getMetadata().getUid(),node);
                }

                @Override
                protected void onPodLost(V1Pod pod) {
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes pod lost: %s", pod.getMetadata().getUid());
                    unregisterConsumer(nodesByPodId.remove(pod.getMetadata().getUid()));
                }

                @Override
                protected void onServiceDiscovery(V1Service service) {
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Kubernetes service discovery: %s", service.getMetadata().getUid());
                    ServiceEndPoint serviceEndPoint = new ServiceEndPoint();
                    serviceEndPoint.setId(new UUID(service.getMetadata().getNamespace().hashCode(), service.getMetadata().getName().hashCode()));
                    serviceEndPoint.setGatewayAddress(service.getMetadata().getName());
                    for(V1ServicePort port : service.getSpec().getPorts()) {
                        if(port.getName().equals(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Kubernetes.SERVICE_PORT_NAME))) {
                            serviceEndPoint.setGatewayPort(port.getPort());
                            break;
                        }
                    }
                    registerConsumer(serviceEndPoint);
                }

                @Override
                protected void onServiceLost(V1Service service) {
                    ServiceEndPoint serviceEndPoint = new ServiceEndPoint();
                    serviceEndPoint.setId(new UUID(service.getMetadata().getNamespace().hashCode(), service.getMetadata().getName().hashCode()));
                    unregisterConsumer(serviceEndPoint);
                }
            });
        }
    }

    /**
     *
     * @return
     */
    private synchronized Map<String,Object> getThisNodeMap() {
        if(thisNodeMap == null) {
            thisNodeMap = Introspection.toMap(thisNode);
        }
        return thisNodeMap;
    }

    private synchronized Map<String,Object> getThisServiceEndPointMap(){
        if(thisServiceEndPointMap == null) {
            thisServiceEndPointMap = Introspection.toMap(thisServiceEndPoint);
        }
        return thisServiceEndPointMap;
    }

    private synchronized Map<String,Object> getOriginData() {
        Map<String,Object> result = new HashMap<>();
        result.put(OriginDataFields.NODE, getThisNodeMap());
        result.put(OriginDataFields.SERVICE, getThisServiceEndPointMap());
        return result;
    }

    /**
     *
     * @param expectedLabels expected labels
     * @param labels labels
     * @return
     */
    public boolean verifyLabels(Map<String,String> expectedLabels, Map<String,String> labels) {
        boolean result = true;
        for(String labelKey : expectedLabels.keySet()) {
            if(!(labels.containsKey(labelKey) && labels.get(labelKey).equals(expectedLabels.get(labelKey)))) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Register a new component into the cluster.
     * @param networkComponent Network component to add.
     */
    @Override
    public void registerConsumer(NetworkComponent networkComponent) {
        Objects.requireNonNull(networkComponent, "Unable to register a null component");
        if(networkComponent instanceof Node) {
            Node node = (Node) networkComponent;
            String lanId = node.getLanId();
            String wanId = node.getWanId();
            if(lanId != null || wanId != null) {
                boolean add = true;
                if (lanId != null && (thisNode.getLanId().equalsIgnoreCase(lanId) || nodesByLanId.containsKey(lanId))) {
                    add = false;
                }
                if (wanId != null && (thisNode.getWanId().equalsIgnoreCase(wanId) || nodesByWanId.containsKey(wanId))) {
                    add = false;
                }
                if (add) {
                    node.setStatus(Node.Status.DISCONNECTED);
                    if (lanId != null) {
                        nodesByLanId.put(lanId, node);
                        node.setId(new UUID(0L, lanId.hashCode()));
                    }

                    if (wanId != null) {
                        nodesByWanId.put(wanId, node);
                    }

                    nodes.put(node.getId(), node);
                    sortedNodes.add(node);
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "New node registered: %s", node);
                }
            }
        } else if(networkComponent instanceof ServiceEndPoint) {
            ServiceEndPoint endPoint = (ServiceEndPoint) networkComponent;
            if(endPoint.getGatewayAddress() != null){
                if(!(thisServiceEndPoint.getGatewayId().equals(endPoint.getGatewayId()) ||
                        endPointsByGatewayId.containsKey(endPoint.getGatewayId()))) {
                    endPoints.put(endPoint.getId(), endPoint);
                    endPointsByGatewayId.put(endPoint.getGatewayId(), endPoint);
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "New service end point registered: %s", endPoint);

                    fork(() -> initServicePublication(endPoint));
                }
            }
        }
    }

    /**
     * This method run continuously publishing all the distributed layers for all the registered services.
     */
    private void initServicePublication(ServiceEndPoint serviceEndPoint) {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                Collection<Message> messages = createServicePublicationCollection();
                ServiceDefinitionMessage serviceDefinitionMessage = new ServiceDefinitionMessage();
                serviceDefinitionMessage.setId(UUID.randomUUID());
                serviceDefinitionMessage.setMessages(messages);
                serviceDefinitionMessage.setServiceId(thisServiceEndPoint.getId());
                serviceDefinitionMessage.setServiceName(thisServiceEndPoint.getName());
                serviceDefinitionMessage.setEventListener(thisServiceEndPoint.isDistributedEventListener());
                serviceDefinitionMessage.setBroadcasting(true);
                Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Sending interfaces to: %s", serviceEndPoint);
                try {
                    Object responseObject = invokeNetworkComponent(serviceEndPoint, serviceDefinitionMessage);
                    if(responseObject != null) {
                        ServiceDefinitionResponseMessage definitionResponse = (ServiceDefinitionResponseMessage) responseObject;
                        fork(() -> {
                            if (definitionResponse.getMessages() != null) {
                                for (Message innerMessage : definitionResponse.getMessages()) {
                                    try {
                                        processMessage(null, innerMessage);
                                    } catch (Exception ex) {
                                        Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                                                "Unable to process one message of the service publication collection: %s",
                                                innerMessage.getClass().toString());
                                    }
                                }
                            }
                        });

                        try {
                            endPoints.get(definitionResponse.getServiceId()).setName(definitionResponse.getServiceName());
                            endPoints.get(definitionResponse.getServiceId()).setDistributedEventListener(definitionResponse.getEventListener());
                        } catch (Exception ex) {
                        }
                    }
                } catch (Exception ex) {
                    Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Unable to make publication %s ---> %s",
                            SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.NETWORKING_HANDSHAKE_DETAILS_AVAILABLE, false), ex,
                            thisServiceEndPoint.getName(), serviceEndPoint.getName());
                    try {
                        Thread.sleep(SystemProperties.getLong(
                                IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.PUBLICATION_TIMEOUT));
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                synchronized (publishMeMonitor) {
                    try {
                        publishMeMonitor.wait();
                    } catch (InterruptedException e) { }
                }
            } catch (Exception ex){
                Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Fail trying to make publication %s ---> %s",
                        SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.NETWORKING_HANDSHAKE_DETAILS_AVAILABLE, false), ex,
                        thisServiceEndPoint.getName(), serviceEndPoint.getName());
            }
        }
    }

    public final void publishMe() {
        synchronized (publishMeMonitor) {
            publishMeMonitor.notifyAll();
        }
    }

    @Override
    public void unregisterConsumer(NetworkComponent networkComponent) {
        if(networkComponent instanceof Node) {
            synchronized (nodes) {
                Node node = (Node) networkComponent;
                String lanId = node.getLanId();
                String wanId = node.getWanId();
                nodesByLanId.remove(lanId);
                nodesByWanId.remove(wanId);
                nodes.remove(node.getId());
                for(Node sortedNode : sortedNodes) {
                    try {
                        if (sortedNode.getLanId().equals(node.getLanId()) || sortedNode.getId().equals(node.getId())) {
                            sortedNodes.remove(sortedNode);
                        }
                    } catch (Exception ex){}
                }
            }
        }
    }

    /**
     * Returns a list with the nodes sorted by id, this list has the same
     * order in all the nodes into the cluster.
     * @return List with sorted nodes.
     */
    private List<Node> getSortedNodes() {
        List < Node > nodes = new ArrayList<>();
        boolean insert = false;
        for(Node node : sortedNodes) {
            if(node.equals(thisNode)) {
                insert = true;
                continue;
            }

            if(insert) {
                nodes.add(0, node);
            } else {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Creates all the message needed to publish the service.
     * @return Collection of messages.
     */
    private Collection<Message> createServicePublicationCollection() {
        LocalLeaf localLeaf;
        Object[] path;
        Collection<Message> messages = new ArrayList<>();
        PublishLayerMessage publishLayerMessage;
        for (DistributedTree.Entry entry : sharedStore.filter(LocalLeaf.class)) {
            localLeaf = (LocalLeaf) entry.getValue();
            path = entry.getPath();
            if (localLeaf.getInstance() instanceof DistributedLayer) {
                publishLayerMessage = new PublishLayerMessage(UUID.randomUUID());
                publishLayerMessage.setPath(path);
                publishLayerMessage.setRegex(((DistributedLayer)localLeaf.getInstance()).getRegex());
                publishLayerMessage.setServiceEndPointId(thisServiceEndPoint.getId());
                messages.add(publishLayerMessage);
            }
        }
        return messages;
    }

    public void incomingMessage(CloudSession session, Message message) {
        String from = OriginDataFields.UNKNOWN;
        if(message.getOriginData() != null) {
            from = JsonUtils.toJsonTree(message.getOriginData()).toString();
        }
        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                "Incoming '%s' message from '%s': %s", message.getClass(), from, message.getId());
        Message responseMessage = null;
        if(message instanceof ServiceDefinitionMessage) {
            try {
                ServiceDefinitionResponseMessage serviceDefinitionResponseMessage = new ServiceDefinitionResponseMessage();
                serviceDefinitionResponseMessage.setId(UUID.randomUUID());
                serviceDefinitionResponseMessage.setMessages(createServicePublicationCollection());
                serviceDefinitionResponseMessage.setServiceId(thisServiceEndPoint.getId());
                serviceDefinitionResponseMessage.setServiceName(thisServiceEndPoint.getName());
                serviceDefinitionResponseMessage.setEventListener(thisServiceEndPoint.isDistributedEventListener());
                serviceDefinitionResponseMessage.setBroadcasting(true);
                ResponseMessage definitionResponse = new ResponseMessage(message);
                definitionResponse.setValue(serviceDefinitionResponseMessage);
                responseMessage = definitionResponse;
            } catch (Exception ex) {
                Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                        "Unable to create publication response message", ex);
            }
            try {
                //This king of messages contains all the information about a service.
                ServiceDefinitionMessage serviceDefinitionMessage = (ServiceDefinitionMessage) message;
                fork(() -> {
                    if (serviceDefinitionMessage.getMessages() != null) {
                        for (Message innerMessage : serviceDefinitionMessage.getMessages()) {
                            try {
                                processMessage(session, innerMessage);
                            } catch (Exception ex) {
                                Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                                        "Unable to process one message of the service publication collection: %s",
                                        innerMessage.getClass().toString());
                            }
                        }
                    }
                });

                try {
                    endPoints.get(((ServiceDefinitionMessage) message).getServiceId()).setName(
                            ((ServiceDefinitionMessage) message).getServiceName());
                    endPoints.get(((ServiceDefinitionMessage) message).getServiceId()).setDistributedEventListener(
                            ((ServiceDefinitionMessage) message).getEventListener());
                } catch (Exception ex){}

                if(SystemProperties.getBoolean(IOSystemProperties.Cloud.Orchestrator.SERVICE_PUBLICATION_REPLICAS_BROADCASTING_ENABLED)) {
                    fork(() -> {
                        //Sent the message for all the replicas
                        if (serviceDefinitionMessage.getBroadcasting() != null && serviceDefinitionMessage.getBroadcasting()) {
                            serviceDefinitionMessage.setBroadcasting(false);
                            for (Node node : nodes.values()) {
                                try {
                                    invokeNetworkComponent(node, serviceDefinitionMessage,
                                            SystemProperties.getLong(IOSystemProperties.Cloud.Orchestrator.SERVICE_PUBLICATION_REPLICAS_BROADCASTING_TIMEOUT));
                                } catch (Exception ex) {
                                    Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                                            "Unable to notify node: %s", node.toString());
                                }
                            }
                        }
                    });
                }
            } catch (Exception ex){
                Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                        "Exception processing publication message: %s", ex, message.getId().toString());
            }
        } else if(message instanceof MessageCollection) {
            MessageCollection collection = (MessageCollection) message;
            for(Message innerMessage : collection.getMessages()) {
                processMessage(session, innerMessage);
            }
        } else {
            responseMessage = processMessage(session, message);
        }

        if(!(message instanceof ResponseMessage)) {
            if (responseMessage == null) {
                responseMessage = new ResponseMessage(message);
            }

            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                    "Sending response message: %s", message.getId());
            sendResponse(session, responseMessage);
        }
    }

    private Message processMessage(CloudSession session, Message message) {
        Message responseMessage = null;
        if(message instanceof NodeIdentificationMessage) {
            NodeIdentificationMessage nodeIdentificationMessage = (NodeIdentificationMessage) message;
            Node node = nodesByLanId.get(nodeIdentificationMessage.getNode().getLanId());
            if(node == null) {
                node = nodesByWanId.get(nodeIdentificationMessage.getNode().getWanId());
            }

            if(node == null && Objects.equals(nodeIdentificationMessage.getNode().getClusterName(), thisNode.getClusterName())) {
                //In this case we need to add the node as a new node
                registerConsumer(nodeIdentificationMessage.getNode());

                //Search again the node into the class collections.
                node = nodesByLanId.get(nodeIdentificationMessage.getNode().getLanId());
                if(node == null) {
                    node = nodesByWanId.get(nodeIdentificationMessage.getNode().getWanId());
                }
            }

            if(node != null) {
                updateNode(node, nodeIdentificationMessage);
                if (session.getConsumer() instanceof CloudClient) {
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Incoming credentials response from %s:%d",
                            node.getLanAddress(), node.getLanPort());
                    if (!connected(node)) {
                        ((CloudClient) session.getConsumer()).disconnect();
                    } else {
                        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Node connected as client %s", node);
                        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Ack sent to %s:%d",
                                node.getLanAddress(), node.getLanPort());
                        responseMessage = new AckMessage(message);
                    }
                } else if (session.getConsumer() instanceof CloudServer) {
                    if (connecting(node)) {
                        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Incoming credentials from %s:%d",
                                node.getLanAddress(), node.getLanPort());
                        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Response credentials to %s:%d",
                                node.getLanAddress(), node.getLanPort());
                        NodeIdentificationMessage returnNodeIdentificationMessage = new NodeIdentificationMessage(thisNode);
                        waitingAck.put(returnNodeIdentificationMessage.getId(), node);
                        responseMessage = returnNodeIdentificationMessage;
                    } else {
                        try {
                            ((CloudServer) session.getConsumer()).send(session, new BusyNodeMessage(thisNode));
                        } catch (IOException e) {
                            //TODO: Close the session
                        }
                    }
                }
            } else {
                server.destroySession(session);
            }
        } else if(message instanceof BusyNodeMessage) {
            BusyNodeMessage busyNodeMessage = (BusyNodeMessage) message;
            Node node = nodesByLanId.get(busyNodeMessage.getNode().getLanId());
            if(node == null) {
                node = nodesByWanId.get(busyNodeMessage.getNode().getWanId());
            }
            if(session.getConsumer() instanceof CloudClient) {
                disconnected(node);
                ((CloudClient)session.getConsumer()).disconnect();
            }
        } else if(message instanceof HidePathMessage) {
            removePath(((HidePathMessage)message).getPath());
        } else if(message instanceof PublishPathMessage) {
            addPath(((PublishPathMessage)message).getPath());
        } else if(message instanceof PublishObjectMessage) {
            PublishObjectMessage publishObjectMessage = (PublishObjectMessage) message;
            for(PublishObjectMessage.Path path : publishObjectMessage.getPaths()) {
                if(path.getValue() != null) {
                    addLocalObject(path.getValue(), path.getNodes(), List.of(),
                            publishObjectMessage.getTimestamp(), path.getPath());
                } else {
                    addRemoteObject(null, path.getNodes(),
                            path.getNodes(), publishObjectMessage.getTimestamp(), path.getPath());
                }
            }
        } else if(message instanceof InvokeMessage) {
            InvokeMessage invokeMessage = (InvokeMessage) message;

            responseMessage = new ResponseMessage(invokeMessage);
            Object object = sharedStore.getInstance(invokeMessage.getPath());
            ((ResponseMessage)responseMessage).setValue(object);
        } else if(message instanceof LockMessage) {
            LockMessage lockMessage = (LockMessage) message;
            responseMessage = new ResponseMessage(lockMessage);
            ((ResponseMessage)responseMessage).setValue(distributedLock(lockMessage.getTimestamp(),
                    lockMessage.getNanos(), lockMessage.getPath()));
        } else if(message instanceof UnlockMessage) {
            UnlockMessage unlockMessage = (UnlockMessage) message;
            distributedUnlock(unlockMessage.getPath());
        } else if(message instanceof SignalMessage) {
            SignalMessage signalMessage = (SignalMessage) message;
            distributedSignal(signalMessage.getLockName(), signalMessage.getConditionName());
        } else if(message instanceof SignalAllMessage) {
            SignalAllMessage signalAllMessage = (SignalAllMessage) message;
            distributedSignalAll(signalAllMessage.getLockName(), signalAllMessage.getConditionName());
        } else if(message instanceof EventMessage) {
            EventMessage eventMessage = (EventMessage) message;
            distributedDispatchEvent(eventMessage.getEvent(), eventMessage.getSessionBean(), eventMessage.getSessionId());
            responseMessage = new ResponseMessage(eventMessage);
            ((ResponseMessage)responseMessage).setValue(true);
        } else if(message instanceof PublishLayerMessage) {
            PublishLayerMessage publishLayerMessage = (PublishLayerMessage) message;
            responseMessage = new ResponseMessage(publishLayerMessage);

            try {
                boolean localImpl = false;
                Object[] path = publishLayerMessage.getPath();
                Class<? extends LayerInterface> layerInterfaceClass = (Class<? extends LayerInterface>) Class.forName((String)path[path.length - 2]);
                String implName = (String)path[path.length - 1];

                try {
                    Layers.get(layerInterfaceClass, implName);
                    localImpl = true;
                } catch (Exception ex) {}

                if(!localImpl) {
                    DistributedLayer distributedLayer = getDistributedLayer(false, publishLayerMessage.getPath());
                    distributedLayer.setRegex(publishLayerMessage.getRegex());
                    distributedLayer.addServiceEndPoint(publishLayerMessage.getServiceEndPointId());
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Remote %s layer founded %s in %s",
                            layerInterfaceClass.getName(), implName, endPoints.get(
                                    publishLayerMessage.getServiceEndPointId()).getGatewayAddress());
                }
                ((ResponseMessage)responseMessage).setValue(true);

                //Get the instance first time in order to crete the cache.
                try {
                    Layers.get(layerInterfaceClass, implName);
                } catch (Exception ex) {}
            } catch (Exception ex) {
                ((ResponseMessage)responseMessage).setThrowable(ex);
            }
        } else if(message instanceof PublishPluginMessage) {
            PublishPluginMessage publishPluginMessage = (PublishPluginMessage) message;
            Layers.publishPlugin(ByteBuffer.wrap(publishPluginMessage.getJarFile()));
        } else if(message instanceof LayerInvokeMessage) {
            LayerInvokeMessage layerInvokeMessage = (LayerInvokeMessage) message;
            Object result = null;
            Throwable throwable = null;
            try {
                result = distributedLayerInvoke(layerInvokeMessage.getSessionId(), layerInvokeMessage.getSessionBean(),
                        layerInvokeMessage.getParameterTypes(), layerInvokeMessage.getParameters(),
                        layerInvokeMessage.getMethodName(), layerInvokeMessage.getPath());
            } catch (Throwable t) {
                throwable = t;
            }
            responseMessage = new ResponseMessage(message);
            ((ResponseMessage)responseMessage).setValue(result);
            ((ResponseMessage)responseMessage).setThrowable(throwable);
        } else if(message instanceof TestNodeMessage) {
            responseMessage = new ResponseMessage(message);
        } else if(message instanceof ResponseMessage) {
            ResponseListener responseListener = responseListeners.get(message.getId());
            if(responseListener != null) {
                responseListener.setMessage((ResponseMessage) message);
                if(message instanceof ServiceDefinitionResponseMessage) {
                    for(Message innerMessage : ((ServiceDefinitionResponseMessage)message).getMessages()) {
                        processMessage(session, innerMessage);
                    }
                }
            } else {
                Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                        "Response listener not found: %s", message.getId());
            }
        } else if(message instanceof AckMessage) {
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Incoming ack from %s:%d",
                    session.getRemoteHost(), session.getRemotePort());
            if(session.getConsumer() instanceof CloudServer) {
                Node node = waitingAck.remove(message.getId());
                if(node != null) {
                    if(connected(node)) {
                        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Node connected as server %s", node);
                    }
                }
            }
        }
        return responseMessage;
    }

    private void nodeBroadcasting(Message message) {
        List<Node> nodeList = new ArrayList<>(nodesByLanId.values());
        for (Node node : nodeList) {
            try {
                fork(() -> invokeNetworkComponent(node, message));
            } catch (Exception ex){}
        }
    }

    private void sendResponse(CloudSession session, Message message) {
        try {
            NetServiceConsumer consumer = session.getConsumer();
            if(consumer instanceof CloudClient) {
                ((CloudClient)consumer).send(message);
            } else {
                ((CloudServer)consumer).send(session, message);
            }
        } catch (IOException e) {
            server.destroySession(session);
        }
    }

    private Object invokeNetworkComponent(NetworkComponent networkComponent, Message message) {
        return invokeNetworkComponent(networkComponent, message,
                SystemProperties.getLong(IOSystemProperties.Cloud.Orchestrator.INVOKE_TIMEOUT));
    }

    private Object invokeNetworkComponent(NetworkComponent networkComponent, Message message, Long timeout) {
        Object result;
        if(message.getId() == null) {
            message.setId(UUID.randomUUID());
        }
        message = populateOriginData(message);

        if(networkComponent != null) {
            CloudClient client;
            try {
                String host = networkComponent instanceof ServiceEndPoint ?
                        ((ServiceEndPoint)networkComponent).getGatewayAddress() :
                        ((Node)networkComponent).getLanAddress();
                Integer port = networkComponent instanceof ServiceEndPoint ?
                        ((ServiceEndPoint)networkComponent).getGatewayPort() :
                        ((Node)networkComponent).getLanPort();
                client = new CloudClient(host, port);
                NetService.getInstance().registerConsumer(client);
            } catch (Exception ex) {
                throw new HCJFRuntimeException("Unable to connect with service: %s", ex, networkComponent.getName());
            }
            try {
                if (client.waitForConnect()) {
                    ResponseListener responseListener = new ResponseListener(networkComponent, timeout);
                    registerListener(message, responseListener);
                    try {
                        try {
                            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                                    "Sending to %s invoke service message: '%s' %s",
                                    networkComponent.getName(),
                                    message.getClass().getName(),
                                    message.getId().toString());
                            client.send(message);
                        } catch (Exception ex) {
                            throw new HCJFRuntimeException("Unable to send message to %s", ex, networkComponent.getName());
                        }
                        result = responseListener.getResponse(message);
                    } finally {
                        destroyListener(message);
                    }
                } else {
                    throw new HCJFRuntimeException("Connection timeout with service: %s", networkComponent.getName());
                }
            } finally {
                try {
                    client.disconnect();
                } catch (Exception ex){}
            }
        } else {
            throw new HCJFRuntimeException("Service end point not found (%s)", networkComponent.getId());
        }
        return result;
    }

    private Message populateOriginData(Message message) {
        if(message instanceof MessageCollection) {
            for(Message innerMessage : ((MessageCollection)message).getMessages()) {
                populateOriginData(innerMessage);
            }
        }
        message.setOriginData(getOriginData());
        return message;
    }

    private void registerListener(Message message, ResponseListener listener) {
        synchronized (responseListeners) {
            while (responseListeners.containsKey(message.getId())) {
                Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Message id crash!! %s", message.getId());
                message.setId(UUID.randomUUID());
            }
            responseListeners.put(message.getId(), listener);
        }
    }

    private void destroyListener(Message message) {
        responseListeners.remove(message.getId());
    }

    private DistributedLock getDistributedLock(Object... path) {
        DistributedLock distributedLock;
        synchronized (sharedStore) {
            distributedLock = (DistributedLock) sharedStore.getInstance(path);
            if (distributedLock == null) {
                distributedLock = new DistributedLock();
                distributedLock.setStatus(DistributedLock.Status.UNLOCKED);
                addLocalObject(distributedLock, List.of(thisNode.getId()), List.of(), System.currentTimeMillis(), path);
            }
        }
        return distributedLock;
    }

    public void lock(Object... path) {
        DistributedLock distributedLock = getDistributedLock(path);
        synchronized (distributedLock) {
            while (!distributedLock.getStatus().equals(DistributedLock.Status.UNLOCKED)) {
                try {
                    distributedLock.wait();
                } catch (InterruptedException e) {
                }
            }
            distributedLock.setStatus(DistributedLock.Status.LOCKING);
        }

        LockMessage lockMessage = new LockMessage(UUID.randomUUID());
        lockMessage.setPath(path);
        lockMessage.setTimestamp(distributedLock.getTimestamp());
        boolean locked;
        while (!distributedLock.getStatus().equals(DistributedLock.Status.LOCKED)) {
            lockMessage.setNanos(distributedLock.getNanos());
            locked = true;
            List<Node> nodeList = new ArrayList<>(nodesByLanId.values());
            for (Node node : nodeList) {
                try {
                    if (!(locked = locked & (boolean) invokeNetworkComponent(node, lockMessage))) {
                        break;
                    }
                } catch (Exception ex){
                    Log.w(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                            "Unable to send lock message to session: ", node.getId());
                }
            }
            if (locked) {
                distributedLock.setStatus(DistributedLock.Status.LOCKED);
            } else {
                distributedLock.setStatus(DistributedLock.Status.WAITING);
                try {
                    synchronized (distributedLock) {
                        distributedLock.wait(5000);
                    }
                } catch (InterruptedException e) { }
            }
        }
    }

    private boolean distributedLock(Long timestamp, Long nanos, Object... path) {
        boolean result;
        DistributedLock distributedLock = getDistributedLock(path);
        synchronized (distributedLock) {
            result = distributedLock.getStatus().equals(DistributedLock.Status.UNLOCKED) ||
                    (distributedLock.getTimestamp() > timestamp && distributedLock.getNanos() > nanos);
        }
        return result;
    }

    public void unlock(Object... path) {
        DistributedLock distributedLock = getDistributedLock(path);
        synchronized (distributedLock) {
            distributedLock.setStatus(DistributedLock.Status.UNLOCKED);
            distributedLock.notifyAll();
        }

        UnlockMessage unlockMessage = new UnlockMessage(UUID.randomUUID());
        unlockMessage.setPath(path);
        nodeBroadcasting(unlockMessage);
    }

    private synchronized void distributedUnlock(Object... path) {
        DistributedLock distributedLock = getDistributedLock(path);
        synchronized (distributedLock) {
            distributedLock.notifyAll();
        }
    }

    public void signal(String lockName, String conditionName) {
        SignalMessage signalMessage = new SignalMessage(UUID.randomUUID());
        signalMessage.setLockName(lockName);
        signalMessage.setConditionName(conditionName);
        nodeBroadcasting(signalMessage);
    }

    private void distributedSignal(String lockName, String conditionName) {
        LockImpl lock = (LockImpl) Cloud.getLock(lockName);
        if(lock != null) {
            ((LockImpl.ConditionImpl)lock.newCondition(conditionName)).distributedSignal();
        }
    }

    public void signalAll(String lockName, String conditionName) {
        SignalAllMessage signalAllMessage = new SignalAllMessage(UUID.randomUUID());
        signalAllMessage.setLockName(lockName);
        signalAllMessage.setConditionName(conditionName);
        nodeBroadcasting(signalAllMessage);
    }

    private void distributedSignalAll(String lockName, String conditionName) {
        LockImpl lock = (LockImpl) Cloud.getLock(lockName);
        if(lock != null) {
            ((LockImpl.ConditionImpl)lock.newCondition(conditionName)).distributedSignalAll();
        }
    }

    public void dispatchEvent(DistributedEvent event) {
        for (ServiceEndPoint serviceEndPoint : endPoints.values()) {
            if(!thisServiceEndPoint.getId().equals(serviceEndPoint.getId()) && serviceEndPoint.isDistributedEventListener()) {
                run(() -> {
                    Integer attempts = SystemProperties.getInteger(IOSystemProperties.Cloud.Orchestrator.Events.ATTEMPTS);
                    Long sleepTime = SystemProperties.getLong(IOSystemProperties.Cloud.Orchestrator.Events.SLEEP_PERIOD_BETWEEN_ATTEMPTS);
                    Boolean success = false;
                    for (int i = 1; i <= attempts; i++) {
                        try {
                            EventMessage eventMessage = new EventMessage(UUID.randomUUID());
                            eventMessage.setSessionId(ServiceSession.getCurrentIdentity().getId());
                            eventMessage.setSessionBean(ServiceSession.getCurrentIdentity().getBody());
                            eventMessage.setEvent(event);
                            Log.i(System.getProperty(IOSystemProperties.Cloud.Orchestrator.Events.LOG_TAG), "Sending event to %s, attempt %d", serviceEndPoint.toString(), i);
                            invokeNetworkComponent(serviceEndPoint, eventMessage);
                            success = true;
                            break;
                        } catch (Exception ex) {
                            Log.w(System.getProperty(IOSystemProperties.Cloud.Orchestrator.Events.LOG_TAG), "Couldn't dispatch event %s, attempt %d", ex, serviceEndPoint.toString(), i);
                        }
                        try {
                            Thread.sleep(sleepTime);
                        } catch (Exception ex) {
                            break;
                    }
                    }
                    if(!success) {
                        try {
                            StoreStrategyLayerInterface storeStrategyLayerInterface = Layers.get(StoreStrategyLayerInterface.class,
                                    SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.Events.STORE_STRATEGY));
                            storeStrategyLayerInterface.storeEvent(event);
                            Log.i(System.getProperty(IOSystemProperties.Cloud.Orchestrator.Events.LOG_TAG), "Event stored");
                        } catch (Exception ex) {
                            Log.w(System.getProperty(IOSystemProperties.Cloud.Orchestrator.Events.LOG_TAG), "Event discarded");
                        }
                    }
                }, ServiceSession.getCurrentIdentity());
            }
        }
    }

    private void distributedDispatchEvent(DistributedEvent event, Map<String,Object> sessionBean, UUID sessionId) {
        ServiceSession newIdentity;
        if(sessionBean != null && !sessionBean.isEmpty()) {
            newIdentity = ServiceSession.findSession(sessionBean);
        } else {
            newIdentity = ServiceSession.findSession(sessionId);
        }
        RemoteEvent remoteEvent = new RemoteEvent(event);
        ServiceSession.runAs(() -> Events.sendEvent(remoteEvent), newIdentity);
    }

    private DistributedLayer getDistributedLayer(boolean local, Object... path) {
        DistributedLayer distributedLayer;
        synchronized (sharedStore) {
            distributedLayer = (DistributedLayer) sharedStore.getInstance(path);
            if (distributedLayer == null) {
                try {
                    distributedLayer = new DistributedLayer(Class.forName((String)path[path.length - 2]),
                            (String)path[path.length - 1]);
                    if(local) {
                        addLocalObject(distributedLayer, List.of(thisNode.getId()), List.of(thisServiceEndPoint.getId()),
                                System.currentTimeMillis(), path);
                    } else {
                        addRemoteObject(distributedLayer, List.of(), List.of(), System.currentTimeMillis(), path);
                    }
                } catch (ClassNotFoundException ex) {
                    throw new HCJFRuntimeException("Class not found, trying to create the distributed layer instance", ex);
                }
            }
        }
        return distributedLayer;
    }

    public boolean isDistributedLayerPublished(Object... path) {
        return sharedStore.getInstance(path) != null;
    }

    public String getRegexFromDistributedLayer(Object... path) {
        String result = null;
        DistributedLayer distributedLayer = (DistributedLayer) sharedStore.getInstance(path);
        if(distributedLayer != null) {
            result = distributedLayer.getRegex();
        }
        return result;
    }

    public void publishDistributedLayer(String regex, Object... path) {
        DistributedLayer distributedLayer = getDistributedLayer(true, path);
        distributedLayer.setRegex(regex);
    }

    public void publishPlugin(byte[] jarFile) {
        PublishPluginMessage publishPluginMessage = new PublishPluginMessage();
        publishPluginMessage.setJarFile(jarFile);
        publishPluginMessage.setId(UUID.randomUUID());
        publishPluginMessage.setSessionId(ServiceSession.getCurrentIdentity().getId());
        nodeBroadcasting(publishPluginMessage);
    }

    public <O extends Object> O layerInvoke(Object[] parameters, Method method, Object... path) {
        O result;
        DistributedLayer distributedLayer = getDistributedLayer(false, path);

        LayerInvokeMessage layerInvokeMessage = new LayerInvokeMessage(UUID.randomUUID());
        layerInvokeMessage.setMethodName(method.getName());
        layerInvokeMessage.setParameterTypes(method.getParameterTypes());
        layerInvokeMessage.setSessionId(ServiceSession.getCurrentIdentity().getId());
        layerInvokeMessage.setSessionBean(ServiceSession.getCurrentIdentity().getBody());
        layerInvokeMessage.setParameters(parameters);
        layerInvokeMessage.setPath(path);

        UUID serviceEndPointId = distributedLayer.getServiceToInvoke();
        if(serviceEndPointId != null) {
                result = (O) invokeNetworkComponent(endPoints.get(serviceEndPointId), layerInvokeMessage);
        } else {
            throw new HCJFRuntimeException("Route not found to the layer: " + distributedLayer.getLayerInterface().getName() + "@" + distributedLayer.getLayerName());
        }

        return result;
    }

    /**
     * This method is called when a layer invoke message incoming.
     * @param sessionId Is of the session that invoke the remote layer.
     * @param sessionBean Serialized session instance.
     * @param parameterTypes Array with all the parameter types.
     * @param parameters Array with all the parameter instances.
     * @param methodName Name of the method.
     * @param path Path to found the layer implementation.
     * @return Return value of the layer invoke.
     */
    private Object distributedLayerInvoke(
            UUID sessionId, Map<String,Object> sessionBean, Class[] parameterTypes,
            Object[] parameters, String methodName, Object... path) {
        Object result;
        DistributedLayer distributedLayer = getDistributedLayer(true, path);
        Class layerInterface = distributedLayer.getLayerInterface();
        Map<String, DistributedLayerInvoker> invokers =
                Introspection.getInvokers(layerInterface, new DistributedLayerInvokerFilter(methodName, parameterTypes));
        if(invokers.size() == 1) {
            LayerInterface layer = Layers.get(layerInterface, distributedLayer.getLayerName());
            try {
                ServiceSession newIdentity;
                if(sessionBean != null && !sessionBean.isEmpty()) {
                    newIdentity = ServiceSession.findSession(sessionBean);
                } else {
                    newIdentity = ServiceSession.findSession(sessionId);
                }
                result = ServiceSession.callAs(() -> invokers.values().iterator().next().invoke(layer, parameters), newIdentity);
            } catch (Exception ex) {
                throw new HCJFRuntimeException("Remote method invocation fail, %s", ex, methodName);
            }
        } else {
            throw new HCJFRuntimeException("Remote method signature not found, %s(%s)", methodName, parameterTypes);
        }
        return result;
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean connected(Node node) {
        Objects.requireNonNull(node, "Null node");

        return changeStatus(node, Node.Status.CONNECTED);
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean connecting(Node node) {
        Objects.requireNonNull(node, "Null node");

        return changeStatus(node, Node.Status.CONNECTING);
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean disconnected(Node node) {
        Objects.requireNonNull(node, "Null node");

        return changeStatus(node, Node.Status.DISCONNECTED);
    }

    /**
     *
     * @param node
     * @param message
     */
    private void updateNode(Node node, NodeIdentificationMessage message) {
        node.setId(message.getNode().getId());
        node.setClusterName(message.getNode().getClusterName());
        node.setDataCenterName(message.getNode().getDataCenterName());
        node.setName(message.getNode().getName());
        node.setVersion(message.getNode().getVersion());
        node.setStartupDate(message.getNode().getStartupDate());
        node.setLanAddress(message.getNode().getLanAddress());
        node.setLanPort(message.getNode().getLanPort());
        node.setWanAddress(message.getNode().getWanAddress());
        node.setWanPort(message.getNode().getWanPort());
    }

    /**
     *
     * @param node
     * @param status
     * @return
     */
    private synchronized boolean changeStatus(Node node, Node.Status status) {
        boolean result = false;
        Node.Status currentStatus = node.getStatus();
        switch (currentStatus) {
            case CONNECTED: {
                if(status.equals(Node.Status.DISCONNECTED)) {
                    result = true;
                }
                break;
            }
            case DISCONNECTED: {
                if(status.equals(Node.Status.CONNECTING) || status.equals(Node.Status.LOST)) {
                    result = true;
                }
                break;
            }
            case CONNECTING: {
                if(status.equals(Node.Status.DISCONNECTED) || status.equals(Node.Status.CONNECTED)) {
                    result = true;
                }
                break;
            }
            case LOST: {
                if(status.equals(Node.Status.DISCONNECTED) || status.equals(Node.Status.CONNECTING)) {
                    result = true;
                }
                break;
            }
        }

        if(result) {
            node.setStatus(status);
        }

        return result;
    }

    public void publishPath(Object... path) {
        synchronized (sharedStore) {
            addPath(path);
            PublishPathMessage publishPathMessage = new PublishPathMessage();
            publishPathMessage.setPath(path);
            nodeBroadcasting(publishPathMessage);
        }
    }

    public void publishObject(Object object, Long timestamp, Object... path) {
        List<UUID> nodeIds = new ArrayList<>();
        nodeIds.add(thisNode.getId());

        List < Node > nodes = getSortedNodes();

        int replicationFactor = SystemProperties.getInteger(
                IOSystemProperties.Cloud.Orchestrator.REPLICATION_FACTOR);

        for (int i = 0; i < (replicationFactor - 1) && !nodes.isEmpty(); i++) {
            nodeIds.add(nodes.get(i).getId());
        }

        addLocalObject(object, nodeIds, List.of(), timestamp, path);

        PublishObjectMessage.Path pathObject = new PublishObjectMessage.Path(path, nodeIds);
        fork(() -> {
            if(!nodes.isEmpty()) {
                int counter = 0;
                for (Node node : nodes) {
                    PublishObjectMessage publishObjectMessage = new PublishObjectMessage(UUID.randomUUID());
                    publishObjectMessage.getPaths().add(pathObject);
                    publishObjectMessage.setTimestamp(timestamp);
                    if(counter < replicationFactor) {
                        pathObject.setValue(object);
                    } else {
                        pathObject.setValue(null);
                    }
                    invokeNetworkComponent(node, publishObjectMessage);
                    counter++;
                }
            }
        });
    }

    public void hidePath(Object... path) {
        removePath(path);
        HidePathMessage hidePathMessage = new HidePathMessage(UUID.randomUUID());
        hidePathMessage.setPath(path);
        nodeBroadcasting(hidePathMessage);
    }

    public <O extends Object> O invokeNode(Object... path) {
        O result = (O) sharedStore.getInstance(path);
        if(result instanceof RemoteLeaf) {
            InvokeMessage getMessage = new InvokeMessage(UUID.randomUUID());
            getMessage.setPath(path);
            Iterator<UUID> ids = ((RemoteLeaf)result).getNodes().iterator();
            Node node = null;
            while (ids.hasNext() && node == null) {
                node = nodes.get(ids.next());
            }

            if(node != null) {
                result = (O) invokeNetworkComponent(node, getMessage);
            } else {
                result = null;
            }
        }
        return result;
    }

    private void removePath(Object... path) {
        sharedStore.remove(path);
    }

    private boolean addPath(Object... path) {
        boolean result = false;
        if(sharedStore.getInstance(path) == null) {
            sharedStore.createPath(path);
            Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Local path added: %s", Arrays.toString(path));
            result = true;
        }
        return result;
    }

    private void addLocalObject(Object object, List<UUID> nodes, List<UUID> serviceEndPoints, Long timestamp, Object... path) {
        sharedStore.addLocalObject(object, nodes, serviceEndPoints, timestamp, path);
        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Local leaf added: %s", Arrays.toString(path));
    }

    private void addRemoteObject(Object object, List<UUID> nodes, List<UUID> serviceEndPoints, Long timestamp, Object... path) {
        sharedStore.addRemoteObject(object, nodes, serviceEndPoints, timestamp, path);
        Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG), "Remote leaf added: %s", Arrays.toString(path));
    }

    private Collection<JoinableMap> getNodesAsJoinableMap() {
        Collection<JoinableMap> result = new ArrayList<>();
        List<Node> nodeList = new ArrayList<>(nodesByLanId.values());
        for (Node node : nodeList) {
            result.add(new JoinableMap(Introspection.toMap(node)));
        }
        return result;
    }

    private Collection<JoinableMap> getServiceAsJoinableMap() {
        Collection<JoinableMap> result = new ArrayList<>();
        for(ServiceEndPoint serviceEndPoint : endPoints.values()) {
            result.add(new JoinableMap(Introspection.toMap(serviceEndPoint)));
        }
        return result;
    }

    private final class ResponseListener {

        private final Long timeout;
        private final NetworkComponent destination;
        private ResponseMessage responseMessage;

        public ResponseListener(NetworkComponent destination, Long timeout) {
            this.destination = destination;
            this.timeout = timeout;
        }

        public Object getResponse(Message message) {
            Object result;
            synchronized (this) {
                if (responseMessage == null) {
                    Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                            "Response listener waiting for id: %s", message.getId().toString());
                    try {
                        this.wait(timeout);
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (responseMessage != null) {
                if (responseMessage.getThrowable() != null) {
                    throw new HCJFRemoteException("Remote exception from %s",
                            responseMessage.getThrowable(), destination.getName());
                } else {
                    result = responseMessage.getValue();
                }
            } else {
                throw new HCJFRemoteInvocationTimeoutException("Remote invocation timeout from %s, message id: %s",
                        destination.getName(), message.getId().toString());
            }
            return result;
        }

        public void setMessage(ResponseMessage message) {
            synchronized (this) {
                Log.d(System.getProperty(IOSystemProperties.Cloud.LOG_TAG),
                        "Response listener notified with id: %s", message.getId().toString());
                responseMessage = message;
                this.notifyAll();
            }
        }
    }

    private static final class DistributedLayerInvoker extends Introspection.Invoker {

        public DistributedLayerInvoker(Class implementationClass, Method method) {
            super(implementationClass, method);
        }

    }

    private static final class DistributedLayerInvokerFilter implements Introspection.InvokerFilter<DistributedLayerInvoker> {

        private final String name;
        private final Class[] parameterTypes;
        private String hash;

        public DistributedLayerInvokerFilter(String name, Class[] parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;

            hash = name;
            if(parameterTypes != null) {
                for (Class type : parameterTypes) {
                    hash += type.getName();
                }
            }
        }

        @Override
        public Introspection.InvokerEntry<DistributedLayerInvoker> filter(Method method) {
            Introspection.InvokerEntry<DistributedLayerInvoker> result = null;
            if(method.getName().equalsIgnoreCase(name) && Arrays.equals(parameterTypes, method.getParameterTypes())) {
                result = new Introspection.InvokerEntry<>(method.getName(),
                        new DistributedLayerInvoker(method.getDeclaringClass(), method));
            }
            return result;
        }

        @Override
        public String getName() {
            return hash;
        }
    }

    private enum ReorganizationAction {

        CONNECT,

        DISCONNECT,

        TIME

    }

    public static final class SystemCloudNodeReadableImplementation extends Layer implements ReadRowsLayerInterface {

        public SystemCloudNodeReadableImplementation() {
            super(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisNode.READABLE_LAYER_IMPLEMENTATION_NAME));
        }

        @Override
        public Collection<JoinableMap> readRows(Queryable queryable) {
            return queryable.evaluate(getInstance().getNodesAsJoinableMap());
        }
    }

    public static final class SystemCloudServiceReadableImplementation extends Layer implements ReadRowsLayerInterface {

        public SystemCloudServiceReadableImplementation() {
            super(SystemProperties.get(IOSystemProperties.Cloud.Orchestrator.ThisServiceEndPoint.READABLE_LAYER_IMPLEMENTATION_NAME));
        }

        @Override
        public Collection<JoinableMap> readRows(Queryable queryable) {
            return queryable.evaluate(getInstance().getServiceAsJoinableMap());
        }
    }
}
