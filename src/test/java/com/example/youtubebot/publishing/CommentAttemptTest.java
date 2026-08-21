package com.example.youtubebot.publishing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentAttemptTest {

    private static final Instant APPROVED_AT = Instant.parse("2026-08-21T00:00:00Z");
    private static final Instant REQUESTED_AT = APPROVED_AT.plusSeconds(1);
    private static final Instant COMPLETED_AT = REQUESTED_AT.plusSeconds(1);

    @Test
    void succeedsAfterPublishingStarts() {
        CommentAttempt attempt = approvedAttempt();

        attempt.startPublishing(REQUESTED_AT);
        attempt.markSucceeded("youtube-comment-id", COMPLETED_AT);

        assertEquals(CommentAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertEquals("youtube-comment-id", attempt.getYoutubeCommentId());
        assertNull(attempt.getErrorCode());
        assertEquals(REQUESTED_AT, attempt.getRequestedAt());
        assertEquals(COMPLETED_AT, attempt.getCompletedAt());
    }

    @Test
    void recordsFailedPublishingWithErrorCode() {
        CommentAttempt attempt = approvedAttempt();

        attempt.startPublishing(REQUESTED_AT);
        attempt.markFailed("youtube.quotaExceeded", COMPLETED_AT);

        assertEquals(CommentAttemptStatus.FAILED, attempt.getStatus());
        assertNull(attempt.getYoutubeCommentId());
        assertEquals("youtube.quotaExceeded", attempt.getErrorCode());
        assertEquals(COMPLETED_AT, attempt.getCompletedAt());
    }

    @Test
    void recordsUnknownPublishingOutcomeWithoutRetrying() {
        CommentAttempt attempt = approvedAttempt();

        attempt.startPublishing(REQUESTED_AT);
        attempt.markUnknown("network.timeout", COMPLETED_AT);

        assertEquals(CommentAttemptStatus.UNKNOWN, attempt.getStatus());
        assertNull(attempt.getYoutubeCommentId());
        assertEquals("network.timeout", attempt.getErrorCode());
    }

    @Test
    void rejectsCompletionBeforePublishingStarts() {
        CommentAttempt attempt = approvedAttempt();

        assertThrows(IllegalStateException.class,
                () -> attempt.markSucceeded("youtube-comment-id", COMPLETED_AT));
    }

    @Test
    void rejectsPublishingBeforeApprovalTime() {
        CommentAttempt attempt = approvedAttempt();

        assertThrows(IllegalArgumentException.class,
                () -> attempt.startPublishing(APPROVED_AT.minusSeconds(1)));
        assertEquals(CommentAttemptStatus.APPROVED, attempt.getStatus());
    }

    @Test
    void rejectsCompletionBeforeRequestTime() {
        CommentAttempt attempt = approvedAttempt();
        attempt.startPublishing(REQUESTED_AT);

        assertThrows(IllegalArgumentException.class,
                () -> attempt.markFailed("network.timeout", APPROVED_AT));
        assertEquals(CommentAttemptStatus.PUBLISHING, attempt.getStatus());
    }

    @Test
    void rejectsAnotherTransitionAfterCompletion() {
        CommentAttempt attempt = approvedAttempt();
        attempt.startPublishing(REQUESTED_AT);
        attempt.markSucceeded("youtube-comment-id", COMPLETED_AT);

        assertThrows(IllegalStateException.class,
                () -> attempt.markUnknown("network.timeout", COMPLETED_AT.plusSeconds(1)));
    }

    private CommentAttempt approvedAttempt() {
        return CommentAttempt.approved(new ApprovedCommentAttempt(
                UUID.randomUUID(),
                "dQw4w9WgXcQ",
                UUID.randomUUID(),
                "AI original text",
                "User-approved text",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                APPROVED_AT));
    }
}
