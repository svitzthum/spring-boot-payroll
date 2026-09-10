package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, PersistenceConfiguration.class, WorkingHoursPersistenceAdapter.class })
class WorkingHoursPersistenceAdapterTest {

	/** Active employee from V2__demo_data.sql. */
	private static final UUID EMPLOYEE = UUID.fromString("22222222-2222-2222-2222-222222222221");

	private static final UUID OTHER_EMPLOYEE = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private WorkingHoursPersistenceAdapter adapter;

	@Test
	void storesAndReadsBackTheMonthlyValue() {
		this.adapter.save(newEntry(YearMonth.of(2026, 8), 152 * 60));

		MonthlyWorkingHours found = this.adapter.find(EMPLOYEE, YearMonth.of(2026, 8)).orElseThrow();

		assertThat(found.employeeId()).isEqualTo(EMPLOYEE);
		assertThat(found.period()).isEqualTo(YearMonth.of(2026, 8));
		assertThat(found.workedTime()).isEqualTo(WorkDuration.ofHours(152));
		assertThat(found.source()).isEqualTo(WorkingHoursSource.MANUAL);
		assertThat(found.version()).contains(0L);
	}

	@Test
	void updatingKeepsTheSingleRowAndRaisesTheVersion() {
		MonthlyWorkingHours stored = this.adapter.save(newEntry(YearMonth.of(2026, 8), 152 * 60));

		stored.recordWorkedTime(WorkDuration.ofHours(160), WorkingHoursSource.TIME_TRACKING);
		MonthlyWorkingHours updated = this.adapter.save(stored);

		assertThat(updated.workedTime()).isEqualTo(WorkDuration.ofHours(160));
		assertThat(updated.source()).isEqualTo(WorkingHoursSource.TIME_TRACKING);
		assertThat(updated.version()).contains(1L);
		assertThat(this.adapter.findByYear(EMPLOYEE, 2026)).hasSize(1);
	}

	@Test
	void findsOnlyTheRequestedYearAndOrdersByPeriod() {
		this.adapter.save(newEntry(YearMonth.of(2026, 11), 150 * 60));
		this.adapter.save(newEntry(YearMonth.of(2026, 1), 140 * 60));
		this.adapter.save(newEntry(YearMonth.of(2025, 12), 130 * 60));

		List<MonthlyWorkingHours> found = this.adapter.findByYear(EMPLOYEE, 2026);

		assertThat(found).extracting(MonthlyWorkingHours::period)
			.containsExactly(YearMonth.of(2026, 1), YearMonth.of(2026, 11));
	}

	@Test
	void separatesEmployees() {
		this.adapter.save(newEntry(YearMonth.of(2026, 8), 152 * 60));

		assertThat(this.adapter.find(OTHER_EMPLOYEE, YearMonth.of(2026, 8))).isEmpty();
	}

	@Test
	void rejectsANewEntryWhenTheMonthIsAlreadyStored() {
		this.adapter.save(newEntry(YearMonth.of(2026, 8), 152 * 60));

		assertThatExceptionOfType(WorkingHoursConflictException.class)
			.isThrownBy(() -> this.adapter.save(newEntry(YearMonth.of(2026, 8), 160 * 60)));
	}

	@Test
	void rejectsAnUpdateBasedOnAnOutdatedVersion() {
		YearMonth august = YearMonth.of(2026, 8);
		this.adapter.save(newEntry(august, 152 * 60));

		MonthlyWorkingHours readByTheDirector = this.adapter.find(EMPLOYEE, august).orElseThrow();
		MonthlyWorkingHours readByTheImport = this.adapter.find(EMPLOYEE, august).orElseThrow();

		readByTheDirector.recordWorkedTime(WorkDuration.ofHours(160), WorkingHoursSource.MANUAL);
		this.adapter.save(readByTheDirector);

		readByTheImport.recordWorkedTime(WorkDuration.ofHours(170), WorkingHoursSource.TIME_TRACKING);

		assertThatExceptionOfType(WorkingHoursConflictException.class)
			.isThrownBy(() -> this.adapter.save(readByTheImport));
		assertThat(this.adapter.find(EMPLOYEE, august).orElseThrow().workedTime())
			.isEqualTo(WorkDuration.ofHours(160));
	}

	private static MonthlyWorkingHours newEntry(YearMonth period, int minutes) {
		return MonthlyWorkingHours.record(EMPLOYEE, period, WorkDuration.ofMinutes(minutes), WorkingHoursSource.MANUAL);
	}

}



