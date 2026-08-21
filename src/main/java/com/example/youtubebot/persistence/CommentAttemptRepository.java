package com.example.youtubebot.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentAttemptRepository extends JpaRepository<CommentAttempt, UUID> {

    List<CommentAttempt> findAllByVideoIdOrderByApprovedAtDesc(String videoId);
}
