package dev.svitzthum.payroll.workinghours.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Amount of time worked, stored as whole minutes.
 *
 * <p>
 * Minutes are the smallest unit the domain accepts: they map to the {@code integer}
 * column without rounding and convert to and from ISO 8601 durations
 * ({@code PT152H30M}) without arithmetic loss. See ADR 0003.
 *
 * <p>
 * Invariants: never negative and never above {@link #MAX_MINUTES}, the number of minutes
 * in a 31 day month. The upper bound is a plausibility guard against typos, not a
 * business rule about acceptable working time; it mirrors the check constraint in the
 * schema so domain and database agree.
 *
 * @param minutes whole minutes worked, between {@code 0} and {@link #MAX_MINUTES}
 */
public record WorkDuration(int minutes) {

	/** 31 days expressed in minutes: the longest month that can physically be worked. */
	public static final int MAX_MINUTES = 31 * 24 * 60;

	public static final WorkDuration ZERO = new WorkDuration(0);

	public WorkDuration {
		if (minutes < 0) {
			throw new IllegalArgumentException("worked time must not be negative, but was " + minutes + " minutes");
		}
		if (minutes > MAX_MINUTES) {
			throw new IllegalArgumentException(
					"worked time must not exceed %d minutes (31 days), but was %d minutes".formatted(MAX_MINUTES, minutes));
		}
	}

	public static WorkDuration ofMinutes(int minutes) {
		return new WorkDuration(minutes);
	}

	public static WorkDuration ofHours(int hours) {
		return of(Duration.ofHours(hours));
	}

	/**
	 * @throws IllegalArgumentException if the duration is negative, has a sub-minute
	 * component or exceeds {@link #MAX_MINUTES}
	 */
	public static WorkDuration of(Duration duration) {
		Objects.requireNonNull(duration, "worked time must not be null");
		if (duration.toSecondsPart() != 0 || duration.toNanosPart() != 0) {
			throw new IllegalArgumentException("worked time must be a whole number of minutes, but was " + duration);
		}
		long totalMinutes = duration.toMinutes();
		if (totalMinutes > MAX_MINUTES) {
			throw new IllegalArgumentException(
					"worked time must not exceed %d minutes (31 days), but was %s".formatted(MAX_MINUTES, duration));
		}
		return new WorkDuration((int) totalMinutes);
	}

	public Duration toDuration() {
		return Duration.ofMinutes(this.minutes);
	}


	@Override
	public String toString() {
		return toDuration().toString();
	}

}
