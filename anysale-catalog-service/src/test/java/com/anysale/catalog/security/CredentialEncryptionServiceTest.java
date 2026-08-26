package com.anysale.catalog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptionServiceTest {

    private CredentialEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new CredentialEncryptionService("my-super-secret-key-32-bytes-long!!");
    }

    @Test
    void shouldEncryptAndDecryptPlainTextSuccessfully() {
        String plainText = "bearer_token_12345_secret_key";
        String encrypted = service.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = service.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertNull(service.encrypt(null));
        assertNull(service.decrypt(null));
    }

    @Test
    void shouldFailDecryptionWithDifferentSecretKey() {
        String plainText = "secret_api_key_value";
        String encrypted = service.encrypt(plainText);

        CredentialEncryptionService anotherService = new CredentialEncryptionService("different-secret-key-alt");
        assertThrows(RuntimeException.class, () -> anotherService.decrypt(encrypted));
    }
}
