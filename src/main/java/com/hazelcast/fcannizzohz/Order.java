package com.hazelcast.fcannizzohz;

import java.io.Serializable;

public record Order(String id, String customerId, String product)
        implements Serializable {
}
