package dev.svitzthum.payroll;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("re-enable in iteration 2 step 4: the time tracking ports have no adapter yet")
class PayrollApplicationTests {

	@Test
	void contextLoads() {
	}

}
