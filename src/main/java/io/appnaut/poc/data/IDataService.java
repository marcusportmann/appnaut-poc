package io.appnaut.poc.data;

import java.util.List;

public interface IDataService {

  /**
   * Create a new data.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createData(Data data);

  /**
   * Create a new data with a nested transaction and throw a checked Exception with no rollback.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithNewTransactionAndNoRollbackOnException(Data data) throws Exception;

  /**
   * Create a new data with a nested transaction and throw a checked Exception.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithNewTransactionAndRollbackOnException(Data data) throws Exception;

  /**
   * Create a new data with a nested transaction and throw a RuntimeException.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithNewTransactionAndRollbackOnRuntimeException(Data data);

  /**
   * Create a new data and throw a checked Exception with no rollback.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithNoRollbackOnException(Data data) throws Exception;

  /**
   * Create a new data and throw a checked Exception.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithRollbackOnException(Data data) throws Exception;

  /**
   * Create a new data and throw a RuntimeException.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  Data createDataWithRollbackOnRuntimeException(Data data);

  /**
   * Retrieve the data.
   *
   * @return the data
   */
  List<Data> getAllData();
}
