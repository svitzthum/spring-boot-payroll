package dev.svitzthum.payroll.workinghours.domain;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WorkDurationTest {

	@Test
	void convertsFromDurationWithoutLoss() {
		Duration original = Duration.parse("PT152H30M");

		assertThat(WorkDuration.of(original).minutes()).isEqualTo(152 * 60 + 30);
		assertThat(WorkDuration.of(original).toDuration()).isEqualTo(original);
	}

	@ParameterizedTest
	@ValueSource(strings = { "PT8H0M30S", "PT0.000000001S" })
	void rejectsAnythingSmallerThanAMinute(String duration) {
		assertThatIllegalArgumentException().isThrownBy(() -> WorkDuration.of(Duration.parse(duration)))
			.withMessageContaining("whole number of minutes");
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -60 })
	void rejectsNegativeTime(int minutes) {
		assertThatIllegalArgumentException().isThrownBy(() -> WorkDuration.ofMinutes(minutes))
			.withMessageContaining("must not be negative");
	}

	@Test
	void acceptsTheLongestPossibleMonth() {
		assertThat(WorkDuration.ofMinutes(WorkDuration.MAX_MINUTES).minutes()).isEqualTo(44_640);
	}

	@Test
	void rejectsMoreThan31Days() {
		assertThatIllegalArgumentException().isThrownBy(() -> WorkDuration.ofMinutes(WorkDuration.MAX_MINUTES + 1))
			.withMessageContaining("31 days");
	}

	@Test
	void rejectsALongDurationBeforeItOverflowsTheMinuteCount() {
		assertThatIllegalArgumentException().isThrownBy(() -> WorkDuration.of(Duration.parse("PT100000H")))
			.withMessageContaining("31 days");
	}

	@Test
	void isValueBased() {
		assertThat(WorkDuration.ofHours(8)).isEqualTo(WorkDuration.ofMinutes(480))
			.hasSameHashCodeAs(WorkDuration.ofMinutes(480));
	}


	@Test
	void printsAsIsoDuration() {
		assertThat(WorkDuration.ofMinutes(152 * 60 + 30)).hasToString("PT152H30M");
	}

}
