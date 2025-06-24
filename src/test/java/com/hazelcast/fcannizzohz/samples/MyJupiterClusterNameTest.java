package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.hazelcast.test.HazelcastTestSupport.assertEqualsEventually;

class MyJupiterClusterNameTest {

    private static TestHazelcastInstanceFactory factory;
    private static HazelcastInstance member1;
    private static HazelcastInstance member2;
    private static HazelcastInstance client;
    private static String clusterName;

    @BeforeAll
    static void setupCluster() {
        clusterName = HazelcastTestSupport.randomName();
        Config config = new Config().setClusterName(clusterName);
        factory = new TestHazelcastInstanceFactory(2);
        member1 = factory.newHazelcastInstance(config);
        member2 = factory.newHazelcastInstance(config);
    }

    @AfterAll
    static void tearDownCluster() {
        factory.shutdownAll();
    }

    @Test
    void testClusterName() {
        assertEqualsEventually(() -> member1.getConfig().getClusterName(), clusterName);
        assertEqualsEventually(() -> member2.getConfig().getClusterName(), clusterName);
    }
}

