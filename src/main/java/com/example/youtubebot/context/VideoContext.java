package com.example.youtubebot.context;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "video_context")
public class VideoContext {

    @Id
    @Column(name = "video_id", length = 11)
    private String videoId;

    @Column(name = "canonical_url", nullable = false)
    private String canonicalUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "video_metadata", nullable = false, columnDefinition = "jsonb")
    private VideoMetadata videoMetadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channel_context", nullable = false, columnDefinition = "jsonb")
    private ChannelContext channelContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "public_comments", nullable = false, columnDefinition = "jsonb")
    private PublicComments publicComments;

    @Column(name = "user_summary")
    private String userSummary;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected VideoContext() {
    }

    public VideoContext(
            String videoId,
            String canonicalUrl,
            VideoMetadata videoMetadata,
            ChannelContext channelContext,
            PublicComments publicComments,
            String userSummary,
            Instant collectedAt,
            Instant expiresAt) {
        this.videoId = videoId;
        this.canonicalUrl = canonicalUrl;
        this.videoMetadata = videoMetadata;
        this.channelContext = channelContext;
        this.publicComments = publicComments;
        this.userSummary = userSummary;
        this.collectedAt = collectedAt;
        this.expiresAt = expiresAt;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public VideoMetadata getVideoMetadata() {
        return videoMetadata;
    }

    public ChannelContext getChannelContext() {
        return channelContext;
    }

    public PublicComments getPublicComments() {
        return publicComments;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
