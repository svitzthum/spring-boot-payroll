package dev.svitzthum.payroll.workinghours;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase.ImportSummary;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

/**
 * Drives the second write path end to end against a real PostgreSQL: the simulated time
 * tracking system, the import service with its per event transaction, the journal and the
 * change history. The scheduler is switched off so the run is triggered by the test
 * rather than by the clock.
 *
 * <p>
 * Manual entry enters through the same inbound port the web adapter uses; that the
 * adapter reaches the port is covered by {@link WorkingHoursIntegrationTest}.
 */
@SpringBootTest(properties = "payroll.import.enabled=false")
@Import(TestcontainersConfiguration.class)
class TimeTrackingImportIntegrationTest {

	/** Employees from V2__demo_data.sql; Clara has left the company. */
	private static final UUID ANNA = UUID.fromString("22222222-2222-2222-2222-222222222221");

	private static final String ANNA_REF = "TT-1001";

	private static final String CLARA_REF = "TT-1003";

	@Autowired
	private ImportTimeTrackingUseCase importTimeTracking;

	@Autowired
	private RecordWorkingHoursUseCase recordWorkingHours;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private Clock clock;

	/**
	 * Only used to provoke the collision in
	 * {@link #retriesWhenTheOtherPathChangedTheSameMonthInBetween()}; unstubbed it is the
	 * real adapter.
	 */
	@MockitoSpyBean
	private WorkingHoursRepository workingHours;

	@BeforeEach
	void clearWhatEarlierRunsWrote() {
		this.jdbc.sql("delete from time_tracking_import").update();
		this.jdbc.sql("delete from monthly_working_hours").update();
	}

	@Test
	void appliesTheReportedTimesAndRecordsTheRejectedOnesAsFailed() {
		ImportSummary summary = this.importTimeTracking.importTimeTracking();

		// two active employees and one who has left, three months each
		assertThat(summary).isEqualTo(new ImportSummary(6, 0, 3));
		assertThat(storedMinutes(lastCompletedMonth())).isEqualTo(reportedMinutesForAnna());
		assertThat(storedSource(lastCompletedMonth())).isEqualTo("TIME_TRACKING");
		assertThat(journalStatus(CLARA_REF, lastCompletedMonth())).isEqualTo("FAILED");
	}

	@Test
	void aSecondRunAppliesNothingAgain() {
		this.importTimeTracking.importTimeTracking();
		long versionAfterTheFirstRun = storedVersion(lastCompletedMonth());

		ImportSummary second = this.importTimeTracking.importTimeTracking();

		assertThat(second.total()).isZero();
		assertThat(countOf("time_tracking_import")).isEqualTo(9);
		assertThat(countOf("monthly_working_hours")).isEqualTo(6);
		assertThat(storedVersion(lastCompletedMonth())).isEqualTo(versionAfterTheFirstRun);
	}

	@Test
	void leavesAMonthAloneThatWasEnteredManually() {
		YearMonth period = lastCompletedMonth();
		recordManually(period, WorkDuration.ofHours(160));

		ImportSummary summary = this.importTimeTracking.importTimeTracking();

		assertThat(summary.skipped()).isEqualTo(1);
		assertThat(storedMinutes(period)).isEqualTo(160 * 60);
		assertThat(storedSource(period)).isEqualTo("MANUAL");
		assertThat(journalStatus(ANNA_REF, period)).isEqualTo("SKIPPED");
	}

	@Test
	void retriesWhenTheOtherPathChangedTheSameMonthInBetween() {
		// what the web adapter causes when it writes between the import's read and write
		willThrow(new WorkingHoursConflictException(ANNA, lastCompletedMonth(), null)).willCallRealMethod()
			.given(this.workingHours)
			.save(any());

		ImportSummary summary = this.importTimeTracking.importTimeTracking();

		// the retry picked the event up again instead of deferring it to the next run
		assertThat(summary).isEqualTo(new ImportSummary(6, 0, 3));
	}

	@Test
	void keepsBothWritesInTheChangeHistory() {
		YearMonth period = lastCompletedMonth();
		this.importTimeTracking.importTimeTracking();

		recordManually(period, WorkDuration.ofHours(160));

		assertThat(historyOf(period)).containsExactly("TIME_TRACKING", "MANUAL");
	}

	private void recordManually(YearMonth period, WorkDuration workedTime) {
		this.recordWorkingHours.recordWorkingHours(
				new RecordWorkingHoursCommand(ANNA, period, workedTime, WorkingHoursSource.MANUAL));
	}

	private YearMonth lastCompletedMonth() {
		return YearMonth.now(this.clock).minusMonths(1);
	}

	/** Mirrors the formula of the simulated system, so the test pins the value it wrote. */
	private int reportedMinutesForAnna() {
		YearMonth period = lastCompletedMonth();
		int seed = ANNA_REF.hashCode() * 31 + period.getYear() * 12 + period.getMonthValue();
		return 120 * 60 + Math.floorMod(seed, 70 * 60 / 5) * 5;
	}

	private int storedMinutes(YearMonth period) {
		return this.jdbc.sql("select minutes_worked from monthly_working_hours where employee_id = ? and period = ?")
			.params(ANNA, firstDayOf(period))
			.query(Integer.class)
			.single();
	}

	private String storedSource(YearMonth period) {
		return this.jdbc.sql("select last_source from monthly_working_hours where employee_id = ? and period = ?")
			.params(ANNA, firstDayOf(period))
			.query(String.class)
			.single();
	}

	private long storedVersion(YearMonth period) {
		return this.jdbc.sql("select version from monthly_working_hours where employee_id = ? and period = ?")
			.params(ANNA, firstDayOf(period))
			.query(Long.class)
			.single();
	}

	private String journalStatus(String employeeReference, YearMonth period) {
		return this.jdbc.sql("select status from time_tracking_import where external_employee_ref = ? and period = ?")
			.params(employeeReference, firstDayOf(period))
			.query(String.class)
			.single();
	}

	private List<String> historyOf(YearMonth period) {
		return this.jdbc.sql("""
				select r.source
				from working_hours_revision r
				join monthly_working_hours h on h.id = r.working_hours_id
				where h.employee_id = ? and h.period = ?
				order by r.changed_at
				""").params(ANNA, firstDayOf(period)).query(String.class).list();
	}

	private long countOf(String table) {
		return this.jdbc.sql("select count(*) from " + table).query(Long.class).single();
	}

	private static LocalDate firstDayOf(YearMonth period) {
		return period.atDay(1);
	}

}

