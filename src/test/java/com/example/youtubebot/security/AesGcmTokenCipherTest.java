package com.example.youtubebot.security;

import com.example.youtubebot.config.TokenEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmTokenCipherTest {

    private static final String REFRESH_TOKEN = "1//sample-refresh-token-한글";

    @Test
    void encryptsAndDecryptsWithAes256Gcm() {
        AesGcmTokenCipher cipher = cipherWithKey((byte) 1, 7);

        EncryptedToken encrypted = cipher.encrypt(REFRESH_TOKEN);

        assertEquals(12, encrypted.nonce().length);
        assertEquals(
                REFRESH_TOKEN.getBytes(StandardCharsets.UTF_8).length + 16,
                encrypted.ciphertext().length);
        assertEquals(7, encrypted.keyVersion());
        assertEquals(REFRESH_TOKEN, cipher.decrypt(encrypted));
    }

    @Test
    void createsANewNonceForEveryEncryption() {
        AesGcmTokenCipher cipher = cipherWithKey((byte) 2, 1);

        EncryptedToken first = cipher.encrypt(REFRESH_TOKEN);
        EncryptedToken second = cipher.encrypt(REFRESH_TOKEN);

        assertFalse(Arrays.equals(first.nonce(), second.nonce()));
        assertFalse(Arrays.equals(first.ciphertext(), second.ciphertext()));
    }

    @Test
    void rejectsTamperedCiphertext() {
        AesGcmTokenCipher cipher = cipherWithKey((byte) 3, 1);
        EncryptedToken encrypted = cipher.encrypt(REFRESH_TOKEN);
        byte[] tampered = encrypted.ciphertext();
        tampered[0] ^= 1;

        EncryptedToken changed = new EncryptedToken(
                tampered, encrypted.nonce(), encrypted.keyVersion());

        assertThrows(TokenEncryptionException.class, () -> cipher.decrypt(changed));
    }

    @Test
    void rejectsWrongKeyAndWrongKeyVersion() {
        AesGcmTokenCipher original = cipherWithKey((byte) 4, 1);
        AesGcmTokenCipher wrongKey = cipherWithKey((byte) 5, 1);
        AesGcmTokenCipher wrongVersion = cipherWithKey((byte) 4, 2);
        EncryptedToken encrypted = original.encrypt(REFRESH_TOKEN);

        assertThrows(TokenEncryptionException.class, () -> wrongKey.decrypt(encrypted));
        assertThrows(TokenEncryptionException.class, () -> wrongVersion.decrypt(encrypted));
    }

    @Test
    void requiresAValidBase64Encoded256BitKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AesGcmTokenCipher(new TokenEncryptionProperties("not-base64", 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AesGcmTokenCipher(new TokenEncryptionProperties(
                        Base64.getEncoder().encodeToString(new byte[16]), 1)));
    }

    @Test
    void encryptedTokenDefensivelyCopiesBinaryValues() {
        byte[] ciphertext = new byte[17];
        byte[] nonce = new byte[12];
        EncryptedToken encrypted = new EncryptedToken(ciphertext, nonce, 1);

        ciphertext[0] = 1;
        nonce[0] = 1;

        assertArrayEquals(new byte[17], encrypted.ciphertext());
        assertArrayEquals(new byte[12], encrypted.nonce());
    }

    private AesGcmTokenCipher cipherWithKey(byte fill, int version) {
        byte[] key = new byte[32];
        Arrays.fill(key, fill);
        TokenEncryptionProperties properties = new TokenEncryptionProperties(
                Base64.getEncoder().encodeToString(key), version);
        return new AesGcmTokenCipher(properties, new SecureRandom());
    }
}
