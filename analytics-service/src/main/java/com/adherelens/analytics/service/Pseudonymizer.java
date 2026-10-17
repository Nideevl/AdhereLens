package com.adherelens.analytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Turns a patient id into a keyed hash (HMAC-SHA256). The same patient always gets the same
 * value, so they can be counted once, but the id cannot be recovered without the secret.
 */
@Component
public class Pseudonymizer {

    private final byte[] secret;

    public Pseudonymizer(@Value("${adherelens.analytics.pseudonym-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String patient(UUID patientId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(patientId.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }
}
