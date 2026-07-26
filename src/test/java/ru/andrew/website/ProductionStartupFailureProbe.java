package ru.andrew.website;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import ru.andrew.website.common.ProductionStartupFailureReporter;

public final class ProductionStartupFailureProbe {
    private ProductionStartupFailureProbe() {
    }

    public static void main(String[] args) {
        Path initializedMarker = Path.of(args[0]);
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        ProductionStartupFailureReporter failureReporter =
                new ProductionStartupFailureReporter();
        application.addListeners(
                failureReporter,
                (ApplicationListener<ApplicationContextInitializedEvent>)
                        event -> markInitialized(initializedMarker));
        AndrewWebsiteApplication.run(
                application,
                failureReporter,
                Arrays.copyOfRange(args, 1, args.length));
    }

    private static void markInitialized(Path marker) {
        try {
            Files.writeString(
                    marker,
                    "initialized",
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Production startup probe failed");
        }
    }
}
