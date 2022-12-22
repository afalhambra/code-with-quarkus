package io.quarkus.ts.monitoring.opentracing.reactive.grpc;

import java.util.Collections;
import java.util.Set;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

public class JaegerAllInOne extends GenericContainer<JaegerAllInOne> {

    private static final int JAEGER_QUERY_PORT = 16686;
    private static final int JAEGER_COLLECTOR_THRIFT_PORT = 14268;
    private static final int JAEGER_HTTP_SERVER_CONFIG = 5778;

    private static final String JAEGER_API_PATH = "/api/traces";

    public JaegerAllInOne(String dockerImageName) {
        super(dockerImageName);
        init();
    }

    protected void init() {
        waitingFor(new BoundPortHttpWaitStrategy(JAEGER_QUERY_PORT))
                .withExposedPorts(JAEGER_QUERY_PORT,
                                  JAEGER_COLLECTOR_THRIFT_PORT,
                                  JAEGER_HTTP_SERVER_CONFIG);
        addFixedExposedPort(5775, 5775, InternetProtocol.UDP);
        addFixedExposedPort(6831, 6831, InternetProtocol.UDP);
        addFixedExposedPort(6832, 6832, InternetProtocol.UDP);
    }

    public String getTraceUrl() {
        return "http://" + getHost() + ":" + getMappedPort(JAEGER_QUERY_PORT) + JAEGER_API_PATH;
    }

    private static class BoundPortHttpWaitStrategy extends HttpWaitStrategy {

        private final int port;

        public BoundPortHttpWaitStrategy(int port) {
            this.port = port;
        }

        @Override
        protected Set<Integer> getLivenessCheckPorts() {
            int mappedPort = this.waitStrategyTarget.getMappedPort(port);
            return Collections.singleton(mappedPort);
        }
    }
}
