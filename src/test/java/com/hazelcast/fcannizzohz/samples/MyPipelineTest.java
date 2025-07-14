package com.hazelcast.fcannizzohz.samples;

import com.hazelcast.collection.IList;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyPipelineTest extends JetTestSupport {

    @Test
    public void testSimplePipeline() {
        Config config = new Config();
        config.setJetConfig(new JetConfig().setEnabled(true));

        HazelcastInstance instance = createHazelcastInstance(config);
        JetService jet = instance.getJet();

        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(1, 2, 3))
         .writeTo(Sinks.list("out"));

        jet.newJob(p).join();

        IList<Integer> result = instance.getList("out");
        assertEquals(3, result.size());
    }

}