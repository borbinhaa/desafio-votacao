package com.gabrieldeborba.voting.config;

/**
 * API version prefixes (bonus 3).
 *
 * <p>Strategy: the version lives in the URI path ({@code /api/v1/...}). It is the most explicit option
 * for a mobile client and for anything in between (proxies, caches, logs, Swagger), and it costs
 * nothing to route.
 *
 * <ul>
 *   <li>Additive, backwards-compatible changes (new optional field, new endpoint) stay in the current
 *       version.
 *   <li>Only breaking changes (removed/renamed field, changed semantics or status code) create
 *       {@code /api/v2}: new controllers under a {@code v2} prefix coexist with v1 while clients
 *       migrate; services and entities are shared.
 *   <li>A retired version announces itself with {@code Deprecation} and {@code Sunset} response
 *       headers before being removed.
 * </ul>
 *
 * <p>Header or media-type versioning was not chosen: it is invisible in the URL, harder to try from a
 * browser or curl, and easy to get wrong in mobile HTTP layers. Spring Framework 7 also ships native
 * API versioning ({@code @RequestMapping(version = "1")}); it would be the natural evolution if
 * several versions ever have to live side by side.
 */
public final class ApiVersion {

    public static final String V1 = "/api/v1";

    private ApiVersion() {}
}
