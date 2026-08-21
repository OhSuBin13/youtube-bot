package com.example.youtubebot.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Enumerated(EnumType.STRING)
    private CommentAttemptStatus status;

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

    private CommentAttempt(ApprovedCommentAttempt input) {
        this.attemptId = input.attemptId();
        this.videoId = input.videoId();
        this.draftId = input.draftId();
        this.aiGeneratedText = input.aiGeneratedText();
        this.approvedText = input.approvedText();
        this.authorChannelId = input.authorChannelId();
        this.targetChannelId = input.targetChannelId();
        this.status = CommentAttemptStatus.APPROVED;
        this.youtubeCommentId = null;
        this.errorCode = null;
        this.approvedAt = input.approvedAt();
        this.requestedAt = null;
        this.completedAt = null;
    }

    public static CommentAttempt approved(ApprovedCommentAttempt input) {
        return new CommentAttempt(input);
    }

    public CommentAttemptStatus getStatus() {
        return status;
    }
}
