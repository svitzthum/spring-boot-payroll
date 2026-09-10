package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps a period to the first day of the month, which is the normalised form the schema
 * expects and enforces with a check constraint.
 */
@Converter
class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

	@Override
	public LocalDate convertToDatabaseColumn(YearMonth period) {
		return (period != null) ? period.atDay(1) : null;
	}

	@Override
	public YearMonth convertToEntityAttribute(LocalDate firstDayOfMonth) {
		return (firstDayOfMonth != null) ? YearMonth.from(firstDayOfMonth) : null;
	}

}

