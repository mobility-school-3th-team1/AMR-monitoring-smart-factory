package com.sfaas.amr_control_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SpringBootTest
@ActiveProfiles("test")
class AmrControlSystemApplicationTests {

	private static final String TEST_JWT_SIGNING_KEY_MATERIAL =
			"amr-unit-test-jwt-signing-key-material";

	@DynamicPropertySource
	static void registerTestProperties(DynamicPropertyRegistry registry) {
		String testJwtSecret = Base64.getEncoder().encodeToString(
				TEST_JWT_SIGNING_KEY_MATERIAL.getBytes(StandardCharsets.UTF_8));
		registry.add("jwt.secret", () -> testJwtSecret);
		registry.add("jwt.expiration", () -> "3600000");
		registry.add("jwt.refresh-expiration", () -> "604800000");
		registry.add("app.demo.password", () -> "test-password");
	}

	@Test
	void contextLoads() {
	}

}
