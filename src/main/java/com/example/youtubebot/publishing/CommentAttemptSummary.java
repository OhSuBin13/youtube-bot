package com.example.youtubebot.publishing;

import java.time.Instant;
import java.util.UUID;

public interface CommentAttemptSummary {

    UUID getAttemptId();

    String getApprovedText();

    CommentAttemptStatus getStatus();

    String getYoutubeCommentId();

    String getErrorCode();

    Instant getApprovedAt();

    Instant getRequestedAt();

    Instant getCompletedAt();
}
