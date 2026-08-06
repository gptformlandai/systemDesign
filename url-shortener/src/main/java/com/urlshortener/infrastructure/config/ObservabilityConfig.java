package com.urlshortener.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Observability configuration — Micrometer metrics and JVM telemetry.
 *
 * <p>Metrics exposed:
 * <ul>
 *   <li>{@code url.redirect.success} — counter per successful redirect</li>
 *   <li>{@code url.redirect.error} — counter per redirect error (tagged by reason)</li>
 *   <li>{@code url.redirect.latency} — timer (p50/p95/p99) for redirect resolution</li>
 *   <li>{@code url.create.success} — counter per successful URL creation</li>
 *   <li>{@code url.create.error} — counter per creation error (tagged by reason)</li>
 *   <li>JVM GC, memory, thread, and CPU metrics</li>
 * </ul>
 *
 * <p>Metrics are scraped by Prometheus at {@code /actuator/prometheus}.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
