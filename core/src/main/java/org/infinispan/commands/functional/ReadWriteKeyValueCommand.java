package org.infinispan.commands.functional;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.write.ValueMatcher;
import org.infinispan.commons.api.functional.EntryView;
import org.infinispan.commons.api.functional.EntryView.ReadWriteEntryView;
import org.infinispan.commons.equivalence.AnyEquivalence;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.functional.impl.EntryViews;
import org.infinispan.metadata.Metadata;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Set;
import java.util.function.BiFunction;

import static org.infinispan.commons.util.Util.toStr;

public final class ReadWriteKeyValueCommand<K, V, R> extends AbstractWriteKeyCommand<K, V> {
   private static final Log log = LogFactory.getLog(ReadWriteKeyValueCommand.class);

   public static final byte COMMAND_ID = 51;

   private V value;
   private BiFunction<V, ReadWriteEntryView<K, V>, R> f;
   private V prevValue;
   private Metadata prevMetadata;

   public ReadWriteKeyValueCommand(K key, V value, BiFunction<V, ReadWriteEntryView<K, V>, R> f,
         CommandInvocationId id) {
      super(key, f.getClass().getAnnotation(SerializeWith.class), id);
      this.value = value;
      this.f = f;
   }

   public ReadWriteKeyValueCommand() {
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
      value = (V) parameters[1];
      f = (BiFunction<V, ReadWriteEntryView<K, V>, R>) parameters[2];
      valueMatcher = (ValueMatcher) parameters[3];
      flags = (Set<Flag>) parameters[4];
      commandInvocationId = (CommandInvocationId) parameters[5];
      prevValue = (V) parameters[6];
      prevMetadata = (Metadata) parameters[7];
   }

   @Override
   public Object[] getParameters() {
      return new Object[]{
         key, value, f, valueMatcher, Flag.copyWithoutRemotableFlags(flags),
         commandInvocationId, prevValue, prevMetadata
      };
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

      MVCCEntry<K, V> e = (MVCCEntry<K, V>) ctx.lookupEntry(key);

      // Could be that the key is not local
      if (e == null) return null;

      // Command only has one previous value, do not override it
      if (prevValue == null && (flags == null || !flags.contains(Flag.COMMAND_RETRY))) {
         prevValue = e.getValue();
         prevMetadata = e.getMetadata();
      }

      // Protect against outdated old value using the value matcher.
      // If the value has been update while on the retry, use the newer value.
      // Also take into account that the value might have been removed.
      // TODO: Configure equivalence function
      if (valueUnchanged(e, prevValue, value) || valueRemoved(e, prevValue)) {
         log.tracef("Execute read-write function on previous value %s and previous metadata %s", prevValue, prevMetadata);
         R ret = f.apply(value, EntryViews.readWrite(e, prevValue, prevMetadata));
         return launderWithCurrentIfReadWriteView(e, ret);
      }

      return f.apply(value, EntryViews.readWrite(e, e.getValue(), e.getMetadata()));
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
   private Object launderWithCurrentIfReadWriteView(MVCCEntry<K, V> e, R ret) {
      if (ret instanceof ReadWriteEntryView)
         return EntryViews.immutableReadWrite(e.getKey(), e.getValue(), e.getMetadata());

      return ret;
   }

   boolean valueRemoved(MVCCEntry<K, V> e, V prevValue) {
      return valueUnchanged(e, prevValue, null);
   }

   boolean valueUnchanged(MVCCEntry<K, V> e, V prevValue, V value) {
      return valueMatcher.matches(e, prevValue, value, AnyEquivalence.getInstance());
   }

   @Override
   public void updateStatusFromRemoteResponse(Object remoteResponse) {
      // TODO: Customise this generated block
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadWriteKeyValueCommand(ctx, this);
   }

   @Override
   public String toString() {
      return new StringBuilder(getClass().getSimpleName())
         .append(" {key=")
         .append(toStr(key))
         .append(", value=").append(toStr(value))
         .append(", prevValue=").append(toStr(prevValue))
         .append(", prevMetadata=").append(toStr(prevMetadata))
         .append(", flags=").append(flags)
         .append(", valueMatcher=").append(valueMatcher)
         .append(", successful=").append(successful)
         .append("}")
         .toString();
   }

}
