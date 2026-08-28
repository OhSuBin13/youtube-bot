package com.example.youtubebot.oauth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, Short> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO oauth_connection (
                id,
                refresh_token_ciphertext,
                refresh_token_nonce,
                key_version,
                granted_scope,
                channel_id,
                channel_name,
                connected_at
            ) VALUES (
                :id,
                :ciphertext,
                :nonce,
                :keyVersion,
                :grantedScope,
                :channelId,
                :channelName,
                :connectedAt
            )
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") short id,
            @Param("ciphertext") byte[] ciphertext,
            @Param("nonce") byte[] nonce,
            @Param("keyVersion") int keyVersion,
            @Param("grantedScope") String grantedScope,
            @Param("channelId") String channelId,
            @Param("channelName") String channelName,
            @Param("connectedAt") Instant connectedAt);
}
