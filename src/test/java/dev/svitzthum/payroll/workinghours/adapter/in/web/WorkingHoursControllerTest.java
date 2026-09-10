package dev.svitzthum.payroll.workinghours.adapter.in.web;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.svitzthum.payroll.workinghours.application.EmployeeNotFoundException;
import dev.svitzthum.payroll.workinghours.application.FuturePeriodException;
import dev.svitzthum.payroll.workinghours.application.InactiveEmployeeException;
import dev.svitzthum.payroll.workinghours.application.WorkingHoursConflictException;
import dev.svitzthum.payroll.workinghours.application.port.in.GetWorkingHoursQuery;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursCommand;
import dev.svitzthum.payroll.workinghours.application.port.in.RecordWorkingHoursUseCase;
import dev.svitzthum.payroll.workinghours.domain.MonthlyWorkingHours;
import dev.svitzthum.payroll.workinghours.domain.WorkDuration;
import dev.svitzthum.payroll.workinghours.domain.WorkingHoursSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkingHoursController.class)
class WorkingHoursControllerTest {

	private static final UUID EMPLOYEE = UUID.fromString("22222222-2222-2222-2222-222222222221");

	private static final String AUGUST_URI = "/api/v1/employees/" + EMPLOYEE + "/working-hours/2026-08";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RecordWorkingHoursUseCase recordWorkingHours;

	@MockitoBean
	private GetWorkingHoursQuery workingHours;

	@Test
	void recordsTheReportedTimeAndReturnsTheStoredState() throws Exception {
		given(this.recordWorkingHours.recordWorkingHours(any())).willAnswer(invocation -> {
			RecordWorkingHoursCommand command = invocation.getArgument(0);
			return MonthlyWorkingHours.restore(command.employeeId(), command.period(), command.workedTime(),
					command.source(), 0L);
		});

		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H30M"}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.employeeId").value(EMPLOYEE.toString()))
			.andExpect(jsonPath("$.period").value("2026-08"))
			.andExpect(jsonPath("$.workedTime").value("PT152H30M"))
			.andExpect(jsonPath("$.source").value("MANUAL"));
	}

	@Test
	void rejectsADurationWithSecondsAsBadRequest() throws Exception {
		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H30M15S"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void rejectsANegativeDurationAsBadRequest() throws Exception {
		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT-1H"}
				"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAMissingDurationAsBadRequest() throws Exception {
		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAnImplausiblyLongDurationAsBadRequest() throws Exception {
		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT800H"}
				"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAMalformedPeriodAsBadRequest() throws Exception {
		this.mockMvc
			.perform(put("/api/v1/employees/" + EMPLOYEE + "/working-hours/August-2026")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"workedTime": "PT152H"}
						"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void reportsAnUnknownEmployeeAsNotFound() throws Exception {
		willThrow(new EmployeeNotFoundException(EMPLOYEE)).given(this.recordWorkingHours).recordWorkingHours(any());

		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H"}
				"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Employee not found"));
	}

	@Test
	void reportsAnInactiveEmployeeAsBadRequest() throws Exception {
		willThrow(new InactiveEmployeeException(EMPLOYEE)).given(this.recordWorkingHours).recordWorkingHours(any());

		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Employee is not active"));
	}

	@Test
	void reportsAFuturePeriodAsBadRequest() throws Exception {
		willThrow(new FuturePeriodException(YearMonth.of(2026, 8), YearMonth.of(2026, 7))).given(this.recordWorkingHours)
			.recordWorkingHours(any());

		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Period lies in the future"));
	}

	@Test
	void reportsAConcurrentModificationAsConflict() throws Exception {
		willThrow(new WorkingHoursConflictException(EMPLOYEE, YearMonth.of(2026, 8), null))
			.given(this.recordWorkingHours)
			.recordWorkingHours(any());

		this.mockMvc.perform(put(AUGUST_URI).contentType(MediaType.APPLICATION_JSON).content("""
				{"workedTime": "PT152H"}
				"""))
			.andExpect(status().isConflict());
	}

	@Test
	void findsASingleMonth() throws Exception {
		given(this.workingHours.findWorkingHours(EMPLOYEE, YearMonth.of(2026, 8)))
			.willReturn(Optional.of(stored(WorkDuration.ofHours(152))));

		this.mockMvc.perform(get(AUGUST_URI))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("2026-08"));
	}

	@Test
	void reportsAMonthWithoutRecordedTimeAsNotFound() throws Exception {
		given(this.workingHours.findWorkingHours(EMPLOYEE, YearMonth.of(2026, 8))).willReturn(Optional.empty());

		this.mockMvc.perform(get(AUGUST_URI))
				.andExpect(status().isNotFound());
	}

	@Test
	void findsAllMonthsOfAYear() throws Exception {
		given(this.workingHours.findWorkingHoursOfYear(EMPLOYEE, 2026))
			.willReturn(List.of(stored(WorkDuration.ofHours(152))));

		this.mockMvc.perform(get("/api/v1/employees/" + EMPLOYEE + "/working-hours").param("year", "2026"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].period").value("2026-08"));
	}

	private static MonthlyWorkingHours stored(WorkDuration workedTime) {
		return MonthlyWorkingHours.restore(EMPLOYEE, YearMonth.of(2026, 8), workedTime, WorkingHoursSource.MANUAL, 0L);
	}

}





