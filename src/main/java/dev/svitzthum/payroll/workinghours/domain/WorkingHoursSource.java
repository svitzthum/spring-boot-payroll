package dev.svitzthum.payroll.workinghours.domain;

/**
 * Channel through which a monthly value was written. Every write records its source so a
 * later precedence rule between manual entry and the time tracking import can be applied
 * without guessing where a value came from.
 */
public enum WorkingHoursSource {

	/** Entered manually, e.g. by the managing director through the REST endpoint. */
	MANUAL,

	/** Transferred from the external time tracking system. */
	TIME_TRACKING

}
