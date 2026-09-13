package dev.svitzthum.payroll.workinghours.application;

/** The event has already been recorded in the import journal. */
public class EventAlreadyImportedException extends RuntimeException {

	public EventAlreadyImportedException(String externalEventId, Throwable cause) {
		super("event %s has already been imported".formatted(externalEventId), cause);
	}

}
