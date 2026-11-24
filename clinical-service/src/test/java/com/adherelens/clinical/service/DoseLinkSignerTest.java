package com.adherelens.clinical.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoseLinkSignerTest {

    private static final String SECRET = "a-test-secret-that-is-at-least-32-chars";
    private static final Instant NOW = Instant.parse("2026-11-19T02:30:00Z");

    private final UUID slotId = UUID.randomUUID();
    private final DoseLinkSigner signer = signerAt(NOW, SECRET);

    private static DoseLinkSigner signerAt(Instant now, String secret) {
        return new DoseLinkSigner(secret, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void validToken_returnsTheSlotId() {
        String token = signer.sign(slotId, NOW.plusSeconds(3600));

        assertThat(signer.verify(token)).isEqualTo(slotId);
    }

    @Test
    void expiredToken_isRejectedAsExpired_includingExactlyAtExpiry() {
        String token = signer.sign(slotId, NOW.plusSeconds(3600));

        assertThatThrownBy(() -> signerAt(NOW.plusSeconds(3600), SECRET).verify(token))
                .isInstanceOf(ExpiredDoseLinkException.class);
        assertThat(signerAt(NOW.plusSeconds(3599), SECRET).verify(token)).isEqualTo(slotId);
    }

    @Test
    void changingTheSlotId_breaksTheSignature() {
        String token = signer.sign(slotId, NOW.plusSeconds(3600));
        String forged = token.replace(slotId.toString(), UUID.randomUUID().toString());

        assertThatThrownBy(() -> signer.verify(forged)).isInstanceOf(InvalidDoseLinkException.class);
    }

    @Test
    void extendingTheExpiry_breaksTheSignature() {
        String token = signer.sign(slotId, NOW.plusSeconds(3600));
        String[] p = token.split("\\.");
        String forged = p[0] + "." + (Long.parseLong(p[1]) + 86_400) + "." + p[2];

        assertThatThrownBy(() -> signer.verify(forged)).isInstanceOf(InvalidDoseLinkException.class);
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String token = signerAt(NOW, "another-secret-that-is-also-32-chars!!").sign(slotId, NOW.plusSeconds(3600));

        assertThatThrownBy(() -> signer.verify(token)).isInstanceOf(InvalidDoseLinkException.class);
    }

    @Test
    void malformedTokens_areRejected() {
        for (String bad : new String[]{null, "", "abc", "a.b", "a.b.c.d", slotId + ".notanumber.sig", "not-a-uuid.123.sig"}) {
            assertThatThrownBy(() -> signer.verify(bad)).isInstanceOf(InvalidDoseLinkException.class);
        }
    }

    @Test
    void shortSecret_refusesToStart() {
        assertThatThrownBy(() -> signerAt(NOW, "too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOSE_LINK_SECRET");
    }
}
