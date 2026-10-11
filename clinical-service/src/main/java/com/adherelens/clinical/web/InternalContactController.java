package com.adherelens.clinical.web;

import com.adherelens.clinical.dto.ContactResponse;
import com.adherelens.clinical.service.ContactService;
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

/**
 * Service-to-service only: no user token, protected by a shared key in the X-Internal-Key
 * header. The security config lets /internal/** through, so this check is the only guard.
 */
@RestController
@RequestMapping("/internal")
public class InternalContactController {

    private final ContactService contactService;
    private final byte[] internalKey;

    public InternalContactController(ContactService contactService,
                                     @Value("${adherelens.internal.api-key}") String internalKey) {
        this.contactService = contactService;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/patients/{patientId}/contact")
    public ResponseEntity<ContactResponse> contact(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @PathVariable UUID patientId,
            @RequestParam UUID hospitalId) {
        if (!keyMatches(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(contactService.contact(hospitalId, patientId));
    }

    private boolean keyMatches(String provided) {
        if (provided == null || internalKey.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(internalKey, provided.getBytes(StandardCharsets.UTF_8));
    }
}
