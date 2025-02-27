package org.infinispan.lucene.impl;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;
import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.TransactionManager;

/**
 * <p>Factory for locks obtained in <code>InfinispanDirectory</code>, this factory produces instances of
 * <code>TransactionalSharedLuceneLock</code>.</p> <p>Usually Lucene acquires the lock when creating an IndexWriter and
 * releases it when closing it; these open-close operations are mapped to transactions as begin-commit, so all changes
 * are going to be effective at IndexWriter close. The advantage is that a transaction rollback will be able to undo all
 * changes applied to the index, but this requires enough memory to hold all the changes until the commit.</p> <p>Using
 * a TransactionalSharedLuceneLock is not compatible with Lucene's default MergeScheduler: use an in-thread
 * implementation like SerialMergeScheduler <code>indexWriter.setMergeScheduler( new SerialMergeScheduler()
 * );</code></p>
 *
 * @author Sanne Grinovero
 * @author Lukasz Moren
 * @see TransactionalSharedLuceneLock
 * @see org.apache.lucene.index.SerialMergeScheduler
 * @since 4.0
 */
@SuppressWarnings("unchecked")
public class TransactionalLockFactory extends LockFactory {

   public static final TransactionalLockFactory INSTANCE = new TransactionalLockFactory();

   private static final Log log = LogFactory.getLog(TransactionalLockFactory.class);

   /**
    * {@inheritDoc}
    */
   @Override
   public TransactionalSharedLuceneLock makeLock(Directory dir, String lockName) {
      if (!(dir instanceof DirectoryLucene)) {
         throw new UnsupportedOperationException("TransactionalSharedLuceneLock can only be used with DirectoryLucene, got: " + dir);
      }
      DirectoryLucene infinispanDirectory = (DirectoryLucene) dir;
      Cache cache = infinispanDirectory.getDistLockCache();
      String indexName = infinispanDirectory.getIndexName();
      TransactionManager tm = cache.getAdvancedCache().getTransactionManager();
      if (tm == null) {
         ComponentStatus status = cache.getAdvancedCache().getComponentRegistry().getStatus();
         if (status.equals(ComponentStatus.RUNNING)) {
            throw new CacheException(
                    "Failed looking up TransactionManager. Check if any transaction manager is associated with Infinispan cache: \'"
                            + cache.getName() + "\'");
         } else {
            throw new CacheException("Failed looking up TransactionManager: the cache is not running");
         }
      }

      TransactionalSharedLuceneLock lock = new TransactionalSharedLuceneLock(cache, indexName, lockName, tm);

      if (log.isTraceEnabled()) {
         log.tracef("Lock prepared, not acquired: %s for index %s", lockName, indexName);
      }
      return lock;
   }

}
