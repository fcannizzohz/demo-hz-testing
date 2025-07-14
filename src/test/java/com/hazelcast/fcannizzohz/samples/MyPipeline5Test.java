package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.collection.IList;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyPipeline5Test {

    @Test
    void testSimplePipeline() {
        TestHazelcastFactory factory = new TestHazelcastFactory();

        Config config = new Config();
        config.setJetConfig(new JetConfig().setEnabled(true));
        HazelcastInstance instance = factory.newHazelcastInstance(config);

        JetService jet = instance.getJet();

        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(1, 2, 3)).writeTo(Sinks.list("out"));

        jet.newJob(p).join();

        IList<Integer> result = instance.getList("out");
        assertEquals(3, result.size());

        factory.shutdownAll();
    }

    @Test
    void testEnrichmentPipeline() {
        TestHazelcastFactory factory = new TestHazelcastFactory();

        Config config = new Config();
        config.setJetConfig(new JetConfig().setEnabled(true));
        HazelcastInstance instance = factory.newHazelcastInstance(config);
        JetService jet = instance.getJet();

        // Set up customer map entries
        IMap<String, Customer> customers = instance.getMap("customers");
        customers.put("c1", new Customer("c1", "Alice"));
        customers.put("c2", new Customer("c2", "Bob"));

        // Build and run the pipeline
        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items("c1", "c2"))
         .mapUsingIMap("customers", id -> id, (id, customer) -> ((Customer) customer).name()).writeTo(Sinks.list("enriched"));
        jet.newJob(p).join();

        // Validate the result
        IList<String> result = instance.getList("enriched");
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0));
        assertEquals("Bob", result.get(1));

        factory.shutdownAll();
    }

    record Customer(String id, String name) {
    }

}
