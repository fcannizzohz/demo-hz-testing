package com.hazelcast.fcannizzohz;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.After;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Why This is a Component Test:
 * <ul>
 *  <li>Involves two cooperating classes (CustomerService, CustomerMapStore)</li>
 *  <li>Uses a real external dependency (H2) and real Hazelcast instance (not mocked)</li>
 *  <li>Does not require the rest of the system (e.g., REST layer, messaging, etc.)</li>
 *  <li>Tests distributed behaviour (eviction, reload) in a focused subsystem</li>
 * </ul>
 * </p>
 */
public class CustomerServiceComponentTest
        extends HazelcastTestSupport {
    private HazelcastInstance hz;

    @Test
    public void customerServiceWithMapStoreInteractions()
            throws Exception {
        // Set up H2 in-memory DB
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        conn.createStatement().execute("CREATE TABLE customers (id VARCHAR PRIMARY KEY, name VARCHAR)");

        // Set up Hazelcast config with MapStore
        Config config = new Config();
        config.getMapConfig("customers").getMapStoreConfig().setEnabled(true).setImplementation(new CustomerMapStore(conn));

        hz = createHazelcastInstance(config);
        CustomerService service = new HzCustomerService(hz);

        // Act
        service.save(new Customer("c1", "Alice")); // should go into both IMap and DB
        Customer fromMap = service.findCustomer("c1");      // should be from IMap

        // Clear IMap to test reloading from DB
        hz.getMap("customers").evictAll();
        Customer fromStore = service.findCustomer("c1");    // should be reloaded from H2

        // Assert
        assertEquals("Alice", fromMap.name());
        assertEquals("Alice", fromStore.name());
    }

    @After
    public void tearDown() {
        hz.shutdown();
    }
}
