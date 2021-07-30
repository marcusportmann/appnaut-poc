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

package io.appnaut.jta;

import io.appnaut.jta.util.TransactionUtil;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.TransactionDefinition.Isolation;
import io.micronaut.transaction.exceptions.CannotCreateTransactionException;
import io.micronaut.transaction.exceptions.IllegalTransactionStateException;
import io.micronaut.transaction.exceptions.NestedTransactionNotSupportedException;
import io.micronaut.transaction.exceptions.NoTransactionException;
import io.micronaut.transaction.exceptions.TransactionException;
import io.micronaut.transaction.exceptions.TransactionSystemException;
import io.micronaut.transaction.exceptions.UnexpectedRollbackException;
import io.micronaut.transaction.hibernate5.HibernateTransactionManager;
import io.micronaut.transaction.jdbc.DelegatingDataSource;
import io.micronaut.transaction.support.AbstractSynchronousTransactionManager;
import io.micronaut.transaction.support.DefaultTransactionStatus;
import io.micronaut.transaction.support.SmartTransactionObject;
import io.micronaut.transaction.support.TransactionSynchronization;
import io.micronaut.transaction.support.TransactionSynchronizationUtils;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

/**
 * The <b>JtaTransactionManager</b> class provides a {@link io.micronaut.transaction.SynchronousTransactionManager}
 * implementation that uses a JTA transaction manager.
 *
 * @author Marcus Portmann
 */
@EachBean(DataSource.class)
@Replaces(HibernateTransactionManager.class)
@TypeHint(JtaTransactionManager.class)
@SuppressWarnings("unused")
public class JtaTransactionManager extends
    AbstractSynchronousTransactionManager<Connection> {

  private final DataSource dataSource;

  private final TransactionManager transactionManager;

  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  private final UserTransaction userTransaction;

  public JtaTransactionManager(TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry,
      UserTransaction userTransaction, DataSource dataSource) {
    setNestedTransactionAllowed(true);
    setFailEarlyOnGlobalRollbackOnly(true);

    this.dataSource = DelegatingDataSource.unwrapDataSource(dataSource);
    this.transactionManager = transactionManager;
    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    this.userTransaction = userTransaction;
  }

  @Override
  public Connection getConnection(Object transaction) {
    return getConnection();
  }

  @Override
  @NonNull
  public Connection getConnection() {
    if (TransactionUtil.transactionExists(transactionManager)) {
      try {
        return dataSource.getConnection();
      } catch (Throwable e) {
        throw new TransactionSystemException(
            "Failed to retrieve the connection from data source associated with the JTA transaction manager",
            e);
      }
    } else {
      throw new TransactionSystemException(
          "Failed to retrieve the connection from data source associated with the JTA transaction manager without an existing transaction");
    }
  }

  /**
   * Returns the JTA TransactionManager that this transaction manager uses.
   *
   * @return the JTA TransactionManager that this transaction manager uses
   */
  public TransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition)
      throws TransactionException {
    if (definition.getIsolationLevel() != Isolation.DEFAULT) {
      throw new CannotCreateTransactionException(
          "The NarayanaTransactionManager does not support custom isolation levels");
    }

    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
    try {
      Duration timeout = determineTimeout(definition);

      if (timeout.compareTo(TransactionDefinition.TIMEOUT_DEFAULT) > 0) {
        jtaTransactionObject.getUserTransaction()
            .setTransactionTimeout((int) timeout.toSeconds());
        if (timeout.toMillis() > 0) {
          jtaTransactionObject.resetTransactionTimeout = true;
        }
      }

      jtaTransactionObject.getUserTransaction().begin();
    } catch (NotSupportedException | UnsupportedOperationException e) {
      // TODO: CHECK THIS, JTA DOES SUPPORT NESTED TRANSACTIONS??? -- MARCUS
      throw new NestedTransactionNotSupportedException(
          "The NarayanaTransactionManager does not support nested transactions", e);
    } catch (Throwable e) {
      throw new CannotCreateTransactionException("Failed to begin the JTA transaction", e);
    }
  }

  @Override
  protected void doCleanupAfterCompletion(Object transaction) {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
    if (jtaTransactionObject.resetTransactionTimeout) {
      try {
        jtaTransactionObject.getUserTransaction().setTransactionTimeout(0);
      } catch (Throwable e) {
        logger.debug("Failed to reset transaction timeout after completing the JTA transaction", e);
      }
    }
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
        .getTransaction();
    try {
      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();

      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        // Should never happen... would have thrown an exception before
        // and as a consequence led to a rollback, not to a commit call.
        // In any case, the transaction is already fully cleaned up.
        throw new NoTransactionException("No JTA transaction found");
      } else if (jtaStatus == Status.STATUS_ROLLEDBACK) {
        // Only really happens on JBoss 4.2 in case of an early timeout...
        // Explicit rollback call necessary to clean up the transaction.
        // IllegalStateException expected on JBoss; call still necessary.
        try {
          jtaTransactionObject.getUserTransaction().rollback();
        } catch (IllegalStateException e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Rollback failure with transaction already marked as rolled back: " + e);
          }
        }
        throw new UnexpectedRollbackException("JTA transaction already rolled back");
      }
      jtaTransactionObject.getUserTransaction().commit();
    } catch (NoTransactionException | UnexpectedRollbackException e) {
      throw e;
    } catch (RollbackException e) {
      throw new UnexpectedRollbackException(
          "JTA transaction unexpectedly rolled back", e);
    } catch (HeuristicMixedException e) {
      throw new TransactionSystemException(
          "Failed to fully commit the JTA transaction as a result of a heuristic decision", e);
      //throw new HeuristicCompletionException(HeuristicCompletionException.State.MIXED, e);
    } catch (HeuristicRollbackException e) {
      throw new TransactionSystemException(
          "The JTA transaction was rolled back as a result of a heuristic decision", e);
      //throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, e);
    } catch (IllegalStateException e) {
      throw new TransactionSystemException(
          "Failed to commit the JTA transaction as a result of an unexpected internal transaction state",
          e);
    } catch (Throwable e) {
      throw new TransactionSystemException("Failed to commit the JTA transaction", e);
    }
  }

  @Override
  protected Object doGetTransaction() throws TransactionException {
    return new JtaTransactionObject(userTransaction);
  }

  @Override
  protected void doResume(Object transaction, Object suspendedResources) {
    try {
      transactionManager.resume(((Transaction) suspendedResources));
    } catch (InvalidTransactionException e) {
      throw new IllegalTransactionStateException("Tried to resume the invalid JTA transaction", e);
    } catch (IllegalStateException e) {
      throw new TransactionSystemException(
          "Failed to resume the JTA transaction as a result of an unexpected internal transaction state",
          e);
    } catch (Throwable e) {
      throw new TransactionSystemException("Failed to resume the JTA transaction", e);
    }
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
        .getTransaction();
    try {
      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();

      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        throw new NoTransactionException("No JTA transaction found");
      }

      jtaTransactionObject.getUserTransaction().rollback();
    } catch (NoTransactionException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw new TransactionSystemException(
          "Failed to rollback the JTA transaction as a result of an unexpected internal transaction state",
          e);
    } catch (Throwable e) {
      throw new TransactionSystemException("Failed to rollback the JTA transaction", e);
    }
  }

  @Override
  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
        .getTransaction();
    if (status.isDebug()) {
      logger.debug("Setting JTA transaction rollback-only");
    }
    try {
      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();

      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        throw new NoTransactionException("No JTA transaction found");
      } else //noinspection StatementWithEmptyBody
        if (jtaStatus == Status.STATUS_ROLLEDBACK) {
          // Do nothing, transaction already rolled back
        } else {
          jtaTransactionObject.getUserTransaction().setRollbackOnly();
        }
    } catch (NoTransactionException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw new TransactionSystemException(
          "Failed to flag the JTA transaction for rollback only as a result of an unexpected internal transaction state",
          e);
    } catch (Throwable e) {
      throw new TransactionSystemException("Failed to flag the JTA transaction for rollback only",
          e);
    }
  }

  @Override
  protected Object doSuspend(Object transaction) {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
    try {
      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();

      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        throw new NoTransactionException("No JTA transaction found");
      }

      return transactionManager.suspend();
    } catch (NoTransactionException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw new TransactionSystemException(
          "Failed to suspend the JTA transaction as a result of an unexpected internal transaction state",
          e);
    } catch (Throwable e) {
      throw new TransactionSystemException("Failed to suspend the JTA transaction", e);
    }
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    return TransactionUtil.transactionExists(transactionManager);
  }

  @Override
  protected void registerAfterCompletionWithExistingTransaction(
      Object transaction, List<TransactionSynchronization> synchronizations) {
    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;

    try {
      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();

      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
        throw new NoTransactionException("No JTA transaction found");
      } else if (jtaStatus == Status.STATUS_ROLLEDBACK) {
        throw new RollbackException("JTA transaction already rolled back");
      }

      this.transactionSynchronizationRegistry.registerInterposedSynchronization(
          new AfterCompletionSynchronization(synchronizations));
    } catch (SystemException e) {
      throw new TransactionSystemException(
          "Failed to register the interposed synchronization with the JTA TransactionSynchronizationRegistry",
          e);
    } catch (Throwable e) {
      // NOTE: JBoss throws plain RuntimeException with RollbackException as cause.
      if (e instanceof NoTransactionException) {
        logger.warn("No JTA transaction found: " +
            "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
            +
            "Immediately invoking after-completion transaction synchronizations with outcome status 'unknown': "
            + e);
        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.UNKNOWN);
      }
      if (e instanceof RollbackException || e.getCause() instanceof RollbackException) {
        logger
            .warn("Participating in existing JTA transaction that has been marked for rollback: " +
                "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
                +
                "Immediately invoking after-completion transaction synchronizations with outcome status 'rollback': "
                +
                e);
        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.ROLLED_BACK);
      } else {
        logger.warn(
            "Unexpected internal transaction state encountered for the existing JTA transaction: " +
                "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
                +
                "Immediately invoking after-completion transaction synchronizations with outcome status 'unknown': "
                +
                e);
        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.UNKNOWN);
      }
    }
  }

  /**
   * Returns true to indicate that a JTA commit will properly handle transactions that have been
   * marked rollback-only at a global level.
   *
   * @return true to indicate that a JTA commit will properly handle transactions that have been
   * marked rollback-only at a global level
   */
  @Override
  protected boolean shouldCommitOnGlobalRollbackOnly() {
    return true;
  }

  /**
   * Returns false to cause a further invocation of doBegin despite an already existing
   * transaction.
   * <p>JTA implementations might support nested transactions via further
   * {@code UserTransaction.begin()} invocations, but never support savepoints.
   *
   * @see #doBegin
   * @see javax.transaction.UserTransaction#begin()
   */
  @Override
  protected boolean useSavepointForNestedTransaction() {
    return false;
  }

  /**
   * The <b>AfterCompletionSynchronization</b> class holds the Micronaut transaction synchronization
   * callbacks that should be invoked when a JTA transaction completes.
   *
   * @author Marcus Portmann
   */
  private static class AfterCompletionSynchronization implements Synchronization {

    private final List<TransactionSynchronization> synchronizations;

    /**
     * Constructs a new <b>JtaAfterCompletionSynchronization</b>.
     *
     * @param synchronizations the transaction synchronization callbacks that should be invoked when
     *                         the JTA transaction completes
     */
    public AfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
      this.synchronizations = synchronizations;
    }

    @Override
    public void afterCompletion(int status) {
      switch (status) {
        case Status.STATUS_COMMITTED:
          try {
            TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
          } finally {
            TransactionSynchronizationUtils.invokeAfterCompletion(
                this.synchronizations, TransactionSynchronization.Status.COMMITTED);
          }
          break;
        case Status.STATUS_ROLLEDBACK:
          TransactionSynchronizationUtils.invokeAfterCompletion(
              this.synchronizations, TransactionSynchronization.Status.ROLLED_BACK);
          break;
        default:
          TransactionSynchronizationUtils.invokeAfterCompletion(
              this.synchronizations, TransactionSynchronization.Status.UNKNOWN);
      }
    }

    @Override
    public void beforeCompletion() {
    }
  }

  /**
   * The <b>JtaTransactionObject</b> class provides a SmartTransactionObject implementation that
   * wraps a JTA UserTransaction. It is used as a transaction object by the JtaTransactionManager.
   *
   * @author Marcus Portmann
   */
  private static class JtaTransactionObject implements SmartTransactionObject {

    private final UserTransaction userTransaction;

    boolean resetTransactionTimeout = false;

    /**
     * Constructs a new <b>JtaTransactionObject</b>.
     *
     * @param userTransaction the JTA UserTransaction
     */
    public JtaTransactionObject(UserTransaction userTransaction) {
      this.userTransaction = userTransaction;
    }

    @Override
    public void flush() {
    }

    /**
     * Returns the JTA UserTransaction associated with the transaction object.
     *
     * @return the JTA UserTransaction associated with the transaction object
     */
    public UserTransaction getUserTransaction() {
      return userTransaction;
    }

    @Override
    public boolean isRollbackOnly() {
      try {
        int jtaStatus = this.userTransaction.getStatus();
        return (jtaStatus == Status.STATUS_MARKED_ROLLBACK
            || jtaStatus == Status.STATUS_ROLLEDBACK);
      } catch (SystemException ex) {
        throw new TransactionSystemException(
            "Failed to retrieve the status of the JTA UserTransaction", ex);
      }
    }
  }
}

//
//
//
//
//
//
///*
// * Copyright 2021 original authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.appnaut.jta.narayana;
//
//    import io.micronaut.context.annotation.EachBean;
//    import io.micronaut.context.annotation.Replaces;
//    import io.micronaut.core.annotation.TypeHint;
//    import io.micronaut.transaction.TransactionDefinition;
//    import io.micronaut.transaction.TransactionDefinition.Isolation;
//    import io.micronaut.transaction.exceptions.CannotCreateTransactionException;
//    import io.micronaut.transaction.exceptions.IllegalTransactionStateException;
//    import io.micronaut.transaction.exceptions.NestedTransactionNotSupportedException;
//    import io.micronaut.transaction.exceptions.NoTransactionException;
//    import io.micronaut.transaction.exceptions.TransactionException;
//    import io.micronaut.transaction.exceptions.TransactionSystemException;
//    import io.micronaut.transaction.exceptions.UnexpectedRollbackException;
//    import io.micronaut.transaction.hibernate5.HibernateTransactionManager;
//    import io.micronaut.transaction.jdbc.ConnectionHolder;
//    import io.micronaut.transaction.jdbc.DataSourceUtils;
//    import io.micronaut.transaction.jdbc.DelegatingDataSource;
//    import io.micronaut.transaction.jdbc.exceptions.CannotGetJdbcConnectionException;
//    import io.micronaut.transaction.support.AbstractSynchronousTransactionManager;
//    import io.micronaut.transaction.support.DefaultTransactionStatus;
//    import io.micronaut.transaction.support.SmartTransactionObject;
//    import io.micronaut.transaction.support.TransactionSynchronization;
//    import io.micronaut.transaction.support.TransactionSynchronizationManager;
//    import io.micronaut.transaction.support.TransactionSynchronizationUtils;
//    import java.sql.Connection;
//    import java.time.Duration;
//    import java.util.List;
//    import javax.sql.DataSource;
//    import javax.transaction.HeuristicMixedException;
//    import javax.transaction.HeuristicRollbackException;
//    import javax.transaction.InvalidTransactionException;
//    import javax.transaction.NotSupportedException;
//    import javax.transaction.RollbackException;
//    import javax.transaction.Status;
//    import javax.transaction.Synchronization;
//    import javax.transaction.SystemException;
//    import javax.transaction.Transaction;
//    import javax.transaction.TransactionManager;
//    import javax.transaction.TransactionSynchronizationRegistry;
//    import javax.transaction.UserTransaction;
//
//@EachBean(DataSource.class)
//@Replaces(HibernateTransactionManager.class)
//@TypeHint(JtaTransactionManager.class)
//public class JtaTransactionManager extends
//    AbstractSynchronousTransactionManager<Connection> {
//
//  private final DataSource dataSource;
//
//  private final TransactionManager transactionManager;
//
//  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
//
//  private final UserTransaction userTransaction;
//
//  public JtaTransactionManager(TransactionManager transactionManager,
//      TransactionSynchronizationRegistry transactionSynchronizationRegistry,
//      UserTransaction userTransaction, DataSource dataSource) {
//    setNestedTransactionAllowed(true);
//    setFailEarlyOnGlobalRollbackOnly(true);
//
//    this.dataSource = DelegatingDataSource.unwrapDataSource(dataSource);
//    this.transactionManager = transactionManager;
//    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
//    this.userTransaction = userTransaction;
//  }
//
//  @Override
//  public Connection getConnection(Object transaction) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//
//    if (jtaTransactionObject.hasConnectionHolder()) {
//      return jtaTransactionObject.getConnectionHolder().getConnection();
//    } else {
//      throw new CannotGetJdbcConnectionException("No JDBC Connection available");
//    }
//  }
//
//  @Override
//  public Connection getConnection() {
//    return DataSourceUtils.getConnection(dataSource, false);
//  }
//
//  /**
//   * Returns the JTA TransactionManager that this transaction manager uses.
//   *
//   * @return the JTA TransactionManager that this transaction manager uses
//   */
//  public TransactionManager getTransactionManager() {
//    return this.transactionManager;
//  }
//
//  /**
//   * Returns the JTA TransactionSynchronizationRegistry that this transaction manager uses.
//   *
//   * @return the JTA TransactionSynchronizationRegistry that this transaction manager uses
//   */
//  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
//    return transactionSynchronizationRegistry;
//  }
//
//  /**
//   * Returns the JTA UserTransaction that this transaction manager uses.
//   *
//   * @return the JTA UserTransaction that this transaction manager uses
//   */
//  public UserTransaction getUserTransaction() {
//    return userTransaction;
//  }
//
//  @Override
//  protected void doBegin(Object transaction, TransactionDefinition definition)
//      throws TransactionException {
//    if (definition.getIsolationLevel() != Isolation.DEFAULT) {
//      throw new CannotCreateTransactionException(
//          "The NarayanaTransactionManager does not support custom isolation levels");
//    }
//
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//    try {
//      Duration timeout = determineTimeout(definition);
//
//      if (timeout.compareTo(TransactionDefinition.TIMEOUT_DEFAULT) > 0) {
//        jtaTransactionObject.getUserTransaction()
//            .setTransactionTimeout((int) timeout.toSeconds());
//        if (timeout.toMillis() > 0) {
//          jtaTransactionObject.resetTransactionTimeout = true;
//        }
//      }
//
//      jtaTransactionObject.getUserTransaction().begin();
//    } catch (NotSupportedException | UnsupportedOperationException e) {
//      // TODO: CHECK THIS, JTA DOES SUPPORT NESTED TRANSACTIONS??? -- MARCUS
//      throw new NestedTransactionNotSupportedException(
//          "The NarayanaTransactionManager does not support nested transactions", e);
//    } catch (Throwable e) {
//      throw new CannotCreateTransactionException("Failed to begin the JTA transaction", e);
//    }
//
//    /*
//     * NOTE: The Micronaut DataSourceTransactionManager initializes  the database connection,
//     *       places it inside
//     */
//
//    if (false) {
//      Connection connection = null;
//      try {
//        if (!jtaTransactionObject.hasConnectionHolder()) {
//          connection = dataSource.getConnection();
//
//          ConnectionHolder connectionHolder = new ConnectionHolder(connection, true);
//          connectionHolder.setSynchronizedWithTransaction(true);
//
//          jtaTransactionObject.setConnectionHolder(connectionHolder, true);
//
//          TransactionSynchronizationManager.bindResource(dataSource, connectionHolder);
//        }
//      } catch (Throwable e) {
//        if (jtaTransactionObject.isNewConnectionHolder()) {
//          DataSourceUtils.releaseConnection(connection, dataSource);
//          jtaTransactionObject.setConnectionHolder(null, false);
//        }
//      }
//    }
//  }
//
//  @Override
//  protected void doCleanupAfterCompletion(Object transaction) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//    if (jtaTransactionObject.resetTransactionTimeout) {
//      try {
//        jtaTransactionObject.getUserTransaction().setTransactionTimeout(0);
//      } catch (Throwable e) {
//        logger.debug("Failed to reset transaction timeout after completing the JTA transaction", e);
//      }
//    }
//
//    if (jtaTransactionObject.isNewConnectionHolder()) {
//      TransactionSynchronizationManager.unbindResource(dataSource);
//    }
//
//    if (jtaTransactionObject.hasConnectionHolder()) {
//      jtaTransactionObject.getConnectionHolder().clear();
//      jtaTransactionObject.setConnectionHolder(null, false);
//    }
//  }
//
//  @Override
//  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
//        .getTransaction();
//    try {
//      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();
//
//      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
//        // Should never happen... would have thrown an exception before
//        // and as a consequence led to a rollback, not to a commit call.
//        // In any case, the transaction is already fully cleaned up.
//        throw new NoTransactionException("No JTA transaction found");
//      } else if (jtaStatus == Status.STATUS_ROLLEDBACK) {
//        // Only really happens on JBoss 4.2 in case of an early timeout...
//        // Explicit rollback call necessary to clean up the transaction.
//        // IllegalStateException expected on JBoss; call still necessary.
//        try {
//          jtaTransactionObject.getUserTransaction().rollback();
//        } catch (IllegalStateException e) {
//          if (logger.isDebugEnabled()) {
//            logger.debug("Rollback failure with transaction already marked as rolled back: " + e);
//          }
//        }
//        throw new UnexpectedRollbackException("JTA transaction already rolled back");
//      }
//      jtaTransactionObject.getUserTransaction().commit();
//    } catch (NoTransactionException | UnexpectedRollbackException e) {
//      throw e;
//    } catch (RollbackException e) {
//      throw new UnexpectedRollbackException(
//          "JTA transaction unexpectedly rolled back", e);
//    } catch (HeuristicMixedException e) {
//      throw new TransactionSystemException(
//          "Failed to fully commit the JTA transaction as a result of a heuristic decision", e);
//      //throw new HeuristicCompletionException(HeuristicCompletionException.State.MIXED, e);
//    } catch (HeuristicRollbackException e) {
//      throw new TransactionSystemException(
//          "The JTA transaction was rolled back as a result of a heuristic decision", e);
//      //throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, e);
//    } catch (IllegalStateException e) {
//      throw new TransactionSystemException(
//          "Failed to commit the JTA transaction as a result of an unexpected internal transaction state",
//          e);
//    } catch (Throwable e) {
//      throw new TransactionSystemException("Failed to commit the JTA transaction", e);
//    }
//  }
//
//  @Override
//  protected Object doGetTransaction() throws TransactionException {
//    ConnectionHolder connectionHolder = (ConnectionHolder) TransactionSynchronizationManager
//        .getResource(dataSource);
//
//    return new JtaTransactionObject(userTransaction, connectionHolder, false);
//  }
//
//  @Override
//  protected void doResume(Object transaction, Object suspendedResources) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//    try {
//      transactionManager.resume(((SuspendedResources) suspendedResources).transaction);
//
//      ConnectionHolder connectionHolder = ((SuspendedResources) suspendedResources).connectionHolder;
//
//      if (connectionHolder != null) {
//        jtaTransactionObject.setConnectionHolder(connectionHolder, false);
//
//        TransactionSynchronizationManager.bindResource(dataSource, connectionHolder);
//      }
//    } catch (InvalidTransactionException e) {
//      throw new IllegalTransactionStateException("Tried to resume the invalid JTA transaction", e);
//    } catch (IllegalStateException e) {
//      throw new TransactionSystemException(
//          "Failed to resume the JTA transaction as a result of an unexpected internal transaction state",
//          e);
//    } catch (Throwable e) {
//      throw new TransactionSystemException("Failed to resume the JTA transaction", e);
//    }
//  }
//
//  @Override
//  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
//        .getTransaction();
//    try {
//      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();
//
//      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
//        throw new NoTransactionException("No JTA transaction found");
//      }
//
//      jtaTransactionObject.getUserTransaction().rollback();
//    } catch (NoTransactionException e) {
//      throw e;
//    } catch (IllegalStateException e) {
//      throw new TransactionSystemException(
//          "Failed to rollback the JTA transaction as a result of an unexpected internal transaction state",
//          e);
//    } catch (Throwable e) {
//      throw new TransactionSystemException("Failed to rollback the JTA transaction", e);
//    }
//  }
//
//  @Override
//  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) status
//        .getTransaction();
//    if (status.isDebug()) {
//      logger.debug("Setting JTA transaction rollback-only");
//    }
//    try {
//      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();
//
//      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
//        throw new NoTransactionException("No JTA transaction found");
//      } else if (jtaStatus == Status.STATUS_ROLLEDBACK) {
//        // Do nothing, transaction already rolled back
//      } else {
//        jtaTransactionObject.getUserTransaction().setRollbackOnly();
//      }
//    } catch (NoTransactionException e) {
//      throw e;
//    } catch (IllegalStateException e) {
//      throw new TransactionSystemException(
//          "Failed to flag the JTA transaction for rollback only as a result of an unexpected internal transaction state",
//          e);
//    } catch (Throwable e) {
//      throw new TransactionSystemException("Failed to flag the JTA transaction for rollback only",
//          e);
//    }
//  }
//
//  @Override
//  protected Object doSuspend(Object transaction) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//    try {
//      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();
//
//      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
//        throw new NoTransactionException("No JTA transaction found");
//      }
//
//      jtaTransactionObject.setConnectionHolder(null,false);
//
//      ConnectionHolder connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(dataSource);
//
//      return new SuspendedResources(transactionManager.suspend(), connectionHolder
//      );
//    } catch (NoTransactionException e) {
//      throw e;
//    } catch (IllegalStateException e) {
//      throw new TransactionSystemException(
//          "Failed to suspend the JTA transaction as a result of an unexpected internal transaction state",
//          e);
//    } catch (Throwable e) {
//      throw new TransactionSystemException("Failed to suspend the JTA transaction", e);
//    }
//  }
//
//  @Override
//  protected boolean isExistingTransaction(Object transaction) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//    try {
//      return (jtaTransactionObject.getUserTransaction().getStatus()
//          != Status.STATUS_NO_TRANSACTION);
//    } catch (Throwable e) {
//      throw new TransactionSystemException(
//          "Failed to retrieve the status of the JTA UserTransaction", e);
//    }
//  }
//
//  @Override
//  protected void registerAfterCompletionWithExistingTransaction(
//      Object transaction, List<TransactionSynchronization> synchronizations) {
//    JtaTransactionObject jtaTransactionObject = (JtaTransactionObject) transaction;
//
//    try {
//      int jtaStatus = jtaTransactionObject.getUserTransaction().getStatus();
//
//      if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
//        throw new NoTransactionException("No JTA transaction found");
//      } else if (jtaStatus == Status.STATUS_ROLLEDBACK) {
//        throw new RollbackException("JTA transaction already rolled back");
//      }
//
//      this.transactionSynchronizationRegistry.registerInterposedSynchronization(
//          new AfterCompletionSynchronization(synchronizations));
//    } catch (SystemException e) {
//      throw new TransactionSystemException(
//          "Failed to register the interposed synchronization with the JTA TransactionSynchronizationRegistry",
//          e);
//    } catch (Throwable e) {
//      // NOTE: JBoss throws plain RuntimeException with RollbackException as cause.
//      if (e instanceof NoTransactionException) {
//        logger.warn("No JTA transaction found: " +
//            "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
//            +
//            "Immediately invoking after-completion transaction synchronizations with outcome status 'unknown': "
//            + e);
//        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.UNKNOWN);
//      }
//      if (e instanceof RollbackException || e.getCause() instanceof RollbackException) {
//        logger
//            .warn("Participating in existing JTA transaction that has been marked for rollback: " +
//                "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
//                +
//                "Immediately invoking after-completion transaction synchronizations with outcome status 'rollback': "
//                +
//                e);
//        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.ROLLED_BACK);
//      } else {
//        logger.warn(
//            "Unexpected internal transaction state encountered for the existing JTA transaction: " +
//                "Cannot register the after-completion transaction synchronizations with the outer JTA transaction: "
//                +
//                "Immediately invoking after-completion transaction synchronizations with outcome status 'unknown': "
//                +
//                e);
//        invokeAfterCompletion(synchronizations, TransactionSynchronization.Status.UNKNOWN);
//      }
//    }
//  }
//
//  /**
//   * Returns true to indicate that a JTA commit will properly handle transactions that have been
//   * marked rollback-only at a global level.
//   *
//   * @return true to indicate that a JTA commit will properly handle transactions that have been
//   * marked rollback-only at a global level
//   */
//  @Override
//  protected boolean shouldCommitOnGlobalRollbackOnly() {
//    return true;
//  }
//
//  /**
//   * Returns false to cause a further invocation of doBegin despite an already existing
//   * transaction.
//   * <p>JTA implementations might support nested transactions via further
//   * {@code UserTransaction.begin()} invocations, but never support savepoints.
//   *
//   * @see #doBegin
//   * @see javax.transaction.UserTransaction#begin()
//   */
//  @Override
//  protected boolean useSavepointForNestedTransaction() {
//    return false;
//  }
//
//  /**
//   * The <b>AfterCompletionSynchronization</b> class holds the Micronaut transaction synchronization
//   * callbacks that should be invoked when a JTA transaction completes.
//   *
//   * @author Marcus Portmann
//   */
//  private class AfterCompletionSynchronization implements Synchronization {
//
//    private final List<TransactionSynchronization> synchronizations;
//
//    /**
//     * Constructs a new <b>JtaAfterCompletionSynchronization</b>.
//     *
//     * @param synchronizations the transaction synchronization callbacks that should be invoked when
//     *                         the JTA transaction completes
//     */
//    public AfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
//      this.synchronizations = synchronizations;
//    }
//
//    @Override
//    public void afterCompletion(int status) {
//      switch (status) {
//        case Status.STATUS_COMMITTED:
//          try {
//            TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
//          } finally {
//            TransactionSynchronizationUtils.invokeAfterCompletion(
//                this.synchronizations, TransactionSynchronization.Status.COMMITTED);
//          }
//          break;
//        case Status.STATUS_ROLLEDBACK:
//          TransactionSynchronizationUtils.invokeAfterCompletion(
//              this.synchronizations, TransactionSynchronization.Status.ROLLED_BACK);
//          break;
//        default:
//          TransactionSynchronizationUtils.invokeAfterCompletion(
//              this.synchronizations, TransactionSynchronization.Status.UNKNOWN);
//      }
//    }
//
//    @Override
//    public void beforeCompletion() {
//    }
//  }
//
//  /**
//   * The <b>JtaTransactionObject</b> class provides a SmartTransactionObject implementation that
//   * wraps a JTA UserTransaction. It is used as a transaction object by the JtaTransactionManager.
//   *
//   * @author Marcus Portmann
//   */
//  private class JtaTransactionObject implements SmartTransactionObject {
//
//    private final UserTransaction userTransaction;
//
//    boolean resetTransactionTimeout = false;
//
//    private ConnectionHolder connectionHolder;
//
//    private boolean newConnectionHolder;
//
//    /**
//     * Constructs a new <b>JtaTransactionObject</b>.
//     *
//     * @param userTransaction the JTA UserTransaction
//     */
//    public JtaTransactionObject(UserTransaction userTransaction, ConnectionHolder connectionHolder,
//        boolean newConnectionHolder) {
//      this.userTransaction = userTransaction;
//      this.connectionHolder = connectionHolder;
//      this.newConnectionHolder = newConnectionHolder;
//    }
//
//    @Override
//    public void flush() {
//      TransactionSynchronizationUtils.triggerFlush();
//    }
//
//    /**
//     * Returns the connection holder associated with the transaction object.
//     *
//     * @return the connection holder associated with the transaction object
//     */
//    public ConnectionHolder getConnectionHolder() {
//      return connectionHolder;
//    }
//
//    /**
//     * Returns the JTA UserTransaction associated with the transaction object.
//     *
//     * @return the JTA UserTransaction associated with the transaction object
//     */
//    public UserTransaction getUserTransaction() {
//      return userTransaction;
//    }
//
//    /**
//     * Returns whether the transaction object has an associated connection holder.
//     *
//     * @return true if the transaction object has an associated connection holder or false otherwise
//     */
//    public boolean hasConnectionHolder() {
//      return (connectionHolder != null);
//    }
//
//    /**
//     * Returns whether the connection holder is new.
//     *
//     * @return true if the connection holder is new or false otherwise
//     */
//    public boolean isNewConnectionHolder() {
//      return newConnectionHolder;
//    }
//
//    @Override
//    public boolean isRollbackOnly() {
//      try {
//        int jtaStatus = this.userTransaction.getStatus();
//        return (jtaStatus == Status.STATUS_MARKED_ROLLBACK
//            || jtaStatus == Status.STATUS_ROLLEDBACK);
//      } catch (SystemException ex) {
//        throw new TransactionSystemException(
//            "Failed to retrieve the status of the JTA UserTransaction", ex);
//      }
//    }
//
//    /**
//     * Set the connection holder.
//     *
//     * @param connectionHolder    the connection holder
//     * @param newConnectionHolder whether the connection holder is new
//     */
//    public void setConnectionHolder(ConnectionHolder connectionHolder,
//        boolean newConnectionHolder) {
//      this.connectionHolder = connectionHolder;
//      this.newConnectionHolder = newConnectionHolder;
//    }
//  }
//
//  /**
//   * The <b>SuspendedResources</b>> class holds the resources for a suspended transaction.
//   *
//   * @author Marcus Portmann
//   */
//  private class SuspendedResources {
//
//    /**
//     * The connection holder for the connection associated with the transaction that was suspended.
//     */
//    final ConnectionHolder connectionHolder;
//
//    /**
//     * The JTA transaction that was suspended.
//     */
//    final Transaction transaction;
//
//    /**
//     * Constructs a new <b>SuspendedResources</b>.
//     *
//     * @param transaction      the JTA transaction that was suspended
//     * @param connectionHolder the connection holder for the connection associated with the
//     *                         transaction that was suspended.
//     */
//    public SuspendedResources(Transaction transaction, ConnectionHolder connectionHolder) {
//      this.transaction = transaction;
//      this.connectionHolder = connectionHolder;
//    }
//  }
//}
