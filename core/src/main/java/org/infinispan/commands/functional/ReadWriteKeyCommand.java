package org.infinispan.commands.functional;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.write.ValueMatcher;
import org.infinispan.commons.api.functional.EntryView.ReadWriteEntryView;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.functional.impl.EntryViews;

import java.util.Set;
import java.util.function.Function;

public final class ReadWriteKeyCommand<K, V, R> extends AbstractWriteKeyCommand<K, V> {

   public static final byte COMMAND_ID = 50;

   private Function<ReadWriteEntryView<K, V>, R> f;

   public ReadWriteKeyCommand(K key, Function<ReadWriteEntryView<K, V>, R> f, CommandInvocationId id) {
      super(key, f.getClass().getAnnotation(SerializeWith.class), id);
      this.f = f;
   }

   public ReadWriteKeyCommand() {
      // No-op, for marshalling
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void setParameters(int commandId, Object[] parameters) {
      if (commandId != COMMAND_ID) throw new IllegalStateException("Invalid method id");
      key = parameters[0];
      f = (Function<ReadWriteEntryView<K, V>, R>) parameters[1];
      valueMatcher = (ValueMatcher) parameters[2];
      flags = (Set<Flag>) parameters[3];
      commandInvocationId = (CommandInvocationId) parameters[4];
   }

   @Override
   public Object[] getParameters() {
      return new Object[]{key, f, valueMatcher, Flag.copyWithoutRemotableFlags(flags), commandInvocationId};
   }

   @Override
   public boolean isConditional() {
      return true;
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      // It's not worth looking up the entry if we're never going to apply the change.
      if (valueMatcher == ValueMatcher.MATCH_NEVER) {
         successful = false;
         return null;
      }

      CacheEntry<K, V> e = ctx.lookupEntry(key);

      // Could be that the key is not local, 'null' is how this is signalled
      if (e == null) return null;

      R ret = f.apply(EntryViews.readWrite(e));
      // TODO: Apply and verify laundering for all the rest of read-write and write-only commands
      return launderIfReadWriteView(ret);
   }

   /**
    * For convenience, a lambda might decide to return the entry view it
    * received as parameter, because that makes easy to return both value and
    * meta parameters back to the client.
    *
    * If the lambda function decides to return an writable entry view,
    * launder it into a read-only entry view to avoid the user trying apply
    * any modifications to the entry view from outside the lambda function.
    */
   private Object launderIfReadWriteView(R ret) {
      if (ret instanceof ReadWriteEntryView)
         return EntryViews.immutableReadWrite((ReadWriteEntryView) ret);

      return ret;
   }

   @Override
   public void updateStatusFromRemoteResponse(Object remoteResponse) {
      // TODO: Customise this generated block
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadWriteKeyCommand(ctx, this);
   }
}
