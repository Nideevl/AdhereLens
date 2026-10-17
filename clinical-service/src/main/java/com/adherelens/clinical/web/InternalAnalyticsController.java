package com.adherelens.clinical.web;

import com.adherelens.clinical.dto.AnalyticsContextResponse;
import com.adherelens.clinical.service.AnalyticsContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Service-to-service only, guarded by the shared X-Internal-Key header (see InternalContactController). */
@RestController
@RequestMapping("/internal")
public class InternalAnalyticsController {

    private final AnalyticsContextService contextService;
    private final byte[] internalKey;

    public InternalAnalyticsController(AnalyticsContextService contextService,
                                       @Value("${adherelens.internal.api-key}") String internalKey) {
        this.contextService = contextService;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/regimens/{regimenId}/analytics-context")
    public ResponseEntity<AnalyticsContextResponse> context(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @PathVariable UUID regimenId,
            @RequestParam UUID hospitalId) {
        if (providedKey == null || internalKey.length == 0
                || !MessageDigest.isEqual(internalKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(contextService.context(hospitalId, regimenId));
    }
}
