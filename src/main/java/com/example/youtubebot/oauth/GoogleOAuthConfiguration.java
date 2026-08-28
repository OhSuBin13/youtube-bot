package com.example.youtubebot.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
public class GoogleOAuthConfiguration {

    static final String GOOGLE_OAUTH_REST_CLIENT = "googleOAuthRestClient";

    @Bean
    Clock googleOAuthClock() {
        return Clock.systemUTC();
    }

    @Bean(GOOGLE_OAUTH_REST_CLIENT)
    RestClient googleOAuthRestClient() {
        return RestClient.builder().build();
    }
}
