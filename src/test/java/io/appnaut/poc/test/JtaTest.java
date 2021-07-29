/*
 * Copyright 2021 Marcus Portmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appnaut.poc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.agroal.api.AgroalDataSource;
import io.appnaut.poc.data.Data;
import io.appnaut.poc.data.IDataService;
import io.micronaut.jdbc.metadata.DataSourcePoolMetadata;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.exceptions.UnexpectedRollbackException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.junit.jupiter.api.Test;

@MicronautTest
public class JtaTest {

  private final SecureRandom random = new SecureRandom();

  @Inject
  private IDataService dataService;

  @Inject
  @Named("db1")
  private DataSource db1DataSource;

  @Inject
  @Named("db1")
  private DataSourcePoolMetadata<AgroalDataSource> db1DataSourcePoolMetadata;

  @Inject
  @Named("db2")
  private DataSource db2DataSource;

  @Inject
  @Named("db2")
  private DataSourcePoolMetadata<AgroalDataSource> db2DataSourcePoolMetadata;

  @Inject
  private TransactionManager transactionManager;

  @Test
  void createDataTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    dataService.createData(newData);

    List<Data> afterData = dataService.getAllData();

    assertEquals(5, afterData.size(), "Failed to retrieve the correct number of data objects");
  }

  @Test
  void createDataWithNewTransactionAndNoRollbackOnExceptionTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    Exception exception = assertThrows(Exception.class,
        () -> dataService.createDataWithNewTransactionAndNoRollbackOnException(newData));

    assertEquals("Testing 1.. 2.. 3..", exception.getMessage());

    /*
     * The default behaviour is to rollback the new nested transaction when a checked Exception
     * is throw by the createDataWithNewTransactionAndNoRollbackOnException method even though no
     * rollbackOn attribute was specified for the @Transactional attribute. Hence the afterData list
     * of data objects should be the same as the beforeData list.
     */
    List<Data> afterData = dataService.getAllData();

    assertEquals(4, afterData.size(), "Failed to retrieve the correct number of data objects");
  }

  @Test
  void createDataWithNewTransactionAndRollbackOnExceptionTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    Exception exception = assertThrows(Exception.class,
        () -> dataService.createDataWithNewTransactionAndRollbackOnException(newData));

    assertEquals("Testing 1.. 2.. 3..", exception.getMessage());

    /*
     * The required behaviour is to rollback the new nested transaction when a checked Exception
     * is throw by the createDataWithNewTransactionAndRollbackOnException method, which has the
     * rollbackOn attribute specified for the @Transactional attribute. Hence the afterData list
     * of data objects should be the same as the beforeData list.
     */
    List<Data> afterData = dataService.getAllData();

    assertEquals(4, afterData.size(), "Failed to retrieve the correct number of data objects");
  }

  @Test
  void createDataWithNewTransactionAndRollbackOnRuntimeExceptionTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    RuntimeException runtimeException = assertThrows(RuntimeException.class,
        () -> dataService.createDataWithNewTransactionAndRollbackOnRuntimeException(newData));

    assertEquals("Testing 1.. 2.. 3..", runtimeException.getMessage());

    /*
     * The required behaviour is to rollback the new nested transaction when a unchecked
     * RuntimeException is throw by the createDataWithNewTransactionAndRollbackOnRuntimeException
     * method, which has the rollbackOn attribute specified for the @Transactional attribute.
     * Hence the afterData list of data objects should be the same as the beforeData list.
     */
    List<Data> afterData = dataService.getAllData();

    assertEquals(4, afterData.size(), "Failed to retrieve the correct number of data objects");
  }

  @Test
  void createDataWithNoRollbackOnExceptionTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    Exception exception = assertThrows(Exception.class,
        () -> dataService.createDataWithNoRollbackOnException(newData));

    assertEquals("Testing 1.. 2.. 3..", exception.getMessage());

    /*
     * We have set the setFailEarlyOnGlobalRollbackOnly flag on the
     * AbstractSynchronousTransactionManager to true (in the JtaTransactionManager).
     * As a result, when the global transaction is flagged for rollback-only because of a checked
     * Exception being thrown by the createDataWithNoRollbackOnException method, we expect an
     * UnexpectedRollbackException to be thrown if we try and perform additional JPA calls as part
     * of the same transaction.
     */
    assertThrows(
        UnexpectedRollbackException.class, () -> dataService.getAllData());
  }

  @Test
  void createDataWithRollbackOnExceptionTest() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    Exception exception = assertThrows(Exception.class,
        () -> dataService.createDataWithRollbackOnException(newData));

    assertEquals("Testing 1.. 2.. 3..", exception.getMessage());

    /*
     * We have set the setFailEarlyOnGlobalRollbackOnly flag on the
     * AbstractSynchronousTransactionManager to true (in the JtaTransactionManager).
     * As a result, when the global transaction is flagged for rollback-only because of a checked
     * Exception being thrown by the createDataWithRollbackOnException method, we expect an
     * UnexpectedRollbackException to be thrown if we try and perform additional JPA calls as part
     * of the same transaction.
     */
    assertThrows(
        UnexpectedRollbackException.class, () -> dataService.getAllData());
  }

  @Test
  void createDataWithRollbackOnRuntimeException() {
    Data newData = getNewData();

    List<Data> beforeData = dataService.getAllData();

    assertEquals(4, beforeData.size(), "Failed to retrieve the correct number of data objects");

    Exception exception = assertThrows(Exception.class,
        () -> dataService.createDataWithRollbackOnRuntimeException(newData));

    assertEquals("Testing 1.. 2.. 3..", exception.getMessage());

    /*
     * We have set the setFailEarlyOnGlobalRollbackOnly flag on the
     * AbstractSynchronousTransactionManager to true (in the JtaTransactionManager).
     * As a result, when the global transaction is flagged for rollback-only because of an unchecked
     * RuntimeException being thrown by the createDataWithRollbackOnRuntimeException method, we
     * expect an UnexpectedRollbackException to be thrown if we try and perform additional JPA calls
     * as part of the same transaction.
     */
    assertThrows(
        UnexpectedRollbackException.class, () -> dataService.getAllData());
  }

  @Test
  void dataSourcePoolMetadataTest() {
    assertEquals(1, db1DataSourcePoolMetadata.getMin());
    assertEquals(2, db2DataSourcePoolMetadata.getMin());
  }

  @Test
  void hybridTest() throws Exception {

    List<Data> beforeData = dataService.getAllData();

    Transaction existingTransaction = transactionManager.suspend();

    transactionManager.begin();

    dataService.createData(getNewData());

    try (Connection connection = db1DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO poc.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
        int value = random.nextInt();

        statement.setLong(1, System.currentTimeMillis());
        statement.setInt(2, value);
        statement.setString(3, "New Test Data " + value);
        statement.setObject(4, LocalDate.now());
        statement.setObject(5, LocalDateTime.now());

        statement.executeUpdate();
      }
    }

    try (Connection connection = db1DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM poc.data")) {
        try (ResultSet rs = statement.executeQuery()) {
          List<Long> ids = new ArrayList<>();

          while (rs.next()) {
            ids.add(rs.getLong(1));
          }

          assertEquals(5, ids.size(), "Failed to retrieve the correct number of IDs");
        }
      }
    }

    try (Connection connection = db2DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO poc.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
        int value = random.nextInt();

        statement.setLong(1, System.currentTimeMillis());
        statement.setInt(2, value);
        statement.setString(3, "New Test Data " + value);
        statement.setObject(4, LocalDate.now());
        statement.setObject(5, LocalDateTime.now());

        statement.executeUpdate();
      }
    }

    List<Data> beforeRollbackData = dataService.getAllData();

    assertEquals(beforeData.size() + 1, beforeRollbackData.size(),
        "Failed to retrieve the correct number of data objects");

    try (Connection connection = db2DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM poc.data")) {
        try (ResultSet rs = statement.executeQuery()) {
          List<Long> ids = new ArrayList<>();

          while (rs.next()) {
            ids.add(rs.getLong(1));
          }

          assertEquals(5, ids.size(), "Failed to retrieve the correct number of IDs");
        }
      }
    }

    transactionManager.rollback();

    transactionManager.resume(existingTransaction);

    List<Data> afterData = dataService.getAllData();

    assertEquals(beforeData.size(), afterData.size(),
        "Failed to retrieve the correct number of data objects");

    try (Connection connection = db1DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM poc.data")) {
        try (ResultSet rs = statement.executeQuery()) {
          List<Long> ids = new ArrayList<>();

          while (rs.next()) {
            ids.add(rs.getLong(1));
          }

          assertEquals(4, ids.size(), "Failed to retrieve the correct number of IDs");
        }
      }
    }

    try (Connection connection = db2DataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM poc.data")) {
        try (ResultSet rs = statement.executeQuery()) {
          List<Long> ids = new ArrayList<>();

          while (rs.next()) {
            ids.add(rs.getLong(1));
          }

          assertEquals(4, ids.size(), "Failed to retrieve the correct number of IDs");
        }
      }
    }
  }

  private Data getNewData() {
    long id = System.currentTimeMillis();

    LocalDateTime now = LocalDateTime.now();

    return new Data(id, random.nextInt(), "Data " + id, now.toLocalDate(), now);
  }
}

