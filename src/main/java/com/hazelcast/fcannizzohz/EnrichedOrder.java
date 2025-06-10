package com.hazelcast.fcannizzohz;

import java.io.Serializable;

public record EnrichedOrder(String orderId, String customerName, String product)
        implements Serializable {
}
