package md.botservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InterestRequest(
        @NotNull(message = "Interests cannot be null")
        @Size(max = 2000, message = "Interests list is too long (max 2000 characters)")
        @Pattern(regexp = "^[^<>{}]*$", message = "Interests contain invalid characters")
        String interest
) {}