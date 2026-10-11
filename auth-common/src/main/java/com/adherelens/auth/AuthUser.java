package com.adherelens.auth;

import com.adherelens.common.model.Role;

import java.util.UUID;

/** The identity read from a verified JWT. hospitalId is null for PLATFORM_ADMIN. */
public record AuthUser(UUID staffId, UUID hospitalId, Role role, String fullName) {
}
