package org.infinispan.commands.functional;

import org.infinispan.commands.Visitor;
import org.infinispan.commons.api.functional.EntryView.WriteEntryView;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.functional.impl.EntryViews;
import org.infinispan.lifecycle.ComponentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class WriteOnlyManyCommand<K, V> extends AbstractWriteManyCommand<K, V> {

   public static final byte COMMAND_ID = 56;

   private Set<? extends K> keys;
   private Consumer<WriteEntryView<V>> f;

   public WriteOnlyManyCommand(Set<? extends K> keys, Consumer<WriteEntryView<V>> f) {
      this.keys = keys;
      this.f = f;
   }

   public WriteOnlyManyCommand(WriteOnlyManyCommand<K, V> command) {
      this.keys = command.getKeys();
      this.f = command.f;
   }

   public WriteOnlyManyCommand() {
   }

   public Set<? extends K> getKeys() {
      return keys;
   }

   public void setKeys(Set<? extends K> keys) {
      this.keys = keys;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void setParameters(int commandId, Object[] parameters) {
      keys = (Set<? extends K>) parameters[0];
      f = (Consumer<WriteEntryView<V>>) parameters[1];
      isForwarded = (Boolean) parameters[2];
   }

   @Override
   public Object[] getParameters() {
      return new Object[]{keys, f, isForwarded};
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitWriteOnlyManyCommand(ctx, this);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      // Can't return a lazy stream here because the current code in
      // EntryWrappingInterceptor expects any changes to be done eagerly,
      // otherwise they're not applied. So, apply the function eagerly and
      // return a lazy stream of the void returns.

      // TODO: Simplify with a collect() call
      List<Void> returns = new ArrayList<>(keys.size());
      keys.forEach(k -> {
         CacheEntry<K, V> cacheEntry = ctx.lookupEntry(k);

         // Could be that the key is not local, 'null' is how this is signalled
         if (cacheEntry != null) {
            f.accept(EntryViews.writeOnly(cacheEntry));
            returns.add(null);
         }
      });
      return returns.stream();
   }

   @Override
   public boolean isReturnValueExpected() {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean canBlock() {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public Set<Object> getAffectedKeys() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void updateStatusFromRemoteResponse(Object remoteResponse) {
      // TODO: Customise this generated block
   }

   @Override
   public boolean ignoreCommandOnStatus(ComponentStatus status) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean isWriteOnly() {
      return true;
   }

}
