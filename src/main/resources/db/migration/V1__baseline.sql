CREATE TABLE oauth_connection (
    id SMALLINT PRIMARY KEY,
    refresh_token_ciphertext BYTEA NOT NULL,
    refresh_token_nonce BYTEA NOT NULL,
    key_version INTEGER NOT NULL,
    granted_scope TEXT NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    channel_name VARCHAR(255) NOT NULL,
    connected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_oauth_connection_singleton CHECK (id = 1),
    CONSTRAINT ck_oauth_connection_ciphertext CHECK (octet_length(refresh_token_ciphertext) > 16),
    CONSTRAINT ck_oauth_connection_nonce CHECK (octet_length(refresh_token_nonce) = 12),
    CONSTRAINT ck_oauth_connection_key_version CHECK (key_version > 0),
    CONSTRAINT ck_oauth_connection_scope CHECK (btrim(granted_scope) <> ''),
    CONSTRAINT ck_oauth_connection_channel_id CHECK (btrim(channel_id) <> ''),
    CONSTRAINT ck_oauth_connection_channel_name CHECK (btrim(channel_name) <> '')
);

CREATE TABLE video_context (
    video_id VARCHAR(11) PRIMARY KEY,
    canonical_url TEXT NOT NULL,
    video_metadata JSONB NOT NULL,
    channel_context JSONB NOT NULL,
    public_comments JSONB NOT NULL,
    user_summary TEXT,
    collected_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_video_context_video_id CHECK (video_id ~ '^[A-Za-z0-9_-]{11}$'),
    CONSTRAINT ck_video_context_canonical_url CHECK (
        canonical_url = 'https://www.youtube.com/watch?v=' || video_id
    ),
    CONSTRAINT ck_video_context_video_metadata CHECK (jsonb_typeof(video_metadata) = 'object'),
    CONSTRAINT ck_video_context_channel_context CHECK (jsonb_typeof(channel_context) = 'object'),
    CONSTRAINT ck_video_context_public_comments CHECK (jsonb_typeof(public_comments) = 'array'),
    CONSTRAINT ck_video_context_expiry CHECK (
        expires_at > collected_at
        AND expires_at <= collected_at + INTERVAL '30 days'
    )
);

CREATE INDEX idx_video_context_expires_at ON video_context (expires_at);

CREATE TABLE ai_generation (
    draft_id UUID PRIMARY KEY,
    video_id VARCHAR(11) NOT NULL REFERENCES video_context(video_id) ON DELETE CASCADE,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    ai_original_text VARCHAR(200) NOT NULL,
    evidence_fields JSONB NOT NULL,
    context_status VARCHAR(20) NOT NULL,
    safety_review VARCHAR(30) NOT NULL,
    risk_topics JSONB NOT NULL,
    generation_note TEXT NOT NULL,
    duplicate_check_result JSONB NOT NULL,
    user_edited_text VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ai_generation_video_id CHECK (video_id ~ '^[A-Za-z0-9_-]{11}$'),
    CONSTRAINT ck_ai_generation_model_name CHECK (btrim(model_name) <> ''),
    CONSTRAINT ck_ai_generation_prompt_version CHECK (btrim(prompt_version) <> ''),
    CONSTRAINT ck_ai_generation_original_text CHECK (
        char_length(ai_original_text) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_ai_generation_evidence_fields CHECK (jsonb_typeof(evidence_fields) = 'array'),
    CONSTRAINT ck_ai_generation_context_status CHECK (
        context_status IN ('sufficient', 'insufficient')
    ),
    CONSTRAINT ck_ai_generation_safety_review CHECK (
        safety_review IN ('passed', 'requires_human_review', 'rejected')
    ),
    CONSTRAINT ck_ai_generation_risk_topics CHECK (jsonb_typeof(risk_topics) = 'array'),
    CONSTRAINT ck_ai_generation_duplicate_check CHECK (jsonb_typeof(duplicate_check_result) = 'object'),
    CONSTRAINT ck_ai_generation_edited_text CHECK (
        user_edited_text IS NULL OR char_length(user_edited_text) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_ai_generation_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_ai_generation_video_id ON ai_generation (video_id);

CREATE TABLE comment_attempt (
    attempt_id UUID PRIMARY KEY,
    video_id VARCHAR(11) NOT NULL,
    draft_id UUID NOT NULL,
    ai_generated_text VARCHAR(200) NOT NULL,
    approved_text VARCHAR(200) NOT NULL,
    author_channel_id VARCHAR(64) NOT NULL,
    target_channel_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    youtube_comment_id VARCHAR(255),
    error_code VARCHAR(100),
    approved_at TIMESTAMPTZ NOT NULL,
    requested_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_comment_attempt_video_id CHECK (video_id ~ '^[A-Za-z0-9_-]{11}$'),
    CONSTRAINT ck_comment_attempt_ai_text CHECK (
        char_length(ai_generated_text) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_comment_attempt_approved_text CHECK (
        char_length(approved_text) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_comment_attempt_author_channel CHECK (btrim(author_channel_id) <> ''),
    CONSTRAINT ck_comment_attempt_target_channel CHECK (btrim(target_channel_id) <> ''),
    CONSTRAINT ck_comment_attempt_status CHECK (
        status IN ('APPROVED', 'PUBLISHING', 'SUCCEEDED', 'FAILED', 'UNKNOWN')
    ),
    CONSTRAINT ck_comment_attempt_youtube_id CHECK (
        youtube_comment_id IS NULL OR btrim(youtube_comment_id) <> ''
    ),
    CONSTRAINT ck_comment_attempt_error_code CHECK (
        error_code IS NULL OR error_code ~ '^[A-Za-z0-9_.-]+$'
    ),
    CONSTRAINT ck_comment_attempt_requested_at CHECK (
        requested_at IS NULL OR requested_at >= approved_at
    ),
    CONSTRAINT ck_comment_attempt_completed_at CHECK (
        completed_at IS NULL
        OR (requested_at IS NOT NULL AND completed_at >= requested_at)
    )
);

CREATE INDEX idx_comment_attempt_video_id ON comment_attempt (video_id);
CREATE INDEX idx_comment_attempt_draft_id ON comment_attempt (draft_id);

CREATE TABLE video_comment_guard (
    video_id VARCHAR(11) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    attempt_id UUID NOT NULL UNIQUE REFERENCES comment_attempt(attempt_id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_video_comment_guard_video_id CHECK (video_id ~ '^[A-Za-z0-9_-]{11}$'),
    CONSTRAINT ck_video_comment_guard_status CHECK (
        status IN ('PUBLISHING', 'SUCCEEDED', 'UNKNOWN')
    ),
    CONSTRAINT ck_video_comment_guard_timestamps CHECK (updated_at >= created_at)
);
