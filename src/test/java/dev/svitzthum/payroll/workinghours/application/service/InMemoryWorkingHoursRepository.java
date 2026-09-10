package dev.svitzthum.payroll.workinghours.application.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.out.WorkingHoursRepository;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;

/**
 * In-memory stand-in for the persistence adapter. It keeps the two behaviours the
 * service depends on: one entry per employee and period, and a conflict when the stored
 * version has moved on.
 */
class InMemoryWorkingHoursRepository implements WorkingHoursRepository {

	private record Key(UUID employeeId, YearMonth period) {
	}

	private final Map<Key, MonthlyWorkingHours> entries = new LinkedHashMap<>();

	private long nextVersion;

	@Override
	public Optional<MonthlyWorkingHours> find(UUID employeeId, YearMonth period) {
		return Optional.ofNullable(this.entries.get(new Key(employeeId, period))).map(InMemoryWorkingHoursRepository::copyOf);
	}

	@Override
	public List<MonthlyWorkingHours> findByYear(UUID employeeId, int year) {
		List<MonthlyWorkingHours> found = new ArrayList<>();
		this.entries.forEach((key, value) -> {
			if (key.employeeId().equals(employeeId) && key.period().getYear() == year) {
				found.add(copyOf(value));
			}
		});
		return List.copyOf(found);
	}

	@Override
	public MonthlyWorkingHours save(MonthlyWorkingHours workingHours) {
		Key key = new Key(workingHours.employeeId(), workingHours.period());
		MonthlyWorkingHours stored = this.entries.get(key);
		if (!versionsMatch(workingHours, stored)) {
			throw new WorkingHoursConflictException(workingHours.employeeId(), workingHours.period(), null);
		}
		MonthlyWorkingHours saved = MonthlyWorkingHours.restore(workingHours.employeeId(), workingHours.period(),
				workingHours.workedTime(), workingHours.source(), this.nextVersion++);
		this.entries.put(key, saved);
		return copyOf(saved);
	}

	private static boolean versionsMatch(MonthlyWorkingHours toSave, MonthlyWorkingHours stored) {
		if (stored == null) {
			return toSave.isNew();
		}
		return toSave.version().equals(stored.version());
	}

	private static MonthlyWorkingHours copyOf(MonthlyWorkingHours source) {
		return source.version()
			.map(version -> MonthlyWorkingHours.restore(source.employeeId(), source.period(), source.workedTime(),
					source.source(), version))
			.orElseGet(() -> MonthlyWorkingHours.record(source.employeeId(), source.period(), source.workedTime(),
					source.source()));
	}

	int size() {
		return this.entries.size();
	}

}

