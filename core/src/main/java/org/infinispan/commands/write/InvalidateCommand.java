package org.infinispan.commands.write;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.util.CollectionFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;


/**
 * Removes an entry from memory.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class InvalidateCommand extends RemoveCommand {
   public static final int COMMAND_ID = 6;
   private static final Log log = LogFactory.getLog(InvalidateCommand.class);
   private static final boolean trace = log.isTraceEnabled();
   protected Object[] keys;

   public InvalidateCommand() {
      // The value matcher will always be the same, so we don't need to serialize it like we do for the other commands
      this.valueMatcher = ValueMatcher.MATCH_ALWAYS;
   }

   public InvalidateCommand(CacheNotifier notifier, Set<Flag> flags, CommandInvocationId commandInvocationId, Object... keys) {
      //valueEquivalence can be null because this command never compares values.
      super(null, null, notifier, flags, null, commandInvocationId);
      this.keys = keys;
      this.notifier = notifier;
   }

   public InvalidateCommand(CacheNotifier notifier, Set<Flag> flags, Collection<Object> keys, CommandInvocationId commandInvocationId) {
      //valueEquivalence can be null because this command never compares values.
      super(null, null, notifier, flags, null, commandInvocationId);
      if (keys == null || keys.isEmpty())
         this.keys = Util.EMPTY_OBJECT_ARRAY;
      else
         this.keys = keys.toArray(new Object[keys.size()]);
      this.notifier = notifier;
   }

   /**
    * Performs an invalidation on a specified entry
    *
    * @param ctx invocation context
    * @return null
    */
   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      if (trace) {
         log.tracef("Invalidating keys %s", Arrays.toString(keys));
      }
      for (Object k : keys) {
         invalidate(ctx, k);
      }
      return null;
   }

   protected void invalidate(InvocationContext ctx, Object keyToInvalidate) throws Throwable {
      key = keyToInvalidate; // so that the superclass can see it
      super.perform(ctx);
   }

   @Override
   public void notify(InvocationContext ctx, Object removedValue, Metadata removedMetadata,
         boolean isPre) {
      notifier.notifyCacheEntryInvalidated(key, removedValue, isPre, ctx, this);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "InvalidateCommand{keys=" +
            Arrays.toString(keys) +
            '}';
   }

   @Override
   public Object[] getParameters() {
      if (keys == null || keys.length == 0) {
         return new Object[]{commandInvocationId, 0};
      } else if (keys.length == 1) {
         return new Object[]{commandInvocationId, 1, keys[0]};
      } else {
         Object[] retval = new Object[keys.length + 2];
         retval[0] = commandInvocationId;
         retval[1] = keys.length;
         System.arraycopy(keys, 0, retval, 2, keys.length);
         return retval;
      }
   }

   @Override
   public void setParameters(int commandId, Object[] args) {
      if (commandId != COMMAND_ID) throw new IllegalStateException("Invalid method id");
      int i = 0;
      commandInvocationId = (CommandInvocationId) args[i++];
      int size = (Integer) args[i++];
      keys = new Object[size];
      if (size == 1) {
         keys[0] = args[i];
      } else if (size > 0) {
         System.arraycopy(args, i, keys, 0, size);
      }
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitInvalidateCommand(ctx, this);
   }

   @Override
   public Object getKey() {
      throw new UnsupportedOperationException("Not supported.  Use getKeys() instead.");
   }

   public Object[] getKeys() {
      return keys;
   }

   @Override
   public Set<Object> getAffectedKeys() {
      return CollectionFactory.makeSet(keys);
   }

   @Override
   public Collection<Object> getKeysToLock() {
      return Arrays.asList(keys);
   }

   @Override
   public boolean ignoreCommandOnStatus(ComponentStatus status) {
      switch (status) {
         case FAILED:
         case STOPPING:
         case TERMINATED:
            return true;
         default:
            return false;
         }
   }

   @Override
   public boolean equals(Object o) {
      if (!super.equals(o)) {
         return false;
      }

      InvalidateCommand that = (InvalidateCommand) o;

      if (!Arrays.equals(keys, that.keys)) {
         return false;
      }
      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (keys != null ? Arrays.hashCode(keys) : 0);
      return result;
   }
}
