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

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.inject.BeanIdentifier;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * The <b>TransactionAwareDataSource</b> class provides a JTA transaction aware data source
 * implementation. It is a replacement for the standard Micronaut TransactionAwareDataSource and
 * just returns the Agroal data source, which is configured to support transaction integration by
 * the {@link AgroalDataSourceFactory}.
 *
 * @author Marcus Portmann
 */
@Singleton
@Replaces(io.micronaut.transaction.jdbc.TransactionAwareDataSource.class)
@Requires(missingBeans = io.micronaut.jdbc.spring.DataSourceTransactionManagerFactory.class)
@SuppressWarnings("unused")
public class TransactionAwareDataSource implements BeanCreatedEventListener<DataSource> {

  /**
   * Constructs a new <b>TransactionAwareDataSource</b>.
   */
  public TransactionAwareDataSource() {
  }

  @Override
  public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
    final BeanIdentifier beanIdentifier = event.getBeanIdentifier();
    String name = beanIdentifier.getName();
    if (name.equalsIgnoreCase("primary")) {
      name = "default";
    }

    return event.getBean();
  }
}
