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
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalPropertiesReader;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.PreDestroy;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <b>AgroalDataSourceFactory</b> class implements the data source factory responsible for
 * creating Agroal data sources.
 *
 * @author Marcus Portmann
 */
@Factory
@SuppressWarnings("unused")
public class AgroalDataSourceFactory implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(AgroalDataSourceFactory.class);

  private final BeanContext beanContext;

  private final List<AgroalDataSource> dataSources = new ArrayList<>();

  /**
   * Constructs a new <b>AgroalDataSourceFactory</b>.
   *
   * @param beanContext the bean context
   */
  public AgroalDataSourceFactory(BeanContext beanContext) {
    this.beanContext = beanContext;
  }

  /**
   * Create an Agroal data source for each data source configuration.
   *
   * @param agroalDataSourceConfiguration the data source configuration
   *
   * @return the Agroal data source
   */
  @EachBean(AgroalDataSourceConfiguration.class)
  public AgroalDataSource agroalDataSource(
      AgroalDataSourceConfiguration agroalDataSourceConfiguration)
      throws SQLException {
    AgroalPropertiesReader agroalPropertiesReader =
        new AgroalPropertiesReader().readProperties(agroalDataSourceConfiguration.getProperties());

    AgroalDataSourceConfigurationSupplier agroalDataSourceConfigurationSupplier =
        agroalPropertiesReader.modify();

    Optional<TransactionManager> transactionManagerOptional = beanContext
        .findBean(TransactionManager.class);

    Optional<TransactionSynchronizationRegistry> transactionSynchronizationRegistryOptional = beanContext
        .findBean(TransactionSynchronizationRegistry.class);

    if (transactionManagerOptional.isPresent() && transactionSynchronizationRegistryOptional
        .isPresent()) {
      Optional<RecoveryManager> recoveryManagerOptional = beanContext
          .findBean(RecoveryManager.class);

      NarayanaTransactionIntegration narayanaTransactionIntegration;

      if (recoveryManagerOptional.isPresent()) {
        narayanaTransactionIntegration =
            new NarayanaTransactionIntegration(
                agroalDataSourceConfiguration.getName(),
                transactionManagerOptional.get(), transactionSynchronizationRegistryOptional.get(),
                recoveryManagerOptional.get());
      } else {
        narayanaTransactionIntegration =
            new NarayanaTransactionIntegration(
                agroalDataSourceConfiguration.getName(),
                transactionManagerOptional.get(), transactionSynchronizationRegistryOptional.get());
      }

      agroalDataSourceConfigurationSupplier
          .connectionPoolConfiguration()
          .transactionIntegration(narayanaTransactionIntegration);
    }

    AgroalDataSource dataSource = AgroalDataSource.from(agroalDataSourceConfigurationSupplier);

    dataSources.add(dataSource);

    return dataSource;
  }

  @Override
  @PreDestroy
  public void close() {
    for (AgroalDataSource dataSource : dataSources) {
      try {
        dataSource.close();
      } catch (Throwable e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to close the data source (" + dataSource + ")", e);
        }
      }
    }

    // Terminate the Narayana Recovery Manager
    Optional<RecoveryManager> recoveryManagerOptional = beanContext
        .findBean(RecoveryManager.class);

    if (recoveryManagerOptional.isPresent()) {
      RecoveryManager recoveryManager = recoveryManagerOptional.get();

      recoveryManager.terminate(false);
    }
  }

  @EachBean(AgroalDataSource.class)
  @Requires(beans = AgroalDataSourceConfiguration.class)
  public AgroalDataSourcePoolMetadata dataSourcePoolMetadata(AgroalDataSource agroalDataSource) {
    return new AgroalDataSourcePoolMetadata(agroalDataSource);
  }
}
