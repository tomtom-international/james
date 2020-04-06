package com.tomtom.james.publisher

import com.tomtom.james.common.api.QueueBacked
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.common.api.publisher.EventPublisher
import org.awaitility.Awaitility
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DisruptorAsyncPublisherSpec extends Specification {
    EventPublisher publisherUnderTest

    private int WORKER_COUNT = 2

    private int PUBLISHER_SIZE = Math.max(2 * WORKER_COUNT, Runtime.getRuntime().availableProcessors() - WORKER_COUNT)

    ExecutorService eventCreator = Executors.newFixedThreadPool(PUBLISHER_SIZE)
    CountingPublisher sleepingPublisher = new CountingPublisher( { it-> sleep(10)})
    CountingPublisher immidiatePublisher = new CountingPublisher( null)

    def "Processes all jobs up to capacity"() {
        given:
        def QUEUE_SIZE = 16384
        def publisher = immidiatePublisher

        publisherUnderTest = new DisruptorAsyncPublisher(publisher,
                "james-context-access-%d",
                WORKER_COUNT,
                QUEUE_SIZE
        )

        when:
        List<Future> futures = submitJobs(QUEUE_SIZE)
        waitForAll(futures)

        then:
        Awaitility.await().atMost(Duration.ofSeconds(1)).until{
            publisher.counter.get() == QUEUE_SIZE
        }

        !hasDroppedJobs(publisherUnderTest)
        jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE)
    }

    def "Each event is processed exactly once."() {
        //given
        Map<Integer, Integer> set = new ConcurrentHashMap<>()
        def QUEUE_SIZE = 16384
        CountingPublisher localPublisher = new CountingPublisher( {

            synchronized (set){
                Integer id = it.content['eventId']
                def orDefault = set.getOrDefault(id, 0)
                set.put(id, orDefault+1)
            }
        })
        def publisher = localPublisher

        publisherUnderTest = new DisruptorAsyncPublisher(publisher,
                "james-context-access-%d",
                WORKER_COUNT,
                QUEUE_SIZE
        )

        when:
        List<Future> futures = submitJobs(QUEUE_SIZE)
        waitForAll(futures)

        then:
        Awaitility.await().atMost(Duration.ofSeconds(2)).until {
            jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE) && publisher.counter.get() == QUEUE_SIZE
        }

        (1..QUEUE_SIZE).every{
            set[it-1] == 1
        }
        set.size() == QUEUE_SIZE
        set.values().every {
            it == 1
        }
        !hasDroppedJobs(publisherUnderTest)
        jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE)

    }

    def "Handles shutdown during processing"() {
        def QUEUE_SIZE = 16384
        def submittedJobs = QUEUE_SIZE
        def publisher = immidiatePublisher
        given:

        publisherUnderTest = new DisruptorAsyncPublisher(publisher,
                "james-context-access-%d",
                WORKER_COUNT,
                QUEUE_SIZE
        )

        when:
        List<Future> futures = submitJobs(submittedJobs)

        then:
        publisherUnderTest.close()
        Awaitility.await().atMost(Duration.ofSeconds(1)).until{
            publisherUnderTest.getJobQueueSize() == 0
        }

        !hasDroppedJobs(publisherUnderTest)

        jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE)

        def processedJobs = publisher.counter.get()
        processedJobs > 0
    }


    def "All submitted jobs are allowed to finish after shutdown"() {
        def QUEUE_SIZE = 1024
        def submittedJobs = QUEUE_SIZE
        def publisher = new CountingPublisher( { it-> sleep(1)})
        given:

        publisherUnderTest = new DisruptorAsyncPublisher(publisher,
                "james-context-access-%d",
                WORKER_COUNT,
                QUEUE_SIZE
        )

        when:
        List<Future> futures = submitJobs(submittedJobs)

        // wait for all events to be published
        waitForAll(futures)

        //still processing some jobs
        boolean hasProcessingJobsBeforeClose = publisher.counter.get() < submittedJobs
        //finish started jobs
        publisherUnderTest.close()

        then:
        hasProcessingJobsBeforeClose

        //processing has finished
        !hasDroppedJobs(publisherUnderTest)

        jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE)
        def processedJobs = publisher.counter.get()
        processedJobs > 0
        processedJobs == submittedJobs
    }


    def "drops jobs when capacity is not enough"(){
        def QUEUE_SIZE = 32

        def submittedJobs = QUEUE_SIZE * 10

        def publisher = sleepingPublisher
        given:

        publisherUnderTest = new DisruptorAsyncPublisher(publisher,
                "james-context-access-%d",
                WORKER_COUNT,
                QUEUE_SIZE
        )

        when:
        List<Future> futures = submitJobs(submittedJobs)
        waitForAll(futures)

        then:
        //processing has finished
        Awaitility.await().atMost(Duration.ofSeconds(1)).until{
            publisherUnderTest.getJobQueueSize() == 0
        }
        hasDroppedJobs(publisherUnderTest)

        jobQueueIsEmpty(publisherUnderTest, QUEUE_SIZE)
        def processedJobs = publisher.counter.get()
        processedJobs > 0
        submittedJobs == processedJobs + publisherUnderTest.getDroppedJobsCount()

    }

    protected boolean jobQueueIsEmpty(QueueBacked publisherUnderTest, int queueSize) {
        publisherUnderTest.getJobQueueRemainingCapacity() == queueSize && publisherUnderTest.getJobQueueSize() == 0
    }

    protected boolean hasDroppedJobs(QueueBacked publisherUnderTest) {
        publisherUnderTest.getDroppedJobsCount() > 0
    }

    protected waitForAll(List<Future> futures) {
        futures.forEach {
            it.get()
        }
    }

    def submitJobs(int submittedJobs){
        List<Future> futures = []
        submittedJobs.times {
            futures.add(eventCreator.submit(new Runnable() {
                @Override
                void run() {
                    def map = new HashMap<String, Object>()
                    map['eventId'] = it
                    publisherUnderTest.publish(new Event(map))
                }
            }))
        }
        return futures
    }
}
