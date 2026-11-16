package com.payledger.web.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String role,
        UUID merchantId,
        long expiresInMinutes
) {
}
