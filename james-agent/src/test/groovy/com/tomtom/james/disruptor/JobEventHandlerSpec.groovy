package com.tomtom.james.disruptor

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class JobEventHandlerSpec extends Specification {

    def hanlderUnterTest = new JobEventHandler();

    def "Should handle job without rethrowing it's exception"() {
        given:
        def job = new Runnable() {
            @Override
            void run() {
                throw new RuntimeException("An exception that should be caught.")
            }
        }

        def event = new JobEvent()
        event.setJob(job)

        when:
        hanlderUnterTest.onEvent(event, 1, false)

        then:
        noExceptionThrown()
    }

    def "Should clear runnable after job is handled"() {
        given:
        AtomicBoolean jobWasRun = new AtomicBoolean(false)
        def job = new Runnable() {
            @Override
            void run() {
                jobWasRun.set(true)
            }
        }

        def event = new JobEvent()
        event.setJob(job)

        when:
        hanlderUnterTest.onEvent(event, 1, false)

        then:
        event.job == null
        jobWasRun.get()
    }
}
