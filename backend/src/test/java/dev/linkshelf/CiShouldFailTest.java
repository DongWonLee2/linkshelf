package dev.linkshelf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Intentional failure to verify that CI detects failing tests. Do not merge.
class CiShouldFailTest {

	@Test
	void ciMustFail() {
		assertEquals(1, 2, "intentional failure: CI failure detection check");
	}

}
