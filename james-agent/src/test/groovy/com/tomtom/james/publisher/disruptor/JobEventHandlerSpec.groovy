package com.tomtom.james.publisher.disruptor

import spock.lang.Specification

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
}
