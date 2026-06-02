package ru.stepanov.selfcontrol.identity;

import java.util.Arrays;

public enum UserRole {
    User,
    Admin;

    private static final String ROLE_PREFIX = "ROLE_";

    public String authority() {
        return ROLE_PREFIX + name().toUpperCase();
    }

    public static UserRole fromTokenClaim(String claim) {
        if (claim == null || claim.isBlank()) {
            throw new IllegalArgumentException("User role claim is empty");
        }
        String normalizedClaim = claim.trim();
        if (normalizedClaim.regionMatches(true, 0, ROLE_PREFIX, 0, ROLE_PREFIX.length())) {
            normalizedClaim = normalizedClaim.substring(ROLE_PREFIX.length());
        }
        String finalNormalizedClaim = normalizedClaim;
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(finalNormalizedClaim))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown user role: " + claim));
    }
}
