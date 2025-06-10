package com.hazelcast.fcannizzohz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class HzCustomerService
        implements CustomerService {
    private final HazelcastInstance instance;

    public HzCustomerService(HazelcastInstance instance) {
        this.instance = instance;
    }

    @Override
    public Customer findCustomer(String id) {
        return customerMap().get(id);
    }

    public void save(Customer customer) {
        customerMap().put(customer.id(), customer);
    }

    private IMap<String, Customer> customerMap() {
        return instance.getMap("customers");
    }

}
