package com.example.youtubebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YoutubeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeBotApplication.class, args);
    }

}
