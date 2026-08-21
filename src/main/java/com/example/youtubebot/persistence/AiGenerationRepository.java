package com.example.youtubebot.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, UUID> {

    List<AiGeneration> findAllByVideoIdOrderByCreatedAtDesc(String videoId);
}
