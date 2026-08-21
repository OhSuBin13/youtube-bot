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

    public void startPublishing(Instant requestedAt) {
        requireStatus(CommentAttemptStatus.APPROVED);
        if (requestedAt == null || requestedAt.isBefore(approvedAt)) {
            throw new IllegalArgumentException("requestedAt must not be before approvedAt");
        }

        this.status = CommentAttemptStatus.PUBLISHING;
        this.requestedAt = requestedAt;
    }

    public void markSucceeded(String youtubeCommentId, Instant completedAt) {
        requirePublishingCompletion(completedAt);
        if (youtubeCommentId == null || youtubeCommentId.isBlank()
                || youtubeCommentId.codePointCount(0, youtubeCommentId.length()) > 255) {
            throw new IllegalArgumentException(
                    "youtubeCommentId must be non-blank and at most 255 characters");
        }

        this.status = CommentAttemptStatus.SUCCEEDED;
        this.youtubeCommentId = youtubeCommentId;
        this.errorCode = null;
        this.completedAt = completedAt;
    }

    public void markFailed(String errorCode, Instant completedAt) {
        completeWithError(CommentAttemptStatus.FAILED, errorCode, completedAt);
    }

    public void markUnknown(String errorCode, Instant completedAt) {
        completeWithError(CommentAttemptStatus.UNKNOWN, errorCode, completedAt);
    }

    private void completeWithError(
            CommentAttemptStatus completedStatus,
            String errorCode,
            Instant completedAt) {
        requirePublishingCompletion(completedAt);
        if (errorCode == null || !errorCode.matches("[A-Za-z0-9_.-]{1,100}")) {
            throw new IllegalArgumentException(
                    "errorCode must contain 1 to 100 letters, digits, dots, underscores, or hyphens");
        }

        this.status = completedStatus;
        this.youtubeCommentId = null;
        this.errorCode = errorCode;
        this.completedAt = completedAt;
    }

    private void requirePublishingCompletion(Instant completedAt) {
        requireStatus(CommentAttemptStatus.PUBLISHING);
        if (completedAt == null || completedAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completedAt must not be before requestedAt");
        }
    }

    private void requireStatus(CommentAttemptStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Cannot transition comment attempt from " + status
                            + "; expected " + expectedStatus);
        }
    }

    public CommentAttemptStatus getStatus() {
        return status;
    }

    public String getYoutubeCommentId() {
        return youtubeCommentId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
