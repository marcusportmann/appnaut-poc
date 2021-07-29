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

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.micronaut.jdbc.metadata.AbstractDataSourcePoolMetadata;

/**
 * The <b>AgroalDataSourcePoolMetadata</b> class provides the {@link
 * io.micronaut.jdbc.metadata.DataSourcePoolMetadata} for an {@link AgroalDataSource}.
 *
 * @author Marcus Portmann
 */
@SuppressWarnings("unused")
public class AgroalDataSourcePoolMetadata
    extends AbstractDataSourcePoolMetadata<AgroalDataSource> {

  private final AgroalConnectionPoolConfiguration connectionPoolConfiguration;

  /**
   * Agroal typed {@link io.micronaut.jdbc.metadata.DataSourcePoolMetadata} object.
   *
   * @param dataSource the datasource
   */
  public AgroalDataSourcePoolMetadata(AgroalDataSource dataSource) {
    super(dataSource);

    this.connectionPoolConfiguration = dataSource.getConfiguration().connectionPoolConfiguration();
  }

  @Override
  public Integer getActive() {
    return (int) getDataSource().getMetrics().activeCount();
  }

  @Override
  public Boolean getDefaultAutoCommit() {
    return connectionPoolConfiguration.connectionFactoryConfiguration().autoCommit();
  }

  @Override
  public Integer getIdle() {
    return (int) getDataSource().getMetrics().availableCount();
  }

  @Override
  public Integer getMax() {
    return (int) getDataSource().getMetrics().maxUsedCount();
  }

  @Override
  public Integer getMin() {
    return connectionPoolConfiguration.minSize();
  }

  @Override
  public String getValidationQuery() {
    return connectionPoolConfiguration.connectionFactoryConfiguration().initialSql();
  }
}
