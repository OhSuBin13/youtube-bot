package com.example.youtubebot.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentGuardRepository extends JpaRepository<VideoCommentGuard, String> {
}
