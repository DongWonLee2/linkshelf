package dev.linkshelf.link;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public record MarkReadRequest(
		@NotNull(message = "true 또는 false여야 합니다") @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean read) {
}
