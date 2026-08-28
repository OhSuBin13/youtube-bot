package com.example.youtubebot.oauth;

import com.example.youtubebot.security.AesGcmTokenCipher;
import com.example.youtubebot.security.EncryptedToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class OAuthTokenStore {

    static final short SINGLETON_ID = 1;

    private final OAuthConnectionRepository repository;
    private final AesGcmTokenCipher tokenCipher;

    public OAuthTokenStore(
            OAuthConnectionRepository repository,
            AesGcmTokenCipher tokenCipher) {
        this.repository = repository;
        this.tokenCipher = tokenCipher;
    }

    @Transactional
    public void create(OAuthConnectionInput input) {
        EncryptedToken encryptedToken = tokenCipher.encrypt(input.refreshToken().value());
        int insertedRows = repository.insertIfAbsent(
                SINGLETON_ID,
                encryptedToken.ciphertext(),
                encryptedToken.nonce(),
                encryptedToken.keyVersion(),
                serializeScopes(input.grantedScopes()),
                input.channel().channelId(),
                input.channel().channelName(),
                input.connectedAt());
        if (insertedRows == 0) {
            throw alreadyConnected();
        }
    }

    @Transactional(readOnly = true)
    public Optional<OAuthConnectionCredentials> find() {
        return repository.findById(SINGLETON_ID).map(connection -> {
            EncryptedToken encryptedToken = new EncryptedToken(
                    connection.getRefreshTokenCiphertext(),
                    connection.getRefreshTokenNonce(),
                    connection.getKeyVersion());
            return new OAuthConnectionCredentials(
                    new RefreshToken(tokenCipher.decrypt(encryptedToken)),
                    deserializeScopes(connection.getGrantedScope()),
                    new YouTubeChannelIdentity(
                            connection.getChannelId(),
                            connection.getChannelName()),
                    connection.getConnectedAt());
        });
    }

    @Transactional(readOnly = true)
    public Optional<OAuthConnectionInfo> findConnectionInfo() {
        return repository.findById(SINGLETON_ID)
                .map(connection -> new OAuthConnectionInfo(
                        new YouTubeChannelIdentity(
                                connection.getChannelId(),
                                connection.getChannelName()),
                        connection.getConnectedAt()));
    }

    @Transactional(readOnly = true)
    public boolean exists() {
        return repository.existsById(SINGLETON_ID);
    }

    @Transactional
    public void delete() {
        repository.deleteById(SINGLETON_ID);
    }

    private GoogleOAuthException alreadyConnected() {
        return new GoogleOAuthException(
                GoogleOAuthErrorCode.ALREADY_CONNECTED,
                "Disconnect the current YouTube channel before connecting another one");
    }

    private String serializeScopes(GrantedScopes scopes) {
        return String.join(" ", new TreeSet<>(scopes.values()));
    }

    private GrantedScopes deserializeScopes(String scopes) {
        return new GrantedScopes(Set.copyOf(Arrays.asList(scopes.split(" "))));
    }
}
