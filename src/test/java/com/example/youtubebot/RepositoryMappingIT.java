package com.example.youtubebot;

import com.example.youtubebot.context.ChannelContext;
import com.example.youtubebot.context.PublicComments;
import com.example.youtubebot.context.VideoContext;
import com.example.youtubebot.context.VideoContextRepository;
import com.example.youtubebot.context.VideoMetadata;
import com.example.youtubebot.generation.AiGeneration;
import com.example.youtubebot.generation.AiGenerationRepository;
import com.example.youtubebot.generation.ContextStatus;
import com.example.youtubebot.generation.DuplicateCheckResult;
import com.example.youtubebot.generation.EvidenceFields;
import com.example.youtubebot.generation.NewAiGeneration;
import com.example.youtubebot.generation.RiskTopics;
import com.example.youtubebot.generation.SafetyReview;
import com.example.youtubebot.publishing.ApprovedCommentAttempt;
import com.example.youtubebot.publishing.CommentAttempt;
import com.example.youtubebot.publishing.CommentAttemptRepository;
import com.example.youtubebot.publishing.CommentAttemptStatus;
import com.example.youtubebot.publishing.GuardStatus;
import com.example.youtubebot.publishing.VideoCommentGuard;
import com.example.youtubebot.publishing.VideoCommentGuardRepository;
import com.example.youtubebot.support.PostgreSqlIntegrationTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RepositoryMappingIT extends PostgreSqlIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VideoContextRepository videoContextRepository;

    @Autowired
    private AiGenerationRepository aiGenerationRepository;

    @Autowired
    private CommentAttemptRepository commentAttemptRepository;

    @Autowired
    private VideoCommentGuardRepository videoCommentGuardRepository;

    @Test
    void repositoriesPersistValidJsonAndDomainFormats() {
        Instant now = Instant.parse("2026-08-20T07:00:00Z");
        String videoId = "dQw4w9WgXcQ";
        UUID draftId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        VideoMetadata videoMetadata = new VideoMetadata(
                "테스트 영상",
                "테스트 영상 설명",
                List.of("테스트", "Spring"),
                "교육",
                "ko",
                "ko",
                "2026-08-20T06:00:00Z",
                "PT5M");
        ChannelContext channelContext = new ChannelContext(
                "테스트 채널",
                "테스트 채널 설명",
                List.of("개발", "Spring"),
                List.of("Technology"));
        PublicComments publicComments = new PublicComments(List.of(
                new PublicComments.PublicComment(
                        "좋은 영상입니다",
                        7,
                        "2026-08-20T06:30:00Z")));
        EvidenceFields evidenceFields = new EvidenceFields(List.of("video.title"));
        RiskTopics riskTopics = new RiskTopics(List.of(RiskTopics.RiskTopic.FINANCE));
        DuplicateCheckResult duplicateCheckResult = new DuplicateCheckResult(false, 0.25);

        videoContextRepository.saveAndFlush(new VideoContext(
                videoId,
                "https://www.youtube.com/watch?v=" + videoId,
                videoMetadata,
                channelContext,
                publicComments,
                "사용자 요약",
                now,
                now.plus(30, ChronoUnit.DAYS)));
        aiGenerationRepository.saveAndFlush(AiGeneration.create(new NewAiGeneration(
                draftId,
                videoId,
                "qwen3:4b",
                "v1",
                "유익한 영상 감사합니다.",
                evidenceFields,
                ContextStatus.SUFFICIENT,
                SafetyReview.REQUIRES_HUMAN_REVIEW,
                riskTopics,
                "영상 제목을 근거로 작성",
                duplicateCheckResult,
                now)));
        commentAttemptRepository.saveAndFlush(CommentAttempt.approved(new ApprovedCommentAttempt(
                attemptId,
                videoId,
                draftId,
                "유익한 영상 감사합니다.",
                "유익한 설명 감사합니다.",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                now)));
        videoCommentGuardRepository.saveAndFlush(new VideoCommentGuard(
                videoId, GuardStatus.PUBLISHING, attemptId, now, now));

        assertTrue(videoContextRepository.existsById(videoId));
        assertTrue(aiGenerationRepository.existsById(draftId));
        assertTrue(commentAttemptRepository.existsById(attemptId));
        assertTrue(videoCommentGuardRepository.existsById(videoId));
        assertEquals("object", jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(video_metadata) FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("array", jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(evidence_fields) FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals("테스트 영상", jdbcTemplate.queryForObject(
                "SELECT video_metadata ->> 'title' FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("개발", jdbcTemplate.queryForObject(
                "SELECT channel_context -> 'keywords' ->> 0 FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("좋은 영상입니다", jdbcTemplate.queryForObject(
                "SELECT public_comments -> 0 ->> 'text' FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("video.title", jdbcTemplate.queryForObject(
                "SELECT evidence_fields ->> 0 FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals("finance", jdbcTemplate.queryForObject(
                "SELECT risk_topics ->> 0 FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals(false, jdbcTemplate.queryForObject(
                "SELECT (duplicate_check_result ->> 'duplicate')::boolean "
                        + "FROM ai_generation WHERE draft_id = ?",
                Boolean.class,
                draftId));
        assertEquals("sufficient", jdbcTemplate.queryForObject(
                "SELECT context_status FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals("requires_human_review", jdbcTemplate.queryForObject(
                "SELECT safety_review FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals(true, jdbcTemplate.queryForObject(
                "SELECT user_edited_text IS NULL AND created_at = updated_at "
                        + "FROM ai_generation WHERE draft_id = ?",
                Boolean.class,
                draftId));
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "SELECT status FROM comment_attempt WHERE attempt_id = ?",
                String.class,
                attemptId));
        assertEquals(true, jdbcTemplate.queryForObject(
                "SELECT youtube_comment_id IS NULL AND error_code IS NULL "
                        + "AND requested_at IS NULL AND completed_at IS NULL "
                        + "FROM comment_attempt WHERE attempt_id = ?",
                Boolean.class,
                attemptId));
        assertEquals("PUBLISHING", jdbcTemplate.queryForObject(
                "SELECT status FROM video_comment_guard WHERE video_id = ?",
                String.class,
                videoId));

        entityManager.clear();
        VideoContext restoredContext = videoContextRepository.findById(videoId).orElseThrow();
        AiGeneration restoredGeneration = aiGenerationRepository.findById(draftId).orElseThrow();
        CommentAttempt restoredAttempt = commentAttemptRepository.findById(attemptId).orElseThrow();
        VideoCommentGuard restoredGuard = videoCommentGuardRepository.findById(videoId).orElseThrow();
        assertEquals(videoMetadata, restoredContext.getVideoMetadata());
        assertEquals(channelContext, restoredContext.getChannelContext());
        assertEquals(publicComments, restoredContext.getPublicComments());
        assertEquals(evidenceFields, restoredGeneration.getEvidenceFields());
        assertEquals(riskTopics, restoredGeneration.getRiskTopics());
        assertEquals(duplicateCheckResult, restoredGeneration.getDuplicateCheckResult());
        assertEquals(ContextStatus.SUFFICIENT, restoredGeneration.getContextStatus());
        assertEquals(SafetyReview.REQUIRES_HUMAN_REVIEW, restoredGeneration.getSafetyReview());
        assertEquals(CommentAttemptStatus.APPROVED, restoredAttempt.getStatus());
        assertEquals(GuardStatus.PUBLISHING, restoredGuard.getStatus());
    }

    @Test
    void commentAttemptTransitionsPersistWithConsistentResultFields() {
        Instant approvedAt = Instant.parse("2026-08-21T00:00:00Z");
        Instant requestedAt = approvedAt.plusSeconds(1);
        Instant completedAt = requestedAt.plusSeconds(1);
        UUID attemptId = UUID.randomUUID();
        CommentAttempt attempt = CommentAttempt.approved(new ApprovedCommentAttempt(
                attemptId,
                "dQw4w9WgXcQ",
                UUID.randomUUID(),
                "AI original text",
                "User-approved text",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                approvedAt));
        commentAttemptRepository.saveAndFlush(attempt);

        attempt.startPublishing(requestedAt);
        commentAttemptRepository.saveAndFlush(attempt);
        attempt.markSucceeded("youtube-comment-id", completedAt);
        commentAttemptRepository.saveAndFlush(attempt);

        entityManager.clear();
        CommentAttempt restored = commentAttemptRepository.findById(attemptId).orElseThrow();
        assertEquals(CommentAttemptStatus.SUCCEEDED, restored.getStatus());
        assertEquals("youtube-comment-id", restored.getYoutubeCommentId());
        assertEquals(requestedAt, restored.getRequestedAt());
        assertEquals(completedAt, restored.getCompletedAt());
        assertNull(restored.getErrorCode());
    }
}
