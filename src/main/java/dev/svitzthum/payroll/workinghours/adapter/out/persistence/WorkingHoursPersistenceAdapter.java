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
 *
 * <p>
 * Every change of the stored value also appends a revision, so it stays visible which
 * source set which value when.
 */
@Component
class WorkingHoursPersistenceAdapter implements WorkingHoursRepository {

	private final MonthlyWorkingHoursJpaRepository entities;

	private final WorkingHoursRevisionJpaRepository revisions;

	WorkingHoursPersistenceAdapter(MonthlyWorkingHoursJpaRepository entities,
			WorkingHoursRevisionJpaRepository revisions) {
		this.entities = entities;
		this.revisions = revisions;
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
		boolean changed = changesTheStoredValue(workingHours, entity);
		WorkingHoursMapper.applyTo(workingHours, entity);
		MonthlyWorkingHoursEntity saved;
		try {
			// flush here so a conflict surfaces as a failure of this call rather than of
			// the surrounding commit
			saved = this.entities.saveAndFlush(entity);
		}
		catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
			throw new WorkingHoursConflictException(employeeId, period, ex);
		}
		if (changed) {
			this.revisions.save(new WorkingHoursRevisionEntity(saved.getId(), saved.getMinutesWorked(),
					saved.getLastSource()));
		}
		return WorkingHoursMapper.toDomain(saved);
	}

	/**
	 * Repeating a call with the value that is already stored is a legitimate upsert, but
	 * it is not a change and must not show up in the history.
	 */
	private static boolean changesTheStoredValue(MonthlyWorkingHours workingHours, MonthlyWorkingHoursEntity entity) {
		return entity.getVersion() == null || entity.getMinutesWorked() != workingHours.workedTime().minutes()
				|| entity.getLastSource() != workingHours.source();
	}

	/**
	 * Guards what the version column alone cannot: this method reloads the row before
	 * writing, so the caller's aggregate may be older than what is now stored. Two cases
	 * matter, and both are reachable when the two write paths collide.
	 *
	 * <p>
	 * A caller that read version 3 while version 4 is stored must not have its value
	 * applied silently. And a caller whose aggregate is new — it found no row — must not
	 * overwrite a row that another writer inserted in the meantime: the reload would find
	 * that row and turn the intended insert into an update, which the unique constraint
	 * cannot catch because no second row is written.
	 */
	private static void requireExpectedVersion(MonthlyWorkingHours workingHours, MonthlyWorkingHoursEntity entity) {
		Optional<Long> stored = Optional.ofNullable(entity.getVersion());
		if (!workingHours.version().equals(stored)) {
			throw new WorkingHoursConflictException(workingHours.employeeId(), workingHours.period(), null);
		}
	}

}



