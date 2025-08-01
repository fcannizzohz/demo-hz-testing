package com.hazelcast.fcannizzohz.samples.junit5;

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.hazelcast.test.HazelcastTestSupport.assertClusterSize;
import static com.hazelcast.test.HazelcastTestSupport.assertClusterSizeEventually;
import static com.hazelcast.test.HazelcastTestSupport.assertTrueEventually;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyClusterTest {

    private static HazelcastInstance member1;
    private static HazelcastInstance member2;
    private static HazelcastInstance client;

    @BeforeAll
    static void setupCluster() {
        TestHazelcastFactory factory = new TestHazelcastFactory(2);
        member1 = factory.newHazelcastInstance();
        member2 = factory.newHazelcastInstance();
        client = factory.newHazelcastClient();
    }

    @AfterAll
    static void tearDownCluster() {
        client.shutdown();
        member1.shutdown();
        member2.shutdown();
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);   // artificial delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void testClusterSizeEventually() {
        assertClusterSizeEventually(2, member1);
        assertClusterSizeEventually(2, member2);
    }

    @Test
    void testClusterSize() {
        assertClusterSize(2, member1);
        assertClusterSize(2, member2);
    }

    @Test
    void testClusterFormed() {
        assertEquals(2, member1.getCluster().getMembers().size());
        assertTrue(client.getCluster().getMembers().contains(member2.getCluster().getLocalMember()));
    }

    @Test
    void testAsyncTasks()
            throws Exception {
        Runnable task = () -> {
            IMap<Integer, String> map = member1.getMap("map");
            // your async logic here
            map.put(1, "one");
            sleep(50);
            map.put(2, "two");
            sleep(100);
        };

        Thread t = new Thread(task);
        t.start();
        t.join();
        assertTrueEventually(() -> assertEquals(2, member2.getMap("map").size()));
        assertTrueEventually(() -> assertFalse(member2.getMap("map").containsKey("3")));
    }
}

