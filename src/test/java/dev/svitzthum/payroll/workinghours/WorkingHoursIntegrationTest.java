package dev.svitzthum.payroll.workinghours;

import java.util.UUID;

import dev.svitzthum.payroll.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Drives the whole hexagon over HTTP against a real PostgreSQL: web adapter, application
 * service, persistence adapter and the constraints of the schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Disabled("re-enable in iteration 2 step 4: the time tracking ports have no adapter yet")
class WorkingHoursIntegrationTest {

	/** Employees from V2__demo_data.sql. */
	private static final UUID ACTIVE_EMPLOYEE = UUID.fromString("22222222-2222-2222-2222-222222222221");

	private static final UUID INACTIVE_EMPLOYEE = UUID.fromString("22222222-2222-2222-2222-222222222223");

	private static final String AUGUST = "/api/v1/employees/" + ACTIVE_EMPLOYEE + "/working-hours/2026-08";

	@Autowired
	private RestTestClient client;

	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void clearRecordedHours() {
		this.jdbc.sql("delete from monthly_working_hours").update();
	}

	@Test
	void recordsWorkingHoursAndReadsThemBack() {
		this.client.put()
			.uri(AUGUST)
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"workedTime": "PT152H30M"}
					""")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.workedTime")
			.isEqualTo("PT152H30M")
			.jsonPath("$.source")
			.isEqualTo("MANUAL");

		this.client.get()
			.uri(AUGUST)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.period")
			.isEqualTo("2026-08")
			.jsonPath("$.workedTime")
			.isEqualTo("PT152H30M");
	}

	@Test
	void repeatingTheCallCorrectsTheValueInsteadOfAddingASecondEntry() {
		record(AUGUST, "PT152H30M");
		record(AUGUST, "PT160H");

		this.client.get()
			.uri("/api/v1/employees/" + ACTIVE_EMPLOYEE + "/working-hours?year=2026")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.length()")
			.isEqualTo(1)
			.jsonPath("$[0].workedTime")
			.isEqualTo("PT160H");
	}

	@Test
	void listsTheMonthsOfTheRequestedYearInOrder() {
		record("/api/v1/employees/" + ACTIVE_EMPLOYEE + "/working-hours/2026-07", "PT150H");
		record(AUGUST, "PT152H");
		record("/api/v1/employees/" + ACTIVE_EMPLOYEE + "/working-hours/2025-12", "PT140H");

		this.client.get()
			.uri("/api/v1/employees/" + ACTIVE_EMPLOYEE + "/working-hours?year=2026")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.length()")
			.isEqualTo(2)
			.jsonPath("$[0].period")
			.isEqualTo("2026-07")
			.jsonPath("$[1].period")
			.isEqualTo("2026-08");
	}

	@Test
	void reportsAnUnknownEmployeeAsNotFound() {
		this.client.put()
			.uri("/api/v1/employees/" + UUID.randomUUID() + "/working-hours/2026-08")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"workedTime": "PT152H"}
					""")
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectHeader()
			.contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void refusesToRecordTimeForAnEmployeeWhoHasLeft() {
		this.client.put()
			.uri("/api/v1/employees/" + INACTIVE_EMPLOYEE + "/working-hours/2026-08")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
					{"workedTime": "PT152H"}
					""")
			.exchange()
			.expectStatus()
			.isBadRequest();
	}

	@Test
	void reportsAMonthWithoutRecordedTimeAsNotFound() {
		this.client.get().uri(AUGUST).exchange().expectStatus().isNotFound();
	}

	@Test
	void theDatabaseRejectsASecondRowForTheSameEmployeeAndMonth() {
		record(AUGUST, "PT152H");

		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> this.jdbc.sql("""
				insert into monthly_working_hours
					(id, employee_id, period, minutes_worked, last_source, version, created_at, updated_at)
				values (?, ?, date '2026-08-01', 9120, 'MANUAL', 0, now(), now())
				""").params(UUID.randomUUID(), ACTIVE_EMPLOYEE).update());
	}

	private void record(String uri, String workedTime) {
		this.client.put()
			.uri(uri)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"workedTime\": \"%s\"}".formatted(workedTime))
			.exchange()
			.expectStatus()
			.isOk();
	}

}

