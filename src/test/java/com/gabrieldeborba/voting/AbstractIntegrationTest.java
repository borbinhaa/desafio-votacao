package com.gabrieldeborba.voting;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

/**
 * Base class for integration tests: full Spring context on a random port backed by a PostgreSQL
 * Testcontainer. Subclasses must be named {@code *IT} so Failsafe runs them on {@code mvn verify}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestcontainersConfiguration.class, AlwaysAbleCpfClientConfiguration.class})
public abstract class AbstractIntegrationTest {}
