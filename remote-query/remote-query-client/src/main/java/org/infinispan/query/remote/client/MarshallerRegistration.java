package org.infinispan.query.remote.client;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 6.0
 */
public final class MarshallerRegistration {

   public static final String QUERY_PROTO_RES = "/org/infinispan/query/remote/client/query.proto";
   public static final String FILTER_PROTO_RES = "/org/infinispan/query/remote/client/filter.proto";
   public static final String MESSAGE_PROTO_RES = "/org/infinispan/protostream/message-wrapping.proto";

   public static void registerMarshallers(SerializationContext ctx) throws IOException, DescriptorParserException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile(QUERY_PROTO_RES, MarshallerRegistration.class.getResourceAsStream(QUERY_PROTO_RES));
      fileDescriptorSource.addProtoFile(FILTER_PROTO_RES, MarshallerRegistration.class.getResourceAsStream(FILTER_PROTO_RES));
      fileDescriptorSource.addProtoFile(MESSAGE_PROTO_RES, MarshallerRegistration.class.getResourceAsStream(MESSAGE_PROTO_RES));
      ctx.registerProtoFiles(fileDescriptorSource);
      ctx.registerMarshaller(new QueryRequest.Marshaller());
      ctx.registerMarshaller(new QueryResponse.Marshaller());
      ctx.registerMarshaller(new ContinuousQueryResult.Marshaller());
      ctx.registerMarshaller(new FilterResult.Marshaller());
   }
}
