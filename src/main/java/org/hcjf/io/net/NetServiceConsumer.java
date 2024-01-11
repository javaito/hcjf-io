package org.hcjf.io.net;

import org.hcjf.errors.HCJFRuntimeException;
import org.hcjf.io.net.ssl.SslPeer;
import org.hcjf.log.Log;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;
import org.hcjf.service.Service;
import org.hcjf.service.ServiceConsumer;
import org.hcjf.service.ServiceSession;

import java.io.IOException;
import java.net.SocketOption;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This consumer provide an interface for the net service.
 * @author javaito
 */
public abstract class NetServiceConsumer<S extends NetSession, D extends Object> implements ServiceConsumer {

    private static final String NAME_TEMPLATE = "%s %s %d";

    private final String name;
    private final Integer port;
    private final NetService.TransportLayerProtocol protocol;
    private NetService service;
    private long writeWaitForTimeout;
    private Boolean decoupledIoAction;
    private Queue<DecoupledAction> actionsQueue;

    public NetServiceConsumer(Integer port, NetService.TransportLayerProtocol protocol) {
        this.port = port;
        this.protocol = protocol;
        writeWaitForTimeout = SystemProperties.getLong(IOSystemProperties.Net.WRITE_TIMEOUT);
        name = String.format(NAME_TEMPLATE, getClass().getName(), protocol.toString(), port);
        decoupledIoAction = false;
    }

    public final Boolean isDecoupledIoAction() {
        return decoupledIoAction;
    }

    /**
     * This method activate the decoupled io actions.
     * @param actionQueueSize Size of the actions queue.
     * @param workersNumber Number of workers to execute the actions.
     */
    public final void decoupleIoAction(Integer actionQueueSize, Integer workersNumber) {

        if(actionQueueSize <= 10) {
            throw new HCJFRuntimeException("The actions queue size can't be smaller than 11 places");
        }

        if(workersNumber <= 0) {
            throw new HCJFRuntimeException("The decoupled function must have at least one worker");
        }

        decoupledIoAction = true;
        actionsQueue = new ArrayBlockingQueue<>(actionQueueSize);
        for (int i = 0; i < workersNumber; i++) {
            Service.run(() -> {
                DecoupledAction decoupledAction;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (actionsQueue) {
                        decoupledAction = actionsQueue.poll();
                    }

                    if(decoupledAction != null) {
                        try {
                            ServiceSession.runAs(decoupledAction::onAction, decoupledAction.getServiceSession().currentIdentity());
                        } catch (Throwable throwable) {
                            Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Decoupled action error", throwable);
                        }
                    } else {
                        try {
                            synchronized (actionsQueue) {
                                actionsQueue.wait();
                            }
                        } catch (Exception ex) {
                            break;
                        }
                    }
                }
            }, ServiceSession.getSystemSession());
        }
    }

    /**
     * Add a new decoupled action into the queue.
     * @param decoupledAction Decoupled action instance.
     */
    protected final void addDecoupledAction(DecoupledAction decoupledAction) {
        if(isDecoupledIoAction()) {
            synchronized (actionsQueue) {
                actionsQueue.add(decoupledAction);
                actionsQueue.notifyAll();
            }
        } else {
            decoupledAction.onAction();
        }
    }

    /**
     * This method return a name to identify the consumer.
     * @return Consumer name.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the waiting time to write a package.
     * @return Waiting time to write a apackage.
     */
    public long getWriteWaitForTimeout() {
        return writeWaitForTimeout;
    }

    /**
     * Set the waiting time to write a package.
     * @param writeWaitForTimeout Waiting time to write a package.
     */
    public void setWriteWaitForTimeout(long writeWaitForTimeout) {
        this.writeWaitForTimeout = writeWaitForTimeout;
    }

    /**
     * This method ser the reference to net service,
     * this method only can be called from the net service
     * that will be associated
     * @param service Net service that will be associated.
     * @throws SecurityException If the method was called from other
     * method that not is NetService.registerConsumer().
     */
    public final void setService(NetService service) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        if(element.getClassName().equals(NetService.class.getName()) &&
                element.getMethodName().equals("registerConsumer")) {
            this.service = service;
        } else {
            throw new SecurityException("The method 'NetServiceConsumer.setService() only can be called from " +
                    "the net service that will be associated.'");
        }
    }

    /**
     * Returns the net service instance of the consumer.
     * @return Net service instance.
     */
    protected final NetService getService() {
        return service;
    }

    /**
     * Return the port of the consumer.
     * @return Port.
     */
    public final Integer getPort() {
        return port;
    }

    /**
     * This method should create the ssl engine for the consumer.
     * @return SSL engine implementation.
     */
    protected SslPeer getSslPeer() {
        throw new  UnsupportedOperationException("Unsupported ssl engine");
    }

    /**
     * Return the transport layer protocol of the consumer.
     * @return Transport layer consumer.
     */
    public final NetService.TransportLayerProtocol getProtocol() {
        return protocol;
    }

    /**
     * Disconnect the specific session.
     * @param session Net session.
     * @param message Disconnection message.
     */
    protected final void disconnect(S session, String message) {
        service.disconnect(session, message);
    }

    /**
     * Returns the shutdown frame to send before the net service shutdown.
     * @param session Session to create the shutdown frame.
     * @return Shutdown frame
     */
    public final byte[] getShutdownFrame(S session) {
        byte[] result = null;
        try {
            D shutdownPackage = getShutdownPackage(session);
            if (shutdownPackage != null) {
                result = encode(shutdownPackage);
            }
        } catch (Exception ex){
            //This exception is totally ignored because the shutdown procedure must be go on
        }
        return result;
    }

    /**
     * Returns the shutdown package to send before the net service shutdown.
     * @param session Session to create the package.
     * @return Shutdown package.
     */
    protected D getShutdownPackage(S session) {
        return null;
    }

    /**
     * This method writes some data over the session indicated,
     * this operation generate a blocking until the net service confirm
     * that the data was written over the communication channel
     * @param session Net session.
     * @param payLoad Data to be written
     * @throws IOException Exception for the io operations
     */
    protected final void write(S session, D payLoad) throws IOException {
        write(session, payLoad, true);
    }

    /**
     * This method writes some data over the session indicated.
     * @param session Net session.
     * @param payLoad Data to be written.
     * @param waitFor If this parameter is true then the operation generate
     *                a blocking over the communication channel.
     * @throws IOException Exception for io operations
     */
    protected final void write(S session, D payLoad, boolean waitFor) throws IOException {
        if(waitFor) {
            NetPackage netPackage = service.writeData(session, encode(payLoad));
            synchronized (netPackage) {
                try {
                    netPackage.wait(getWriteWaitForTimeout());
                } catch (InterruptedException e) {
                    Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Write wait for interrupted", e);
                }
            }

            switch (netPackage.getPackageStatus()) {
                case CONNECTION_CLOSE: {
                    throw new IOException("Connection Close");
                }
                case IO_ERROR: {
                    throw new IOException("IO Error");
                }
                case REJECTED_SESSION_LOCK: {
                    throw new IOException("Session locked");
                }
                case UNKNOWN_SESSION: {
                    throw new IOException("Unknown session");
                }
            }
        } else {
            NetPackage netPackage = service.writeData(session, encode(payLoad));
        }
    }

    /**
     * This method abstracts the connection event to use the entities of the domain's implementation.
     * @param netPackage Connection package.
     */
    public final void onConnect(NetPackage netPackage) {
        onConnect((S) netPackage.getSession(), decode(netPackage), netPackage);
    }

    /**
     * Method that must be implemented by the custom implementation to know when a session is connected
     * @param session Connected session.
     * @param payLoad Decoded package payload.
     * @param netPackage Original package.
     */
    protected void onConnect(S session, D payLoad, NetPackage netPackage) {}

    /**
     * This method abstracts the disconnection event to use the entities of the domain's implementation.
     * @param netPackage Disconnection package.
     */
    public final void onDisconnect(NetPackage netPackage) {
        synchronized (netPackage) {
            netPackage.notify();
        }

        onDisconnect((S) netPackage.getSession(), netPackage);
    }

    /**
     * Method must be implemented by the custom implementation to known when a session is disconnected
     * @param session Disconnected session.
     * @param netPackage Original package.
     */
    protected void onDisconnect(S session, NetPackage netPackage) {}

    /**
     * When the net service receive data call this method to process the package
     * @param netPackage Net package.
     */
    public final void onRead(NetPackage netPackage) {
        S session = (S) netPackage.getSession();
        D decodedPackage = decode(netPackage);
        try {
            session = checkSession(session, decodedPackage, netPackage);
            session.setChecked(true);

            try {
                onRead(session, decodedPackage, netPackage);
            } catch (Exception ex) {
                Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "On read method fail", ex);
            }
        } catch (Exception ex){
            Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Check session fail", ex);
            session.setChecked(false);
            onCheckSessionError(session, decodedPackage, netPackage, ex);
        }
    }

    /**
     * When the net service receive data, this method is called to process the package.
     * @param session Net session.
     * @param payLoad Net package decoded
     * @param netPackage Net package.
     */
    protected void onRead(S session, D payLoad, NetPackage netPackage) {}


    /**
     * When an exception is occurred while checking session,
     * this method is called to process the package according the exception information.
     * @param session Net session.
     * @param payLoad Net package decoded
     * @param netPackage Net package.
     * @param cause Error cause.
     */
    protected void onCheckSessionError(S session, D payLoad, NetPackage netPackage, Throwable cause) {}

    /**
     * When the net service write data then call this method to process the package.
     * @param netPackage Net package.
     */
    public final void onWrite(NetPackage netPackage) {
        synchronized (netPackage) {
            netPackage.notify();
        }
        onWrite((S)netPackage.getSession(), netPackage);
    }

    /**
     * When the net service write data then call this method to process the package.
     * @param session Net session.
     * @param netPackage Net package.
     */
    protected void onWrite(S session, NetPackage netPackage){}

    /**
     * This method decode the implementation data.
     * @param payLoad Implementation data.
     * @return Implementation data encoded.
     */
    protected abstract byte[] encode(D payLoad);

    /**
     * This method decode the net package to obtain the implementation data
     * @param netPackage Net package.
     * @return Return the implementation data.
     */
    protected abstract D decode(NetPackage netPackage);

    /**
     * Destroy the session.
     * @param session Net session to be destroyed
     */
    public abstract void destroySession(NetSession session);

    /**
     * Check the channel session.
     * @param session Created session.
     * @param payLoad Decoded package.
     * @param netPackage Net package.
     * @return Updated session.
     */
    public abstract S checkSession(S session, D payLoad, NetPackage netPackage);

    /**
     * Return the socket options of the implementation.
     * @return Socket options.
     */
    public Map<SocketOption, Object> getSocketOptions() {
        return null;
    }

    /**
     * Interface to decoupled the read action.
     */
    public static abstract class DecoupledAction {

        public final ServiceSession serviceSession;

        public DecoupledAction(ServiceSession serviceSession) {
            this.serviceSession = serviceSession;
        }

        public ServiceSession getServiceSession() {
            return serviceSession;
        }

        public abstract void onAction();
    }
}