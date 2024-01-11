package org.hcjf.properties;

import org.hcjf.cloud.impl.DefaultCloudServiceImpl;

import java.util.Properties;

public class IOSystemProperties  extends Properties implements DefaultProperties {

    public static final class FileSystem {
        public static final String SERVICE_NAME = "hcjf.file.system.service.name";
        public static final String SERVICE_PRIORITY = "hcjf.file.system.service.priority";
        public static final String LOG_TAG = "hcjf.file.system.log.tag";
        public static final String POLLING_WAIT_TIME = "hcjf.file.system.polling.wait.time";
    }

    public static final class Net {
        public static final String SERVICE_NAME = "hcjf.net.service.name";
        public static final String LOG_TAG = "hcjf.net.log.tag";
        public static final String INPUT_BUFFER_SIZE = "hcjf.net.input.buffer.size";
        public static final String OUTPUT_BUFFER_SIZE = "hcjf.net.output.buffer.size";
        public static final String DISCONNECT_AND_REMOVE = "hcjf.net.disconnect.and.remove";
        public static final String CONNECTION_TIMEOUT_AVAILABLE = "hcjf.net.connection.timeout.available";
        public static final String CONNECTION_TIMEOUT = "hcjf.net.connection.timeout";
        public static final String WRITE_TIMEOUT = "hcjf.net.write.timeout";
        public static final String IO_UDP_LRU_SESSIONS_SIZE = "hcjf.net.io.udp.lru.sessions.size";
        public static final String IO_UDP_LRU_ADDRESSES_SIZE = "hcjf.net.io.udp.lru.addresses.size";
        public static final String IO_QUEUE_SIZE = "hcjf.net.io.queue.size";
        public static final String IO_THREAD_POOL_KEEP_ALIVE_TIME = "hcjf.net.io.thread.pool.keep.alive.time";
        public static final String IO_THREAD_POOL_NAME = "hcjf.net.io.thread.pool.name";
        public static final String DEFAULT_INPUT_BUFFER_SIZE = "hcjf.net.default.input.buffer.size";
        public static final String DEFAULT_OUTPUT_BUFFER_SIZE = "hcjf.net.default.output.buffer.size";
        public static final String IO_THREAD_DIRECT_ALLOCATE_MEMORY = "hcjf.net.io.thread.direct.allocate.memory";
        public static final String SSL_MAX_IO_THREAD_POOL_SIZE = "hcjf.net.ssl.max.io.thread.pool.size";
        public static final String PORT_PROVIDER_TIME_WINDOWS_SIZE = "hcjf.net.port.provider.time.windows.size";
        public static final String PORT_PROBE_CONNECTION_TIMEOUT = "hcjf.net.port.probe.connection.timeout";
        public static final String REMOTE_ADDRESS_INTO_NET_PACKAGE = "hcjf.net.remote.address.into.net.package";
        public static final String REMOTE_ADDRESS_INTO_NET_SESSION = "hcjf.net.remote.address.into.net.session";
        public static final String NIO_SELECTOR_HEALTH_CHECKER_RUNNING_TIME = "hcjf.net.nio.selector.health.checker.running.time";
        public static final String NIO_SELECTOR_HEALTH_CHECKER_SAMPLE_TIME = "hcjf.net.nio.selector.health.checker.sample.time";
        public static final String NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_THRESHOLD = "hcjf.net.nio.selector.health.checker.dangerous.threshold";
        public static final String NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_REPEATS = "hcjf.net.nio.selector.health.checker.dangerous.repeats";
        public static final String NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_ACTION = "hcjf.net.nio.selector.health.checker.dangerous.action";

        public static final class Broadcast {
            public static final String SERVICE_NAME = "hcjf.net.broadcast.service.name";
            public static final String LOG_TAG = "hcjf.net.broadcast.log.tag";
            public static final String INTERFACE_NAME = "hcjf.net.broadcast.interface.name";
            public static final String IP_VERSION = "hcjf.net.broadcast.ip.version";
            public static final String SENDER_DELAY = "hcjf.net.broadcast.sender.delay";
            public static final String SIGNATURE_ALGORITHM = "hcjf.net.broadcast.signature.algorithm";
            public static final String RECEIVER_BUFFER_SIZE = "hcjf.net.broadcast.receiver.buffer.size";
        }

        public static final class KubernetesSpy {
            public static final String SERVICE_NAME = "hcjf.net.kubernetes.service.name";
            public static final String LOG_TAG = "hcjf.net.kubernetes.log.tag";
            public static final String CLIENT_CONNECTION_TIMEOUT = "hcjf.net.kubernetes.client.connection.timeout";
            public static final String TASK_SLEEP_TIME = "hcjf.net.kubernetes.task.sleep.time";
            public static final String CURL_COMMAND = "hcjf.net.kubernetes.curl.command";
            public static final String CURL_COMMAND_AUTHENTICATION_HEADER = "hcjf.net.kubernetes.curl.command.authentication.header";
            public static final String CURL_COMMAND_CACERT_PARAMETER = "hcjf.net.kubernetes.curl.command.cacert.parameter";
            public static final String CACERT_FILE_PATH = "hcjf.net.kubernetes.cacert.file.path";
            public static final String TOKEN_FILE_PATH = "hcjf.net.kubernetes.token.file.path";
            public static final String NAMESPACE_FILE_PATH = "hcjf.net.kubernetes.namespace.file.path";
            public static final String MASTER_NODE_HOST = "hcjf.net.kubernetes.master.node.host";
            public static final String MASTER_NODE_PORT = "hcjf.net.kubernetes.master.node.port";
            public static final String AUTHORIZATION_HEADER = "hcjf.net.kubernetes.authorization.header";
            public static final String JSON_DATE_FORMAT = "hcjf.net.kubernetes.json.date.format";

            public static final class EndPoints {
                public static final String LIST_PODS = "hcjf.net.kubernetes.end.points.list.pods";
                public static final String LIST_SERVICES = "hcjf.net.kubernetes.end.points.list.services";
                public static final String LIST_END_POINTS = "hcjf.net.kubernetes.end.points.list.end_points";
            }
        }

        public static final class Ssl {
            public static final String DEFAULT_PROTOCOL = "hcjf.net.ssl.default.protocol";
            public static final String IO_THREAD_NAME = "hcjf.net.ssl.io.thread.name";
            public static final String ENGINE_THREAD_NAME = "hcjf.net.ssl.engine.thread.name";
            public static final String DEFAULT_KEYSTORE_PASSWORD = "hcjf.net.ssl.default.keystore.password";
            public static final String DEFAULT_KEY_PASSWORD = "hcjf.net.ssl.default.key.password";
            public static final String DEFAULT_KEYSTORE_FILE_PATH = "hcjf.net.ssl.default.keystore.file.path";
            public static final String DEFAULT_TRUSTED_CERTS_FILE_PATH = "hcjf.net.ssl.default.trusted.certs.file.path";
            public static final String DEFAULT_KEY_TYPE = "hcjf.net.ssl.default.key.type";

        }

        public static final class Messages {
            public static final String LOG_TAG = "hcjf.net.messages.log.tag";
            public static final String SERVER_DECOUPLED_IO_ACTION = "hcjf.net.messages.server.decoupled.io.action";
            public static final String SERVER_IO_QUEUE_SIZE = "hcjf.net.messages.server.io.queue.size";
            public static final String SERVER_IO_WORKERS = "hcjf.net.messages.server.io.workers";
        }

        public static final class Http {
            public static final String INPUT_LOG_ENABLED = "hcjf.net.http.server.input.log.enabled";
            public static final String OUTPUT_LOG_ENABLED = "hcjf.net.http.server.output.log.enabled";
            public static final String LOG_TAG = "hcjf.net.http.server.log.tag";
            public static final String SERVER_NAME = "hcjf.net.http.server.name";
            public static final String RESPONSE_DATE_HEADER_FORMAT_VALUE = "hcjf.net.http.response.date.header.format.value";
            public static final String INPUT_LOG_BODY_MAX_LENGTH = "hcjf.net.http.input.log.body.max.length";
            public static final String OUTPUT_LOG_BODY_MAX_LENGTH = "hcjf.net.http.output.log.body.max.length";
            public static final String DEFAULT_SERVER_PORT = "hcjf.net.http.default.server.port";
            public static final String DEFAULT_CLIENT_PORT = "hcjf.net.http.default.client.port";
            public static final String STREAMING_LIMIT_FILE_SIZE = "hcjf.net.http.streaming.limit.file.size";
            public static final String DEFAULT_ERROR_FORMAT_SHOW_STACK = "hcjf.net.http.default.error.format.show.stack";
            public static final String DEFAULT_CLIENT_CONNECT_TIMEOUT = "hcjf.net.http.default.client.connect.timeout";
            public static final String DEFAULT_CLIENT_READ_TIMEOUT = "hcjf.net.http.default.client.read.timeout";
            public static final String DEFAULT_CLIENT_WRITE_TIMEOUT = "hcjf.net.http.default.client.write.timeout";
            public static final String DEFAULT_GUEST_SESSION_NAME = "hcjf.net.http.default.guest.session.name";
            public static final String DEFAULT_FILE_CHECKSUM_ALGORITHM = "hcjf.net.http.default.file.checksum.algorithm";
            public static final String ENABLE_AUTOMATIC_RESPONSE_CONTENT_LENGTH = "hcjf.net.http.enable.automatic.response.content.length";
            public static final String AUTOMATIC_CONTENT_LENGTH_SKIP_CODES = "hcjf.net.http.automatic.content.length.skip.codes";
            public static final String MAX_PACKAGE_SIZE = "hcjf.net.http.max.package.size";
            public static final String SERVER_DECOUPLED_IO_ACTION = "hcjf.net.http.server.decoupled.io.action";
            public static final String SERVER_IO_QUEUE_SIZE = "hcjf.net.http.server.io.queue.size";
            public static final String SERVER_IO_WORKERS = "hcjf.net.http.server.io.workers";
            public static final String HOST_ACCESS_CONTROL_REGEX_START_CHAR = "hcjf.net.http.host.access.control.regex.start.char";
            public static final String CLIENT_RESPONSE_HANDLER_QUEUE_SIZE = "hcjf.net.http.client.response.handler.queue.size";

            public static final class Http2 {
                public static final String HEADER_TABLE_SIZE = "hcjf.net.http.http2.header.table.size";
                public static final String ENABLE_PUSH = "hcjf.net.http.http2.enable.push";
                public static final String MAX_CONCURRENT_STREAMS = "hcjf.net.http.http2.max.concurrent.streams";
                public static final String INITIAL_WINDOWS_SIZE = "hcjf.net.http.http2.initial.windows.size";
                public static final String MAX_FRAME_SIZE = "hcjf.net.http.http2.max.frame.size";
                public static final String MAX_HEADER_LIST_SIZE = "hcjf.net.http.http2.max.header.list.size";
                public static final String STREAM_FRAMES_QUEUE_MAX_SIZE = "hcjf.net.http.http2.stream.frames.queue.max.size";
            }

            public static final class Folder {
                public static final String LOG_TAG = "hcjf.net.http.folder.log.tag";
                public static final String FORBIDDEN_CHARACTERS = "hcjf.net.http.folder.forbidden.characters";
                public static final String FILE_EXTENSION_REGEX = "hcjf.net.http.folder.file.extension.regex";
                public static final String DEFAULT_HTML_DOCUMENT = "hcjf.net.http.folder.default.html.document";
                public static final String DEFAULT_HTML_BODY = "hcjf.net.http.folder.default.html.body";
                public static final String DEFAULT_HTML_ROW = "hcjf.net.http.folder.default.html.row";
                public static final String ZIP_CONTAINER = "hcjf.net.http.folder.zip.container";
                public static final String ZIP_TEMP_PREFIX = "hcjf.net.http.folder.zip.temp.prefix";
                public static final String JAR_CONTAINER = "hcjf.net.http.folder.jar.container";
                public static final String JAR_TEMP_PREFIX = "hcjf.net.http.folder.jar.temp.prefix";
            }

            public static final class EndPoint {

                public static final class Json {
                    public static final String DATE_FORMATS = "hcjf.net.http.end.point.json.date.formats";
                }

            }

            public static final class DataSources {
                public static final String SERVICE_NAME = "hcjf.net.http.data.sources.service.name";
                public static final String SERVICE_PRIORITY = "hcjf.net.http.data.sources.service.priority";
                public static final String THREAD_POOL_ENABLED = "hcjf.net.http.data.sources.service.thread.pool.enabled";
                public static final String THREAD_POOL_CORE_SIZE = "hcjf.net.http.data.sources.service.thread.pool.core.size";
                public static final String THREAD_POOL_MAX_SIZE = "hcjf.net.http.data.sources.service.thread.pool.max.size";
                public static final String THREAD_POOL_KEEP_ALIVE_TIME = "hcjf.net.http.data.sources.service.thread.pool.keep.alive.time";
            }
        }

        public static final class Https {
            public static final String DEFAULT_SERVER_PORT = "hcjf.net.https.default.server.port";
            public static final String DEFAULT_CLIENT_PORT = "hcjf.net.https.default.server.port";
        }

        public static final class Rest {
            public static final String DEFAULT_MIME_TYPE = "hcjf.rest.default.mime.type";
            public static final String DEFAULT_ENCODING_IMPL = "hcjf.rest.default.encoding.impl";
            public static final String QUERY_PATH = "hcjf.rest.query.path";
            public static final String QUERY_PARAMETER = "hcjf.rest.query.parameter.path";
            public static final String BODY_FIELD = "hcjf.net.http.rest.body.field";
            public static final String QUERY_FIELD = "hcjf.net.http.rest.query.field";
            public static final String QUERIES_FIELD = "hcjf.net.http.rest.queries.field";
            public static final String DATA_SOURCE_FIELD = "hcjf.net.http.rest.data.source.field";
            public static final String COMMAND_FIELD = "hcjf.net.http.rest.command.field";
            public static final String COMMANDS_FIELD = "hcjf.net.http.rest.commands.field";
        }

    }

    public static final class ProcessDiscovery {
        public static final String LOG_TAG = "hcjf.process.log.tag";
        public static final String SERVICE_NAME = "hcjf.process.discovery.service.name";
        public static final String SERVICE_PRIORITY = "hcjf.process.discovery.service.priority";
        public static final String DELAY = "hcjf.process.delay";
    }

    public static class Cloud {
        public static final String SERVICE_NAME = "hcjf.cloud.name";
        public static final String SERVICE_PRIORITY = "hcjf.cloud.priority";
        public static final String IMPL = "hcjf.cloud.impl";
        public static final String LOG_TAG = "hcjf.cloud.log.tag";

        public static class Orchestrator {
            public static final String SERVICE_NAME = "hcjf.cloud.orchestrator.name";
            public static final String SERVICE_PRIORITY = "hcjf.cloud.orchestrator.service.priority";
            public static final String AVAILABLE = "hcjf.cloud.orchestrator.available";
            public static final String SERVER_LISTENER_PORT = "hcjf.cloud.orchestrator.server.listener.port";
            public static final String CONNECTION_LOOP_WAIT_TIME = "hcjf.cloud.orchestrator.connection.loop.wait.time";
            public static final String NODE_LOST_TIMEOUT = "hcjf.cloud.orchestrator.node.lost.timeout";
            public static final String ACK_TIMEOUT = "hcjf.cloud.orchestrator.ack.timeout";
            public static final String CLUSTER_NAME = "hcjf.cloud.orchestrator.cluster.name";
            public static final String WAGON_TIMEOUT = "hcjf.cloud.orchestrator.wagon.timeout";
            public static final String REORGANIZATION_TIMEOUT = "hcjf.cloud.orchestrator.reorganization.timeout";
            public static final String REORGANIZATION_WARNING_TIME_LIMIT = "hcjf.cloud.orchestrator.reorganization.warning.time.limit";
            public static final String INVOKE_TIMEOUT = "hcjf.cloud.orchestrator.invokeNode.timeout";
            public static final String TEST_NODE_TIMEOUT = "hcjf.cloud.orchestrator.test.node.timeout";
            public static final String REPLICATION_FACTOR = "hcjf.cloud.orchestrator.replication.factor";
            public static final String NODES = "hcjf.cloud.orchestrator.nodes";
            public static final String SERVICE_END_POINTS = "hcjf.cloud.orchestrator.service.end.points";
            public static final String SERVICE_PUBLICATION_REPLICAS_BROADCASTING_ENABLED = "hcjf.cloud.orchestrator.service.publication.broadcasting.enabled";
            public static final String SERVICE_PUBLICATION_REPLICAS_BROADCASTING_TIMEOUT = "hcjf.cloud.orchestrator.service.publication.broadcasting.timeout";
            public static final String NETWORKING_HANDSHAKE_DETAILS_AVAILABLE = "hcjf.cloud.orchestrator.networking.handshake.details.available";

            public static final class Events {
                public static final String LOG_TAG = "hcjf.cloud.orchestrator.events.log.tag";
                public static final String TIMEOUT = "hcjf.cloud.orchestrator.events.timeout";
                public static final String ATTEMPTS = "hcjf.cloud.orchestrator.events.attempts";
                public static final String SLEEP_PERIOD_BETWEEN_ATTEMPTS = "hcjf.cloud.orchestrator.events.sleep.period.between.attempts";
                public static final String STORE_STRATEGY = "hcjf.cloud.orchestrator.events.store.strategy";
            }

            public static final class Kubernetes {
                public static final String ENABLED = "hcjf.cloud.orchestrator.kubernetes.enabled";
                public static final String POD_LABELS = "hcjf.cloud.orchestrator.kubernetes.pod.labels";
                public static final String NAMESPACE = "hcjf.cloud.orchestrator.kubernetes.namespace";
                public static final String SERVICE_NAME = "hcjf.cloud.orchestrator.kubernetes.service.name";
                public static final String SERVICE_LABELS = "hcjf.cloud.orchestrator.kubernetes.service.labels";
                public static final String SERVICE_PORT_NAME = "hcjf.cloud.orchestrator.kubernetes.service.port.name";
                public static final String ALLOW_PHASES = "hcjf.cloud.orchestrator.kubernetes.allow.phases";
            }

            public static final class ThisNode {
                public static final String READABLE_LAYER_IMPLEMENTATION_NAME = "hcjf.cloud.orchestrator.this.node.readable.layer.implementation.name";
                public static final String ID = "hcjf.cloud.orchestrator.this.node.id";
                public static final String NAME = "hcjf.cloud.orchestrator.this.node.name";
                public static final String VERSION = "hcjf.cloud.orchestrator.this.node.version";
                public static final String CLUSTER_NAME = "hcjf.cloud.orchestrator.this.node.cluster.name";
                public static final String DATA_CENTER_NAME = "hcjf.cloud.orchestrator.this.node.data.center.name";
                public static final String LAN_ADDRESS = "hcjf.cloud.orchestrator.this.node.lan.address";
                public static final String LAN_PORT = "hcjf.cloud.orchestrator.this.node.lan.port";
                public static final String WAN_ADDRESS = "hcjf.cloud.orchestrator.this.node.wan.address";
                public static final String WAN_PORT = "hcjf.cloud.orchestrator.this.node.wan.port";
            }

            public static final class ThisServiceEndPoint {
                public static final String READABLE_LAYER_IMPLEMENTATION_NAME = "hcjf.cloud.orchestrator.this.service.end.point.readable.layer.implementation.name";
                public static final String ID = "hcjf.cloud.orchestrator.this.service.end.point.id";
                public static final String NAME = "hcjf.cloud.orchestrator.this.service.end.point.name";
                public static final String GATEWAY_ADDRESS = "hcjf.cloud.orchestrator.this.service.end.point.gateway.address";
                public static final String GATEWAY_PORT = "hcjf.cloud.orchestrator.this.service.end.point.gateway.port";
                public static final String PUBLICATION_TIMEOUT = "hcjf.cloud.orchestrator.this.service.end.point.publication.timeout";
                public static final String DISTRIBUTED_EVENT_LISTENER = "hcjf.cloud.orchestrator.this.service.end.point.distributed.event.listener";
            }

            public static final class Broadcast {
                public static final String ENABLED = "hcjf.cloud.orchestrator.broadcast.enabled";
                public static final String TASK_NAME = "hcjf.cloud.orchestrator.broadcast.task.name";
                public static final String IP_VERSION = "hcjf.cloud.orchestrator.broadcast.ip.version";
                public static final String INTERFACE_NAME = "hcjf.cloud.orchestrator.broadcast.interface.name";
                public static final String PORT = "hcjf.cloud.orchestrator.broadcast.port";
            }
        }

        public static class TimerTask {
            public static final String MIN_VALUE_OF_DELAY = "hcjf.cloud.timer.task.min.value.of.delay";
            public static final String MAP_NAME = "hcjf.cloud.timer.task.map.name";
            public static final String MAP_SUFFIX_NAME = "hcjf.cloud.timer.task.map.suffix.name";
            public static final String LOCK_SUFFIX_NAME = "hcjf.cloud.timer.task.lock.suffix.name";
            public static final String CONDITION_SUFFIX_NAME = "hcjf.cloud.timer.task.condition.suffix.name";
        }

        public static class Cache {
            public static final String MAP_SUFFIX_NAME = "hcjf.cloud.cache.map.suffix.name";
            public static final String LOCK_SUFFIX_NAME = "hcjf.cloud.cache.lock.suffix.name";
            public static final String CONDITION_SUFFIX_NAME = "hcjf.cloud.cache.condition.suffix.name";
            public static final String SIZE_STRATEGY_MAP_SUFFIX_NAME = "hcjf.cloud.cache.size.strategy.map.suffix.name";
        }

        public static class Queue {
            public static final String LOCK_NAME_TEMPLATE = "hcjf.cloud.queue.lock.name.template";
            public static final String CONDITION_NAME_TEMPLATE = "hcjf.cloud.queue.condition.name.template";
            public static final String DEFAULT_SIZE = "hcjf.cloud.queue.default.size";
        }
    }

    public static final class Event {
        public static final String LOG_TAG = "hcjf.event.log.tag";
        public static final String SERVICE_NAME = "hcjf.event.service.name";
        public static final String SERVICE_PRIORITY = "hcjf.event.service.priority";
    }

    private Properties properties;

    public IOSystemProperties() {

        properties = new Properties();
        properties.put(FileSystem.SERVICE_NAME, "FileSystemWatcherService");
        properties.put(FileSystem.SERVICE_PRIORITY, "1");
        properties.put(FileSystem.LOG_TAG, "FILE_SYSTEM_WATCHER_SERVICE");
        properties.put(FileSystem.POLLING_WAIT_TIME, "5000");

        properties.put(Net.SERVICE_NAME, "Net service");
        properties.put(Net.LOG_TAG, "NET_SERVICE");
        properties.put(Net.INPUT_BUFFER_SIZE, "1024");
        properties.put(Net.OUTPUT_BUFFER_SIZE, "1024");
        properties.put(Net.CONNECTION_TIMEOUT_AVAILABLE, "true");
        properties.put(Net.CONNECTION_TIMEOUT, "30000");
        properties.put(Net.DISCONNECT_AND_REMOVE, "true");
        properties.put(Net.WRITE_TIMEOUT, "100");
        properties.put(Net.IO_UDP_LRU_ADDRESSES_SIZE, "1000");
        properties.put(Net.IO_UDP_LRU_SESSIONS_SIZE, "1000");
        properties.put(Net.IO_QUEUE_SIZE, "1000000");
        properties.put(Net.IO_THREAD_POOL_KEEP_ALIVE_TIME, "120");
        properties.put(Net.IO_THREAD_POOL_NAME, "IoThreadPool");
        properties.put(Net.DEFAULT_INPUT_BUFFER_SIZE, "1024");
        properties.put(Net.DEFAULT_OUTPUT_BUFFER_SIZE, "1024");
        properties.put(Net.IO_THREAD_DIRECT_ALLOCATE_MEMORY, "false");
        properties.put(Net.SSL_MAX_IO_THREAD_POOL_SIZE, "2");
        properties.put(Net.PORT_PROVIDER_TIME_WINDOWS_SIZE, "15000");
        properties.put(Net.PORT_PROBE_CONNECTION_TIMEOUT, "1000");
        properties.put(Net.REMOTE_ADDRESS_INTO_NET_PACKAGE, "false");
        properties.put(Net.REMOTE_ADDRESS_INTO_NET_SESSION, "false");
        properties.put(Net.NIO_SELECTOR_HEALTH_CHECKER_RUNNING_TIME, "1000");
        properties.put(Net.NIO_SELECTOR_HEALTH_CHECKER_SAMPLE_TIME, "2000");
        properties.put(Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_THRESHOLD, "60");
        properties.put(Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_REPEATS, "5");
        properties.put(Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_ACTION, "RECREATE_SELECTOR"); //Valid values [RECREATE_SELECTOR, SHUTDOWN, VOID]
        properties.put(Net.Http.HOST_ACCESS_CONTROL_REGEX_START_CHAR,"^");
        properties.put(Net.Http.CLIENT_RESPONSE_HANDLER_QUEUE_SIZE, "1000");

        properties.put(Net.Broadcast.SERVICE_NAME, "Broadcast service");
        properties.put(Net.Broadcast.LOG_TAG, "BROADCAST");
        properties.put(Net.Broadcast.INTERFACE_NAME, "eth0");
        properties.put(Net.Broadcast.IP_VERSION, "4");
        properties.put(Net.Broadcast.SENDER_DELAY, "30000");
        properties.put(Net.Broadcast.SIGNATURE_ALGORITHM, "SHA-1");
        properties.put(Net.Broadcast.RECEIVER_BUFFER_SIZE, "1024");

        properties.put(Net.KubernetesSpy.SERVICE_NAME, "Kubernetes Spy Service");
        properties.put(Net.KubernetesSpy.LOG_TAG, "KUBERNETES_SPY");
        properties.put(Net.KubernetesSpy.CLIENT_CONNECTION_TIMEOUT, "10000");
        properties.put(Net.KubernetesSpy.TASK_SLEEP_TIME, "5000");
        properties.put(Net.KubernetesSpy.CURL_COMMAND, "curl");
        properties.put(Net.KubernetesSpy.CURL_COMMAND_AUTHENTICATION_HEADER, "-H 'Authorization: Bearer %s'");
        properties.put(Net.KubernetesSpy.CURL_COMMAND_CACERT_PARAMETER, "--cacert %s");
        properties.put(Net.KubernetesSpy.CACERT_FILE_PATH, "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
        properties.put(Net.KubernetesSpy.TOKEN_FILE_PATH, "/var/run/secrets/kubernetes.io/serviceaccount/token");
        properties.put(Net.KubernetesSpy.NAMESPACE_FILE_PATH, "/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        properties.put(Net.KubernetesSpy.MASTER_NODE_HOST, "KUBERNETES_PORT_443_TCP_ADDR");
        properties.put(Net.KubernetesSpy.MASTER_NODE_PORT, "KUBERNETES_SERVICE_PORT");
        properties.put(Net.KubernetesSpy.EndPoints.LIST_PODS, "https://%s:%s/api/v1/namespaces/%s/pods");
        properties.put(Net.KubernetesSpy.EndPoints.LIST_SERVICES, "https://%s:%s/api/v1/namespaces/%s/services");
        properties.put(Net.KubernetesSpy.EndPoints.LIST_END_POINTS, "https://%s:%s/api/v1/namespaces/%s/endpoints");
        properties.put(Net.KubernetesSpy.AUTHORIZATION_HEADER, "Bearer %s");
        properties.put(Net.KubernetesSpy.JSON_DATE_FORMAT, "yyyy-MM-dd'T'HH:mm:ss'Z'");

        properties.put(Net.Ssl.DEFAULT_KEY_PASSWORD, "hcjfkeypassword");
        properties.put(Net.Ssl.DEFAULT_KEY_TYPE, "JKS");
        properties.put(Net.Ssl.DEFAULT_KEYSTORE_PASSWORD, "hcjfkeystorepassword");
        properties.put(Net.Ssl.DEFAULT_KEYSTORE_FILE_PATH, "/home/javaito/Git/HolandaCatalinaFw/src/main/resources/org/hcjf/io/net/https/keystore.jks");
        properties.put(Net.Ssl.DEFAULT_TRUSTED_CERTS_FILE_PATH, "/home/javaito/Git/HolandaCatalinaFw/src/main/resources/org/hcjf/io/net/https/cacerts.jks");
        properties.put(Net.Ssl.DEFAULT_PROTOCOL, "TLSv1.2");
        properties.put(Net.Ssl.IO_THREAD_NAME, "SslIoThread");
        properties.put(Net.Ssl.ENGINE_THREAD_NAME, "SslEngineThread");

        properties.put(Net.Messages.LOG_TAG, "MESSAGES");
        properties.put(Net.Messages.SERVER_DECOUPLED_IO_ACTION, "true");
        properties.put(Net.Messages.SERVER_IO_QUEUE_SIZE, "100000");
        properties.put(Net.Messages.SERVER_IO_WORKERS, "5");

        properties.put(Net.Http.INPUT_LOG_ENABLED, "false");
        properties.put(Net.Http.OUTPUT_LOG_ENABLED, "false");
        properties.put(Net.Http.LOG_TAG, "HTTP_SERVER");
        properties.put(Net.Http.SERVER_NAME, "HCJF Web Server");
        properties.put(Net.Http.RESPONSE_DATE_HEADER_FORMAT_VALUE, "EEE, dd MMM yyyy HH:mm:ss z");
        properties.put(Net.Http.INPUT_LOG_BODY_MAX_LENGTH, "128");
        properties.put(Net.Http.OUTPUT_LOG_BODY_MAX_LENGTH, "128");
        properties.put(Net.Http.DEFAULT_SERVER_PORT, "80");
        properties.put(Net.Http.DEFAULT_CLIENT_PORT, "80");
        properties.put(Net.Http.STREAMING_LIMIT_FILE_SIZE, "10240");
        properties.put(Net.Http.DEFAULT_ERROR_FORMAT_SHOW_STACK, "true");
        properties.put(Net.Http.DEFAULT_CLIENT_CONNECT_TIMEOUT, "10000");
        properties.put(Net.Http.DEFAULT_CLIENT_READ_TIMEOUT, "10000");
        properties.put(Net.Http.DEFAULT_CLIENT_WRITE_TIMEOUT, "10000");
        properties.put(Net.Http.DEFAULT_GUEST_SESSION_NAME, "Http guest session");
        properties.put(Net.Http.DEFAULT_FILE_CHECKSUM_ALGORITHM, "MD5");
        properties.put(Net.Http.ENABLE_AUTOMATIC_RESPONSE_CONTENT_LENGTH, "true");
        properties.put(Net.Http.AUTOMATIC_CONTENT_LENGTH_SKIP_CODES, "[304]");
        properties.put(Net.Http.MAX_PACKAGE_SIZE, Integer.toString(20 * 1024 * 1024));
        properties.put(Net.Http.SERVER_DECOUPLED_IO_ACTION, "true");
        properties.put(Net.Http.SERVER_IO_QUEUE_SIZE, "100000");
        properties.put(Net.Http.SERVER_IO_WORKERS, "5");

        properties.put(Net.Https.DEFAULT_SERVER_PORT, "443");
        properties.put(Net.Https.DEFAULT_CLIENT_PORT, "443");

        properties.put(Net.Http.Http2.HEADER_TABLE_SIZE, "4096");
        properties.put(Net.Http.Http2.ENABLE_PUSH, "true");
        properties.put(Net.Http.Http2.MAX_CONCURRENT_STREAMS, "-1");
        properties.put(Net.Http.Http2.INITIAL_WINDOWS_SIZE, "65535");
        properties.put(Net.Http.Http2.MAX_FRAME_SIZE, "16384");
        properties.put(Net.Http.Http2.MAX_HEADER_LIST_SIZE, "-1");
        properties.put(Net.Http.Http2.STREAM_FRAMES_QUEUE_MAX_SIZE, "10");

        properties.put(Net.Http.Folder.LOG_TAG, "FOLDER_CONTEXT");
        properties.put(Net.Http.Folder.FORBIDDEN_CHARACTERS, "[]");
        properties.put(Net.Http.Folder.FILE_EXTENSION_REGEX, "\\.(?=[^\\.]+$)");
        properties.put(Net.Http.Folder.DEFAULT_HTML_DOCUMENT, "<!DOCTYPE html><html><head><title>%s</title><body>%s</body></html></head>");
        properties.put(Net.Http.Folder.DEFAULT_HTML_BODY, "<table>%s</table>");
        properties.put(Net.Http.Folder.DEFAULT_HTML_ROW, "<tr><th><a href=\"%s\">%s</a></th></tr>");
        properties.put(Net.Http.Folder.ZIP_CONTAINER, System.getProperty("user.home"));
        properties.put(Net.Http.Folder.ZIP_TEMP_PREFIX, "hcjf_zip_temp");
        properties.put(Net.Http.Folder.JAR_CONTAINER, System.getProperty("user.home"));
        properties.put(Net.Http.Folder.JAR_TEMP_PREFIX, "hcjf_jar_temp");

        properties.put(Net.Http.EndPoint.Json.DATE_FORMATS, " [dd/M/yyyy,dd/MM/yyyy]");

        properties.put(Net.Http.DataSources.SERVICE_NAME, "DataSourcesService");
        properties.put(Net.Http.DataSources.SERVICE_PRIORITY, "0");
        properties.put(Net.Http.DataSources.THREAD_POOL_ENABLED, "true");
        properties.put(Net.Http.DataSources.THREAD_POOL_CORE_SIZE, "200");
        properties.put(Net.Http.DataSources.THREAD_POOL_MAX_SIZE, "500");
        properties.put(Net.Http.DataSources.THREAD_POOL_KEEP_ALIVE_TIME, "60");

        properties.put(Net.Rest.DEFAULT_MIME_TYPE, "application/json");
        properties.put(Net.Rest.DEFAULT_ENCODING_IMPL, "hcjf");
        properties.put(Net.Rest.QUERY_PATH, "query");
        properties.put(Net.Rest.QUERY_PARAMETER, "q");
        properties.put(Net.Rest.BODY_FIELD, "_body");
        properties.put(Net.Rest.QUERY_FIELD, "_query");
        properties.put(Net.Rest.QUERIES_FIELD, "_queries");
        properties.put(Net.Rest.DATA_SOURCE_FIELD, "_dataSource");
        properties.put(Net.Rest.COMMAND_FIELD, "_command");
        properties.put(Net.Rest.COMMANDS_FIELD, "_commands");

        properties.put(ProcessDiscovery.LOG_TAG, "PROCESS_DISCOVERY");
        properties.put(ProcessDiscovery.SERVICE_NAME, "Process Discovery Service");
        properties.put(ProcessDiscovery.SERVICE_PRIORITY, "1");
        properties.put(ProcessDiscovery.DELAY, "3000");

        properties.put(Cloud.SERVICE_NAME, "CloudService");
        properties.put(Cloud.SERVICE_PRIORITY, "0");
        properties.put(Cloud.IMPL, DefaultCloudServiceImpl.class.getName());
        properties.put(Cloud.LOG_TAG, "CLOUD");
        properties.put(Cloud.Orchestrator.SERVICE_NAME, "CloudDefaultImplService");
        properties.put(Cloud.Orchestrator.AVAILABLE, "false");
        properties.put(Cloud.Orchestrator.SERVICE_PRIORITY, "0");
        properties.put(Cloud.Orchestrator.SERVER_LISTENER_PORT, "18080");
        properties.put(Cloud.Orchestrator.CONNECTION_LOOP_WAIT_TIME, "5000");
        properties.put(Cloud.Orchestrator.NODE_LOST_TIMEOUT, "1800000");
        properties.put(Cloud.Orchestrator.ACK_TIMEOUT, "2000");
        properties.put(Cloud.Orchestrator.REORGANIZATION_TIMEOUT, "2000");
        properties.put(Cloud.Orchestrator.REORGANIZATION_WARNING_TIME_LIMIT, "1500");
        properties.put(Cloud.Orchestrator.WAGON_TIMEOUT, "10000");
        properties.put(Cloud.Orchestrator.INVOKE_TIMEOUT, "120000");
        properties.put(Cloud.Orchestrator.TEST_NODE_TIMEOUT, "2000");
        properties.put(Cloud.Orchestrator.REPLICATION_FACTOR, "2");
        properties.put(Cloud.Orchestrator.NODES, "[]");
        properties.put(Cloud.Orchestrator.SERVICE_END_POINTS, "[]");
        properties.put(Cloud.Orchestrator.SERVICE_PUBLICATION_REPLICAS_BROADCASTING_ENABLED, "true");
        properties.put(Cloud.Orchestrator.SERVICE_PUBLICATION_REPLICAS_BROADCASTING_TIMEOUT, "2000");
        properties.put(Cloud.Orchestrator.NETWORKING_HANDSHAKE_DETAILS_AVAILABLE, "false");
        properties.put(Cloud.Orchestrator.CLUSTER_NAME, "hcjf");
        properties.put(Cloud.Orchestrator.ThisNode.READABLE_LAYER_IMPLEMENTATION_NAME, "system_cloud_node");
        properties.put(Cloud.Orchestrator.ThisNode.NAME, "hcjf-node");
        properties.put(Cloud.Orchestrator.ThisNode.VERSION, "0");
        properties.put(Cloud.Orchestrator.ThisNode.LAN_ADDRESS, "127.0.0.1");
        properties.put(Cloud.Orchestrator.ThisNode.LAN_PORT, "18080");
        properties.put(Cloud.Orchestrator.ThisServiceEndPoint.READABLE_LAYER_IMPLEMENTATION_NAME, "system_cloud_service");
        properties.put(Cloud.Orchestrator.ThisServiceEndPoint.PUBLICATION_TIMEOUT, "3600000");
        properties.put(Cloud.Orchestrator.ThisServiceEndPoint.DISTRIBUTED_EVENT_LISTENER, "false");
        properties.put(Cloud.Orchestrator.Broadcast.ENABLED, "false");
        properties.put(Cloud.Orchestrator.Broadcast.TASK_NAME, "Cloud discovery");
        properties.put(Cloud.Orchestrator.Broadcast.IP_VERSION, "4");
        properties.put(Cloud.Orchestrator.Broadcast.INTERFACE_NAME, "eth0");
        properties.put(Cloud.Orchestrator.Broadcast.PORT, "16000");
        properties.put(Cloud.Orchestrator.Kubernetes.ENABLED, "false");
        properties.put(Cloud.Orchestrator.Kubernetes.POD_LABELS, "[]");
        properties.put(Cloud.Orchestrator.Kubernetes.SERVICE_LABELS, "[]");
        properties.put(Cloud.Orchestrator.Kubernetes.SERVICE_PORT_NAME, "hcjf-k8s-port");
        properties.put(Cloud.Orchestrator.Kubernetes.ALLOW_PHASES, "[Running]");
        properties.put(Cloud.Orchestrator.Events.LOG_TAG, "DISTRIBUTED_EVENT");
        properties.put(Cloud.Orchestrator.Events.TIMEOUT, "3000");
        properties.put(Cloud.Orchestrator.Events.ATTEMPTS, "5");
        properties.put(Cloud.Orchestrator.Events.SLEEP_PERIOD_BETWEEN_ATTEMPTS, "3000");
        properties.put(Cloud.Orchestrator.Events.STORE_STRATEGY, "default");

        properties.put(Cloud.TimerTask.MIN_VALUE_OF_DELAY, "10000");
        properties.put(Cloud.TimerTask.MAP_NAME, "hcjf.cloud.timer.task.map");
        properties.put(Cloud.TimerTask.MAP_SUFFIX_NAME, "hcjf.cloud.timer.task.map.");
        properties.put(Cloud.TimerTask.LOCK_SUFFIX_NAME, "hcjf.cloud.timer.task.lock.");
        properties.put(Cloud.TimerTask.CONDITION_SUFFIX_NAME, "hcjf.cloud.timer.task.condition.");
        properties.put(Cloud.Cache.MAP_SUFFIX_NAME, "hcjf.cloud.cache.map.");
        properties.put(Cloud.Cache.LOCK_SUFFIX_NAME, "hcjf.cloud.cache.lock.");
        properties.put(Cloud.Cache.CONDITION_SUFFIX_NAME, "hcjf.cloud.cache.condition.");
        properties.put(Cloud.Cache.SIZE_STRATEGY_MAP_SUFFIX_NAME, "hcjf.cloud.cache.size.strategy.map.");
        properties.put(Cloud.Queue.LOCK_NAME_TEMPLATE, "hcjf.cloud.queue.lock.name.%s");
        properties.put(Cloud.Queue.CONDITION_NAME_TEMPLATE, "hcjf.cloud.queue.condition.name.%s");
        properties.put(Cloud.Queue.DEFAULT_SIZE, "100000");

        properties.put(Event.LOG_TAG, "EVENTS");
        properties.put(Event.SERVICE_NAME, "Events");
        properties.put(Event.SERVICE_PRIORITY, "0");
    }

    @Override
    public Properties getDefaults() {
        return properties;
    }
}
