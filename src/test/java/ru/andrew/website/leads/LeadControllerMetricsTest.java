package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LeadControllerMetricsTest {
    @ParameterizedTest
    @EnumSource(AcceptanceOutcome.class)
    void controllerRecordsEveryInternalAcceptanceOutcome(
            AcceptanceOutcome outcome) {
        LeadRequest request = mock(LeadRequest.class);
        LeadAcceptanceService acceptance =
                mock(LeadAcceptanceService.class);
        when(acceptance.accept(request)).thenReturn(outcome);
        var registry = new SimpleMeterRegistry();
        var controller = new LeadController(
                acceptance, new LeadMetrics(registry));

        var response = controller.submit(request);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNull();
        assertThat(registry.find("andrew.leads.accepted")
                        .tag("outcome", outcome.name().toLowerCase())
                        .counter())
                .satisfies(counter ->
                        assertThat(counter.count()).isEqualTo(1.0));
    }
}
