package com.gabrieldeborba.voting.vote;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, UUID> {}
