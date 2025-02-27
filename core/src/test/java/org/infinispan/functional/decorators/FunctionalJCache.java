package org.infinispan.functional.decorators;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.api.functional.EntryView.ReadEntryView;
import org.infinispan.commons.api.functional.EntryView.ReadWriteEntryView;
import org.infinispan.commons.api.functional.FunctionalMap.ReadOnlyMap;
import org.infinispan.commons.api.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.commons.api.functional.FunctionalMap.WriteOnlyMap;
import org.infinispan.commons.api.functional.Listeners.ReadWriteListeners;
import org.infinispan.commons.api.functional.Listeners.WriteListeners;
import org.infinispan.commons.api.functional.Param.FutureMode;
import org.infinispan.commons.api.functional.Traversable;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadOnlyMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.infinispan.functional.impl.Traversables;
import org.infinispan.functional.impl.WriteOnlyMapImpl;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.infinispan.util.functional.MarshallableFunctionalInterfaces.*;
import static org.infinispan.util.functional.MarshallableFunctionalInterfaces.removeConsumer;

/**
 * A {@link Cache} implementation that uses the operations exposed by
 * {@link ReadOnlyMap}, {@link WriteOnlyMap} and {@link ReadWriteMap}, and
 * validates their usefulness.
 */
public final class FunctionalJCache<K, V> implements Cache<K, V>, FunctionalListeners<K, V> {

   final ReadOnlyMap<K, V> readOnly;
   final WriteOnlyMap<K, V> writeOnly;
   final ReadWriteMap<K, V> readWrite;

   // Rudimentary constructor, we'll provide more idiomatic construction
   // via main Infinispan class which is still to be defined
   private FunctionalJCache(FunctionalMapImpl<K, V> map) {
      FunctionalMapImpl<K, V> blockingMap = map.withParams(FutureMode.COMPLETED);
      this.readOnly = ReadOnlyMapImpl.create(blockingMap);
      this.writeOnly = WriteOnlyMapImpl.create(blockingMap);
      this.readWrite = ReadWriteMapImpl.create(blockingMap);
   }

   public static <K, V> Cache<K, V> create(AdvancedCache<K, V> cache) {
      return new FunctionalJCache<>(FunctionalMapImpl.create(cache));
   }

   public static <K, V> Cache<K, V> create(FunctionalMapImpl<K, V> map) {
      return new FunctionalJCache<>(map);
   }

   @Override
   public WriteListeners<K, V> writeOnlyListeners() {
      return writeOnly.listeners();
   }

   @Override
   public ReadWriteListeners<K, V> readWriteListeners() {
      return readWrite.listeners();
   }

   @Override
   public V get(K key) {
      return await(readOnly.eval(key, ro -> ro.find().orElse(null)));
   }

   @Override
   public Map<K, V> getAll(Set<? extends K> keys) {
      Traversable<ReadEntryView<K, V>> t = readOnly.evalMany(keys, ro -> ro);
      return t.collect(HashMap::new, (m, ro) -> ro.find().map(v -> m.put(ro.key(), v)), HashMap::putAll);
   }

   @Override
   public boolean containsKey(K key) {
      return await(readOnly.eval(key, e -> e.find().isPresent()));
   }

   @Override
   public void put(K key, V value) {
      await(writeOnly.eval(key, value, setValueConsumer()));
   }

   @Override
   public V getAndPut(K key, V value) {
      return await(readWrite.eval(key, value, setValueReturnPrevOrNull()));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      // Since blocking is in use, there's no need to consume the iterator for
      // the put all effects to be executed.
      // With blocking, the iterator gets pro-actively consumed, and the
      // return offers the possibility to re-iterate by the user.
      // Since the iteration here has no result, we can skip the iteration altogether.
      writeOnly.evalMany(map, setValueConsumer());
   }

   @Override
   public boolean putIfAbsent(K key, V value) {
      return await(readWrite.eval(key, value, setValueIfAbsentReturnBoolean()));
   }

   @Override
   public boolean remove(K key) {
      return await(readWrite.eval(key, removeReturnBoolean()));
   }

   @Override
   public boolean remove(K key, V oldValue) {
      return await(readWrite.eval(key, oldValue, removeIfValueEqualsReturnBoolean()));
   }

   @Override
   public V getAndRemove(K key) {
      return await(readWrite.eval(key, removeReturnPrevOrNull()));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return await(readWrite.eval(key, newValue, setValueIfEqualsReturnBoolean(oldValue)));
   }

   @Override
   public boolean replace(K key, V value) {
      return await(readWrite.eval(key, value, setValueIfPresentReturnBoolean()));
   }

   @Override
   public V getAndReplace(K key, V value) {
      return await(readWrite.eval(key, value, setValueIfPresentReturnPrevOrNull()));
   }

   @Override
   public void removeAll(Set<? extends K> keys) {
      // Since blocking is in use, there's no need to consume the iterator for
      // the put all effects to be executed.
      // With blocking, the iterator gets pro-actively consumed, and the
      // return offers the possibility to re-iterate by the user.
      // Since the iteration here has no result, we can skip the iteration altogether.
      writeOnly.evalMany(keys, removeConsumer());
   }

   @Override
   public void removeAll() {
      writeOnly.evalAll(removeConsumer());
   }

   @Override
   public void clear() {
      await(writeOnly.truncate());
   }

   @Override
   public Iterator<Entry<K, V>> iterator() {
      Traversable<Entry<K, V>> t = readOnly.entries().map(rw -> new Entry<K, V>() {
         @Override
         public K getKey() {
            return rw.key();
         }

         @Override
         public V getValue() {
            return rw.get();
         }

         @Override
         public <T> T unwrap(Class<T> clazz) {
            return null;
         }

         // In Java 8, default remove() implementation is unsupported, but
         // adding support for it would be relatively trivial if following
         // similar solution to the one in ConcurrentMapDecorator
      });
      return Traversables.asIterator(t);
   }

   @Override
   public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
      return await(readWrite.eval(key, new InvokeFunction<>(entryProcessor, arguments)));
   }

   @SerializeWith(value = InvokeFunction.Externalizer0.class)
   private static final class InvokeFunction<K, V, T> implements Function<ReadWriteEntryView<K, V>, T> {
      private final EntryProcessor<K, V, T> entryProcessor;
      private final Object[] arguments;

      private InvokeFunction(EntryProcessor<K, V, T> entryProcessor, Object[] arguments) {
         this.entryProcessor = entryProcessor;
         this.arguments = arguments;
      }

      @Override
      public T apply(ReadWriteEntryView<K, V> rw) {
         return entryProcessor.process(new ReadWriteMutableEntry<>(rw), arguments);
      }

      public static final class Externalizer0 implements Externalizer<InvokeFunction<?, ?, ?>> {
         public void writeObject(ObjectOutput oo, InvokeFunction<?, ?, ?> o) throws IOException {
            oo.writeObject(o.entryProcessor);
            oo.writeInt(o.arguments.length);
            for (Object argument : o.arguments)
               oo.writeObject(argument);
         }
         public InvokeFunction<?, ?, ?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            EntryProcessor<?, ?, ?> entryProcessor = (EntryProcessor<?, ?, ?>) input.readObject();
            int length = input.readInt();
            Object[] arguments = new Object[length];
            for (int i = 0; i < length; i++) arguments[i] = input.readObject();
            return new InvokeFunction<>(entryProcessor, arguments);
         }
      }
   }

   private static final class ReadWriteMutableEntry<K, V> implements MutableEntry<K, V> {
      final ReadWriteEntryView<K, V> rw;

      private ReadWriteMutableEntry(ReadWriteEntryView<K, V> rw) {
         this.rw = rw;
      }

      @Override
      public boolean exists() {
         return rw.find().isPresent();
      }

      @Override
      public void remove() {
         rw.remove();
      }

      @Override
      public void setValue(V value) {
         rw.set(value);
      }

      @Override
      public K getKey() {
         return rw.key();
      }

      @Override
      public V getValue() {
         return rw.find().orElse(null);
      }

      @Override
      public <T> T unwrap(Class<T> clazz) {
         return null;  // TODO: Customise this generated block
      }
   }

   @Override
   public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
      Traversable<EntryProcessorResultWithKey<K, T>> t = readWrite.evalMany(
            keys, new InvokeAllFunction<>(entryProcessor, arguments));

      return t.collect(HashMap::new, (m, res) -> m.put(res.key, res), HashMap::putAll);
   }

   @SerializeWith(value = InvokeAllFunction.Externalizer0.class)
   private static final class InvokeAllFunction<K, V, T> implements Function<ReadWriteEntryView<K, V>, EntryProcessorResultWithKey<K, T>> {
      private final EntryProcessor<K, V, T> entryProcessor;
      private final Object[] arguments;

      private InvokeAllFunction(EntryProcessor<K, V, T> entryProcessor, Object[] arguments) {
         this.entryProcessor = entryProcessor;
         this.arguments = arguments;
      }

      @Override
      public EntryProcessorResultWithKey<K, T> apply(ReadWriteEntryView<K, V> rw) {
         T res = entryProcessor.process(new ReadWriteMutableEntry<>(rw), arguments);
         return new EntryProcessorResultWithKey<>(rw.key(), res);
      }

      public static final class Externalizer0 implements Externalizer<InvokeAllFunction<?, ?, ?>> {
         public void writeObject(ObjectOutput oo, InvokeAllFunction<?, ?, ?> o) throws IOException {
            oo.writeObject(o.entryProcessor);
            oo.writeInt(o.arguments.length);
            for (Object argument : o.arguments)
               oo.writeObject(argument);
         }
         public InvokeAllFunction<?, ?, ?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            EntryProcessor<?, ?, ?> entryProcessor = (EntryProcessor<?, ?, ?>) input.readObject();
            int length = input.readInt();
            Object[] arguments = new Object[length];
            for (int i = 0; i < length; i++) arguments[i] = input.readObject();
            return new InvokeAllFunction<>(entryProcessor, arguments);
         }
      }
   }

   @SerializeWith(EntryProcessorResultWithKey.Externalizer0.class)
   private static final class EntryProcessorResultWithKey<K, T> implements EntryProcessorResult<T> {
      final K key;
      final T t;

      public EntryProcessorResultWithKey(K key, T t) {
         this.key = key;
         this.t = t;
      }

      @Override
      public T get() throws EntryProcessorException {
         return t;
      }

      public static final class Externalizer0 implements Externalizer<EntryProcessorResultWithKey<?, ?>> {
         @Override
         public void writeObject(ObjectOutput oo, EntryProcessorResultWithKey<?, ?> o) throws IOException {
            oo.writeObject(o.key);
            oo.writeObject(o.t);
         }

         @Override
         public EntryProcessorResultWithKey<?, ?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Object key = input.readObject();
            Object t = input.readObject();
            return new EntryProcessorResultWithKey<>(key, t);
         }
      }
   }

   @Override
   public void close() {
      try {
         readOnly.close();
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   @Override
   public boolean isClosed() {
      return readOnly.getStatus().isTerminated();
   }

   @Override
   public String getName() {
      return readOnly.getName();
   }

   ////////////////////////////////////////////////////////////////////////////

   @Override
   public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
      // TODO: Customise this generated block
   }

   @Override
   public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> T unwrap(Class<T> clazz) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheManager getCacheManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
      // TODO: Customise this generated block
   }

   @Override
   public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
      // TODO: Customise this generated block
   }

   private static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}
