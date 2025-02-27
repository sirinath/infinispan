package org.infinispan.commons.marshall;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that this class should be serialized with an instance of the given
 * {@link Externalizer} class.
 *
 * Any externalizer type referred by this annotation must be either
 * {@link java.io.Serializable} or {@link java.io.Externalizable} because the
 * marshalling infrastructure will ship an instance of the externalizer to any
 * node that's no aware of this externalizer, hence allowing for dynamic
 * externalizer discovery.
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface SerializeWith {

   /**
    * Specify the externalizer class to be used by the annotated class.
    *
    * @return the externalizer type
    */
   Class<? extends Externalizer<?>> value();

   /**
    * TODO
    * @return
    */
   SerializeValueMatcher valueMatcher() default SerializeValueMatcher.MATCH_ALWAYS;

}
