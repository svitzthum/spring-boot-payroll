package dev.svitzthum.payroll.workinghours.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.EmployeeNotFoundException;
import dev.svitzthum.payroll.workinghours.application.FuturePeriodException;
import dev.svitzthum.payroll.workinghours.application.InactiveEmployeeException;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WorkingHoursServiceTest {

	private static final Clock SEPTEMBER_2026 = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);

	private final InMemoryWorkingHoursRepository workingHours = new InMemoryWorkingHoursRepository();

	private final InMemoryEmployeeDirectory employees = new InMemoryEmployeeDirectory();

	private WorkingHoursService service;

	private UUID employee;

	@BeforeEach
	void setUp() {
		this.service = new WorkingHoursService(this.workingHours, this.employees, SEPTEMBER_2026);
		this.employee = this.employees.addActiveEmployee();
	}

	@Test
	void storesTheReportedTimeForTheEmployeeAndMonth() {
		MonthlyWorkingHours recorded = this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));

		assertThat(recorded.employeeId()).isEqualTo(this.employee);
		assertThat(recorded.period()).isEqualTo(YearMonth.of(2026, 8));
		assertThat(recorded.workedTime()).isEqualTo(WorkDuration.ofHours(152));
		assertThat(recorded.source()).isEqualTo(WorkingHoursSource.MANUAL);
		assertThat(recorded.isNew()).isFalse();
	}

	@Test
	void recordingTwiceUpdatesTheSingleEntryInsteadOfAddingOne() {
		this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));
		MonthlyWorkingHours updated = this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 160 * 60));

		assertThat(this.workingHours.size()).isEqualTo(1);
		assertThat(updated.workedTime()).isEqualTo(WorkDuration.ofHours(160));
	}

	@Test
	void recordingTheSameValueTwiceIsIdempotent() {
		MonthlyWorkingHours first = this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));
		MonthlyWorkingHours second = this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));

		assertThat(second.workedTime()).isEqualTo(first.workedTime());
		assertThat(this.workingHours.size()).isEqualTo(1);
	}

	@Test
	void anImportCanOverwriteAManualEntryAndTheSourceFollows() {
		this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));

		MonthlyWorkingHours imported = this.service.recordWorkingHours(new RecordWorkingHoursCommand(this.employee,
				YearMonth.of(2026, 8), WorkDuration.ofHours(160), WorkingHoursSource.TIME_TRACKING));

		assertThat(imported.source()).isEqualTo(WorkingHoursSource.TIME_TRACKING);
	}

	@Test
	void acceptsTheCurrentMonth() {
		MonthlyWorkingHours recorded = this.service.recordWorkingHours(command(YearMonth.of(2026, 9), 80 * 60));

		assertThat(recorded.period()).isEqualTo(YearMonth.of(2026, 9));
	}

	@Test
	void rejectsAPeriodThatHasNotStartedYet() {
		assertThatExceptionOfType(FuturePeriodException.class)
			.isThrownBy(() -> this.service.recordWorkingHours(command(YearMonth.of(2026, 10), 152 * 60)));

		assertThat(this.workingHours.size()).isZero();
	}

	@Test
	void rejectsAnUnknownEmployee() {
		UUID unknown = UUID.randomUUID();

		assertThatExceptionOfType(EmployeeNotFoundException.class)
			.isThrownBy(() -> this.service.recordWorkingHours(new RecordWorkingHoursCommand(unknown,
					YearMonth.of(2026, 8), WorkDuration.ofHours(152), WorkingHoursSource.MANUAL)));
	}

	@Test
	void rejectsAnEmployeeWhoHasLeft() {
		UUID inactive = this.employees.addInactiveEmployee();

		assertThatExceptionOfType(InactiveEmployeeException.class)
			.isThrownBy(() -> this.service.recordWorkingHours(new RecordWorkingHoursCommand(inactive,
					YearMonth.of(2026, 8), WorkDuration.ofHours(152), WorkingHoursSource.MANUAL)));
	}

	@Test
	void findsARecordedMonth() {
		this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));

		assertThat(this.service.findWorkingHours(this.employee, YearMonth.of(2026, 8))).isPresent();
		assertThat(this.service.findWorkingHours(this.employee, YearMonth.of(2026, 7))).isEmpty();
	}

	@Test
	void findsAllMonthsOfAYear() {
		this.service.recordWorkingHours(command(YearMonth.of(2026, 7), 150 * 60));
		this.service.recordWorkingHours(command(YearMonth.of(2026, 8), 152 * 60));
		this.service.recordWorkingHours(command(YearMonth.of(2025, 8), 140 * 60));

		List<MonthlyWorkingHours> found = this.service.findWorkingHoursOfYear(this.employee, 2026);

		assertThat(found).hasSize(2)
			.extracting(MonthlyWorkingHours::period)
			.containsExactly(YearMonth.of(2026, 7), YearMonth.of(2026, 8));
	}

	@Test
	void queriesFailForAnUnknownEmployee() {
		UUID unknown = UUID.randomUUID();

		assertThatExceptionOfType(EmployeeNotFoundException.class)
			.isThrownBy(() -> this.service.findWorkingHours(unknown, YearMonth.of(2026, 8)));
		assertThatExceptionOfType(EmployeeNotFoundException.class)
			.isThrownBy(() -> this.service.findWorkingHoursOfYear(unknown, 2026));
	}

	@Test
	void readingIsStillPossibleForAnEmployeeWhoHasLeft() {
		UUID inactive = this.employees.addInactiveEmployee();

		assertThat(this.service.findWorkingHoursOfYear(inactive, 2026)).isEmpty();
	}

	private RecordWorkingHoursCommand command(YearMonth period, int minutes) {
		return new RecordWorkingHoursCommand(this.employee, period, WorkDuration.ofMinutes(minutes),
				WorkingHoursSource.MANUAL);
	}

}

