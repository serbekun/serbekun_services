package com.serbekun.ss.service.cipher;

import java.util.Random;
import java.security.SecureRandom;

public class PasswordGenerator {

    static final String LETTERS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final String DIGITS   = "0123456789";
    static final String SPECIALS = "!@#$%^&*()_+";

    private final Random random;

    public PasswordGenerator() {
        this(new SecureRandom());
    }

    PasswordGenerator(Random random) {
        this.random = random;
    }

    /**
     * 
     * Generate 
     * 
     * @param length password length
     * @param withDigits
     * @param withSpecials 
     * @return password string
     */
    public String generate(int length, boolean withDigits, boolean withSpecials) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive: " + length);
        }
        String alphabet = LETTERS
                + (withDigits ? DIGITS : "")
                + (withSpecials ? SPECIALS : "");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}