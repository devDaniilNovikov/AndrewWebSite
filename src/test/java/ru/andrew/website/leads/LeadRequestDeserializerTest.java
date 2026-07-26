package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pins the exact Jackson reporter delegation for otherwise non-returning error
 * paths. {@link LeadControllerContractTest} exercises the same inputs through
 * the real JsonMapper/MockMvc boundary and proves the public 400 contract.
 */
class LeadRequestDeserializerTest {
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final LeadRequestDeserializer deserializer = new LeadRequestDeserializer();

    @Test
    void reportsNonObjectRootThroughTheDeserializationContext() throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);

        assertThat(deserialize(NODES.arrayNode(), context)).isNull();
        verify(context)
                .reportInputMismatch(
                        LeadRequest.class, "Lead request must be a JSON object");
    }

    @Test
    void reportsUnknownPropertyThroughTheDeserializationContext() throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("unexpected", true);

        assertThat(deserialize(request, context)).isNull();
        verify(context)
                .reportInputMismatch(
                        LeadRequest.class, "Lead request contains an unknown property");
    }

    @Test
    void reportsNonStringRequestIdThroughTheDeserializationContext()
            throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("requestId", 1);

        assertThat(deserialize(request, context).requestId()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class,
                        "requestId",
                        "Property must be a UUID string");
    }

    @Test
    void reportsNonCanonicalRequestIdThroughTheDeserializationContext()
            throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("requestId", "not-a-uuid");

        assertThat(deserialize(request, context).requestId()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class,
                        "requestId",
                        "Property must be a canonical UUID string");
    }

    @Test
    void reportsNonStringTextThroughTheDeserializationContext() throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("name", false);

        assertThat(deserialize(request, context).name()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class, "name", "Property must be a string");
    }

    @Test
    void reportsNonStringIntentThroughTheDeserializationContext() throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("intent", 1);

        assertThat(deserialize(request, context).intent()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class,
                        "intent",
                        "Property must be a lead intent string");
    }

    @Test
    void reportsUnknownStringIntentThroughTheDeserializationContext()
            throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("intent", "unknown");

        assertThat(deserialize(request, context).intent()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class,
                        "intent",
                        "Property must be exactly repair or maintenance");
    }

    @Test
    void acceptsMaintenanceAsAnExactIntentValue() throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("intent", "maintenance");

        assertThat(deserialize(request, context).intent()).isEqualTo(LeadIntent.maintenance);
    }

    @Test
    void reportsNonBooleanConsentThroughTheDeserializationContext()
            throws JacksonException {
        DeserializationContext context = mock(DeserializationContext.class);
        ObjectNode request = NODES.objectNode().put("consent", "true");

        assertThat(deserialize(request, context).consent()).isNull();
        verify(context)
                .reportPropertyInputMismatch(
                        LeadRequest.class, "consent", "Property must be a boolean");
    }

    private LeadRequest deserialize(JsonNode root, DeserializationContext context)
            throws JacksonException {
        JsonParser parser = mock(JsonParser.class);
        when(context.readTree(parser)).thenReturn(root);
        return deserializer.deserialize(parser, context);
    }
}
