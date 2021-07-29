//package io.appnaut.agroal;
//
//import java.sql.SQLException;
//import javax.sql.XAConnection;
//import javax.transaction.xa.XAException;
//import javax.transaction.xa.XAResource;
//import javax.transaction.xa.Xid;
//
//public class ErrorConditionXAResource implements AutoCloseable, XAResource {
//
//  private final SQLException error;
//
//  private final XAConnection xaConnection;
//
//  public ErrorConditionXAResource(String dataSourceName, XAConnection xaConnection, SQLException error) {
//    this.xaConnection = xaConnection;
//    this.error = error;
//  }
//
//  @Override
//  public void close() throws XAException {
//    try {
//      xaConnection.close();
//    } catch (SQLException e) {
//      XAException xaException = new XAException(XAException.XAER_RMFAIL);
//      xaException.initCause(e);
//      throw xaException;
//    }
//  }
//
//  @Override
//  public void commit(Xid xid, boolean onePhase) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public void end(Xid xid, int flags) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public void forget(Xid xid) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public int getTransactionTimeout() throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public boolean isSameRM(XAResource xares) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public int prepare(Xid xid) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public Xid[] recover(int flag) throws XAException {
//    if (flag == TMENDRSCAN) {
//      close();
//    }
//    throw createXAException();
//  }
//
//  @Override
//  public void rollback(Xid xid) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public boolean setTransactionTimeout(int seconds) throws XAException {
//    throw createXAException();
//  }
//
//  @Override
//  public void start(Xid xid, int flags) throws XAException {
//    throw createXAException();
//  }
//
//  private XAException createXAException() {
//    XAException xaException = new XAException(XAException.XAER_RMFAIL);
//    xaException.initCause(error);
//    return xaException;
//  }
//}