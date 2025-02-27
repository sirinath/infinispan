package org.infinispan.client.hotrod.impl.operations;

import net.jcip.annotations.Immutable;

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.event.ClientListenerNotifier;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HotRodConstants;
import org.infinispan.client.hotrod.impl.query.RemoteQuery;
import org.infinispan.client.hotrod.impl.transport.Transport;
import org.infinispan.client.hotrod.impl.transport.TransportFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for {@link org.infinispan.client.hotrod.impl.operations.HotRodOperation} objects.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Immutable
public class OperationsFactory implements HotRodConstants {

   private static final Flag[] FORCE_RETURN_VALUE = {Flag.FORCE_RETURN_VALUE};

   private final ThreadLocal<List<Flag>> flagsMap = new ThreadLocal<List<Flag>>();

   private final TransportFactory transportFactory;

   private final byte[] cacheNameBytes;

   private final AtomicInteger topologyId;

   private final boolean forceReturnValue;

   private final Codec codec;

   private final ClientListenerNotifier listenerNotifier;

   public OperationsFactory(TransportFactory transportFactory, String cacheName,
                            AtomicInteger topologyId, boolean forceReturnValue, Codec codec,
                            ClientListenerNotifier listenerNotifier) {
      this.transportFactory = transportFactory;
      this.cacheNameBytes = RemoteCacheManager.cacheNameBytes(cacheName);
      this.topologyId = topologyId;
      this.forceReturnValue = forceReturnValue;
      this.codec = codec;
      this.listenerNotifier = listenerNotifier;
   }

   public ClientListenerNotifier getListenerNotifier() {
      return listenerNotifier;
   }

   public byte[] getCacheName() {
      return cacheNameBytes;
   }

   public GetOperation newGetKeyOperation(byte[] key) {
      return new GetOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags());
   }

   public GetAllOperation newGetAllOperation(Set<byte[]> keys) {
      return new GetAllOperation(
            codec, transportFactory, keys, cacheNameBytes, topologyId, flags());
   }

   public RemoveOperation newRemoveOperation(byte[] key) {
      return new RemoveOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags());
   }

   public RemoveIfUnmodifiedOperation newRemoveIfUnmodifiedOperation(byte[] key, long version) {
      return new RemoveIfUnmodifiedOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags(), version);
   }

   public ReplaceIfUnmodifiedOperation newReplaceIfUnmodifiedOperation(byte[] key,
            byte[] value, long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit, long version) {
      return new ReplaceIfUnmodifiedOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags(),
            value, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit, version);
   }

   public GetWithVersionOperation newGetWithVersionOperation(byte[] key) {
      return new GetWithVersionOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags());
   }

   public GetWithMetadataOperation newGetWithMetadataOperation(byte[] key) {
      return new GetWithMetadataOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags());
   }

   public StatsOperation newStatsOperation() {
      return new StatsOperation(
            codec, transportFactory, cacheNameBytes, topologyId, flags());
   }

   public PutOperation newPutKeyValueOperation(byte[] key, byte[] value,
          long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
      return new PutOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags(),
            value, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
   }

   public PutAllOperation newPutAllOperation(Map<byte[], byte[]> map,
          long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
      return new PutAllOperation(
            codec, transportFactory, map, cacheNameBytes, topologyId, flags(),
            lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
   }

   public PutIfAbsentOperation newPutIfAbsentOperation(byte[] key, byte[] value,
             long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return new PutIfAbsentOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags(),
            value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
   }

   public ReplaceOperation newReplaceOperation(byte[] key, byte[] values,
           long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
      return new ReplaceOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags(),
            values, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
   }

   public ContainsKeyOperation newContainsKeyOperation(byte[] key) {
      return new ContainsKeyOperation(
            codec, transportFactory, key, cacheNameBytes, topologyId, flags());
   }

   public ClearOperation newClearOperation() {
      return new ClearOperation(
            codec, transportFactory, cacheNameBytes, topologyId, flags());
   }

   public BulkGetOperation newBulkGetOperation(int size) {
      return new BulkGetOperation(
            codec, transportFactory, cacheNameBytes, topologyId, flags(), size);
   }

   public BulkGetKeysOperation newBulkGetKeysOperation(int scope) {
      return new BulkGetKeysOperation(
         codec, transportFactory, cacheNameBytes, topologyId, flags(), scope);
   }

   public AddClientListenerOperation newAddClientListenerOperation(Object listener) {
      return new AddClientListenerOperation(codec, transportFactory,
            cacheNameBytes, topologyId, flags(), listenerNotifier,
            listener, null, null);
   }

   public AddClientListenerOperation newAddClientListenerOperation(
         Object listener, byte[][] filterFactoryParams, byte[][] converterFactoryParams) {
      return new AddClientListenerOperation(codec, transportFactory,
            cacheNameBytes, topologyId, flags(), listenerNotifier,
            listener, filterFactoryParams, converterFactoryParams);
   }

   public RemoveClientListenerOperation newRemoveClientListenerOperation(Object listener) {
      return new RemoveClientListenerOperation(codec, transportFactory,
            cacheNameBytes, topologyId, flags(), listenerNotifier, listener);
   }

   /**
    * Construct a ping request directed to a particular node.
    *
    * @param transport represents the node to which the operation is directed
    * @return a ping operation for a particular node
    */
   public PingOperation newPingOperation(Transport transport) {
      return new PingOperation(codec, topologyId, transport, cacheNameBytes);
   }

   /**
    * Construct a fault tolerant ping request. This operation should be capable
    * to deal with nodes being down, so it will find the first node successful
    * node to respond to the ping.
    *
    * @return a ping operation for the cluster
    */
   public FaultTolerantPingOperation newFaultTolerantPingOperation() {
      return new FaultTolerantPingOperation(
            codec, transportFactory, cacheNameBytes, topologyId, flags());
   }

   public QueryOperation newQueryOperation(RemoteQuery remoteQuery) {
      return new QueryOperation(
            codec, transportFactory, cacheNameBytes, topologyId, flags(), remoteQuery);
   }

   public SizeOperation newSizeOperation() {
      return new SizeOperation(codec, transportFactory, cacheNameBytes, topologyId, flags());
   }

   public ExecuteOperation newExecuteOperation(String taskName, Map<String, byte[]> marshalledParams) {
		return new ExecuteOperation(codec, transportFactory, cacheNameBytes, topologyId, flags(), taskName, marshalledParams);
	}

   public Flag[] flags() {
      List<Flag> flags = this.flagsMap.get();
      this.flagsMap.remove();
      if (forceReturnValue) {
         if (flags == null) {
            return FORCE_RETURN_VALUE;
         } else {
            flags.add(Flag.FORCE_RETURN_VALUE);
         }
      }
      return flags != null ? flags.toArray(new Flag[0]) : null;
   }

   public void setFlags(Flag[] flags) {
      List<Flag> list = new ArrayList<Flag>();
      for(Flag flag : flags)
         list.add(flag);
      this.flagsMap.set(list);
   }

   public void addFlags(Flag... flags) {
      List<Flag> list = this.flagsMap.get();
      if (list == null) {
         list = new ArrayList<Flag>();
         this.flagsMap.set(list);
      }
      for(Flag flag : flags)
         list.add(flag);
   }

   public boolean hasFlag(Flag flag) {
      List<Flag> list = this.flagsMap.get();
      return list != null && list.contains(flag);
   }

   public CacheTopologyInfo getCacheTopologyInfo() {
      return transportFactory.getCacheTopologyInfo(cacheNameBytes);
   }

   public IterationStartOperation newIterationStartOperation(String filterConverterFactory, Set<Integer> segments, int batchSize) {
      return new IterationStartOperation(codec, flags(), cacheNameBytes, topologyId, filterConverterFactory, segments, batchSize, transportFactory);
   }

   public IterationEndOperation newIterationEndOperation(String iterationId, Transport transport) {
      return new IterationEndOperation(codec, flags(), cacheNameBytes, topologyId, iterationId, transportFactory, transport);
   }

   public IterationNextOperation newIterationNextOperation(String iterationId, Transport transport) {
      return new IterationNextOperation(codec, flags(), cacheNameBytes, topologyId, iterationId, transport);
   }
}
