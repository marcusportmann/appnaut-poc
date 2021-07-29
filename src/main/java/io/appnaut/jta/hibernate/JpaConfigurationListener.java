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

package io.appnaut.jta.hibernate;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import javax.inject.Singleton;

/**
 * The <b>JpaConfigurationListener</b> class implements a BeanCreatedEventListener that modifies the
 * JpaConfiguration to enable JTA transaction support for Hibernate.
 *
 * @author Marcus Portmann
 */
@Singleton
@SuppressWarnings("unused")
public class JpaConfigurationListener implements BeanCreatedEventListener<JpaConfiguration> {

  @Override
  public JpaConfiguration onCreated(BeanCreatedEvent<JpaConfiguration> event) {
    JpaConfiguration jpaConfiguration = event.getBean();

    // Enable JTA transaction support for Hibernate
    jpaConfiguration.getProperties().put("hibernate.transaction.coordinator_class", "jta");
    jpaConfiguration.getProperties().put("hibernate.transaction.jta.platform", "JBossTS");

    return jpaConfiguration;
  }
}
