package dev.svitzthum.payroll.workinghours.adapter.out.timetracking;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem.TimeTrackingEvent;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedTimeTrackingSystemTest {

	private static final Clock SEPTEMBER_2026 = Clock.fixed(Instant.parse("2026-09-13T08:00:00Z"), ZoneOffset.UTC);

	private final SimulatedTimeTrackingSystem timeTracking = new SimulatedTimeTrackingSystem(
			new TimeTrackingProperties(List.of("TT-1001", "TT-1002"), 3), SEPTEMBER_2026);

	@Test
	void reportsTheConfiguredMonthsForEveryEmployee() {
		List<TimeTrackingEvent> events = this.timeTracking.fetchEvents();

		assertThat(events).hasSize(6)
			.extracting(TimeTrackingEvent::period)
			.containsOnly(YearMonth.of(2026, 6), YearMonth.of(2026, 7), YearMonth.of(2026, 8));
	}

	@Test
	void doesNotReportTheCurrentOrAFuturePeriod() {
		assertThat(this.timeTracking.fetchEvents()).extracting(TimeTrackingEvent::period)
			.allSatisfy(period -> assertThat(period.isBefore(YearMonth.of(2026, 9))).isTrue());
	}

	@Test
	void keepsEventIdsStableSoTheJournalRecognisesThem() {
		List<TimeTrackingEvent> first = this.timeTracking.fetchEvents();
		List<TimeTrackingEvent> second = this.timeTracking.fetchEvents();

		assertThat(second).isEqualTo(first);
		assertThat(first).extracting(TimeTrackingEvent::externalEventId).contains("TT-1001-2026-08");
	}

	@Test
	void reportsValuesTheDomainAccepts() {
		assertThat(this.timeTracking.fetchEvents()).extracting(TimeTrackingEvent::minutesWorked)
			.allSatisfy(minutes -> assertThat(WorkDuration.ofMinutes(minutes).minutes()).isBetween(120 * 60, 190 * 60));
	}

}


