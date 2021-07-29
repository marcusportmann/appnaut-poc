/*
 * Copyright 2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appnaut.jta.agroal;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import io.agroal.api.transaction.TransactionAware;
import io.appnaut.jta.util.TransactionUtil;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

public class NarayanaTransactionIntegration implements
    io.agroal.api.transaction.TransactionIntegration {

  private static final ConcurrentMap<ResourceRecoveryFactory, XAResourceRecoveryHelperImpl> xaResourceRecoveryHelperImplCache = new ConcurrentHashMap<>();

  /**
   * The name of the data source the transaction integration is associated with.
   */
  private final String dataSourceName;

  // In order to construct a UID that is globally unique, simply pair a UID with an InetAddress.
  private final UUID key = UUID.randomUUID();

  private final RecoveryManager recoveryManager;

  private final TransactionManager transactionManager;

  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  public NarayanaTransactionIntegration(String dataSourceName,
      TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
    this(dataSourceName, transactionManager, transactionSynchronizationRegistry, null);
  }

  public NarayanaTransactionIntegration(String dataSourceName,
      TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry,
      RecoveryManager recoveryManager) {
    this.dataSourceName = dataSourceName;
    this.transactionManager = transactionManager;
    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    this.recoveryManager = recoveryManager;
  }

  @Override
  public void addResourceRecoveryFactory(ResourceRecoveryFactory resourceRecoveryFactory) {

    XARecoveryModule xaRecoveryModule = null;
    for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
      if (recoveryModule instanceof XARecoveryModule) {
        xaRecoveryModule = (XARecoveryModule) recoveryModule;
        break;
      }
    }

    if (xaRecoveryModule == null) {
      throw new IllegalStateException("Failed to retrieve the XARecoveryModule");
    }

    xaRecoveryModule.addXAResourceRecoveryHelper(
        xaResourceRecoveryHelperImplCache.computeIfAbsent(resourceRecoveryFactory, rrf -> {
          try {
            return new XAResourceRecoveryHelperImpl(rrf.getRecoveryConnection()
                .getXAResource());
          } catch (Throwable e) {
            throw new RuntimeException(
                "Failed to add the resource recovery factory to the XA recovery module", e);
          }
        }));
  }

  @Override
  public void associate(TransactionAware transactionAware, XAResource xaResource)
      throws SQLException {
    try {
      if (transactionRunning()) {
        if (transactionSynchronizationRegistry.getResource(key) == null) {
          transactionSynchronizationRegistry
              .registerInterposedSynchronization(new InterposedSynchronization(transactionAware));
          transactionSynchronizationRegistry.putResource(key, transactionAware);

          XAResource xaResourceToEnlist;
          if (xaResource != null) {
            xaResourceToEnlist = new TransactionAwareXAResource(dataSourceName, transactionAware,
                xaResource);
          } else {
            xaResourceToEnlist = new LocalXAResource(dataSourceName, transactionAware);
          }
          transactionManager.getTransaction().enlistResource(xaResourceToEnlist);
        } else {
          transactionAware.transactionStart();
        }
      }
      transactionAware.transactionCheckCallback(this::transactionRunning);
    } catch (Exception e) {
      throw new SQLException("Failed to associate the connection with an existing transaction", e);
    }
  }

  @Override
  public boolean disassociate(TransactionAware transactionAware) throws SQLException {
    if (transactionRunning()) {
      transactionSynchronizationRegistry.putResource(key, null);
    }

    return true;
  }

  @Override
  public TransactionAware getTransactionAware() throws SQLException {
    if (transactionRunning()) {
      return (TransactionAware) transactionSynchronizationRegistry.getResource(key);
    }
    return null;
  }

  @Override
  public void removeResourceRecoveryFactory(ResourceRecoveryFactory resourceRecoveryFactory) {

    XARecoveryModule xaRecoveryModule = null;
    for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
      if (recoveryModule instanceof XARecoveryModule) {
        xaRecoveryModule = (XARecoveryModule) recoveryModule;
        break;
      }
    }

    if (xaRecoveryModule == null) {
      throw new IllegalStateException("Failed to retrieve the XARecoveryModule");
    }

    try {
      xaRecoveryModule.removeXAResourceRecoveryHelper(xaResourceRecoveryHelperImplCache.remove(resourceRecoveryFactory));
    } catch (Throwable e) {
      throw new RuntimeException("Failed to remove the resource recovery factory from the XA recovery module", e);
    }
  }

  private boolean transactionRunning() throws SQLException {
    return TransactionUtil.transactionExists(transactionManager);
  }

  private static class InterposedSynchronization implements Synchronization {

    private final TransactionAware transactionAware;

    InterposedSynchronization(TransactionAware transactionAware) {
      this.transactionAware = transactionAware;
    }

    public void afterCompletion(int status) {
      try {
        transactionAware.transactionEnd();
      } catch (Throwable ignored) {
      }
    }

    public void beforeCompletion() {
    }
  }

  /**
   * The <b>XAResourceRecoveryImpl</b> class provides an implementation of the the
   * XAResourceRecoveryHelper interface.
   *
   * @author Marcus Portmann
   */
  private static class XAResourceRecoveryHelperImpl implements XAResourceRecoveryHelper {

    private final XAResource[] xaResources;

    public XAResourceRecoveryHelperImpl(XAResource[] xaResources) {
      this.xaResources = xaResources;
    }

    public XAResourceRecoveryHelperImpl(XAResource xaResource) {
      this.xaResources = new XAResource[1];
      this.xaResources[0] = xaResource;
    }

    @Override
    public XAResource[] getXAResources() {
      return xaResources;
    }

    @Override
    public boolean initialise(String p) throws Exception {
      return true;
    }
  }
}

//
////
////package io.appnaut.agroal;
////
////    import io.agroal.api.transaction.TransactionAware;
////    import io.appnaut.transaction.RecoveryXAResourceRegistry;
////    import io.appnaut.transaction.RecoveryXAResources;
////    import java.sql.SQLException;
////    import java.util.UUID;
////    import java.util.concurrent.ConcurrentHashMap;
////    import java.util.concurrent.ConcurrentMap;
////    import javax.sql.XAConnection;
////    import javax.transaction.Status;
////    import javax.transaction.Synchronization;
////    import javax.transaction.Transaction;
////    import javax.transaction.TransactionManager;
////    import javax.transaction.TransactionSynchronizationRegistry;
////    import javax.transaction.xa.XAResource;
////    import io.agroal.api.transaction.TransactionIntegration;
////
////public class NarayanaTransactionIntegration implements TransactionIntegration {
////
////  // Use this cache as method references are not stable (they are used as bridge between RecoveryConnectionFactory and RecoveryXAResourceRegistry)
////  private static final ConcurrentMap<ResourceRecoveryFactory, RecoveryXAResources> resourceRecoveryFactoryCache = new ConcurrentHashMap<>();
////
////  private final String dataSourceName;
////
////  // In order to construct a UID that is globally unique, simply pair a UID with an InetAddress.
////  private final UUID key = UUID.randomUUID();
////
////  private final RecoveryXAResourceRegistry recoveryXAResourceRegistry;
////
////  private final TransactionManager transactionManager;
////
////  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
////
////  public NarayanaTransactionIntegration(String dataSourceName, TransactionManager transactionManager,
////      TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
////    this(dataSourceName, transactionManager, transactionSynchronizationRegistry, null);
////  }
////
////  public NarayanaTransactionIntegration(String dataSourceName, TransactionManager transactionManager,
////      TransactionSynchronizationRegistry transactionSynchronizationRegistry,
////      RecoveryXAResourceRegistry recoveryXAResourceRegistry) {
////    this.dataSourceName = dataSourceName;
////    this.transactionManager = transactionManager;
////    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
////    this.recoveryXAResourceRegistry = recoveryXAResourceRegistry;
////  }
////
////
////  @Override
////  public void addResourceRecoveryFactory(ResourceRecoveryFactory factory) {
////    if (recoveryXAResourceRegistry != null) {
////      recoveryXAResourceRegistry.addRecoveryXAResources(resourceRecoveryFactoryCache
////          .computeIfAbsent(factory, f -> new AgroalRecoveryXAResources(dataSourceName, f)));
////    }
////  }
////
////  @Override
////  public void associate(TransactionAware transactionAware, XAResource xaResource)
////      throws SQLException {
//
////  }
////
////  @Override
////  public boolean disassociate(TransactionAware transactionAware) throws SQLException {
////  }
////
////  @Override
////  public TransactionAware getTransactionAware() throws SQLException {
////  }
////
////  @Override
////  public void removeResourceRecoveryFactory(ResourceRecoveryFactory factory) {
////    if (recoveryXAResourceRegistry != null) {
////      recoveryXAResourceRegistry
////          .removeRecoveryXAResources(resourceRecoveryFactoryCache.remove(factory));
////    }
////  }
////
//
////
////  // This auxiliary class is a contraption due to the fact that XAResource is not closable.
////  // It creates RecoveryXAResource wrappers that keeps track of lifecycle and closes the associated connection.
////  private static class AgroalRecoveryXAResources implements RecoveryXAResources {
////
////    private static final XAResource[] EMPTY_RESOURCES = new XAResource[0];
////
////    private final String dataSourceName;
////
////    private final ResourceRecoveryFactory resourceRecoveryFactory;
////
////    @SuppressWarnings("WeakerAccess")
////    AgroalRecoveryXAResources(String dataSourceName, ResourceRecoveryFactory factory) {
////      this.dataSourceName = dataSourceName;
////      this.resourceRecoveryFactory = factory;
////    }
////
////    @Override
////    @SuppressWarnings("resource")
////    public XAResource[] getXAResources() {
////      XAConnection xaConnection = resourceRecoveryFactory.getRecoveryConnection();
////      try {
////        return xaConnection == null ? EMPTY_RESOURCES
////            : new XAResource[]{new RecoveryXAResource(dataSourceName, xaConnection)};
////      } catch (SQLException e) {
////        return new XAResource[]{new ErrorConditionXAResource(dataSourceName, xaConnection, e)};
////      }
////    }
////  }
////
////  private static class InterposedSynchronization implements Synchronization {
////
////    private final TransactionAware transactionAware;
////
////    InterposedSynchronization(TransactionAware transactionAware) {
////      this.transactionAware = transactionAware;
////    }
////
////    public void afterCompletion(int status) {
////      try {
////        this.transactionAware.transactionEnd();
////      } catch (Throwable ignored) {
////      }
////    }
////
////    public void beforeCompletion() {
////    }
////  }
////}
