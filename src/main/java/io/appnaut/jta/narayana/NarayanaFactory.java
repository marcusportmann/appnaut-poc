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

package io.appnaut.jta.narayana;

import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

/**
 * The <b>TransactionFactory</b> class produces the JTA transaction management beans for the
 * Narayana transactions toolkit.
 *
 * @author Marcus Portmann
 * @see <a href="https://narayana.io">Naryana</a>
 */
@Factory
@SuppressWarnings("unused")
public class NarayanaFactory {

  static {
    TxControl.setXANodeName(nodeName());
  }

  /**
   * Retrieve the XA node name.
   *
   * @return the XA node name
   */
  private static String nodeName() {
    try {
      java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();

      return localMachine.getHostName().toLowerCase();
    } catch (Throwable e) {
      return "Unknown";
    }
  }

  /**
   * Returns the Narayana recovery manager.
   *
   * @return the Narayana recovery manager
   */
  @Bean
  public RecoveryManager recoveryManager() {
    RecoveryManager recoveryManager = RecoveryManager.manager();

    recoveryManager.initialize();

    return recoveryManager;
  }

  /**
   * Returns the Narayana JTA transaction manager.
   *
   * @return the Narayana JTA transaction manager
   */
  @Bean
  public TransactionManager transactionManager() {
    return com.arjuna.ats.jta.TransactionManager.transactionManager();
  }

  /**
   * Returns the Narayana JTA transaction synchronization registry.
   *
   * @return the Narayana JTA transaction synchronization registry
   */
  @Bean
  @Requires(beans = {TransactionManager.class})
  public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
    return new com.arjuna.ats.internal.jta.transaction.arjunacore
        .TransactionSynchronizationRegistryImple();
  }

  /**
   * Returns the Narayana JTA user transaction.
   *
   * @return the Narayana JTA user transaction
   */
  @Bean
  public UserTransaction userTransaction() {
    return com.arjuna.ats.jta.UserTransaction.userTransaction();
  }
}
