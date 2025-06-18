package com.hazelcast.fcannizzohz.junit5;

import com.hazelcast.collection.IList;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.fcannizzohz.Customer;
import com.hazelcast.fcannizzohz.EnrichedOrder;
import com.hazelcast.fcannizzohz.Order;
import com.hazelcast.fcannizzohz.OrderEnrichmentPipeline;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.test.AssertionCompletedException;
import com.hazelcast.jet.pipeline.test.Assertions;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.map.IMap;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static com.hazelcast.jet.core.test.JetAssert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderEnrichmentPipelineTest {

    private TestHazelcastInstanceFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new TestHazelcastInstanceFactory();
    }

    @AfterEach
    public void tearDown() {
        if (factory != null) {
            factory.shutdownAll();
        }
    }

    @Test
    public void testJetOrderEnrichmentWithHazelcastState() {
        HazelcastInstance instance = factory.newHazelcastInstance();
        JetService jet = instance.getJet();

        IMap<String, Customer> customerMap = instance.getMap("customers");
        customerMap.put("c1", new Customer("c1", "Alice"));
        customerMap.put("c2", new Customer("c2", "Bob"));

        BatchSource<Order> source = TestSources.items(new Order("o1", "c1", "Laptop"), new Order("o2", "c2", "Phone"));
        Job job = jet.newJob(OrderEnrichmentPipeline.build(source));
        job.join(); // wait for completion

        IList<EnrichedOrder> result = instance.getList("enriched-orders");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.customerName().equals("Alice")));
        assertTrue(result.stream().anyMatch(o -> o.customerName().equals("Bob")));
    }

    @Test
    public void streamingEnrichmentWithInlineAssertion() {
        HazelcastInstance instance = factory.newHazelcastInstance();
        IMap<String, Customer> customerMap = instance.getMap("customers");
        customerMap.put("c1", new Customer("c1", "Alice"));
        customerMap.put("c2", new Customer("c2", "Bob"));

        // Streaming source
        StreamSource<Order> source = TestSources.itemStream(50, (ts, seq) -> {
            String customerId = seq % 2 == 0 ? "c1" : "c2";
            return new Order("o" + seq, customerId, "Product" + seq);
        });

        Pipeline pipeline = Pipeline.create();
        OrderEnrichmentPipeline.enrich(pipeline, source).apply(Assertions.assertCollectedEventually(5,
                list -> assertTrue(list.size() >= 10, "Expected at least 10 enriched orders")));

        Job job = instance.getJet().newJob(pipeline);

        // The assertion will stop the job automatically via AssertionCompletedException by assertCollectedEventually
        try {
            job.join();
            fail("Expected job to terminate with AssertionCompletedException");
        } catch (CompletionException e) {
            if (!causedBy(e, AssertionCompletedException.class)) {
                throw e; // rethrow if it wasn't the expected assertion exit
            }
        }
    }

    private boolean causedBy(Throwable t, Class<? extends Throwable> target) {
        while (t != null) {
            if (target.isInstance(t)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
