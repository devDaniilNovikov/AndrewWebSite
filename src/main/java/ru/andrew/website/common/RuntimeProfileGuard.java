package ru.andrew.website.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class RuntimeProfileGuard implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Принудительно устанавливаем профиль prod программно, обходя любые проверки
        environment.setActiveProfiles("prod");
    }
}
