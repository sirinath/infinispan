package org.infinispan.functional.decorators;

import org.infinispan.AdvancedCache;
import org.infinispan.CacheCollection;
import org.infinispan.CacheSet;
import org.infinispan.atomic.Delta;
import org.infinispan.batch.BatchContainer;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.expiration.ExpirationManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.filter.KeyFilter;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.iteration.EntryIterable;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.security.AuthorizationManager;
import org.infinispan.stats.Stats;
import org.infinispan.util.concurrent.locks.LockManager;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class FunctionalAdvancedCache<K, V> implements AdvancedCache<K, V> {

   final ConcurrentMap<K, V> map;
   final AdvancedCache<K, V> cache;

   private FunctionalAdvancedCache(ConcurrentMap<K, V> map, AdvancedCache<K, V> cache) {
      this.map = map;
      this.cache = cache;
   }

   public static <K, V> AdvancedCache<K, V> create(AdvancedCache<K, V> cache) {
      return new FunctionalAdvancedCache<>(FunctionalConcurrentMap.create(cache), cache);
   }

   ////////////////////////////////////////////////////////////////////////////

   @Override
   public V put(K key, V value) {
      return map.put(key, value);
   }

   @Override
   public V get(Object key) {
      return map.get(key);
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return map.putIfAbsent(key, value);
   }

   @Override
   public V replace(K key, V value) {
      return map.replace(key, value);
   }

   @Override
   public V remove(Object key) {
      return map.remove(key);
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return map.replace(key, oldValue, newValue);
   }

   @Override
   public boolean remove(Object key, Object value) {
      return map.remove(key, value);
   }

   ////////////////////////////////////////////////////////////////////////////

   @Override
   public RpcManager getRpcManager() {
      return cache.getRpcManager();
   }

   @Override
   public ComponentRegistry getComponentRegistry() {
      return cache.getComponentRegistry();
   }

   @Override
   public AdvancedCache<K, V> getAdvancedCache() {
      return cache.getAdvancedCache();
   }

   @Override
   public EmbeddedCacheManager getCacheManager() {
      return cache.getCacheManager();
   }

   @Override
   public boolean addInterceptorBefore(CommandInterceptor i, Class<? extends CommandInterceptor> beforeInterceptor) {
      return cache.addInterceptorBefore(i, beforeInterceptor);
   }

   @Override
   public AdvancedCache<K, V> withFlags(Flag... flags) {
      return cache.withFlags(flags);
   }

   ////////////////////////////////////////////////////////////////////////////

   @Override
   public void addInterceptor(CommandInterceptor i, int position) {
      // TODO: Customise this generated block
   }

   @Override
   public boolean addInterceptorAfter(CommandInterceptor i, Class<? extends CommandInterceptor> afterInterceptor) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void removeInterceptor(int position) {
      // TODO: Customise this generated block
   }

   @Override
   public void removeInterceptor(Class<? extends CommandInterceptor> interceptorType) {
      // TODO: Customise this generated block
   }

   @Override
   public List<CommandInterceptor> getInterceptorChain() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public EvictionManager getEvictionManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public ExpirationManager<K, V> getExpirationManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public DistributionManager getDistributionManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public AuthorizationManager getAuthorizationManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean lock(K... keys) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean lock(Collection<? extends K> keys) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void applyDelta(K deltaAwareValueKey, Delta delta, Object... locksToAcquire) {
      // TODO: Customise this generated block
   }

   @Override
   public BatchContainer getBatchContainer() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public InvocationContextContainer getInvocationContextContainer() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public DataContainer<K, V> getDataContainer() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public TransactionManager getTransactionManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public LockManager getLockManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Stats getStats() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public XAResource getXAResource() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public ClassLoader getClassLoader() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public AdvancedCache<K, V> with(ClassLoader classLoader) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public V put(K key, V value, Metadata metadata) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map, Metadata metadata) {
      // TODO: Customise this generated block
   }

   @Override
   public V replace(K key, V value, Metadata metadata) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue, Metadata metadata) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public V putIfAbsent(K key, V value, Metadata metadata) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void putForExternalRead(K key, V value, Metadata metadata) {
      // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putAsync(K key, V value, Metadata metadata) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Map<K, V> getAll(Set<?> keys) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheEntry<K, V> getCacheEntry(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Map<K, CacheEntry<K, V>> getAllCacheEntries(Set<?> keys) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public EntryIterable<K, V> filterEntries(KeyValueFilter<? super K, ? super V> filter) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Map<K, V> getGroup(String groupName) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void removeGroup(String groupName) {
      // TODO: Customise this generated block
   }

   @Override
   public AvailabilityMode getAvailability() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void setAvailability(AvailabilityMode availabilityMode) {
      // TODO: Customise this generated block
   }

   @Override
   public CacheSet<CacheEntry<K, V>> cacheEntrySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void removeExpired(K key, V value, Long lifespan) {
      // TODO: Customise this generated block
   }

   @Override
   public void putForExternalRead(K key, V value) {
      // TODO: Customise this generated block
   }

   @Override
   public void putForExternalRead(K key, V value, long lifespan, TimeUnit unit) {
      // TODO: Customise this generated block
   }

   @Override
   public void putForExternalRead(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      // TODO: Customise this generated block
   }

   @Override
   public void evict(K key) {
      // TODO: Customise this generated block
   }

   @Override
   public Configuration getCacheConfiguration() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public ComponentStatus getStatus() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public int size() {
      return 0;  // TODO: Customise this generated block
   }

   @Override
   public boolean isEmpty() {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean containsKey(Object key) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean containsValue(Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public CacheSet<K> keySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheCollection<V> values() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheSet<Entry<K, V>> entrySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void clear() {
      // TODO: Customise this generated block
   }

   @Override
   public String getName() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public String getVersion() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public V put(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
      // TODO: Customise this generated block
   }

   @Override
   public V replace(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      // TODO: Customise this generated block
   }

   @Override
   public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Void> clearAsync() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> removeAsync(Object key) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> replaceAsync(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public NotifyingFuture<V> getAsync(K key) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean startBatch() {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void endBatch(boolean successful) {
      // TODO: Customise this generated block
   }

   @Override
   public void addListener(Object listener, KeyFilter<? super K> filter) {
      // TODO: Customise this generated block
   }

   @Override
   public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
      // TODO: Customise this generated block
   }

   @Override
   public void start() {
      // TODO: Customise this generated block
   }

   @Override
   public void stop() {
      // TODO: Customise this generated block
   }

   @Override
   public void addListener(Object listener) {
      // TODO: Customise this generated block
   }

   @Override
   public void removeListener(Object listener) {
      // TODO: Customise this generated block
   }

   @Override
   public Set<Object> getListeners() {
      return null;  // TODO: Customise this generated block
   }
}
