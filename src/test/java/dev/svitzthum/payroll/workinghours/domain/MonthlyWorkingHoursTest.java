package dev.svitzthum.payroll.workinghours.domain;

import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MonthlyWorkingHoursTest {

	private static final UUID EMPLOYEE = UUID.randomUUID();

	private static final YearMonth AUGUST = YearMonth.of(2026, 8);

	@Test
	void isCreatedWithoutAVersionUntilItIsStored() {
		MonthlyWorkingHours hours = MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.MANUAL);

		assertThat(hours.isNew()).isTrue();
		assertThat(hours.version()).isEmpty();
		assertThat(hours.employeeId()).isEqualTo(EMPLOYEE);
		assertThat(hours.period()).isEqualTo(AUGUST);
		assertThat(hours.source()).isEqualTo(WorkingHoursSource.MANUAL);
	}

	@Test
	void carriesTheVersionOfARestoredAggregate() {
		MonthlyWorkingHours hours = MonthlyWorkingHours.restore(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.MANUAL, 3L);

		assertThat(hours.isNew()).isFalse();
		assertThat(hours.version()).contains(3L);
	}

	@Test
	void overwritesTheAbsoluteValueAndRemembersTheSource() {
		MonthlyWorkingHours hours = MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.TIME_TRACKING);

		hours.recordWorkedTime(WorkDuration.ofHours(160), WorkingHoursSource.MANUAL);

		assertThat(hours.workedTime()).isEqualTo(WorkDuration.ofHours(160));
		assertThat(hours.source()).isEqualTo(WorkingHoursSource.MANUAL);
	}

	@Test
	void recordingTheSameValueAgainChangesNothing() {
		MonthlyWorkingHours hours = MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.MANUAL);

		hours.recordWorkedTime(WorkDuration.ofHours(152), WorkingHoursSource.MANUAL);

		assertThat(hours.workedTime()).isEqualTo(WorkDuration.ofHours(152));
	}

	@Test
	void identityIsEmployeeAndPeriodRatherThanTheRecordedValue() {
		MonthlyWorkingHours original = MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.MANUAL);
		MonthlyWorkingHours corrected = MonthlyWorkingHours.restore(EMPLOYEE, AUGUST, WorkDuration.ofHours(160),
				WorkingHoursSource.TIME_TRACKING, 7L);

		assertThat(original).isEqualTo(corrected).hasSameHashCodeAs(corrected);
	}

	@Test
	void differsFromTheSameEmployeeInAnotherMonth() {
		MonthlyWorkingHours august = MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ofHours(152),
				WorkingHoursSource.MANUAL);
		MonthlyWorkingHours september = MonthlyWorkingHours.record(EMPLOYEE, YearMonth.of(2026, 9),
				WorkDuration.ofHours(152), WorkingHoursSource.MANUAL);

		assertThat(august).isNotEqualTo(september);
	}

	@Test
	void rejectsMissingMandatoryParts() {
		assertThatNullPointerException()
			.isThrownBy(() -> MonthlyWorkingHours.record(null, AUGUST, WorkDuration.ZERO, WorkingHoursSource.MANUAL));
		assertThatNullPointerException()
			.isThrownBy(() -> MonthlyWorkingHours.record(EMPLOYEE, null, WorkDuration.ZERO, WorkingHoursSource.MANUAL));
		assertThatNullPointerException()
			.isThrownBy(() -> MonthlyWorkingHours.record(EMPLOYEE, AUGUST, null, WorkingHoursSource.MANUAL));
		assertThatNullPointerException()
			.isThrownBy(() -> MonthlyWorkingHours.record(EMPLOYEE, AUGUST, WorkDuration.ZERO, null));
	}

}
