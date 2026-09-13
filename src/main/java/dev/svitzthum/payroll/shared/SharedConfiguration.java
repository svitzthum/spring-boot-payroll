package dev.svitzthum.payroll.shared;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Infrastructure that is not tied to a single adapter.
 *
 * <p>
 * {@code @EnableResilientMethods} activates the {@code @Retryable} support of Spring
 * Framework. Its advisor has a higher precedence than the transaction advisor, so every
 * attempt runs in its own transaction and therefore reads fresh state.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class SharedConfiguration {

	/** Injected instead of calling {@code now()} statically, so tests can fix the time. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}

