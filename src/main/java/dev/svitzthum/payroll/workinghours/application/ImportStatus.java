package dev.svitzthum.payroll.workinghours.application;

/** What happened to an event of the external time tracking system. */
public enum ImportStatus {

	/** The monthly value was written. */
	APPLIED,

	/** A manual entry took precedence, see ADR 0006. */
	SKIPPED,

	/** The event could not be processed, for example because the employee is unknown. */
	FAILED

}
