package org.infinispan.client.hotrod;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.test.InternalRemoteCacheManager;
import org.infinispan.client.hotrod.test.SingleHotRodServerTest;
import org.infinispan.commons.util.concurrent.FutureListener;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.testng.Assert.assertNotEquals;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Test(groups = "functional", testName = "client.hotrod.RemoteAsyncAPITest")
public class RemoteAsyncAPITest extends SingleHotRodServerTest {

   @Override
   protected RemoteCacheManager getRemoteCacheManager() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.forceReturnValues(isForceReturnValuesViaConfiguration());
      builder.addServer().host("127.0.0.1").port(hotrodServer.getPort());
      return new InternalRemoteCacheManager(builder.build());
   }

   protected boolean isForceReturnValuesViaConfiguration() {
      return true;
   }

   protected RemoteCache<String, String> remote() {
      return remoteCacheManager.getCache();
   }

   public void testPutAsync() throws Exception {
      // put
      Future<String> f = remote().putAsync("k", "v");
      testFuture(f, null);
      testK("v");

      f = remote().putAsync("k", "v2");
      testFuture(f, "v");
      testK("v2");
   }

   public void testPutAsyncWithListener() throws Exception {
      NotifyingFuture<String> f = remote().putAsync("k", "v");
      testFutureWithListener(f, null);
      testK("v");

      f = remote().putAsync("k", "v2");
      testFutureWithListener(f, "v");
      testK("v2");
   }

   public void testPutAllAsync() throws Exception {
      Future<Void> f = remote().putAllAsync(Collections.singletonMap("k", "v3"));
      testFuture(f, null);
      testK("v3");
   }

   public void testPutAllAsyncWithListener() throws Exception {
      NotifyingFuture<Void> f = remote().putAllAsync(Collections.singletonMap("k", "v3"));
      testFutureWithListener(f, null);
      testK("v3");
   }

   public void testPutIfAbsentAsync() throws Exception {
      remote().put("k", "v3");
      testK("v3");

      Future<String> f = remote().putIfAbsentAsync("k", "v4");
      testFuture(f, "v3");
      assertEquals("v3", remote().remove("k"));

      f = remote().putIfAbsentAsync("k", "v5");
      testFuture(f, null);
      testK("v5");
   }

   public void testPutIfAbsentAsyncWithListener() throws Exception {
      remote().put("k", "v3");
      testK("v3");

      NotifyingFuture<String> f = remote().putIfAbsentAsync("k", "v4");
      testFutureWithListener(f, "v3");
      assertEquals("v3", remote().remove("k"));

      f = remote().putIfAbsentAsync("k", "v5");
      testFutureWithListener(f, null);
      testK("v5");
   }

   public void testRemoveAsync() throws Exception {
      remote().put("k", "v3");
      testK("v3");

      Future<String> f = remote().removeAsync("k");
      testFuture(f, "v3");
      testK(null);
   }

   public void testRemoveAsyncWithListener() throws Exception {
      remote().put("k", "v3");
      testK("v3");

      NotifyingFuture<String> f = remote().removeAsync("k");
      testFutureWithListener(f, "v3");
      testK(null);
   }

   public void testGetAsync() throws Exception {
      remote().put("k", "v");
      testK("v");

      Future<String> f = remote().getAsync("k");
      testFuture(f, "v");
      testK("v");
   }

   public void testGetAsyncWithListener() throws Exception {
      remote().put("k", "v");
      testK("v");

      NotifyingFuture<String> f = remote().getAsync("k");
      testFutureWithListener(f, "v");
   }

   public void testRemoveWithVersionAsync() throws Exception {
      remote().put("k", "v4");
      VersionedValue value = remote().getVersioned("k");

      Future<Boolean> f = remote().removeWithVersionAsync("k", value.getVersion() + 1);
      testFuture(f, false);
      testK("v4");

      f = remote().removeWithVersionAsync("k", value.getVersion());
      testFuture(f, true);
      testK(null);
   }

   public void testRemoveWithVersionAsyncWithListener() throws Exception {
      remote().put("k", "v4");
      VersionedValue value = remote().getVersioned("k");

      NotifyingFuture<Boolean> f = remote().removeWithVersionAsync("k", value.getVersion() + 1);
      testFutureWithListener(f, false);
      testK("v4");

      f = remote().removeWithVersionAsync("k", value.getVersion());
      testFutureWithListener(f, true);
      testK(null);
   }

   public void testReplaceAsync() throws Exception {
      testK(null);
      Future<String> f = remote().replaceAsync("k", "v5");
      testFuture(f, null);
      testK(null);

      remote().put("k", "v");
      testK("v");
      f = remote().replaceAsync("k", "v5");
      testFuture(f, "v");
      testK("v5");
   }

   public void testReplaceAsyncWithListener() throws Exception {
      testK(null);
      NotifyingFuture<String> f = remote().replaceAsync("k", "v5");
      testFutureWithListener(f, null);
      testK(null);

      remote().put("k", "v");
      testK("v");
      f = remote().replaceAsync("k", "v5");
      testFutureWithListener(f, "v");
      testK("v5");
   }

   public void testReplaceWithVersionAsync() throws Exception {
      remote().put("k", "v");
      VersionedValue versioned1 = remote().getVersioned("k");

      Future<Boolean> f = remote().replaceWithVersionAsync("k", "v2", versioned1.getVersion());
      testFuture(f, true);

      VersionedValue versioned2 = remote().getVersioned("k");
      assertNotEquals(versioned1.getVersion(), versioned2.getVersion());
      assertEquals(versioned2.getValue(), "v2");

      f = remote().replaceWithVersionAsync("k", "v3", versioned1.getVersion());
      testFuture(f, false);
      testK("v2");
   }

   public void testReplaceWithVersionAsyncWithListener() throws Exception {
      remote().put("k", "v");
      VersionedValue versioned1 = remote().getVersioned("k");

      NotifyingFuture<Boolean> f = remote().replaceWithVersionAsync("k", "v2", versioned1.getVersion());
      testFutureWithListener(f, true);

      VersionedValue versioned2 = remote().getVersioned("k");
      assertNotEquals(versioned1.getVersion(), versioned2.getVersion());
      assertEquals(versioned2.getValue(), "v2");

      f = remote().replaceWithVersionAsync("k", "v3", versioned1.getVersion());
      testFutureWithListener(f, false);
      testK("v2");
   }

   private <T> void testK(T expected) {
      assertEquals(expected, remote().get("k"));
   }

   private <T> void testFuture(Future<T> f, T expected) throws ExecutionException, InterruptedException {
      assertNotNull(f);
      assertFalse(f.isCancelled());
      T value = f.get();
      assertEquals("Obtained " + value, expected, value);
      assertTrue(f.isDone());
   }

   private <T> void testFutureWithListener(NotifyingFuture<T> f, T expected) throws InterruptedException {
      assertNotNull(f);
      AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
      CountDownLatch latch = new CountDownLatch(1);
      f.attachListener(new TestingListener<T>(expected, ex, latch));
      if (!latch.await(5, TimeUnit.SECONDS)) {
         fail("Not finished within 5 seconds");
      }
      if (ex.get() != null) {
         throw new AssertionError(ex.get());
      }
   }

   private static class TestingListener<T> implements FutureListener<T> {
      private final T expected;
      private final AtomicReference<Throwable> exception;
      private final CountDownLatch latch;

      private TestingListener(T expected, AtomicReference<Throwable> exception, CountDownLatch latch) {
         this.expected = expected;
         this.exception = exception;
         this.latch = latch;
      }

      @Override
      public void futureDone(Future<T> future) {
         try {
            assertNotNull(future);
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());
            T value = future.get();
            assertEquals("Obtained " + value, expected, value);
         } catch (Throwable t) {
            exception.set(t);
         } finally {
            latch.countDown();
         }
      }
   }
}
