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

import io.agroal.api.configuration.supplier.AgroalPropertiesReader;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import java.util.Map;
import java.util.Properties;

/**
 * The <b>AgroalDataSourceConfiguration</b> class holds the configuration information for an Agroal
 * JDBC data source.
 *
 * @author Marcus Portmann
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
@SuppressWarnings("unused")
public class AgroalDataSourceConfiguration implements BasicJdbcConfiguration {

  private final CalculatedSettings calculatedSettings;

  /**
   * The name of the data source.
   */
  private final String name;

  private final Properties properties = new Properties();

  private int maxPoolSize = 5;

  private int minPoolSize = 1;

  /**
   * Constructs a new <b>AgroalDataSourceConfiguration</b>.
   *
   * @param name the name of the data source configured from properties
   */
  public AgroalDataSourceConfiguration(@Parameter String name) {
    super();
    this.calculatedSettings = new CalculatedSettings(this);
    this.name = name;
  }

  @Override
  public String getConfiguredDriverClassName() {
    return properties.getProperty(AgroalPropertiesReader.PROVIDER_CLASS_NAME);
  }

  @Override
  public String getConfiguredPassword() {
    return properties.getProperty(AgroalPropertiesReader.CREDENTIAL);
  }

  @Override
  public String getConfiguredUrl() {
    return properties.getProperty(AgroalPropertiesReader.JDBC_URL);
  }

  @Override
  public String getConfiguredUsername() {
    return properties.getProperty(AgroalPropertiesReader.PRINCIPAL);
  }

  @Override
  public String getConfiguredValidationQuery() {
    return calculatedSettings.getValidationQuery();
  }

  @Override
  public String getDriverClassName() {
    return calculatedSettings.getDriverClassName();
  }

  /**
   * Returns the maximum size of the connection pool.
   *
   * @return the maximum size of the connection pool
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * Returns the minimum size of the connection pool.
   *
   * @return the minimum size of the connection pool
   */
  public int getMinPoolSize() {
    return minPoolSize;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getPassword() {
    return calculatedSettings.getPassword();
  }

  /**
   * Returns the properties used to configure the Agroal data source.
   * <p/>
   * See: https://agroal.github.io/docs.html
   *
   * @return the properties used to configure the Agroal data source
   */
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String getUrl() {
    return calculatedSettings.getUrl();
  }

  @Override
  public String getUsername() {
    return calculatedSettings.getUsername();
  }

  @Override
  public String getValidationQuery() {
    return properties.getProperty(AgroalPropertiesReader.INITIAL_SQL);
  }

  @Override
  public void setDataSourceProperties(Map<String, ?> dsProperties) {
    if (dsProperties != null) {
      dsProperties.forEach((s, o) -> {
        if (o != null) {
          properties.setProperty(s, o.toString());
        }
      });
    }
  }

  @Override
  public void setDriverClassName(String driverClassName) {
    properties.setProperty(AgroalPropertiesReader.PROVIDER_CLASS_NAME, driverClassName);
  }

  /**
   * Set the maximum size of the connection pool.
   *
   * @param maxPoolSize the maximum size of the connection pool
   */
  public void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;

    properties.setProperty(
        AgroalPropertiesReader.MAX_SIZE, Integer.toString(maxPoolSize));
  }

  /**
   * Set the minimum size of the connection pool.
   *
   * @param minPoolSize the minimum size of the connection pool
   */
  public void setMinPoolSize(int minPoolSize) {
    this.minPoolSize = minPoolSize;

    properties.setProperty(
        AgroalPropertiesReader.MIN_SIZE, Integer.toString(minPoolSize));
  }

  @Override
  public void setPassword(String password) {
    properties.setProperty(AgroalPropertiesReader.CREDENTIAL, password);
  }

  @Override
  public void setUrl(String url) {
    properties.setProperty(AgroalPropertiesReader.JDBC_URL, url);
  }

  @Override
  public void setUsername(String username) {
    properties.setProperty(AgroalPropertiesReader.PRINCIPAL, username);
  }
}








