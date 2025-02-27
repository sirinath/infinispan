package org.infinispan.query.remote.impl;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.locking.PessimisticLockingInterceptor;
import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;
import org.infinispan.jmx.annotations.Parameter;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.query.remote.CompatibilityProtoStreamMarshaller;
import org.infinispan.query.remote.ProtobufMetadataManager;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.query.remote.impl.indexing.IndexingMetadata;
import org.infinispan.query.remote.impl.indexing.IndexingMetadataCreator;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;

import javax.management.MBeanException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author anistor@redhat.com
 * @since 7.0
 */
@MBean(objectName = ProtobufMetadataManagerConstants.OBJECT_NAME,
      description = "Component that acts as a manager and container for Protocol Buffers message type definitions in the scope of a CacheManger.")
public final class ProtobufMetadataManagerImpl implements ProtobufMetadataManager {

   private Cache<String, String> protobufSchemaCache;

   private ObjectName objectName;

   private final SerializationContext serCtx;

   private EmbeddedCacheManager cacheManager;

   public ProtobufMetadataManagerImpl() {
      org.infinispan.protostream.config.Configuration.Builder configBuilder = new org.infinispan.protostream.config.Configuration.Builder();
      configBuilder
            .messageAnnotation(IndexingMetadata.INDEXED_ANNOTATION)
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
                  .booleanType()
                  .defaultValue(true)
               .annotationMetadataCreator(new IndexingMetadataCreator())
            .fieldAnnotation(IndexingMetadata.INDEXED_FIELD_ANNOTATION)
               .attribute("index")
                  .booleanType()
                  .defaultValue(true)
               .attribute("store")
                  .booleanType()
                  .defaultValue(true);
      serCtx = ProtobufUtil.newSerializationContext(configBuilder.build());
      try {
         MarshallerRegistration.registerMarshallers(serCtx);
      } catch (IOException | DescriptorParserException e) {
         throw new CacheException("Failed to initialise the Protobuf serialization context", e);
      }
   }

   @Inject
   protected void init(EmbeddedCacheManager cacheManager, InternalCacheRegistry internalCacheRegistry) {
      this.cacheManager = cacheManager;
      internalCacheRegistry.registerInternalCache(PROTOBUF_METADATA_CACHE_NAME,
                                                  getProtobufMetadataCacheConfig().build(),
                                                  EnumSet.of(InternalCacheRegistry.Flag.USER));
   }

   /**
    * Obtain the cache, lazily.
    */
   Cache<String, String> getCache() {
      if (protobufSchemaCache == null) {
         protobufSchemaCache = cacheManager.getCache(PROTOBUF_METADATA_CACHE_NAME);
      }
      return protobufSchemaCache;
   }

   private ConfigurationBuilder getProtobufMetadataCacheConfig() {
      GlobalConfiguration globalConfiguration = cacheManager.getGlobalComponentRegistry().getGlobalConfiguration();
      CacheMode cacheMode = globalConfiguration.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL;

      ConfigurationBuilder cfg = new ConfigurationBuilder();
      cfg.transaction()
            .transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable()
            .transaction().lockingMode(LockingMode.PESSIMISTIC).syncCommitPhase(true).syncRollbackPhase(true)
            .locking().isolationLevel(IsolationLevel.READ_COMMITTED).useLockStriping(false)
            .clustering().cacheMode(cacheMode).sync()
            .stateTransfer().fetchInMemoryState(true).awaitInitialTransfer(false)
            .compatibility().enable().marshaller(new CompatibilityProtoStreamMarshaller())
            .customInterceptors().addInterceptor()
            .interceptor(new ProtobufMetadataManagerInterceptor()).after(PessimisticLockingInterceptor.class);
      return cfg;
   }

   @Override
   public ObjectName getObjectName() {
      return objectName;
   }

   @Override
   public void setObjectName(ObjectName objectName) {
      this.objectName = objectName;
   }

   @Override
   public void registerMarshaller(BaseMarshaller<?> marshaller) {
      serCtx.registerMarshaller(marshaller);
   }

   @ManagedOperation(description = "Registers a Protobuf definition file", displayName = "Register Protofile")
   @Override
   public void registerProtofile(@Parameter(name = "fileName", description = "the name of the .proto file") String fileName,
                                 @Parameter(name = "contents", description = "contents of the file") String contents) {
      getCache().put(fileName, contents);
   }

   @ManagedOperation(description = "Registers a set of Protobuf definition files", displayName = "Register Protofiles")
   @Override
   public void registerProtofiles(@Parameter(name = "fileNames", description = "names of the protofiles") String[] fileNames,
                                  @Parameter(name = "fileContents", description = "content of the files") String[] contents) throws Exception {
      if (fileNames.length != contents.length) {
         throw new MBeanException(new IllegalArgumentException("invalid parameter sizes"));
      }
      Map<String, String> files = new HashMap<String, String>(fileNames.length);
      for (int i = 0; i < fileNames.length; i++) {
         files.put(fileNames[i], contents[i]);
      }
      getCache().putAll(files);
   }

   @ManagedAttribute(description = "The names of all Protobuf files", displayName = "Protofile Names")
   @Override
   public String[] getProtofileNames() {
      Set<String> fileNames = new HashSet<String>();
      for (String k : getCache().keySet()) {
         if (k.endsWith(PROTO_KEY_SUFFIX)) {
            fileNames.add(k);
         }
      }
      return fileNames.toArray(new String[fileNames.size()]);
   }

   @ManagedOperation(description = "Get the contents of a protobuf definition file", displayName = "Get Protofile")
   @Override
   public String getProtofile(@Parameter(name = "fileName", description = "the name of the .proto file") String fileName) {
      if (!fileName.endsWith(PROTO_KEY_SUFFIX)) {
         throw new IllegalArgumentException("The file name must have \".proto\" suffix");
      }
      return getCache().get(fileName);
   }

   @ManagedAttribute(description = "The names of the files that have errors, if any", displayName = "Files With Errors")
   @Override
   public String[] getFilesWithErrors() {
      String filesWithErrors = getCache().get(ERRORS_KEY_SUFFIX);
      if (filesWithErrors == null) {
         return null;
      }
      return filesWithErrors.split("\n");
   }

   @ManagedOperation(description = "Obtains the errors associated with a protobuf definition file", displayName = "Get Errors For A File")
   @Override
   public String getFileErrors(@Parameter(name = "fileName", description = "the name of the .proto file") String fileName) {
      if (!fileName.endsWith(PROTO_KEY_SUFFIX)) {
         throw new IllegalArgumentException("The file name must have \".proto\" suffix");
      }
      return getCache().get(fileName + ERRORS_KEY_SUFFIX);
   }

   SerializationContext getSerializationContext() {
      return serCtx;
   }

   /**
    * Obtains the ProtobufMetadataManagerImpl instance associated to a cache manager.
    */
   private static ProtobufMetadataManagerImpl getProtobufMetadataManager(EmbeddedCacheManager cacheManager) {
      if (cacheManager == null) {
         throw new IllegalArgumentException("cacheManager cannot be null");
      }
      ProtobufMetadataManagerImpl metadataManager = (ProtobufMetadataManagerImpl) cacheManager.getGlobalComponentRegistry().getComponent(ProtobufMetadataManager.class);
      if (metadataManager == null) {
         throw new IllegalStateException("ProtobufMetadataManager not initialised yet!");
      }
      return metadataManager;
   }

   public static SerializationContext getSerializationContext(EmbeddedCacheManager cacheManager) {
      return new DelegatingSerializationContext(getProtobufMetadataManager(cacheManager));
   }

   public static SerializationContext getSerializationContextInternal(EmbeddedCacheManager cacheManager) {
      return getProtobufMetadataManager(cacheManager).getSerializationContext();
   }
}
