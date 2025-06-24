# Testing Hazelcast-powered Applications

`Version 5.5`

This guide shows you how to write unit/integration/component, in-JVM tests for Hazelcast clusters and
clients using the `com.hazelcast.test.HazelcastTestSupport` base class for JUnit 4 tests, and the
`com.hazelcast.test.TestHazelcastInstanceFactory` for JUnit 5 (Jupiter)–style tests.
To create client instances, use `com.hazelcast.test.client.TestHazelcastFactory`; this class extends
`com.hazelcast.test.TestHazelcastInstanceFactory` hence it can be used to manage both client and server
instances.

It covers creating mock clusters, injecting custom configurations, isolating tests, and cleaning up resources,
with code examples and best practices.

## Dependency

Add the tests classifier to your Hazelcast dependency to pull in the testing support classes:

```xml
<dependency>
  <groupId>com.hazelcast</groupId>
  <artifactId>hazelcast</artifactId>
  <version>5.5.0</version>
  <classifier>tests</classifier>
</dependency>
```

## JUnit 4: `HazelcastTestSupport`

### Extend the Support Class

Have your test class extend `com.hazelcast.test.HazelcastTestSupport`.
This base class bundles convenient factory methods and assertions.

```java
@RunWith(HazelcastParallelClassRunner.class)
public class MyClusterTest extends HazelcastTestSupport {
// ...
}
```

### Creating Members with “Mock” Network

Use the built-in factory to spin up in-process Hazelcast members that communicate via a mock network.
No real sockets are opened.

```java
public class MyClusterTest extends HazelcastTestSupport {

    @Test
    public void given_2nodeCluster_when_operationExecuted_then_clusterIsFormed() {
        // given
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        HazelcastInstance member1 = factory.newHazelcastInstance();
        HazelcastInstance member2 = factory.newHazelcastInstance();
        // when
        // ... perform operations on member1 or member2 ...
        // then
        assertClusterSizeEventually(2, member1); // provided by HazelcastTestSupport
    }
}
```

You can also pass a custom Config:

```java
Config config = new Config();
config.setProperty("hazelcast.some.property", "value");
TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
HazelcastInstance[] members = factory.newInstances(config, 2);
```

By default the "mock" network is used, hence the instances spin up much faster because the network stack is skipped.
The same tests can be executed with the full network stack by setting the system property `-Dhazelcast.test.use.network=true`.

### Creating Clients

The `TestHazelcastFactory` (an extension of `TestHazelcastinstanceFactory`) can produce client instances that automatically
discover and connect to your mock cluster:

```java
TestHazelcastFactory factory = createHazelcastFactory(2);
HazelcastInstance[] members = factory.newInstances(2);
HazelcastInstance client = factory.newHazelcastClient();
// ... use client.getMap(...), etc. ...
```

### Using Random Cluster Names

To avoid cross-test interference when tests run in parallel, isolate each cluster by assigning it a unique name:

```java
@Test
public void isolatedClustersDontInterfere() {
   String clusterName = randomName();
   Config config = new Config().setClusterName(clusterName);

   TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
   HazelcastInstance[] members = factory.newInstances(config, 2);

    ClientConfig clientConfig = new ClientConfig().setClusterName(clusterName);
    HazelcastInstance client = factory.newHazelcastClient(clientConfig);
    // ...
}
```

### Cleaning Up Resources

After each test, created resources should be cleaned:

```java
@After
public void tearDown() {
    factory.shutdownAll();
}
```

### Class Runners

`HazelcastTestSupport` comes with two JUnit runners:

`HazelcastSerialClassRunner` – runs tests in series.

`HazelcastParallelClassRunner` – runs tests in parallel threads.

Both runners disable phone-home, shorten join timeouts, and print thread-dumps on failures by default.

### Repetitive Test Execution

Use the `@Repeat(n)` annotation to repeat flaky tests:

```java
@RunWith(HazelcastSerialClassRunner.class)
public class FlakyTest extends HazelcastTestSupport {

    @Repeat(5)
    @Test
    public void testFlakyBehavior() {
        // ...
    }
}
```

This can help surface intermittent failures.

## JUnit 5 (Jupiter): `TestHazelcastInstanceFactory`

In JUnit 5 you directly instantiate and use TestHazelcastInstanceFactory:

```java
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyJupiterClusterTest {

    private static TestHazelcastFactory factory;
    private static HazelcastInstance member1;
    private static HazelcastInstance member2;
    private static HazelcastInstance client;

    @BeforeAll
    static void setupCluster() {
        factory = new TestHazelcastFactory(2);
        member1 = factory.newHazelcastInstance();
        member2 = factory.newHazelcastInstance();
        client = factory.newHazelcastClient();
    }

    @AfterAll
    static void tearDownCluster() {
        factory.shutdownAll();
    }

    @Test
    void testClusterFormed() {
        assertEquals(2, member1.getCluster().getMembers().size());
        assertTrue(client.getCluster().getMembers().contains(member2.getCluster().getLocalMember()));
    }
}

```

You can also pass a Config or ClientConfig to the factory:

```java
Config config = new Config().setClusterName("jupiter-" + UUID.randomUUID());
TestHazelcastInstanceFactory factory = new TestHazelcastInstanceFactory(2);
member1 = factory.newHazelcastInstance(config);
member2 = factory.newHazelcastInstance(config);
```

## Assertion Methods

`HazelcastTestSupport` offers a rich set of static assertion methods to validate both cluster state and
asynchronous behavior. Below are the most commonly used ones.

### Static imports

For brevity, statically import the methods you need:

```java
import static com.hazelcast.test.HazelcastTestSupport.assertClusterSize;
import static com.hazelcast.test.HazelcastTestSupport.assertClusterSizeEventually;
import static com.hazelcast.test.HazelcastTestSupport.assertTrueEventually;
import static com.hazelcast.test.HazelcastTestSupport.assertOpenEventually;
```

### Cluster topology assertions

- `assertClusterSize(int expected, HazelcastInstance instance)`
  Immediately checks that the given instance sees exactly expected members in its cluster.
- `assertClusterSizeEventually(int expected, HazelcastInstance instance)`
  Polls until the cluster reaches the expected size (or fails after a default timeout).

### Asynchronous condition assertions

- `assertTrueEventually(AssertTask task)`
  Repeatedly invokes `task.run()` until it completes without throwing
  an exception, or a timeout is reached. Use this whenever you need to wait for an async condition to become true.
   ```java
   // wait up to the default timeout for the map to contain 3 entries
   assertTrueEventually(() -> assertEquals(3, map.size()));
   ```
- Using `assertTrueEventually` for `"false"` conditions
  You can invert any check by throwing on success. For example, to wait until an entry has been removed:
   ```java
   assertTrueEventually(() -> assertFalse(map.containsKey(1)));
   ```

Waiting on futures and latches

- `assertOpenEventually(CountDownLatch latch)`
  Blocks until `latch.await()` returns, or the default timeout elapses.
- `assertOpenEventually(ICompletableFuture<?> future)`
  Waits for the given Hazelcast future to complete.

Overloads accepting a timeout parameter let you customize wait durations:

```java
assertOpenEventually(latch, 30);          // seconds
assertTrueEventually(task, 60);          // seconds
```

These methods—especially `assertTrueEventually` and `assertOpenEventually` are indispensable for testing asynchronous behavior in
Hazelcast-powered applications. For the full API, see the `HazelcastTestSupport` source in the hazelcast-tests classifier.

## Best Practices

- Isolate tests with random cluster names to avoid port/cross-cluster conflicts.
- Clean up all instances in a teardown (`shutdownAll()).
- Favor mock networking (fast, no real sockets) for unit tests; use real instances only for true integration tests.
- Keep clusters small—3 nodes is usually enough to validate replication and partitioning logic.
- Assert cluster topology early (e.g. `assertClusterSizeEventually`) before exercising distributed logic.
- Use `@Repeat` or retries sparingly to root-cause flakiness rather than masking real issues.

This approach yields reliable, fast, and isolated testing of Hazelcast-powered applications across both
JUnit 4 and JUnit 5.