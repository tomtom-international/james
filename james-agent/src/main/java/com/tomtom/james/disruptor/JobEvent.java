package com.tomtom.james.disruptor;

import com.lmax.disruptor.EventFactory;

import java.lang.ref.SoftReference;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class JobEvent {

    private AtomicReference<SoftReference<Runnable>> job = new AtomicReference<>();

    public JobEvent() {
        //no need to init an event.
    }

    public Runnable getJob() {
        return Optional
                .of(job)
                .map(AtomicReference::get)
                .map(SoftReference::get)
                .orElse(null);
    }

    public void setJob(final Runnable job) {
        this.job.set(new SoftReference<>(job));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JobEvent.class.getSimpleName() + "[", "]")
                .add("job=" + job)
                .toString();
    }

    public static final class Factory implements EventFactory<JobEvent> {

        @Override
        public JobEvent newInstance() {
            return new JobEvent();
        }
    }
}
