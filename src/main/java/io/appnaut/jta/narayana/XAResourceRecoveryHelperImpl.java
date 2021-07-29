///*
// * Copyright 2021 original authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.appnaut.jta.narayana;
//
//import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
//import javax.transaction.xa.XAResource;
//
///**
// * The <b>XAResourceRecoveryImpl</b> class provides an implementation of the the
// * XAResourceRecoveryHelper interface.
// *
// * @author Marcus Portmann
// */
//public class XAResourceRecoveryHelperImpl implements XAResourceRecoveryHelper {
//
//  private final XAResource[] xaResources;
//
//  public XAResourceRecoveryHelperImpl(XAResource[] xaResources) {
//    this.xaResources = xaResources;
//  }
//
//  public XAResourceRecoveryHelperImpl(XAResource xaResource) {
//    this.xaResources = new XAResource[1];
//    this.xaResources[0] = xaResource;
//  }
//
//  @Override
//  public XAResource[] getXAResources() {
//    return xaResources;
//  }
//
//  @Override
//  public boolean initialise(String p) throws Exception {
//    return true;
//  }
//}
