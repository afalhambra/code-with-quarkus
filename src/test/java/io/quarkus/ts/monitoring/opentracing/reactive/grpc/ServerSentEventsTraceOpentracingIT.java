package io.quarkus.ts.monitoring.opentracing.reactive.grpc;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusIntegrationTest
public class ServerSentEventsTraceOpentracingIT {

    private static final String PING_ENDPOINT = "/%s-ping";
    private static final String PONG_ENDPOINT = "/%s-pong";

    private JaegerAllInOne jaeger = new JaegerAllInOne("jaegertracing/all-in-one:latest");

    @BeforeEach
    public void setup() {
        jaeger.start();
    }

    @AfterEach
    public void tearDown() {
        jaeger.stop();
    }

    @Test
    public void testServerClientTrace() {
        System.out.println("Jaeger logs: " + jaeger.getLogs());
        // When calling ping, the rest will invoke also the pong rest endpoint.
        given()
                .when().get(pingEndpoint())
                .then().statusCode(HttpStatus.SC_OK)
                .body(containsString("ping pong"));

        // Then both ping and pong rest endpoints should have the same trace Id.
        String pingTraceId = given()
                .when().get(pingEndpoint() + "/lastTraceId")
                .then().statusCode(HttpStatus.SC_OK).and().extract().asString();

        assertTraceIdWithPongService(pingTraceId);

        // Then Jaeger is invoked
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> given()
                .when().get(jaeger.getTraceUrl() + "?traceID=" + pingTraceId)
                .then().statusCode(HttpStatus.SC_OK)
                .and().body(allOf(containsString(pingEndpoint()), containsString(pongEndpoint()))));
    }

    protected void assertTraceIdWithPongService(String expected) {
        String pongTraceId = given()
                .when().get(pongEndpoint() + "/lastTraceId")
                .then().statusCode(HttpStatus.SC_OK).and().extract().asString();

        assertEquals(expected, pongTraceId);
    }

    String pingEndpoint() {
        return String.format(PING_ENDPOINT, "server-sent-events");
    }

    String pongEndpoint() {
        return String.format(PONG_ENDPOINT, "server-sent-events");
    }
}
