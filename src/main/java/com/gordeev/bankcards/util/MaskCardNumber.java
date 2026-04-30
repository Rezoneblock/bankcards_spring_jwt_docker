package com.gordeev.bankcards.util;

import org.springframework.stereotype.Service;

@Service
public class MaskCardNumber {
    public static String mask(String lastFourDigits) {
        if (lastFourDigits == null || lastFourDigits.length() != 4) {
            throw new IllegalArgumentException("Ошибка маскировки. Неверный формат последних 4 цифр карты: " + lastFourDigits);
        }
        return "**** **** **** " + lastFourDigits;
    }
}
