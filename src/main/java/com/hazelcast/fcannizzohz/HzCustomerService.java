package com.hazelcast.fcannizzohz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class HzCustomerService implements CustomerService {
    private final HazelcastInstance instance;

    public HzCustomerService(HazelcastInstance instance) {
        this.instance = instance;
    }

    @Override
    public Customer findCustomer(String id) {
        IMap<String, Customer> map = this.instance.getMap("customers");
        return map.get(id);
    }
}
