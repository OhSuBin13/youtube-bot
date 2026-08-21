package com.example.youtubebot.publishing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_comment_guard")
public class VideoCommentGuard {

    @Id
    @Column(name = "video_id", length = 11)
    private String videoId;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GuardStatus status;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VideoCommentGuard() {
    }

    public VideoCommentGuard(
            String videoId,
            GuardStatus status,
            UUID attemptId,
            Instant createdAt,
            Instant updatedAt) {
        this.videoId = videoId;
        this.status = status;
        this.attemptId = attemptId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public GuardStatus getStatus() {
        return status;
    }
}
