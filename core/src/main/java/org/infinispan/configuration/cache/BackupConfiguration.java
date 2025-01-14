package org.infinispan.configuration.cache;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.ConfigurationElement;
import org.infinispan.configuration.parsing.Element;

/**
 * @author Mircea.Markus@jboss.com
 * @since 5.2
 */
public class BackupConfiguration extends ConfigurationElement<BackupConfiguration> {
   public static final AttributeDefinition<String> SITE = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.SITE, null, String.class).autoPersist(false).immutable().build();
   public static final AttributeDefinition<BackupConfiguration.BackupStrategy> STRATEGY = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.STRATEGY, BackupConfiguration.BackupStrategy.ASYNC).immutable().build();
   public static final AttributeDefinition<Long> REPLICATION_TIMEOUT = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.TIMEOUT, 15000L).build();
   public static final AttributeDefinition<BackupFailurePolicy> FAILURE_POLICY = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.BACKUP_FAILURE_POLICY, BackupFailurePolicy.WARN).build();
   public static final AttributeDefinition<String> FAILURE_POLICY_CLASS = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.FAILURE_POLICY_CLASS, null, String.class).immutable().build();
   public static final AttributeDefinition<Boolean> USE_TWO_PHASE_COMMIT = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.USE_TWO_PHASE_COMMIT, false).immutable().build();
   public static final AttributeDefinition<Boolean> ENABLED = AttributeDefinition.builder(org.infinispan.configuration.parsing.Attribute.ENABLED, true).immutable().build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(BackupConfiguration.class, SITE, STRATEGY, REPLICATION_TIMEOUT, FAILURE_POLICY,  FAILURE_POLICY_CLASS, USE_TWO_PHASE_COMMIT, ENABLED);
   }

   private final Attribute<String> site;
   private final Attribute<BackupConfiguration.BackupStrategy> strategy;
   private final Attribute<Long> replicationTimeout;
   private final Attribute<BackupFailurePolicy> backupFailurePolicy;
   private final Attribute<String> failurePolicyClass;
   private final Attribute<Boolean> useTwoPhaseCommit;
   private final Attribute<Boolean> enabled;
   private final AttributeSet attributes;
   private final TakeOfflineConfiguration takeOfflineConfiguration;
   private final XSiteStateTransferConfiguration xSiteStateTransferConfiguration ;

   public BackupConfiguration(AttributeSet attributes, TakeOfflineConfiguration takeOfflineConfiguration, XSiteStateTransferConfiguration xSiteStateTransferConfiguration) {
      super(Element.BACKUP, attributes, takeOfflineConfiguration, xSiteStateTransferConfiguration);
      this.attributes = attributes.checkProtection();
      this.takeOfflineConfiguration = takeOfflineConfiguration;
      this.xSiteStateTransferConfiguration = xSiteStateTransferConfiguration;
      this.site = attributes.attribute(SITE);
      this.strategy = attributes.attribute(STRATEGY);
      this.replicationTimeout = attributes.attribute(REPLICATION_TIMEOUT);
      this.backupFailurePolicy = attributes.attribute(FAILURE_POLICY);
      this.failurePolicyClass = attributes.attribute(FAILURE_POLICY_CLASS);
      this.useTwoPhaseCommit = attributes.attribute(USE_TWO_PHASE_COMMIT);
      this.enabled = attributes.attribute(ENABLED);
   }

   /**
    * Returns the name of the site where this cache backups its data.
    */
   public String site() {
      return site.get();
   }

   /**
    * How does the backup happen: sync or async.
    */
   public BackupStrategy strategy() {
      return strategy.get();
   }

   public TakeOfflineConfiguration takeOffline() {
      return takeOfflineConfiguration;
   }

   /**
    * If the failure policy is set to {@link BackupFailurePolicy#CUSTOM} then the failurePolicyClass is required and
    * should return the fully qualified name of a class implementing {@link CustomFailurePolicy}
    */
   public String failurePolicyClass() {
      return failurePolicyClass.get();
   }

   public boolean isAsyncBackup() {
      return strategy() == BackupStrategy.ASYNC;
   }

   public boolean isSyncBackup() {
      return strategy() == BackupStrategy.SYNC;
   }

   public long replicationTimeout() {
      return replicationTimeout.get();
   }

   public BackupConfiguration replicationTimeout(long timeout) {
      replicationTimeout.set(timeout);
      return this;
   }

   public BackupFailurePolicy backupFailurePolicy() {
      return backupFailurePolicy.get();
   }

   public enum BackupStrategy {
      SYNC, ASYNC
   }

   public boolean isTwoPhaseCommit() {
      return useTwoPhaseCommit.get();
   }

   /**
    * @see BackupConfigurationBuilder#enabled(boolean).
    */
   public boolean enabled() {
      return enabled.get();
   }

   public XSiteStateTransferConfiguration stateTransfer() {
      return xSiteStateTransferConfiguration;
   }
}
