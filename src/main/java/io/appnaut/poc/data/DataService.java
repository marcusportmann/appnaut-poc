package io.appnaut.poc.data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

/**
 * The <b>CodesService</b> class provides the Codes Service implementation.
 *
 * @author Marcus Portmann
 */
@Singleton
public class DataService implements IDataService {

  private final DataRepository dataRepository;

  public DataService(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  /**
   * Create a new data.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRED)
  @Override
  public Data createData(Data data) {
    return dataRepository.saveAndFlush(data);
  }

  /**
   * Create a new data with a nested transaction and throw a checked Exception with no rollback.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRES_NEW)
  public Data createDataWithNewTransactionAndNoRollbackOnException(Data data) throws Exception {
    var savedData = dataRepository.saveAndFlush(data);

    throw new Exception("Testing 1.. 2.. 3..");
  }

  /**
   * Create a new data with a nested transaction and throw a checked Exception.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRES_NEW, rollbackOn = Exception.class)
  public Data createDataWithNewTransactionAndRollbackOnException(Data data) throws Exception {
    var savedData = dataRepository.saveAndFlush(data);

    throw new Exception("Testing 1.. 2.. 3..");
  }

  /**
   * Create a new data with a nested transaction and throw a RuntimeException.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRES_NEW, rollbackOn = RuntimeException.class)
  public Data createDataWithNewTransactionAndRollbackOnRuntimeException(Data data) {
    var savedData = dataRepository.saveAndFlush(data);

    throw new RuntimeException("Testing 1.. 2.. 3..");
  }

  /**
   * Create a new data and throw a checked Exception with no rollback.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRED)
  public Data createDataWithNoRollbackOnException(Data data) throws Exception {
    var savedData = dataRepository.saveAndFlush(data);

    throw new Exception("Testing 1.. 2.. 3..");
  }

  /**
   * Create a new data and throw a checked Exception.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
  public Data createDataWithRollbackOnException(Data data) throws Exception {
    var savedData = dataRepository.saveAndFlush(data);

    throw new Exception("Testing 1.. 2.. 3..");
  }

  /**
   * Create a new data and throw a RuntimeException.
   *
   * @param data the data
   *
   * @return the data that was created
   */
  @Transactional(value = TxType.REQUIRED, rollbackOn = RuntimeException.class)
  public Data createDataWithRollbackOnRuntimeException(Data data) {
    var savedData = dataRepository.saveAndFlush(data);

    throw new RuntimeException("Testing 1.. 2.. 3..");
  }

  /**
   * Retrieve all the data.
   *
   * @return all the data
   */
  @Override
  public List<Data> getAllData() {
    return StreamSupport.stream(dataRepository.findAll().spliterator(), false)
        .collect(Collectors.toList());
  }

}
