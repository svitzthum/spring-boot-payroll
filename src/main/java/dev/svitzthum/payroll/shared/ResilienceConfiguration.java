package dev.svitzthum.payroll.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Enables the {@code @Retryable} support of Spring Framework. Its advisor has a higher
 * precedence than the transaction advisor, so every attempt runs in its own transaction
 * and therefore reads fresh state.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfiguration {

}

