package ru.andrew.website.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a test that needs a real PostgreSQL started through Testcontainers.
 *
 * <p>The test keeps the {@code database} JUnit tag for explicit filtering and is skipped
 * automatically wherever no Docker daemon is reachable (for example the hosting
 * provider's build runner), so {@code mvn package} stays green there while CI with
 * Docker still executes the full suite.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("database")
@ExtendWith(DockerAvailabilityCondition.class)
public @interface DatabaseTest {
}
