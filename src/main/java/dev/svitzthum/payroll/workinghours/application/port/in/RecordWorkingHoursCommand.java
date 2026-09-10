package dev.svitzthum.payroll.workinghours.application.port.in;

import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;

/**
 * Absolute working time of one employee for one month, together with the channel that
 * reports it. Carrying the source in the command is what lets the REST adapter and the
 * scheduled import of iteration 2 share the same use case.
 */
public record RecordWorkingHoursCommand(UUID employeeId, YearMonth period, WorkDuration workedTime,
		WorkingHoursSource source) {

	public RecordWorkingHoursCommand {
		Objects.requireNonNull(employeeId, "employee id must not be null");
		Objects.requireNonNull(period, "period must not be null");
		Objects.requireNonNull(workedTime, "worked time must not be null");
		Objects.requireNonNull(source, "source must not be null");
	}

}

