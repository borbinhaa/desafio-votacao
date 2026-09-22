package com.gabrieldeborba.voting.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

/** Body of the "open session" call. {@code durationMinutes} is optional and defaults to 1 minute. */
public record OpenVotingSessionRequest(@Nullable @Min(1) @Max(MAX_DURATION_MINUTES) Integer durationMinutes) {

    public static final int DEFAULT_DURATION_MINUTES = 1;
    public static final int MAX_DURATION_MINUTES = 24 * 60;

    public int durationMinutesOrDefault() {
        return durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes;
    }
}
