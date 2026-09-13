package dev.svitzthum.payroll.workinghours.domain;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The authoritative amount of time an employee actually worked in one calendar month.
 *
 * <p>
 * This is the aggregate root of the feature: exactly one instance exists per employee and
 * period, which is why employee and period together form its identity and are immutable.
 * The recorded time and its source can change — a later correction overwrites the
 * absolute value rather than adding a delta, which is what makes the write path
 * idempotent.
 *
 * <p>
 * The aggregate carries the optimistic locking version it was loaded with. It is absent
 * for an aggregate that has never been stored and is passed back to the repository port
 * on save so a concurrent modification can be detected (see ADR 0004).
 */
public final class MonthlyWorkingHours {

	private final UUID employeeId;

	private final YearMonth period;

	private WorkDuration workedTime;

	private WorkingHoursSource source;

	private final Long version;

	private MonthlyWorkingHours(UUID employeeId, YearMonth period, WorkDuration workedTime, WorkingHoursSource source,
			Long version) {
		this.employeeId = Objects.requireNonNull(employeeId, "employee id must not be null");
		this.period = Objects.requireNonNull(period, "period must not be null");
		this.workedTime = Objects.requireNonNull(workedTime, "worked time must not be null");
		this.source = Objects.requireNonNull(source, "source must not be null");
		this.version = version;
	}

	/**
	 * Creates a monthly value that has not been stored yet.
	 */
	public static MonthlyWorkingHours record(UUID employeeId, YearMonth period, WorkDuration workedTime,
			WorkingHoursSource source) {
		return new MonthlyWorkingHours(employeeId, period, workedTime, source, null);
	}

	/**
	 * Rebuilds an aggregate from stored state. Only the persistence adapter should call
	 * this, since only it owns the version.
	 */
	public static MonthlyWorkingHours restore(UUID employeeId, YearMonth period, WorkDuration workedTime,
			WorkingHoursSource source, long version) {
		return new MonthlyWorkingHours(employeeId, period, workedTime, source, version);
	}

	/**
	 * Overwrites the recorded time with the new absolute value and remembers which
	 * channel wrote it. Recording the same value again is a no-op in effect, which is
	 * what makes the use case idempotent.
	 */
	public void recordWorkedTime(WorkDuration workedTime, WorkingHoursSource source) {
		this.workedTime = Objects.requireNonNull(workedTime, "worked time must not be null");
		this.source = Objects.requireNonNull(source, "source must not be null");
	}

	/**
	 * Whether a value reported by the given source may replace the current one. A value
	 * entered manually is a correction of what the time tracking system delivered, so
	 * the import must not undo it (see ADR 0006).
	 */
	public boolean acceptsUpdateFrom(WorkingHoursSource source) {
		Objects.requireNonNull(source, "source must not be null");
		return source == WorkingHoursSource.MANUAL || this.source != WorkingHoursSource.MANUAL;
	}

	public UUID employeeId() {
		return this.employeeId;
	}

	public YearMonth period() {
		return this.period;
	}

	public WorkDuration workedTime() {
		return this.workedTime;
	}

	public WorkingHoursSource source() {
		return this.source;
	}

	public Optional<Long> version() {
		return Optional.ofNullable(this.version);
	}

	/** {@code true} if this aggregate has never been stored. */
	public boolean isNew() {
		return this.version == null;
	}

	/**
	 * Equality follows the aggregate identity — employee and period — not the current
	 * value, so a corrected instance still represents the same monthly record.
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof MonthlyWorkingHours that && this.employeeId.equals(that.employeeId)
				&& this.period.equals(that.period);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.employeeId, this.period);
	}

	@Override
	public String toString() {
		return "MonthlyWorkingHours[employeeId=%s, period=%s, workedTime=%s, source=%s]".formatted(this.employeeId,
				this.period, this.workedTime, this.source);
	}

}
