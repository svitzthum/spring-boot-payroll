package dev.svitzthum.payroll.workinghours;

import java.time.YearMonth;
import java.util.UUID;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursUseCase;
import dev.svitzthum.payroll.workinghours.application.port.out.WorkingHoursRepository;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;

/**
 * The invariant the whole assignment turns on: when the director and the import write the
 * same month, one of them has to lose loudly rather than silently.
 *
 * <p>
 * Both tests provoke the collision where it is actually reachable — after a use case has
 * read the month and before it writes it back. That window is what neither the unique
 * constraint nor the precedence check can cover on its own: the check reads a state that
 * the other path may invalidate a moment later.
 */
@SpringBootTest(properties = "payroll.import.enabled=false")
@Import(TestcontainersConfiguration.class)
class ConcurrentWriteIntegrationTest {

	/** Active employee from V2__demo_data.sql. */
	private static final UUID ANNA = UUID.fromString("22222222-2222-2222-2222-222222222221");

	private static final YearMonth AUGUST = YearMonth.of(2026, 8);

	@Autowired
	private RecordWorkingHoursUseCase recordWorkingHours;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	/** The hook for the collision; unstubbed it is the real persistence adapter. */
	@MockitoSpyBean
	private WorkingHoursRepository workingHours;

	@BeforeEach
	void clearWhatEarlierTestsWrote() {
		this.jdbc.sql("delete from monthly_working_hours").update();
	}

	@Test
	void refusesTheImportsValueWhenAManualEntryAppearedAfterItsRead() {
		// nothing is stored yet, so the import legitimately decided to apply its value
		commitsBeforeTheNextWrite(() -> theDirectorRecords(WorkDuration.ofHours(160)));

		assertThatExceptionOfType(WorkingHoursConflictException.class)
			.isThrownBy(() -> theImportRecords(WorkDuration.ofHours(140)));

		// the manual entry survived: precedence holds even when the check came too early
		assertThat(storedWorkedTime()).isEqualTo(WorkDuration.ofHours(160));
		assertThat(storedSource()).isEqualTo(WorkingHoursSource.MANUAL);
	}

	@Test
	void refusesTheDirectorsCorrectionWhenTheImportCommittedAfterHisRead() {
		theImportRecords(WorkDuration.ofHours(140));

		// the stored value still comes from time tracking, so the import may write again
		commitsBeforeTheNextWrite(() -> theImportRecords(WorkDuration.ofHours(145)));

		assertThatExceptionOfType(WorkingHoursConflictException.class)
			.isThrownBy(() -> theDirectorRecords(WorkDuration.ofHours(160)));

		// the director is told to look again rather than writing over what he never saw
		assertThat(storedWorkedTime()).isEqualTo(WorkDuration.ofHours(145));
		assertThat(storedSource()).isEqualTo(WorkingHoursSource.TIME_TRACKING);
	}

	private void theDirectorRecords(WorkDuration workedTime) {
		this.recordWorkingHours.recordWorkingHours(
				new RecordWorkingHoursCommand(ANNA, AUGUST, workedTime, WorkingHoursSource.MANUAL));
	}

	/**
	 * What the import writes once the use case has let it through — at the moment it
	 * reads, nothing manual is stored.
	 */
	private void theImportRecords(WorkDuration workedTime) {
		this.recordWorkingHours.recordWorkingHours(
				new RecordWorkingHoursCommand(ANNA, AUGUST, workedTime, WorkingHoursSource.TIME_TRACKING));
	}

	/**
	 * Lets the competing write commit on the next save, which is the first thing the
	 * running use case does after its read. Only that one save is intercepted, so the
	 * competing write itself goes through untouched.
	 */
	private void commitsBeforeTheNextWrite(Runnable competingWrite) {
		willAnswer((invocation) -> {
			inItsOwnTransaction(competingWrite);
			return invocation.callRealMethod();
		}).willCallRealMethod().given(this.workingHours).save(any());
	}

	/**
	 * A separate transaction, so the value is really committed and visible to the one that
	 * is still running — just like a write arriving through the other adapter.
	 */
	private void inItsOwnTransaction(Runnable work) {
		TransactionTemplate ownTransaction = new TransactionTemplate(this.transactionManager);
		ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		ownTransaction.executeWithoutResult((status) -> work.run());
	}

	private WorkDuration storedWorkedTime() {
		return WorkDuration.ofMinutes(
				this.jdbc.sql("select minutes_worked from monthly_working_hours where employee_id = ? and period = ?")
					.params(ANNA, AUGUST.atDay(1))
					.query(Integer.class)
					.single());
	}

	private WorkingHoursSource storedSource() {
		return WorkingHoursSource.valueOf(
				this.jdbc.sql("select last_source from monthly_working_hours where employee_id = ? and period = ?")
					.params(ANNA, AUGUST.atDay(1))
					.query(String.class)
					.single());
	}

}

