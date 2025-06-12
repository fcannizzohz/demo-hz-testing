package com.hazelcast.fcannizzohz;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapStore;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.After;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Why This is a Component Test:
 * <ul>
 *  <li>Involves the component under test and its private dependencies (CustomerService, CustomerMapStore via Hazelcast)</li>
 *  <li>Uses a real external dependency (H2) and real Hazelcast instance (not mocked)</li>
 *  <li>Does not require the rest of the system (e.g., REST layer, messaging, etc.)</li>
 *  <li>Tests distributed behaviour (eviction, reload) in a focused subsystem</li>
 * </ul>
 * </p>
 */
public class CustomerServiceComponentTest
        extends HazelcastTestSupport {

    private HazelcastInstance hz;

    private static final String CREATE_TABLE_SQL = "CREATE TABLE customers (id VARCHAR PRIMARY KEY, name VARCHAR)";

    @Test
    public void customerServiceWithMapStoreInteractions()
            throws Exception {
        // Set up H2 in-memory DB
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        conn.createStatement().execute(CREATE_TABLE_SQL);

        // Set up Hazelcast config with MapStore
        Config config = new Config();
        config.setClusterName(randomName());
        // inject custom Map Store
        config.getMapConfig("customers").getMapStoreConfig().setEnabled(true).setImplementation(new SQLCustomerMapStore(conn));

        // Create hz instance
        hz = createHazelcastInstance(config);

        CustomerService service = new HzCustomerService(hz);

        // Act - save new customer
        service.save(new Customer("c1", "Alice")); // should go into both IMap and DB
        Customer fromMap = service.findCustomer("c1");      // should be from IMap

        // Clear IMap to test reloading from DB
        hz.getMap("customers").evictAll();
        Customer fromStore = service.findCustomer("c1");    // should be reloaded from H2

        // Assert
        assertEquals("Alice", fromMap.name());
        assertEquals("Alice", fromStore.name());
    }

    @Test
    public void customerServiceWithMapStoreFailure()
            throws Exception {

        // mock MapStore to inject failure
        MapStore<String, Customer> failingMapStore = (MapStore<String, Customer>) mock(MapStore.class);
        when(failingMapStore.load("c1")).thenThrow(new RuntimeException("Injected failure"));

        // Set up Hazelcast config with MapStore
        Config config = new Config();
        config.setClusterName(randomName());
        // inject custom Map Store
        config.getMapConfig("customers")
              .getMapStoreConfig()
              .setEnabled(true)
              .setImplementation(failingMapStore);

        // Create hz instance
        hz = createHazelcastInstance(config);

        CustomerService service = new HzCustomerService(hz);
        ServiceException ex = assertThrows(ServiceException.class, () -> {
            // Act - expected injected failure from MapStore
            service.findCustomer("c1");
        });

        assertEquals("Find customer failed", ex.getMessage());
        assertEquals("Injected failure", ex.getCause().getMessage());
    }

    @After
    public void tearDown() {
        if (hz != null) {
            hz.shutdown();
        }
    }
}
