package ru.andrew.website;

import java.util.function.IntConsumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.andrew.website.common.ProductionStartupFailureReporter;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AndrewWebsiteApplication {
    public static void main(String[] args) {
        main(args, System::exit);
    }

    static void main(String[] args, IntConsumer processTerminator) {
        SpringApplication application =
                new SpringApplication(AndrewWebsiteApplication.class);
        ProductionStartupFailureReporter failureReporter =
                new ProductionStartupFailureReporter();
        application.addListeners(failureReporter);
        try {
            run(application, failureReporter, args);
        } catch (RuntimeException | Error failure) {
            if (failureReporter.isProductionFailureReportingEnabled()) {
                processTerminator.accept(1);
            }
            throw failure;
        }
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
