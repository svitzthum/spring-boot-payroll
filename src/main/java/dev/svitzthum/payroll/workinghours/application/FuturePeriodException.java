package dev.svitzthum.payroll.workinghours.application;

import java.time.YearMonth;

/** Working time can only be reported for a month that has started. */
public class FuturePeriodException extends RuntimeException {

	private final YearMonth period;

	public FuturePeriodException(YearMonth period, YearMonth currentPeriod) {
		super("period %s lies in the future, the current period is %s".formatted(period, currentPeriod));
		this.period = period;
	}

	public YearMonth period() {
		return this.period;
	}

}

