package com.gabrieldeborba.voting.vote;

/** Projection of {@code count(*) ... group by choice}; built by JPQL, not loaded from entities. */
public record VoteCount(VoteChoice choice, long total) {}
