package com.hazelcast.fcannizzohz;

public interface CustomerService {
    Customer findCustomer(String number);

    void save(Customer customer);
}
