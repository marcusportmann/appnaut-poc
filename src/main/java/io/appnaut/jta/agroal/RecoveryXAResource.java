//package io.appnaut.agroal;
//
//import java.sql.SQLException;
//import javax.sql.XAConnection;
//import javax.transaction.xa.XAException;
//import javax.transaction.xa.XAResource;
//import javax.transaction.xa.Xid;
//
//public class RecoveryXAResource implements AutoCloseable, XAResource {
//
//  private final String dataSourceName;
//
//  private final XAResource xaResource;
//
//  private XAConnection xaConnection;
//
//  public RecoveryXAResource(String dataSourceName, XAConnection xaConnection) throws SQLException {
//    this.dataSourceName = dataSourceName;
//    this.xaConnection = xaConnection;
//    this.xaResource = xaConnection.getXAResource();
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
//    } finally {
//      xaConnection = null;
//    }
//  }
//
//  @Override
//  public void commit(Xid xid, boolean onePhase) throws XAException {
//    xaResource.commit(xid, onePhase);
//  }
//
//  @Override
//  public void end(Xid xid, int flags) throws XAException {
//    xaResource.end(xid, flags);
//  }
//
//  @Override
//  public void forget(Xid xid) throws XAException {
//    xaResource.forget(xid);
//  }
//
//  @Override
//  public int getTransactionTimeout() throws XAException {
//    return xaResource.getTransactionTimeout();
//  }
//
//  @Override
//  public boolean isSameRM(XAResource xares) throws XAException {
//    return xaResource.isSameRM(xares);
//  }
//
//  @Override
//  public int prepare(Xid xid) throws XAException {
//    return xaResource.prepare(xid);
//  }
//
//  @Override
//  public Xid[] recover(int flag) throws XAException {
//    if (xaConnection == null) {
//      throw new XAException(XAException.XAER_RMFAIL);
//    }
//    Xid[] value = xaResource.recover(flag);
//    if (flag == TMENDRSCAN && (value == null || value.length == 0)) {
//      close();
//    }
//    return value;
//  }
//
//  @Override
//  public void rollback(Xid xid) throws XAException {
//    xaResource.rollback(xid);
//  }
//
//  @Override
//  public boolean setTransactionTimeout(int seconds) throws XAException {
//    return xaResource.setTransactionTimeout(seconds);
//  }
//
//  @Override
//  public void start(Xid xid, int flags) throws XAException {
//    xaResource.start(xid, flags);
//  }
//}
