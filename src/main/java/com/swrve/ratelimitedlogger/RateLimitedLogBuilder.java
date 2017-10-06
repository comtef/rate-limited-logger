package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;


/**
 * Factory to create new RateLimitedLog instances in a fluent Builder style.  Start with
 * RateLimitedLog.withRateLimit(logger).
 */
public class RateLimitedLogBuilder {
    private final Logger logger;
    private final int maxRate;
    private final Duration periodLength;
    private Stopwatch stopwatch = new Stopwatch();
    private Optional<CounterMetric> stats = Optional.empty();

    public static class MissingRateAndPeriod {
        private final Logger logger;

        MissingRateAndPeriod(Logger logger) {
            this.logger = logger;
        }

        /**
         * Specify the maximum count of logs in every time period.  Required.
         */
        public MissingPeriod maxRate(int rate) {
            return new MissingPeriod(logger, rate);
        }
    }

    public static class MissingPeriod {
        private final Logger logger;
        private final int maxRate;

        private MissingPeriod(Logger logger, int rate) {
            Objects.requireNonNull(logger);
            this.logger = logger;
            this.maxRate = rate;
        }

        /**
         * Specify the time period.  Required.
         */
        public RateLimitedLogBuilder every(Duration duration) {
            Objects.requireNonNull(duration);
            return new RateLimitedLogBuilder(logger, maxRate, duration);
        }
    }

    private RateLimitedLogBuilder(Logger logger, int maxRate, Duration periodLength) {
        this.logger = logger;
        this.maxRate = maxRate;
        this.periodLength = periodLength;
    }

    /**
     * Specify that the rate-limited logger should compute time using @param stopwatch.
     */
    public RateLimitedLogBuilder withStopwatch(Stopwatch stopwatch) {
        this.stopwatch = Objects.requireNonNull(stopwatch);
        return this;
    }

    /**
     * Optional: should we record metrics about the call rate using @param stats.  Default is not to record metrics
     */
    public RateLimitedLogBuilder recordMetrics(CounterMetric stats) {
        this.stats = Optional.of(Objects.requireNonNull(stats));
        return this;
    }

    /**
     * @return a fully-built RateLimitedLog matching the requested configuration.
     */
    public RateLimitedLog build() {
        if (maxRate <= 0) {
            throw new IllegalArgumentException("maxRate must be > 0");
        }
        if (periodLength.toMillis() <= 0) {
            throw new IllegalArgumentException("period must be non-zero");
        }
        stopwatch.start();
        return new RateLimitedLog(logger,
                new RateLimitedLogWithPattern.RateAndPeriod(maxRate, periodLength), stopwatch,
                stats, RateLimitedLog.REGISTRY);
    }
}
