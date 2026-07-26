package ru.andrew.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.andrew.website.common.ProductionStartupFailureReporter;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AndrewWebsiteApplication {
    public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        ProductionStartupFailureReporter failureReporter =
                new ProductionStartupFailureReporter();
        application.addListeners(failureReporter);
        run(application, failureReporter, args);
    }

    static ConfigurableApplicationContext run(
            SpringApplication application,
            ProductionStartupFailureReporter failureReporter,
            String[] args) {
        try {
            failureReporter.prepareForArguments(
                    application, args);
            return application.run(args);
        } catch (RuntimeException | Error failure) {
            failureReporter.report();
            throw failure;
        }
    }
}
