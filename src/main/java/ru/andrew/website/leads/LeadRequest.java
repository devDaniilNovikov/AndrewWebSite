package ru.andrew.website.leads;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = LeadRequestDeserializer.class)
public record LeadRequest(
        @NotNull UUID requestId,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[+0-9(). -]+$") String phone,
        String comment,
        @NotBlank String sourcePath,
        @NotNull LeadIntent intent,
        @NotNull @AssertTrue Boolean consent,
        String website) {
}
