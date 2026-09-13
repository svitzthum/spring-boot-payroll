package dev.svitzthum.payroll.workinghours.application.port.out;

import java.time.Instant;
import java.time.YearMonth;

import dev.svitzthum.payroll.workinghours.application.ImportStatus;

/**
 * Records which events of the external system have been processed and what happened to
 * them. This is what makes the import idempotent, see ADR 0007.
 */
public interface TimeTrackingImportJournal {

	boolean contains(String externalEventId);

	/**
	 * @throws dev.svitzthum.payroll.workinghours.application.EventAlreadyImportedException
	 * if the event has been recorded in the meantime, for example by another application
	 * instance
	 */
	void record(ImportedEvent event);

	record ImportedEvent(String externalEventId, String externalEmployeeRef, YearMonth period, int minutesWorked,
			ImportStatus status, String detail, Instant importedAt) {
	}

}
