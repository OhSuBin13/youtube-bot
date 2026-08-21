package com.example.youtubebot.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
    private EvidenceFields evidenceFields;

    @Column(name = "context_status", nullable = false, length = 20)
    @Convert(converter = ContextStatusConverter.class)
    private ContextStatus contextStatus;

    @Column(name = "safety_review", nullable = false, length = 30)
    @Convert(converter = SafetyReviewConverter.class)
    private SafetyReview safetyReview;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_topics", nullable = false, columnDefinition = "jsonb")
    private RiskTopics riskTopics;

    @Column(name = "generation_note", nullable = false)
    private String generationNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "duplicate_check_result", nullable = false, columnDefinition = "jsonb")
    private DuplicateCheckResult duplicateCheckResult;

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
            EvidenceFields evidenceFields,
            ContextStatus contextStatus,
            SafetyReview safetyReview,
            RiskTopics riskTopics,
            String generationNote,
            DuplicateCheckResult duplicateCheckResult,
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

    public ContextStatus getContextStatus() {
        return contextStatus;
    }

    public SafetyReview getSafetyReview() {
        return safetyReview;
    }

    public EvidenceFields getEvidenceFields() {
        return evidenceFields;
    }

    public RiskTopics getRiskTopics() {
        return riskTopics;
    }

    public DuplicateCheckResult getDuplicateCheckResult() {
        return duplicateCheckResult;
    }
}
