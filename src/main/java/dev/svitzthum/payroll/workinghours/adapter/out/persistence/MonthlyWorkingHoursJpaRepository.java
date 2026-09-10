package dev.svitzthum.payroll.workinghours.adapter.out.persistence;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MonthlyWorkingHoursJpaRepository extends JpaRepository<MonthlyWorkingHoursEntity, UUID> {

	Optional<MonthlyWorkingHoursEntity> findByEmployeeIdAndPeriod(UUID employeeId, YearMonth period);

	/**
	 * Returns the working hours for all months within the given range, ordered by period.
	 */
	@Query("""
			select entry from MonthlyWorkingHoursEntity entry
			where entry.employeeId = :employeeId and entry.period between :from and :to
			order by entry.period
			""")
	List<MonthlyWorkingHoursEntity> findByEmployeeIdAndPeriodBetween(@Param("employeeId") UUID employeeId,
			@Param("from") YearMonth from, @Param("to") YearMonth to);

}
