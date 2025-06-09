package com.hazelcast.fcannizzohz;

import java.io.Serializable;

public record Customer(String id, String name) implements Serializable {
}
