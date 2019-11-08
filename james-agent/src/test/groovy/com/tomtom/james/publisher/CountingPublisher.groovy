package com.tomtom.james.publisher

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.common.api.publisher.EventPublisher

import java.util.concurrent.atomic.AtomicLong

class CountingPublisher implements EventPublisher {

    private final AtomicLong counter = new AtomicLong()
    private final Closure beforePublish

    CountingPublisher(Closure<Event> beforePublish) {
        this.beforePublish = beforePublish
    }

    public long getProcessedJobs(){
        return counter.get();
    }

    @Override
    void initialize(EventPublisherConfiguration configuration) {

    }

    @Override
    void publish(Event evt) {
        beforePublish?.call(evt)
        counter.incrementAndGet()
    }

    @Override
    void close() {

    }

    @Override
    String getId() {
        return "counting.publisher"
    }
}