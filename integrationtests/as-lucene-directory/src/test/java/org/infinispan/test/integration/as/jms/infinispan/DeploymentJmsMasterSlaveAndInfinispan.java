package org.infinispan.test.integration.as.jms.infinispan;

import org.infinispan.test.integration.as.jms.controller.RegistrationController;
import org.infinispan.test.integration.as.jms.controller.RegistrationMdb;
import org.infinispan.test.integration.as.jms.infinispan.controller.MembersCache;
import org.infinispan.test.integration.as.jms.model.RegisteredMember;
import org.infinispan.test.integration.as.jms.util.RegistrationConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Create deployments for JMS Master/Slave configuration integration tests storing the index in Infinispan. Make sure to
 * test for a secured JMS environment.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public final class DeploymentJmsMasterSlaveAndInfinispan {

   private DeploymentJmsMasterSlaveAndInfinispan() {
      //not allowed
   }

   public static Archive<?> createMaster(String deploymentName) throws Exception {
      return baseArchive(deploymentName, masterPersistenceXml(deploymentName))
            .addClass(RegistrationMdb.class)
            .addAsWebInfResource(activeMqJmsXml(), "activemq-jms.xml")
            ;
   }

   public static Archive<?> createSlave(String deploymentName) throws Exception {
      return baseArchive(deploymentName, slavePersistenceXml(deploymentName));
   }

   private static WebArchive baseArchive(String name, PersistenceDescriptor unitDef) throws Exception {
      WebArchive webArchive = ShrinkWrap
            .create(WebArchive.class, name + ".war")
            .addClasses(RegistrationController.class, RegisteredMember.class, RegistrationConfiguration.class, SearchNewEntityJmsMasterSlaveAndInfinispan.class, MembersCache.class)
            .addAsResource(new StringAsset(unitDef.exportAsString()), "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      return webArchive;
   }

   private static PersistenceDescriptor masterPersistenceXml(String name)
         throws Exception {
      return commonUnitDef(name).up().up();
   }

   private static PersistenceDescriptor slavePersistenceXml(String name)
         throws Exception {
      return commonUnitDef(name)
            .createProperty()
            .name("hibernate.search.default.worker.backend")
            .value("jms")
            .up()
                  //We could use a Local ConnectionFactory but then we would bypass the authentication:
                  //we actually want to verify we're able to authenticate
            .createProperty()
            .name("hibernate.search.default.worker.jms.connection_factory")
            .value("jboss/exported/jms/RemoteConnectionFactory")
            .up()
            .createProperty()
            .name("hibernate.search.default.worker.jms.queue")
            .value(RegistrationConfiguration.DESTINATION_QUEUE)
            .up()
                  //Authentication credentials are specified in the AS7 configuration files
                  //See properties files in server/standalone/configuration
            .createProperty()
            .name("hibernate.search.default.worker.jms.login")
            .value("guest")
            .up()
            .createProperty()
            .name("hibernate.search.default.worker.jms.password")
            .value("password")
            .up()
            .up()
            .up();
   }

   private static Properties<PersistenceUnit<PersistenceDescriptor>> commonUnitDef(String name) throws Exception {
      return Descriptors.create(PersistenceDescriptor.class)
            .createPersistenceUnit()
            .name("pu-" + name)
            .jtaDataSource("java:jboss/datasources/ExampleDS")
            .getOrCreateProperties()
            .createProperty()
            .name("hibernate.hbm2ddl.auto")
            .value("create-drop")
            .up()
            .createProperty()
            .name("hibernate.search.default.lucene_version")
            .value("LUCENE_CURRENT")
            .up()
            .createProperty()
            .name("hibernate.search.default.directory_provider")
            .value("infinispan")
            .up()
            .createProperty()
            .name("hibernate.search.default.worker.execution")
            .value("sync")
            .up();
   }

   private static Asset activeMqJmsXml() {
      String activeMqXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                  + "<messaging-deployment xmlns=\"urn:jboss:messaging-activemq-deployment:1.0\">"
                  + "<server>"
                  + "<jms-destinations>"
                  + "<jms-queue name=\"hsearchQueue\">"
                  + "<entry name=\"" + RegistrationConfiguration.DESTINATION_QUEUE + "\"/>"
                  + "</jms-queue>"
                  + "</jms-destinations>"
                  + "</server>"
                  + "</messaging-deployment>";
      return new StringAsset(activeMqXml);
   }
}
