package com.adherelens.identity.dto;

import com.adherelens.identity.domain.HospitalStatus;

import java.util.UUID;

public record HospitalResponse(UUID id, String code, String name, HospitalStatus status) {
}
