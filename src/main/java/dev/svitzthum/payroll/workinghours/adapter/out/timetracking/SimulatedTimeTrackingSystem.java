package dev.svitzthum.payroll.workinghours.adapter.out.timetracking;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import dev.svitzthum.payroll.workinghours.application.port.out.TimeTrackingSystem;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stands in for the external time tracking system, which is fictitious in this
 * assignment. It fabricates the reported times instead of calling a real service, so the
 * application runs without any additional infrastructure.
 *
 * <p>
 * The data is deterministic: the same employee and month always yield the same event id
 * and the same value. That is what lets the import journal recognise an event it has
 * already applied, so repeated runs are visibly idempotent rather than accidentally so.
 *
 * <p>
 * A real integration would be a second implementation of {@link TimeTrackingSystem} —
 * an HTTP client, for instance — and nothing outside this package would change.
 */
@Component
@EnableConfigurationProperties(TimeTrackingProperties.class)
class SimulatedTimeTrackingSystem implements TimeTrackingSystem {

	private static final int MINIMUM_MINUTES = 120 * 60;

	private static final int RANGE_IN_MINUTES = 70 * 60;

	private final TimeTrackingProperties properties;

	private final Clock clock;

	SimulatedTimeTrackingSystem(TimeTrackingProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public List<TimeTrackingEvent> fetchEvents() {
		YearMonth currentPeriod = YearMonth.now(this.clock);
		List<TimeTrackingEvent> events = new ArrayList<>();
		for (String employeeReference : this.properties.employeeReferences()) {
			for (int monthsBack = this.properties.months(); monthsBack >= 1; monthsBack--) {
				YearMonth period = currentPeriod.minusMonths(monthsBack);
				events.add(new TimeTrackingEvent(eventId(employeeReference, period), employeeReference, period,
						reportedMinutes(employeeReference, period)));
			}
		}
		return List.copyOf(events);
	}

	/**
	 * Stable across runs, because a month that has been reported once keeps its identity.
	 */
	private static String eventId(String employeeReference, YearMonth period) {
		return "%s-%s".formatted(employeeReference, period);
	}

	/**
	 * A deterministic pseudo random value between 120 h and 189 h 55 min, in steps of
	 * five minutes.
	 */
	private static int reportedMinutes(String employeeReference, YearMonth period) {
		int seed = employeeReference.hashCode() * 31 + period.getYear() * 12 + period.getMonthValue();
		return MINIMUM_MINUTES + Math.floorMod(seed, RANGE_IN_MINUTES / 5) * 5;
	}

}

