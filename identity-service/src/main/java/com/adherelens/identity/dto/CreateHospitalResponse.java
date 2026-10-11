package com.adherelens.identity.dto;

import java.util.UUID;

/** apiKey is returned only here, once. Only its hash is stored. */
public record CreateHospitalResponse(UUID hospitalId, String code, String name, String apiKey, UUID adminStaffId) {
}
