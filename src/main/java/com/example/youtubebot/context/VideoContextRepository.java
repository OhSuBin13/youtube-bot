package com.example.youtubebot.context;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface VideoContextRepository extends JpaRepository<VideoContext, String> {

    List<VideoContext> findAllByExpiresAtBefore(Instant now);

    long deleteByExpiresAtBefore(Instant now);
}
