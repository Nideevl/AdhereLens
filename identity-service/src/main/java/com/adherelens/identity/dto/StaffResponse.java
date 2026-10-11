package com.adherelens.identity.dto;

import com.adherelens.common.model.Role;

import java.util.UUID;

public record StaffResponse(UUID id, String email, String fullName, Role role, String specialty, boolean active) {
}
