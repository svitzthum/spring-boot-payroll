package dev.svitzthum.payroll.workinghours.application.service;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.EmployeeNotFoundException;
import dev.svitzthum.payroll.workinghours.application.FuturePeriodException;
import dev.svitzthum.payroll.workinghours.application.InactiveEmployeeException;
import dev.svitzthum.payroll.workinghours.application.port.in.GetWorkingHoursQuery;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursUseCase;
import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory;
import dev.svitzthum.payroll.workinghours.application.port.out.EmployeeDirectory.Employee;
import dev.svitzthum.payroll.workinghours.application.port.out.WorkingHoursRepository;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the inbound ports and owns the transaction boundary, so the REST adapter
 * and the scheduled import of iteration 2 get identical semantics.
 */
@Service
class WorkingHoursService implements RecordWorkingHoursUseCase, GetWorkingHoursQuery {

	private final WorkingHoursRepository workingHours;

	private final EmployeeDirectory employees;

	private final Clock clock;

	WorkingHoursService(WorkingHoursRepository workingHours, EmployeeDirectory employees, Clock clock) {
		this.workingHours = workingHours;
		this.employees = employees;
		this.clock = clock;
	}

	/**
	 * The rejections below happen before anything is written, so they must not mark the
	 * transaction for rollback: the import calls this method inside its own transaction
	 * and goes on to journal the rejected event.
	 */
	@Override
	@Transactional(noRollbackFor = { EmployeeNotFoundException.class, InactiveEmployeeException.class,
			FuturePeriodException.class })
	public MonthlyWorkingHours recordWorkingHours(RecordWorkingHoursCommand command) {
		Employee employee = requireEmployee(command.employeeId());
		if (!employee.active()) {
			throw new InactiveEmployeeException(employee.id());
		}
		YearMonth currentPeriod = YearMonth.now(this.clock);
		if (command.period().isAfter(currentPeriod)) {
			throw new FuturePeriodException(command.period(), currentPeriod);
		}
		MonthlyWorkingHours monthlyWorkingHours = this.workingHours.find(command.employeeId(), command.period())
			.map(existing -> {
				existing.recordWorkedTime(command.workedTime(), command.source());
				return existing;
			})
			.orElseGet(() -> MonthlyWorkingHours.record(command.employeeId(), command.period(), command.workedTime(),
					command.source()));
		return this.workingHours.save(monthlyWorkingHours);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MonthlyWorkingHours> findWorkingHours(UUID employeeId, YearMonth period) {
		requireEmployee(employeeId);
		return this.workingHours.find(employeeId, period);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonthlyWorkingHours> findWorkingHoursOfYear(UUID employeeId, int year) {
		requireEmployee(employeeId);
		return this.workingHours.findByYear(employeeId, year);
	}

	private Employee requireEmployee(UUID employeeId) {
		return this.employees.find(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));
	}

}

