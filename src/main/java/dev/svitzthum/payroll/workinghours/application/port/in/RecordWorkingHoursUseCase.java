package dev.svitzthum.payroll.workinghours.application.port.in;

import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

/**
 * Records the working time of one employee for one month. Writing is idempotent: the
 * command carries the absolute value, so repeating it leads to the same stored state.
 */
public interface RecordWorkingHoursUseCase {

	MonthlyWorkingHours recordWorkingHours(RecordWorkingHoursCommand command);

}

