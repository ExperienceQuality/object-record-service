package com.xq.routineservice.shared;

import java.util.UUID;

public final class GatewayHeaders {
    public static final String USER = "X-User-Id";

    private GatewayHeaders() {
    }

    public static UUID requireUserId(String value) {
        String userId = requireIdentity(value, USER);
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_USER_ID", "X-User-Id must be a UUID");
        }
    }

    private static String requireIdentity(String value, String header) {
        if (value == null || value.isBlank()) {
            throw ApiException.unauthorized("Missing trusted gateway header " + header);
        }
        return value.trim();
    }
}
