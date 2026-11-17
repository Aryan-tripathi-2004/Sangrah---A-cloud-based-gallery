package com.example.Billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "stripe.api.key=sk_test_mock_for_context_load")
class BillingApplicationTests {

	@Test
	void contextLoads() {
	}

}
