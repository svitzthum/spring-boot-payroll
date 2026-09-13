package dev.svitzthum.payroll.workinghours.adapter.in.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase.ImportSummary;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Proves that the import really is triggered without anyone asking for it. The use case
 * itself is mocked, so this is about the schedule and nothing else. The initial delay
 * gives the test method time to stub before the first run.
 */
@SpringBootTest(properties = { "payroll.import.enabled=true", "payroll.import.initial-delay=1s",
		"payroll.import.interval=1s" })
@Import(TestcontainersConfiguration.class)
class TimeTrackingImportJobTest {

	private final CountDownLatch triggered = new CountDownLatch(1);

	@MockitoBean
	private ImportTimeTrackingUseCase importTimeTracking;

	@Test
	void runsTheImportOnItsOwn() throws Exception {
		given(this.importTimeTracking.importTimeTracking()).willAnswer(invocation -> {
			this.triggered.countDown();
			return new ImportSummary(1, 0, 0);
		});

		assertThat(this.triggered.await(10, TimeUnit.SECONDS)).isTrue();
	}

}


