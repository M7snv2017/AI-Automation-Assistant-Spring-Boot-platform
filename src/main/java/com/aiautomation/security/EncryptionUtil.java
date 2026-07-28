package com.aiautomation.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    // Standard application key for encrypting credentials at rest
    private static final byte[] KEY = "AI_AUTOMATION_S3CR3T_K3Y_2026!".substring(0, 16).getBytes(StandardCharsets.UTF_8);

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return null;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting credential", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return null;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback if plain text was previously saved
            return cipherText;
        }
    }
}
