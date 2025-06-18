package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.Repeat;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastSerialClassRunner.class)
public class FlakyTest extends HazelcastTestSupport {

    private static final AtomicInteger run = new AtomicInteger();
    private static final AtomicInteger counter = new AtomicInteger();
    private HazelcastInstance member1;

    @Before
    public void setUp() {
        member1 = createHazelcastInstance();
    }

    @After
    public void tearDown() {
        if(member1 != null) {
            member1.shutdown();
        }
    }

    @Repeat(5)
    @Test
    public void testFlakyBehavior() {

        IMap<String, String> map = member1.getMap("flakyMap");
        // simulate intermittent behavior: succeed only half the time
        if (System.currentTimeMillis() % 2 == 0) {
            map.put("key", "value");
            counter.incrementAndGet();
        }

        System.out.println("> " + run.incrementAndGet() + " " + counter.get());
        // then: assert that the map put worked only half of the time
        assertTrue("Counter should have been less than 5: value=" + counter.get(), counter.get() < 5);
    }
}
