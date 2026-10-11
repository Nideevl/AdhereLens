package com.adherelens.notification.dto;

import java.util.UUID;

/** What clinical-service returns for a contact lookup. The phone is used once and never stored here. */
public record ContactResponse(UUID patientId, String phone) {
}
