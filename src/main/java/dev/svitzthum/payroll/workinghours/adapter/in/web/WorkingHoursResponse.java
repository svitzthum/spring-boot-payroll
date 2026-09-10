package dev.svitzthum.payroll.workinghours.adapter.in.web;

import java.time.Duration;
import java.time.YearMonth;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;

record WorkingHoursResponse(UUID employeeId, YearMonth period, Duration workedTime, WorkingHoursSource source) {

	static WorkingHoursResponse of(MonthlyWorkingHours workingHours) {
		return new WorkingHoursResponse(workingHours.employeeId(), workingHours.period(),
				workingHours.workedTime().toDuration(), workingHours.source());
	}

}

