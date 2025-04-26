package org.memmcol.gridflexbackendservice.config;

import com.hazelcast.config.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastNearCacheConfig {

	@Value("${hazelcast-enabled}")
	private boolean enabled;

	@Value("${hazelcast-directory}")
	private String director;

	@Value("${hazelcast.time-to-live-seconds}")
	private int userCacheTimeToLiveSeconds;

	@Value("${hazelcast-store-initial-delay-seconds}")
	private int storeInitialDelaySeconds;

	@Value("${hazelcast-store-interval-seconds}")
	private int setStoreIntervalSeconds;

	@Value("${hazelcast-max-idle-seconds}")
	private int maxIdleTime;

	@Value("${hazelcast-invalidate-on-change}")
	private boolean invalidateOnChange;

	@Value("${hazelcast-serialize-key}")
	private boolean serializeKey;

	@Value("${hazelcast-local-entries}")
	private boolean localEntry;

	@Value("${hazelcast.max-size}")
	private int userCacheMaxSize;

	@Bean
	NearCacheConfig nearCacheConfig() {
		EvictionConfig evictionConfig = new EvictionConfig().setMaxSizePolicy(MaxSizePolicy.ENTRY_COUNT)
				.setEvictionPolicy(EvictionPolicy.LRU).setSize(userCacheMaxSize);

		NearCachePreloaderConfig preloaderConfig = new NearCachePreloaderConfig().setEnabled(enabled)
				.setDirectory(director).setStoreInitialDelaySeconds(storeInitialDelaySeconds)
				.setStoreIntervalSeconds(setStoreIntervalSeconds);

		return new NearCacheConfig().setName("auth-Cache").setInMemoryFormat(InMemoryFormat.OBJECT)
				.setSerializeKeys(serializeKey).setInvalidateOnChange(invalidateOnChange)
//				.setTimeToLiveSeconds(userCacheTimeToLiveSeconds)
				.setMaxIdleSeconds(maxIdleTime)
				.setEvictionConfig(evictionConfig).setCacheLocalEntries(localEntry)
				.setLocalUpdatePolicy(NearCacheConfig.LocalUpdatePolicy.INVALIDATE).setPreloaderConfig(preloaderConfig);
	}
}

