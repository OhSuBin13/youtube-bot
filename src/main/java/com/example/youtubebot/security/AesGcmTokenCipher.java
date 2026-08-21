package com.example.youtubebot.security;

import com.example.youtubebot.config.TokenEncryptionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesGcmTokenCipher {

    static final int NONCE_LENGTH_BYTES = 12;
    static final int TAG_LENGTH_BYTES = 16;
    private static final int TAG_LENGTH_BITS = TAG_LENGTH_BYTES * 8;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String AAD_PREFIX = "youtube-refresh-token:v";

    private final SecretKeySpec key;
    private final int keyVersion;
    private final SecureRandom secureRandom;

    @Autowired
    public AesGcmTokenCipher(TokenEncryptionProperties properties) {
        this(properties, new SecureRandom());
    }

    AesGcmTokenCipher(TokenEncryptionProperties properties, SecureRandom secureRandom) {
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(properties.key());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "YOUTUBE_TOKEN_ENCRYPTION_KEY must be valid Base64", exception);
        }
        if (decodedKey.length != 32) {
            Arrays.fill(decodedKey, (byte) 0);
            throw new IllegalArgumentException(
                    "YOUTUBE_TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        this.key = new SecretKeySpec(decodedKey, "AES");
        Arrays.fill(decodedKey, (byte) 0);
        this.keyVersion = properties.keyVersion();
        this.secureRandom = secureRandom;
    }

    public EncryptedToken encrypt(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] plaintext = refreshToken.getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(aad(keyVersion));
            return new EncryptedToken(cipher.doFinal(plaintext), nonce, keyVersion);
        } catch (GeneralSecurityException exception) {
            throw new TokenEncryptionException("Could not encrypt refresh token", exception);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public String decrypt(EncryptedToken encryptedToken) {
        if (encryptedToken.keyVersion() != keyVersion) {
            throw new TokenEncryptionException("Unsupported token encryption key version", null);
        }

        byte[] plaintext = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, encryptedToken.nonce()));
            cipher.updateAAD(aad(encryptedToken.keyVersion()));
            plaintext = cipher.doFinal(encryptedToken.ciphertext());
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException exception) {
            throw new TokenEncryptionException("Refresh token authentication failed", exception);
        } catch (GeneralSecurityException exception) {
            throw new TokenEncryptionException("Could not decrypt refresh token", exception);
        } finally {
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private byte[] aad(int version) {
        return (AAD_PREFIX + version).getBytes(StandardCharsets.UTF_8);
    }
}
