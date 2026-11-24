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

    def "Workers should run each job exactly once"() {
        given:
        def workers = JobEventHandler.workers(3)
        def runs = new java.util.concurrent.atomic.AtomicInteger[7]
        def events = (0..<7).collect { i ->
            runs[i] = new java.util.concurrent.atomic.AtomicInteger()
            def event = new JobEvent()
            event.setJob({ runs[i].incrementAndGet() } as Runnable)
            event
        }

        when:
        events.eachWithIndex { event, seq -> workers.each { it.onEvent(event, seq, false) } }

        then:
        runs*.get() == [1] * 7
        events.every { it.job == null }
    }
}
