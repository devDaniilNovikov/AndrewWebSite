package ru.andrew.website.leads;

import java.util.Set;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

public final class LeadRequestDeserializer extends ValueDeserializer<LeadRequest> {
    private static final Set<String> KNOWN_PROPERTIES = Set.of(
            "requestId",
            "name",
            "phone",
            "comment",
            "sourcePath",
            "intent",
            "consent",
            "website");

    @Override
    public LeadRequest deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        JsonNode root = context.readTree(parser);
        if (!root.isObject()) {
            return context.reportInputMismatch(
                    LeadRequest.class, "Lead request must be a JSON object");
        }
        ObjectNode object = root.asObject();
        if (!KNOWN_PROPERTIES.containsAll(object.propertyNames())) {
            return context.reportInputMismatch(
                    LeadRequest.class, "Lead request contains an unknown property");
        }
        return new LeadRequest(
                optionalUuid(object, "requestId", context),
                optionalText(object, "name", false, context),
                optionalText(object, "phone", false, context),
                optionalText(object, "comment", true, context),
                optionalText(object, "sourcePath", false, context),
                optionalIntent(object, "intent", context),
                optionalBoolean(object, "consent", context),
                optionalText(object, "website", true, context));
    }

    private static UUID optionalUuid(
            ObjectNode object, String property, DeserializationContext context) {
        JsonNode value = object.get(property);
        if (value == null) {
            return null;
        }
        if (!value.isString()) {
            return context.reportPropertyInputMismatch(
                    LeadRequest.class, property, "Property must be a UUID string");
        }
        return context.readTreeAsValue(value, UUID.class);
    }

    private static String optionalText(
            ObjectNode object,
            String property,
            boolean nullable,
            DeserializationContext context) {
        JsonNode value = object.get(property);
        if (value == null) {
            return null;
        }
        if (nullable && value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            return context.reportPropertyInputMismatch(
                    LeadRequest.class, property, "Property must be a string");
        }
        return value.stringValue();
    }

    private static LeadIntent optionalIntent(
            ObjectNode object, String property, DeserializationContext context) {
        JsonNode value = object.get(property);
        if (value == null) {
            return null;
        }
        if (!value.isString()) {
            return context.reportPropertyInputMismatch(
                    LeadRequest.class, property, "Property must be a lead intent string");
        }
        return context.readTreeAsValue(value, LeadIntent.class);
    }

    private static Boolean optionalBoolean(
            ObjectNode object, String property, DeserializationContext context) {
        JsonNode value = object.get(property);
        if (value == null) {
            return null;
        }
        if (!value.isBoolean()) {
            return context.reportPropertyInputMismatch(
                    LeadRequest.class, property, "Property must be a boolean");
        }
        return value.booleanValue();
    }
}
