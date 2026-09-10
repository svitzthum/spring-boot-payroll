package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.out.WorkingHoursRepository;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Implements the outbound port on top of JPA and turns the two database level guards —
 * the unique constraint on (employee, period) and the version column — into the domain
 * level {@link WorkingHoursConflictException}.
 */
@Component
class WorkingHoursPersistenceAdapter implements WorkingHoursRepository {

	private final MonthlyWorkingHoursJpaRepository entities;

	WorkingHoursPersistenceAdapter(MonthlyWorkingHoursJpaRepository entities) {
		this.entities = entities;
	}

	@Override
	public Optional<MonthlyWorkingHours> find(UUID employeeId, YearMonth period) {
		return this.entities.findByEmployeeIdAndPeriod(employeeId, period).map(WorkingHoursMapper::toDomain);
	}

	@Override
	public List<MonthlyWorkingHours> findByYear(UUID employeeId, int year) {
		return this.entities.findByEmployeeIdAndPeriodBetween(employeeId, YearMonth.of(year, 1), YearMonth.of(year, 12))
			.stream()
			.map(WorkingHoursMapper::toDomain)
			.toList();
	}

	@Override
	public MonthlyWorkingHours save(MonthlyWorkingHours workingHours) {
		UUID employeeId = workingHours.employeeId();
		YearMonth period = workingHours.period();
		MonthlyWorkingHoursEntity entity = this.entities.findByEmployeeIdAndPeriod(employeeId, period)
			.orElseGet(() -> new MonthlyWorkingHoursEntity(employeeId, period));
		requireExpectedVersion(workingHours, entity);
		WorkingHoursMapper.applyTo(workingHours, entity);
		try {
			// flush here so a conflict surfaces as a failure of this call rather than of
			// the surrounding commit
			return WorkingHoursMapper.toDomain(this.entities.saveAndFlush(entity));
		}
		catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
			throw new WorkingHoursConflictException(employeeId, period, ex);
		}
	}

	private static void requireExpectedVersion(MonthlyWorkingHours workingHours, MonthlyWorkingHoursEntity entity) {
		Optional<Long> stored = Optional.ofNullable(entity.getVersion());
		if (!workingHours.version().equals(stored)) {
			throw new WorkingHoursConflictException(workingHours.employeeId(), workingHours.period(), null);
		}
	}

}



