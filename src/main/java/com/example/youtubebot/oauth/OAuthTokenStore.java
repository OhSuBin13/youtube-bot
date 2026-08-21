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
    public void save(OAuthConnectionInput input) {
        EncryptedToken encryptedToken = tokenCipher.encrypt(input.refreshToken());
        OAuthConnection connection = new OAuthConnection(
                SINGLETON_ID,
                encryptedToken.ciphertext(),
                encryptedToken.nonce(),
                encryptedToken.keyVersion(),
                serializeScopes(input.grantedScopes()),
                input.channelId(),
                input.channelName(),
                input.connectedAt());
        repository.save(connection);
    }

    @Transactional(readOnly = true)
    public Optional<OAuthConnectionCredentials> find() {
        return repository.findById(SINGLETON_ID).map(connection -> {
            EncryptedToken encryptedToken = new EncryptedToken(
                    connection.getRefreshTokenCiphertext(),
                    connection.getRefreshTokenNonce(),
                    connection.getKeyVersion());
            return new OAuthConnectionCredentials(
                    tokenCipher.decrypt(encryptedToken),
                    deserializeScopes(connection.getGrantedScope()),
                    connection.getChannelId(),
                    connection.getChannelName(),
                    connection.getConnectedAt());
        });
    }

    @Transactional
    public void delete() {
        repository.deleteById(SINGLETON_ID);
    }

    private String serializeScopes(Set<String> scopes) {
        return String.join(" ", new TreeSet<>(scopes));
    }

    private Set<String> deserializeScopes(String scopes) {
        return Set.copyOf(Arrays.asList(scopes.split(" ")));
    }
}
