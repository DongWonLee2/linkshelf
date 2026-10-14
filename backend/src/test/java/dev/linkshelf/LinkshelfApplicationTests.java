package dev.linkshelf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LinkshelfApplicationTests {

	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Test
	void contextLoads() {
	}

	/**
	 * Tests call deleteAll(); if src/test/resources/config/application.properties stops overriding the main file
	 * database, they would wipe local data (PR #11 review).
	 */
	@Test
	@DisplayName("tests run against an in-memory H2 database, never the main file database")
	void tests_use_in_memory_database() {
		assertThat(datasourceUrl).startsWith("jdbc:h2:mem:");
	}

}
