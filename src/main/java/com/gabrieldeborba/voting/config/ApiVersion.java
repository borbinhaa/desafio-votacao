package com.gabrieldeborba.voting.config;

/**
 * API version prefixes (bonus 3). The version lives in the URI path: additive changes stay in the
 * current version, breaking changes get a new prefix with its own controllers coexisting with the old
 * ones. The full rationale is in the README ("Versionamento da API").
 */
public final class ApiVersion {

    public static final String V1 = "/api/v1";

    private ApiVersion() {}
}
