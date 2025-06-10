package com.hazelcast.fcannizzohz;

import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;

public class OrderEnrichmentPipeline {
    public static Pipeline build(BatchSource<Order> source) {
        Pipeline p = Pipeline.create();

        p.readFrom(source).mapUsingIMap("customers", Order::customerId, getEnrichment()).writeTo(Sinks.list("enriched-orders"));

        return p;
    }

    private static BiFunctionEx<Order, Customer, EnrichedOrder> getEnrichment() {
        return (order, customer) -> {
            if (customer == null) {
                return null;
            }
            return new EnrichedOrder(order.id(), customer.name(), order.product());
        };
    }
}
