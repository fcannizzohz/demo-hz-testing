package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class MyMapListenerTest extends HazelcastTestSupport {

    @Test
    public void updateTriggersListener() {
        // create Hazelcast member
        HazelcastInstance instance = createHazelcastInstance();

        // create mock listener
        EntryUpdatedListener<String, String> mockListener = mock(EntryUpdatedListener.class);

        // register the listener
        IMap<String, String> map = instance.getMap("test-map");
        map.addEntryListener(mockListener, true);

        // insert and update an entry
        map.put("key1", "initial");
        map.put("key1", "updated");

        // verify the listener received the update
        verify(mockListener, timeout(1000).times(1)).entryUpdated(any(EntryEvent.class));

        instance.shutdown();
    }
}
