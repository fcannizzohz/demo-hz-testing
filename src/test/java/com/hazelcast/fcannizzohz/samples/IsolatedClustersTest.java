package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.test.HazelcastParallelClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.hazelcast.test.HazelcastTestSupport.assertClusterSizeEventually;
import static com.hazelcast.test.HazelcastTestSupport.assertEqualsEventually;
import static com.hazelcast.test.HazelcastTestSupport.randomName;

@RunWith(HazelcastParallelClassRunner.class)
public class IsolatedClustersTest {

    @Test
    public void isolatedClustersDontInterfere_clusterA() {
        String clusterName = randomName();
        Config serverConfig = new Config().setClusterName(clusterName);
        ClientConfig clientConfig = new ClientConfig().setClusterName(clusterName);

        TestHazelcastFactory factory = new TestHazelcastFactory(2);
        HazelcastInstance[] members = factory.newInstances(serverConfig, 2);
        HazelcastInstance client = factory.newHazelcastClient(clientConfig);

        IMap<String, String> map = client.getMap("isolatedMap");
        map.put("key", "valueA");

        // verify cluster formed and data is available
        assertClusterSizeEventually(2, members[0]);
        assertEqualsEventually(() -> map.get("key"), "valueA");

        factory.shutdownAll();
    }

    @Test
    public void isolatedClustersDontInterfere_clusterB() {
        String clusterName = randomName();
        Config serverConfig = new Config().setClusterName(clusterName);
        ClientConfig clientConfig = new ClientConfig().setClusterName(clusterName);

        TestHazelcastFactory factory = new TestHazelcastFactory(2);
        HazelcastInstance[] members = factory.newInstances(serverConfig, 2);
        HazelcastInstance client = factory.newHazelcastClient(clientConfig);

        IMap<String, String> map = client.getMap("isolatedMap");
        map.put("key", "valueB");

        // verify cluster formed and data is available
        assertClusterSizeEventually(2, members[0]);
        assertEqualsEventually(() -> map.get("key"), "valueB");

        factory.shutdownAll();

    }
}
