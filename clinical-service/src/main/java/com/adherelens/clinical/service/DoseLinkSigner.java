package com.adherelens.clinical.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Signs and checks the one-time dose link sent to outpatients.
 *
 * Token format: {doseSlotId}.{expiryEpochSeconds}.{hmacSha256Hex}. It is stateless: nothing is
 * stored, the signature proves clinical-service issued it. It is tied to one dose slot and dies
 * at the slot's window close. Single use comes from the slot itself, because a slot with an
 * outcome rejects a second one.
 */
@Component
public class DoseLinkSigner {

    private static final String DOMAIN = "dose-link:v1:";
    private static final int MIN_SECRET_LENGTH = 32;

    private final byte[] secret;
    private final Clock clock;

    public DoseLinkSigner(@Value("${adherelens.dose-link.secret}") String secret, Clock clock) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("DOSE_LINK_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String sign(UUID doseSlotId, Instant expiresAt) {
        String payload = doseSlotId + "." + expiresAt.getEpochSecond();
        return payload + "." + mac(payload);
    }

    /** Returns the dose slot id. Throws if the token is malformed or forged, or if it has expired. */
    public UUID verify(String token) {
        if (token == null) {
            throw new InvalidDoseLinkException();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new InvalidDoseLinkException();
        }
        UUID slotId;
        long expiresAtSeconds;
        try {
            slotId = UUID.fromString(parts[0]);
            expiresAtSeconds = Long.parseLong(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new InvalidDoseLinkException();
        }
        String expected = mac(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidDoseLinkException();
        }
        if (!Instant.now(clock).isBefore(Instant.ofEpochSecond(expiresAtSeconds))) {
            throw new ExpiredDoseLinkException();
        }
        return slotId;
    }

    private String mac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((DOMAIN + payload).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }
}
