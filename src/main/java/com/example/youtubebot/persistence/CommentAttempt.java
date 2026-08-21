package com.example.youtubebot.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comment_attempt")
public class CommentAttempt {

    @Id
    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "video_id", nullable = false, length = 11)
    private String videoId;

    @Column(name = "draft_id", nullable = false)
    private UUID draftId;

    @Column(name = "ai_generated_text", nullable = false, length = 200)
    private String aiGeneratedText;

    @Column(name = "approved_text", nullable = false, length = 200)
    private String approvedText;

    @Column(name = "author_channel_id", nullable = false, length = 64)
    private String authorChannelId;

    @Column(name = "target_channel_id", nullable = false, length = 64)
    private String targetChannelId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "youtube_comment_id")
    private String youtubeCommentId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected CommentAttempt() {
    }

    public CommentAttempt(
            UUID attemptId,
            String videoId,
            UUID draftId,
            String aiGeneratedText,
            String approvedText,
            String authorChannelId,
            String targetChannelId,
            String status,
            String youtubeCommentId,
            String errorCode,
            Instant approvedAt,
            Instant requestedAt,
            Instant completedAt) {
        this.attemptId = attemptId;
        this.videoId = videoId;
        this.draftId = draftId;
        this.aiGeneratedText = aiGeneratedText;
        this.approvedText = approvedText;
        this.authorChannelId = authorChannelId;
        this.targetChannelId = targetChannelId;
        this.status = status;
        this.youtubeCommentId = youtubeCommentId;
        this.errorCode = errorCode;
        this.approvedAt = approvedAt;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
    }
}
