package dev.svitzthum.payroll;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("re-enable in step 4: the outbound ports have no adapter yet, so the context cannot start")
class PayrollApplicationTests {

	@Test
	void contextLoads() {
	}

}
