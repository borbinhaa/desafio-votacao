package com.gabrieldeborba.voting.config;

/**
 * API version prefixes. The API is versioned in the URI path: breaking changes get a new prefix
 * (and new controllers) while additive changes stay in the current version.
 */
public final class ApiVersion {

    public static final String V1 = "/api/v1";

    private ApiVersion() {}
}
