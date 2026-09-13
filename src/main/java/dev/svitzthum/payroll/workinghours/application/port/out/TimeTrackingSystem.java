package dev.svitzthum.payroll.workinghours.application.port.out;

import java.time.YearMonth;
import java.util.List;

/**
 * The external time tracking system. The application pulls a feed of changes from it; a
 * correction in the source system appears as a new event with its own id, which is what
 * makes the id usable for detecting what has already been imported.
 */
public interface TimeTrackingSystem {

	List<TimeTrackingEvent> fetchEvents();

	/**
	 * One reported monthly total. The values are unvalidated on purpose: they come from
	 * outside, and an event that cannot be processed has to be recorded as failed rather
	 * than rejected on arrival.
	 */
	record TimeTrackingEvent(String externalEventId, String externalEmployeeRef, YearMonth period, int minutesWorked) {
	}

}
