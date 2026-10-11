package com.adherelens.clinical.dto;

import java.time.Instant;
import java.util.UUID;

public record RegimenChangeResponse(
        UUID changeId,
        Instant changedAt,
        UUID changedBy,
        String changedByName,
        String changedByRole,
        String field,
        String oldValue,
        String newValue,
        String reason
) {
}
