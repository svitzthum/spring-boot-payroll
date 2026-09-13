package dev.svitzthum.payroll.workinghours.adapter.out.timetracking;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the simulated time tracking system.
 *
 * @param employeeReferences the identifiers the external system uses, matching
 * {@code employee.external_employee_ref}
 * @param months how many completed months are reported, counted back from the current one
 */
@ConfigurationProperties("payroll.time-tracking")
record TimeTrackingProperties(List<String> employeeReferences, int months) {

	TimeTrackingProperties {
		employeeReferences = (employeeReferences != null) ? List.copyOf(employeeReferences) : List.of();
		months = (months > 0) ? months : 3;
	}

}

