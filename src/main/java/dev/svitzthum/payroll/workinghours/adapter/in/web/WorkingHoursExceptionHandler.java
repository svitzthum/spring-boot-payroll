package dev.svitzthum.payroll.workinghours.adapter.in.web;

import dev.svitzthum.payroll.workinghours.application.EmployeeNotFoundException;
import dev.svitzthum.payroll.workinghours.application.FuturePeriodException;
import dev.svitzthum.payroll.workinghours.application.InactiveEmployeeException;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the exceptions of the application layer to RFC 9457 responses. Bean Validation,
 * malformed bodies and unparsable path variables are handled by Spring itself, which
 * produces a {@code ProblemDetail} as well because {@code spring.mvc.problemdetails} is
 * enabled.
 */
@RestControllerAdvice
class WorkingHoursExceptionHandler {

	@ExceptionHandler(EmployeeNotFoundException.class)
	ProblemDetail handleEmployeeNotFound(EmployeeNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Employee not found", ex.getMessage());
	}

	@ExceptionHandler(InactiveEmployeeException.class)
	ProblemDetail handleInactiveEmployee(InactiveEmployeeException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Employee is not active", ex.getMessage());
	}

	@ExceptionHandler(FuturePeriodException.class)
	ProblemDetail handleFuturePeriod(FuturePeriodException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Period lies in the future", ex.getMessage());
	}

	@ExceptionHandler(WorkingHoursConflictException.class)
	ProblemDetail handleConflict(WorkingHoursConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Concurrent modification", ex.getMessage());
	}

	/**
	 * A violated domain invariant that Bean Validation did not catch, such as a duration
	 * above the plausible upper bound. It is a bad request, not a server error.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		return problemDetail;
	}

}



