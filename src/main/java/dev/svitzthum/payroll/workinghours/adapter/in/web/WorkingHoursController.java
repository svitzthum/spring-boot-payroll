package dev.svitzthum.payroll.workinghours.adapter.in.web;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.port.in.GetWorkingHoursQuery;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursUseCase;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST adapter for the monthly working hours. The write endpoint is an idempotent upsert
 * of the absolute value, which is why it is a {@code PUT} on the resource identified by
 * employee and period rather than a {@code POST}.
 */
@RestController
@RequestMapping("/api/v1/employees/{employeeId}/working-hours")
class WorkingHoursController {

	private final RecordWorkingHoursUseCase recordWorkingHours;

	private final GetWorkingHoursQuery workingHours;

	WorkingHoursController(RecordWorkingHoursUseCase recordWorkingHours, GetWorkingHoursQuery workingHours) {
		this.recordWorkingHours = recordWorkingHours;
		this.workingHours = workingHours;
	}

	@PutMapping("/{period}")
	WorkingHoursResponse record(@PathVariable UUID employeeId, @PathVariable YearMonth period,
			@Valid @RequestBody RecordWorkingHoursRequest request) {
		RecordWorkingHoursCommand command = new RecordWorkingHoursCommand(employeeId, period,
				WorkDuration.of(request.workedTime()), WorkingHoursSource.MANUAL);
		return WorkingHoursResponse.of(this.recordWorkingHours.recordWorkingHours(command));
	}

	@GetMapping("/{period}")
	WorkingHoursResponse find(@PathVariable UUID employeeId, @PathVariable YearMonth period) {
		return this.workingHours.findWorkingHours(employeeId, period)
			.map(WorkingHoursResponse::of)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
					"no working hours recorded for employee %s and %s".formatted(employeeId, period)));
	}

	@GetMapping
	List<WorkingHoursResponse> findYear(@PathVariable UUID employeeId, @RequestParam int year) {
		return this.workingHours.findWorkingHoursOfYear(employeeId, year)
			.stream()
			.map(WorkingHoursResponse::of)
			.toList();
	}

	/**
	 * Payload of the upsert: the absolute time worked in the month, as an ISO 8601
	 * duration such as {@code PT152H30M}. The bounds are not repeated here — the
	 * invariants of {@code WorkDuration} are the single source of truth and surface as a
	 * {@code 400} through the exception handler.
	 */
	record RecordWorkingHoursRequest(@NotNull Duration workedTime) {
	}

}




