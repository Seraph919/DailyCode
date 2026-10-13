package com.dailycode.bot.application.leetcode;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class VerificationTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        // making it humain readable
        String randomText = HexFormat.of().formatHex(randomBytes);
        return "dailycode-" + randomText;
    }
}
