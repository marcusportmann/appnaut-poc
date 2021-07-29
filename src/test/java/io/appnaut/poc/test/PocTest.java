package io.appnaut.poc.test;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.SynchronousTransactionManager;
import io.micronaut.transaction.TransactionStatus;
import io.micronaut.transaction.hibernate5.HibernateTransactionManager;
import io.micronaut.transaction.jdbc.DataSourceTransactionManager;
import io.micronaut.transaction.jdbc.DataSourceUtils;
import io.micronaut.transaction.jdbc.DelegatingDataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest(rollback = false)
//@MicronautTest
class PocTest {

  @Inject
  EmbeddedApplication<?> application;

  private final SecureRandom random = new SecureRandom();



  @Inject
  @Named("default")
  SynchronousTransactionManager<?> defaultTransactionManager;


  @Inject
  @Named("db1")
  SynchronousTransactionManager<?> db1TransactionManager;


  @Inject
  private ApplicationContext applicationContext;

  @Inject
  @Named("db1")
  private DataSource db1DataSource;

  @Inject
  @Named("default")
  private DataSource defaultDataSource;

  @Test
  @Transactional
  void jdbcTest() throws Exception {
    db1TransactionManager.executeWrite((status) -> {
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

      return Boolean.TRUE;
    });
  }

//  @Inject
//  @Named("db1")
//  private DataSource db1DataSource;
//
//  @Inject
//  @Named("db2")
//  private DataSource db2DataSource;
//
//  @Inject
//  private SynchronousTransactionManager<?> transactionManager;
//
//  @Test
//  void simpleJdbcTest() throws Exception {
//
//    transactionManager.executeWrite((status) -> {
//      var db1DataSourceUnwrapped = DelegatingDataSource.unwrapDataSource(db1DataSource);
//
//      try (Connection connection = DataSourceUtils.getConnection(db1DataSourceUnwrapped, true)) {
//        try (PreparedStatement statement = connection.prepareStatement(
//            "INSERT INTO demo.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
//          int value = random.nextInt();
//
//          statement.setLong(1, System.currentTimeMillis());
//          statement.setInt(2, value);
//          statement.setString(3, "New Test Data " + value);
//          statement.setObject(4, LocalDate.now());
//          statement.setObject(5, LocalDateTime.now());
//
//          statement.executeUpdate();
//        }
//      }
//
//      var db2DataSourceUnwrapped = DelegatingDataSource.unwrapDataSource(db2DataSource);
//
//      try (Connection connection = DataSourceUtils.getConnection(db2DataSourceUnwrapped, true)) {
//        try (PreparedStatement statement = connection.prepareStatement(
//            "INSERT INTO demo.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
//          int value = random.nextInt();
//
//          statement.setLong(1, System.currentTimeMillis());
//          statement.setInt(2, value);
//          statement.setString(3, "New Test Data " + value);
//          statement.setObject(4, LocalDate.now());
//          statement.setObject(5, LocalDateTime.now());
//
//          statement.executeUpdate();
//        }
//      }
//
//      //throw new RuntimeException("Testing 1.. 2.. 3..");
//
//      return Boolean.TRUE;
//    });
//
//    transactionManager.commit(TransactionStatus.
//
//
//
//    //transactionManager.setRollbackOnly();
//
//  }







//  @Inject
//  @Named("db2")
//  private DataSource db2DataSource;


//  @Test
//  @Transactional
//  void transactionRecoveryTest() throws Exception {
//
//    Random random = new SecureRandom();
//
//    //System.out.println("transactionManager = " + transactionManager);
//
////    //Transaction transaction =  transactionManager.getTransaction();
////
////    System.out.println("transaction = "  + transaction);
////
////    if (transaction  != null) {
////      System.out.println("transaction.getStatus() = " + transaction.getStatus());
////    }
//
//    System.out.println("defaultDataSource = " + defaultDataSource);
//    System.out.println("db1DataSource = " + db1DataSource);
//    //System.out.println("db2DataSource = " + db2DataSource);
//
//    try (Connection connection = DataSourceUtils.getConnection(db1DataSource)) {
//    //try (Connection connection = db1DataSource.getConnection()) {
//      try (PreparedStatement statement = connection.prepareStatement("INSERT INTO demo.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
//        int value = random.nextInt();
//
//        statement.setLong(1, System.currentTimeMillis());
//        statement.setInt(2,  value);
//        statement.setString(3,"New Test Data " + value);
//        statement.setObject(4, LocalDate.now());
//        statement.setObject(5, LocalDateTime.now());
//
//        statement.executeUpdate();
//      }
//    }
////
////
////    try (Connection connection = db2DataSource.getConnection()) {
////      try (PreparedStatement statement = connection.prepareStatement("INSERT INTO demo.data (id, integer_value, string_value, date_value, timestamp_value) VALUES (?, ?, ?, ?, ?)")) {
////        int value = random.nextInt();
////
////        statement.setLong(1, System.currentTimeMillis());
////        statement.setInt(2,  value);
////        statement.setString(3,"Test Data " + value);
////        statement.setObject(4, LocalDate.now());
////        statement.setObject(5, LocalDateTime.now());
////
////        statement.executeUpdate();
////      }
////    }
//
//
//    //System.exit(-1);
//
//  }


}
