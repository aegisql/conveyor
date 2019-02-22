package com.aegisql.conveyor.delay;

import com.aegisql.conveyor.Expireable;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedExpireable implements Delayed, Expireable {

    private final long expirationTime;

    public DelayedExpireable(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public DelayedExpireable(Expireable expireable) {
        this(expireable.getExpirationTime());
    }

    public DelayedExpireable(Instant instant) {
        this(instant.toEpochMilli());
    }

    @Override
    public long getExpirationTime() {
        return expirationTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delta;
        if( expirationTime <= 0 ) {
            delta = Long.MAX_VALUE;
        } else {
            delta = expirationTime - System.currentTimeMillis();
        }
        return unit.convert(delta, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),o.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelayedExpireable that = (DelayedExpireable) o;
        return expirationTime == that.expirationTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expirationTime);
    }

    @Override
    public String toString() {
        return "DelayedExpireable{" +
                " expirationTime=" + expirationTime +
                " delayTime=" + getDelay(TimeUnit.MILLISECONDS) +
                '}';
    }
}
