package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.Repeat;

import java.util.concurrent.atomic.AtomicInteger;


import static org.junit.Assert.assertNotNull;
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

        IMap<String, Integer> map = member1.getMap("map");

        // simulate intermittent behavior: succeed only half the time
        if (System.currentTimeMillis() % 2 == 0) {
            map.put("key", counter.incrementAndGet());
        }

        System.out.println("> run=" + run.incrementAndGet() + ", value=" + map.get("key"));

        // then: assert that the map put worked only half of the time
        Integer v = map.get("key");
        // since this test is repeated 5 times, it'll always pass in this example but
        // in reality, at some point the real flaky test will fail
        assertTrue("Map value should be less than 5: value=" + v, v < 5);
    }
}
