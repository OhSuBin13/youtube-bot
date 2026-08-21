package com.example.youtubebot.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_generation")
public class AiGeneration {

    @Id
    @Column(name = "draft_id")
    private UUID draftId;

    @Column(name = "video_id", nullable = false, length = 11)
    private String videoId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "ai_original_text", nullable = false, length = 200)
    private String aiOriginalText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_fields", nullable = false, columnDefinition = "jsonb")
    private String evidenceFields;

    @Column(name = "context_status", nullable = false, length = 20)
    private String contextStatus;

    @Column(name = "safety_review", nullable = false, length = 30)
    private String safetyReview;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_topics", nullable = false, columnDefinition = "jsonb")
    private String riskTopics;

    @Column(name = "generation_note", nullable = false)
    private String generationNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "duplicate_check_result", nullable = false, columnDefinition = "jsonb")
    private String duplicateCheckResult;

    @Column(name = "user_edited_text", length = 200)
    private String userEditedText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiGeneration() {
    }

    public AiGeneration(
            UUID draftId,
            String videoId,
            String modelName,
            String promptVersion,
            String aiOriginalText,
            String evidenceFields,
            String contextStatus,
            String safetyReview,
            String riskTopics,
            String generationNote,
            String duplicateCheckResult,
            String userEditedText,
            Instant createdAt,
            Instant updatedAt) {
        this.draftId = draftId;
        this.videoId = videoId;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.aiOriginalText = aiOriginalText;
        this.evidenceFields = evidenceFields;
        this.contextStatus = contextStatus;
        this.safetyReview = safetyReview;
        this.riskTopics = riskTopics;
        this.generationNote = generationNote;
        this.duplicateCheckResult = duplicateCheckResult;
        this.userEditedText = userEditedText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
