package dev.linkshelf.link;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Accepts only JSON {@code true}/{@code false}. Anything else becomes null so {@code @NotNull} reports the field
 * (R-LINK-17). Jackson 3 would otherwise coerce {@code "true"} and {@code 1}; the global setting is left alone because
 * it also applies to the save API (research R5).
 */
class StrictBooleanDeserializer extends ValueDeserializer<Boolean> {

	@Override
	public Boolean deserialize(JsonParser parser, DeserializationContext context) {
		JsonToken token = parser.currentToken();
		if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
			return token == JsonToken.VALUE_TRUE;
		}
		parser.skipChildren();
		return null;
	}

}
