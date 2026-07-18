package ru.andrew.website.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class RuntimeProfileGuard implements InitializingBean {
    private static final Set<String> ALLOWED = Set.of("test", "local", "prod");

    private final Environment environment;

    public RuntimeProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        Set<String> active = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.toUnmodifiableSet());
        if (active.size() != 1 || !ALLOWED.containsAll(active)) {
            throw new ApplicationContextException(
                    "Exactly one active profile is required: test, local, or prod");
        }
    }
}
