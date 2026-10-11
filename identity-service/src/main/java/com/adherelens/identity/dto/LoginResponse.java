package com.adherelens.identity.dto;

import com.adherelens.common.model.Role;

import java.util.UUID;

public record LoginResponse(String token, Role role, UUID hospitalId, String fullName) {
}
