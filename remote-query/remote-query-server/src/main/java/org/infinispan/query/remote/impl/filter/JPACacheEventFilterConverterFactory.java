package org.infinispan.query.remote.impl.filter;

import org.infinispan.commons.CacheException;
import org.infinispan.filter.NamedFactory;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterFactory;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.query.remote.client.BaseProtoStreamMarshaller;
import org.kohsuke.MetaInfServices;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 7.2
 */
@NamedFactory(name = JPACacheEventFilterConverterFactory.FACTORY_NAME)
@MetaInfServices
public final class JPACacheEventFilterConverterFactory implements CacheEventFilterConverterFactory {

   public static final String FACTORY_NAME = "query-dsl-filter-converter-factory";

   private final BaseProtoStreamMarshaller paramMarshaller = new BaseProtoStreamMarshaller() {

      private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(new Configuration.Builder().build());

      @Override
      protected SerializationContext getSerializationContext() {
         return serializationContext;
      }
   };

   @Override
   public CacheEventFilterConverter<?, ?, ?> getFilterConverter(Object[] params) {
      String jpql;
      try {
         jpql = (String) paramMarshaller.objectFromByteBuffer((byte[]) params[0]);
      } catch (IOException e) {
         throw new CacheException(e);
      } catch (ClassNotFoundException e) {
         throw new CacheException(e);
      }
      //todo [anistor] test this in compat mode too!
      return new JPAProtobufCacheEventFilterConverter(new JPAProtobufFilterAndConverter(jpql));
   }
}

