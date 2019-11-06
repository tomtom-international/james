package com.tomtom.james.publisher.disruptor;

import com.lmax.disruptor.EventFactory;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class JobEvent {

    private AtomicReference<Runnable> job = new AtomicReference<>();

    public JobEvent() {
    }

    public Runnable getJob() {
        return job.get();
    }

    public void setJob(final Runnable job) {
        this.job.set(job);
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
