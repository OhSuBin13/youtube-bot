package com.example.youtubebot.publishing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentGuardRepository extends JpaRepository<VideoCommentGuard, String> {
}
