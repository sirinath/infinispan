package org.infinispan.functional.impl;

import org.infinispan.commons.api.functional.Param;
import org.infinispan.commons.api.functional.Param.FutureMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Internal class that encapsulates collection of parameters used to tweak
 * functional map operations.
 *
 * DESIGN RATIONALES:
 * <ul>
 *    <il>Internally, parameters are stored in an array which is indexed by a
 *    parameter's {@link Param#id()}
 *    </il>
 *    <il>All parameters have default values which are stored in a static
 *    array field in {@link Params} class, which are used to as base collection
 *    when adding or overriding parameters.
 *    </il>
 * </ul>
 *
 * @since 8.0
 */
final class Params {

   private static final Param<?>[] DEFAULTS = new Param<?>[]{
      FutureMode.defaultValue(),
   };

   final Param<?>[] params;

   private Params(Param<?>[] params) {
      this.params = params;
   }

   /**
    * Checks whether all the parameters passed in are already present in the
    * current parameters. This method can be used to optimise the decision on
    * whether the parameters collection needs updating at all.
    */
   public boolean containsAll(Param<?>... ps) {
      List<Param<?>> paramsToCheck = Arrays.asList(ps);
      List<Param<?>> paramsCurrent = Arrays.asList(params);
      return paramsCurrent.containsAll(paramsToCheck);
   }

   /**
    * Adds all parameters and returns a new parameter collection.
    */
   public Params addAll(Param<?>... ps) {
      List<Param<?>> paramsToAdd = Arrays.asList(ps);
      Param<?>[] paramsAll = Arrays.copyOf(params, params.length);
      paramsToAdd.forEach(p -> paramsAll[p.id()] = p);
      return new Params(paramsAll);
   }

   /**
    * Retrieve a param given its identifier. Callers are expected to know the
    * exact type of parameter that will be returned. Such assumption is
    * possible because as indicated in {@link Param} implementations will
    * only come from Infinispan itself.
    */
   @SuppressWarnings("unchecked")
   public <T> Param<T> get(int index) {
      return (Param<T>) params[index];
   }

   @Override
   public String toString() {
      return "Params=" + Arrays.toString(params);
   }

   public static Params create() {
      return new Params(DEFAULTS);
   }

   public static Params from(Param<?>... ps) {
      List<Param<?>> paramsToAdd = Arrays.asList(ps);
      List<Param<?>> paramsDefaults = Arrays.asList(DEFAULTS);
      if (paramsDefaults.containsAll(paramsToAdd))
         return create(); // All parameters are defaults, don't do more work

      Param<?>[] paramsAll = Arrays.copyOf(DEFAULTS, DEFAULTS.length);
      paramsToAdd.forEach(p -> paramsAll[p.id()] = p);
      return new Params(paramsAll);
   }

   static <T> CompletableFuture<T> withFuture(Param<FutureMode> futureParam,
         ExecutorService asyncExec, Supplier<T> s) {
      switch (futureParam.get()) {
         case COMPLETED:
            // If completed, complete the future directly with the result.
            // No separate thread or executor is instantiated.
            return CompletableFuture.completedFuture(s.get());
         case ASYNC:
            // If async, execute the supply function asynchronously,
            // and return a future that's completed when the supply
            // function returns.
            return CompletableFuture.supplyAsync(s, asyncExec);
         default:
            throw new IllegalStateException();
      }
   }

}
