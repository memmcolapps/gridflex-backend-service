package org.memmcol.gridflexbackendservice.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {
	@Value("${hazelcast.cluster-name}")
	private String clusterName;

	@Value("${hazelcast.network.port}")
	private int networkPort;

	@Value("${hazelcast.instance-name}")
	private String instanceName;

	@Value("${hazelcast.network.join.multicast.enabled}")
	private boolean multicastEnabled;

	@Autowired
	private NearCacheConfig nearCacheConfig;

	@Bean
	public HazelcastInstance hazelcastInstance() {
		Config config = new Config();
		config.setInstanceName(instanceName); // Or use a property for this as well
		config.setClusterName(clusterName);
		config.getNetworkConfig().setPort(networkPort);
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(multicastEnabled);
//        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true)
//        .setMembersArrays.asList("192.168.1.100", "192.168.1.101"))

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("auditCache")
//				.setTimeToLiveSeconds(86400)
				.setMaxIdleSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("userCache")
//				.setTimeToLiveSeconds(86400)
				.setMaxIdleSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("customerCache")
//				.setTimeToLiveSeconds(86400)
				.setMaxIdleSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("nodeCache")
//				.setTimeToLiveSeconds(86400)
				.setMaxIdleSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("meterCache")
//				.setTimeToLiveSeconds(86400)
				.setMaxIdleSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure OTP Cache (Expires in 60 Seconds)
		config.addMapConfig(new MapConfig("otpCache")
				.setTimeToLiveSeconds(60)
				.setMaxIdleSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("verifiedUsers")
				.setTimeToLiveSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Band Cache
		config.addMapConfig(new MapConfig("bandCache")
				.setTimeToLiveSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));
		// Set up Near Cache
//		config.getMapConfig("near-cache").setNearCacheConfig(nearCacheConfig);

		// Configure Band Cache
		config.addMapConfig(new MapConfig("debtCache")
				.setTimeToLiveSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("debitCreditCache")
				.setTimeToLiveSeconds(18000)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));
		// Set up Near Cache
		config.getMapConfig("near-cache").setNearCacheConfig(nearCacheConfig);

		return Hazelcast.newHazelcastInstance(config);
	}
}

