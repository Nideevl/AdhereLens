package com.adherelens.clinical.dto;

import java.util.UUID;

/** Where to reach a patient: their own phone, or the guardian's if they have none. */
public record ContactResponse(UUID patientId, String phone) {
}
