package dev.svitzthum.payroll.workinghours.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.ImportStatus;
import dev.svitzthum.payroll.workinghours.application.port.in.ImportTimeTrackingUseCase.ImportSummary;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeTrackingImportServiceTest {

	private static final Clock SEPTEMBER_2026 = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);

	private static final YearMonth AUGUST = YearMonth.of(2026, 8);

	private static final String REFERENCE = "TT-1001";

	private final InMemoryWorkingHoursRepository workingHours = new InMemoryWorkingHoursRepository();

	private final InMemoryEmployeeDirectory employees = new InMemoryEmployeeDirectory();

	private final InMemoryTimeTrackingSystem timeTracking = new InMemoryTimeTrackingSystem();

	private final InMemoryTimeTrackingImportJournal journal = new InMemoryTimeTrackingImportJournal();

	private TimeTrackingImportService service;

	private WorkingHoursService workingHoursService;

	private UUID employee;

	@BeforeEach
	void setUp() {
		this.workingHoursService = new WorkingHoursService(this.workingHours, this.employees, SEPTEMBER_2026);
		TimeTrackingEventProcessor processor = new TimeTrackingEventProcessor(this.workingHoursService, this.employees,
				this.journal, SEPTEMBER_2026);
		this.service = new TimeTrackingImportService(this.timeTracking, this.journal, processor);
		this.employee = this.employees.addActiveEmployee(REFERENCE);
	}

	@Test
	void writesTheReportedTimeThroughTheSameUseCaseAsTheRestAdapter() {
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, 152 * 60);

		ImportSummary summary = this.service.importTimeTracking();

		assertThat(summary).isEqualTo(new ImportSummary(1, 0, 0));
		MonthlyWorkingHours stored = this.workingHours.find(this.employee, AUGUST).orElseThrow();
		assertThat(stored.workedTime()).isEqualTo(WorkDuration.ofHours(152));
		assertThat(stored.source()).isEqualTo(WorkingHoursSource.TIME_TRACKING);
	}

	@Test
	void recordsWhatItDidInTheJournal() {
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, 152 * 60);

		this.service.importTimeTracking();

		assertThat(this.journal.entry("evt-1").status()).isEqualTo(ImportStatus.APPLIED);
		assertThat(this.journal.entry("evt-1").minutesWorked()).isEqualTo(152 * 60);
		assertThat(this.journal.entry("evt-1").importedAt()).isEqualTo(Instant.parse("2026-09-10T12:00:00Z"));
	}

	@Test
	void appliesAnEventOnlyOnceEvenIfTheImportRunsAgain() {
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, 152 * 60);
		this.service.importTimeTracking();

		ImportSummary second = this.service.importTimeTracking();

		assertThat(second.total()).isZero();
		assertThat(this.journal.entries()).hasSize(1);
	}

	@Test
	void doesNotOverwriteAValueThatWasEnteredManually() {
		this.workingHoursService.recordWorkingHours(new RecordWorkingHoursCommand(this.employee, AUGUST,
				WorkDuration.ofHours(160), WorkingHoursSource.MANUAL));
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, 152 * 60);

		ImportSummary summary = this.service.importTimeTracking();

		assertThat(summary).isEqualTo(new ImportSummary(0, 1, 0));
		assertThat(this.workingHours.find(this.employee, AUGUST).orElseThrow().workedTime())
			.isEqualTo(WorkDuration.ofHours(160));
		assertThat(this.journal.entry("evt-1").status()).isEqualTo(ImportStatus.SKIPPED);
	}

	@Test
	void overwritesItsOwnEarlierValue() {
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, 152 * 60);
		this.service.importTimeTracking();
		this.timeTracking.reports("evt-2", REFERENCE, AUGUST, 160 * 60);

		this.service.importTimeTracking();

		assertThat(this.workingHours.find(this.employee, AUGUST).orElseThrow().workedTime())
			.isEqualTo(WorkDuration.ofHours(160));
	}

	@Test
	void failsAnEventWithAnUnknownEmployeeReferenceWithoutStoppingTheRun() {
		this.timeTracking.reports("evt-1", "TT-9999", AUGUST, 152 * 60);
		this.timeTracking.reports("evt-2", REFERENCE, AUGUST, 150 * 60);

		ImportSummary summary = this.service.importTimeTracking();

		assertThat(summary).isEqualTo(new ImportSummary(1, 0, 1));
		assertThat(this.journal.entry("evt-1").status()).isEqualTo(ImportStatus.FAILED);
		assertThat(this.journal.entry("evt-1").detail()).contains("TT-9999");
	}

	@Test
	void failsAnEventWithAValueTheDomainRejects() {
		this.timeTracking.reports("evt-1", REFERENCE, AUGUST, -5);

		ImportSummary summary = this.service.importTimeTracking();

		assertThat(summary).isEqualTo(new ImportSummary(0, 0, 1));
		assertThat(this.journal.entry("evt-1").status()).isEqualTo(ImportStatus.FAILED);
		assertThat(this.workingHours.find(this.employee, AUGUST)).isEmpty();
	}

	@Test
	void failsAnEventForAPeriodThatHasNotStartedYet() {
		this.timeTracking.reports("evt-1", REFERENCE, YearMonth.of(2026, 10), 152 * 60);

		ImportSummary summary = this.service.importTimeTracking();

		assertThat(summary).isEqualTo(new ImportSummary(0, 0, 1));
		assertThat(this.journal.entry("evt-1").detail()).contains("2026-10");
	}

}
