package dev.svitzthum.payroll;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "payroll.import.enabled=false")
class PayrollApplicationTests {

	@Test
	void contextLoads() {
	}

}
