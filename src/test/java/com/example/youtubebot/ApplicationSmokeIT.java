package com.example.youtubebot;

import com.example.youtubebot.support.PostgreSqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationSmokeIT extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @Test
    void actuatorHealthIsPublicAndReportsPostgresqlUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertTrue(response.body().contains("\"db\""));
        assertTrue(response.body().contains("\"database\":\"PostgreSQL\""));
    }

    @Test
    void localOAuthPageLoadsWithoutSeparateFormLogin() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(302, response.statusCode());
        String location = response.headers().firstValue("Location").orElseThrow();
        assertTrue(location.endsWith("/oauth"));

        HttpResponse<String> page = get("/oauth");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("YouTube 작성 채널"));
    }

    @Test
    void disconnectRequiresCsrfToken() throws Exception {
        HttpResponse<String> response = postDisconnectWithoutCsrf();

        assertEquals(403, response.statusCode());
    }

    private HttpResponse<String> postDisconnectWithoutCsrf() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + serverPort + "/oauth/disconnect"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {
            return client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + serverPort + path))
                .GET()
                .build();
        try(HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
