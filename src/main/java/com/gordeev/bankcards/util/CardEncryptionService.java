package com.gordeev.bankcards.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class CardEncryptionService {

    @Value("${card.encryption.password}")
    private String password;

    @Value("${card.encryption.salt}")
    private String salt;

    private TextEncryptor encryptor;

    @PostConstruct
    public void init(){
        String hexSalt = convertToHex(salt);
        this.encryptor = Encryptors.text(password, hexSalt);
    }

    private String convertToHex(String str) {
        StringBuilder sb = new StringBuilder();
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String encryptCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Номер карты не может быть пустым");
        }

        return encryptor.encrypt(cardNumber);
    }

    public String decryptCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Номер карты не может быть пустым");
        }

        return encryptor.decrypt(cardNumber);
    }
}
