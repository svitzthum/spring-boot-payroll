package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.Instant;
import java.time.YearMonth;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import dev.svitzthum.payroll.workinghours.application.EventAlreadyImportedException;
import dev.svitzthum.payroll.workinghours.application.ImportStatus;
import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingImportJournal.ImportedEvent;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, PersistenceConfiguration.class,
		TimeTrackingImportJournalAdapter.class })
class TimeTrackingImportJournalAdapterTest {

	private static final YearMonth AUGUST = YearMonth.of(2026, 8);

	@Autowired
	private TimeTrackingImportJournalAdapter journal;

	@Autowired
	private TimeTrackingImportJpaRepository entries;

	@Test
	void remembersAnEventItHasSeen() {
		this.journal.record(event("TT-1001-2026-08", ImportStatus.APPLIED, null));

		assertThat(this.journal.contains("TT-1001-2026-08")).isTrue();
		assertThat(this.journal.contains("TT-1001-2026-07")).isFalse();
	}

	@Test
	void keepsWhatTheExternalSystemReportedAndWhatBecameOfIt() {
		this.journal.record(event("TT-9999-2026-08", ImportStatus.FAILED, "unknown employee reference TT-9999"));

		TimeTrackingImportEntity stored = this.entries.findByExternalEventId("TT-9999-2026-08").orElseThrow();

		assertThat(stored.getExternalEmployeeRef()).isEqualTo("TT-9999");
		assertThat(stored.getPeriod()).isEqualTo(AUGUST);
		assertThat(stored.getMinutesWorked()).isEqualTo(9120);
		assertThat(stored.getStatus()).isEqualTo(ImportStatus.FAILED);
		assertThat(stored.getDetail()).isEqualTo("unknown employee reference TT-9999");
		assertThat(stored.getImportedAt()).isNotNull();
	}

	@Test
	void refusesToRecordTheSameEventTwice() {
		this.journal.record(event("TT-1001-2026-08", ImportStatus.APPLIED, null));

		assertThatExceptionOfType(EventAlreadyImportedException.class)
			.isThrownBy(() -> this.journal.record(event("TT-1001-2026-08", ImportStatus.APPLIED, null)));
	}

	private static ImportedEvent event(String externalEventId, ImportStatus status, String detail) {
		String employeeRef = externalEventId.substring(0, externalEventId.indexOf("-2"));
		return new ImportedEvent(externalEventId, employeeRef, AUGUST, 9120, status, detail, Instant.now());
	}

}

