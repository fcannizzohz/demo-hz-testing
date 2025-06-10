package com.hazelcast.fcannizzohz;

import com.hazelcast.collection.IList;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.map.IMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderEnrichmentPipelineTest
        extends JetTestSupport {
    @Test
    public void testJetOrderEnrichmentWithHazelcastState() {
        // 🔹 JetTestSupport sets up clean Jet instances
        HazelcastInstance instance = createHazelcastInstance();

        JetService jet = instance.getJet();

        // 🔹 Populate customer state
        IMap<String, Customer> customerMap = instance.getMap("customers");
        customerMap.put("c1", new Customer("c1", "Alice"));
        customerMap.put("c2", new Customer("c2", "Bob"));

        // 🔹 Run the job
        BatchSource<Order> source = TestSources.items(
                new Order("o1", "c1", "Laptop"),
                new Order("o2", "c2", "Phone")
        );
        Job job = jet.newJob(OrderEnrichmentPipeline.build(source));
        job.join(); // wait for completion

        // 🔹 Validate output
        IList<EnrichedOrder> result = instance.getList("enriched-orders");
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.customerName().equals("Alice")));
        assertTrue(result.stream().anyMatch(o -> o.customerName().equals("Bob")));
    }
}
