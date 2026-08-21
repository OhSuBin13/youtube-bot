package com.example.youtubebot.security;

import java.util.Arrays;

public record EncryptedToken(byte[] ciphertext, byte[] nonce, int keyVersion) {

    public EncryptedToken {
        if (ciphertext == null || ciphertext.length <= AesGcmTokenCipher.TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("Ciphertext is missing or too short");
        }
        if (nonce == null || nonce.length != AesGcmTokenCipher.NONCE_LENGTH_BYTES) {
            throw new IllegalArgumentException("AES-GCM nonce must be 12 bytes");
        }
        if (keyVersion <= 0) {
            throw new IllegalArgumentException("Key version must be positive");
        }
        ciphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        nonce = Arrays.copyOf(nonce, nonce.length);
    }

    @Override
    public byte[] ciphertext() {
        return Arrays.copyOf(ciphertext, ciphertext.length);
    }

    @Override
    public byte[] nonce() {
        return Arrays.copyOf(nonce, nonce.length);
    }
}
