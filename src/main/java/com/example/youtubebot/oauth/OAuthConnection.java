package com.example.youtubebot.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;

@Entity
@Table(name = "oauth_connection")
public class OAuthConnection {

    @Id
    private Short id;

    @Column(name = "refresh_token_ciphertext", nullable = false, columnDefinition = "bytea")
    private byte[] refreshTokenCiphertext;

    @Column(name = "refresh_token_nonce", nullable = false, columnDefinition = "bytea")
    private byte[] refreshTokenNonce;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Column(name = "granted_scope", nullable = false)
    private String grantedScope;

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    protected OAuthConnection() {
    }

    OAuthConnection(
            short id,
            byte[] refreshTokenCiphertext,
            byte[] refreshTokenNonce,
            int keyVersion,
            String grantedScope,
            String channelId,
            String channelName,
            Instant connectedAt) {
        this.id = id;
        this.refreshTokenCiphertext = Arrays.copyOf(
                refreshTokenCiphertext, refreshTokenCiphertext.length);
        this.refreshTokenNonce = Arrays.copyOf(refreshTokenNonce, refreshTokenNonce.length);
        this.keyVersion = keyVersion;
        this.grantedScope = grantedScope;
        this.channelId = channelId;
        this.channelName = channelName;
        this.connectedAt = connectedAt;
    }

    public byte[] getRefreshTokenCiphertext() {
        return Arrays.copyOf(refreshTokenCiphertext, refreshTokenCiphertext.length);
    }

    public byte[] getRefreshTokenNonce() {
        return Arrays.copyOf(refreshTokenNonce, refreshTokenNonce.length);
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public String getGrantedScope() {
        return grantedScope;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }
}
