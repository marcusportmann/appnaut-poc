package io.appnaut.jta.narayana;///*
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
//package io.appnaut.transaction.narayana;
//
//import com.arjuna.ats.arjuna.recovery.RecoveryManager;
//import com.arjuna.ats.arjuna.recovery.RecoveryModule;
//import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
//import com.arjuna.ats.jta.recovery.SerializableXAResourceDeserializer;
//import io.appnaut.transaction.RecoveryXAResourceRegistry;
//import io.appnaut.transaction.RecoveryXAResources;
//import javax.annotation.PreDestroy;
//
///**
// * The <b>NarayanaRecoveryXAResourceRegistry</b> class provides access to the Narayana-specific
// * implementation of the registry that stores the <b>XAResources</b> necessary to recover crashed XA
// * transactions.
// *
// * @author Marcus Portmann
// */
//public class NarayanaRecoveryXAResourceRegistry implements RecoveryXAResourceRegistry {
//
//  private final RecoveryManager recoveryManager;
//
//  public NarayanaRecoveryXAResourceRegistry() {
//    RecoveryManager.delayRecoveryManagerThread();
//
//    this.recoveryManager = RecoveryManager.manager();
//  }
//
//  /**
//   * Register the XAResources necessary to recover crashed XA transactions.
//   *
//   * @param recoveryXAResources the recovery XAResources to register
//   */
//  @Override
//  public void addRecoveryXAResources(RecoveryXAResources recoveryXAResources) {
//    XARecoveryModule xaRecoveryModule = null;
//    for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
//      if (recoveryModule instanceof XARecoveryModule) {
//        xaRecoveryModule = (XARecoveryModule) recoveryModule;
//        break;
//      }
//    }
//
//    if (xaRecoveryModule == null) {
//      throw new IllegalStateException("Failed to retrieve the XARecoveryModule");
//    }
//
//    xaRecoveryModule.addXAResourceRecoveryHelper(
//        new XAResourceRecoveryHelperImpl(recoveryXAResources.getXAResources()));
//  }
//
//  public void addSerializableXAResourceDeserializer(
//      SerializableXAResourceDeserializer serializableXAResourceDeserializer) {
//
//    XARecoveryModule xaRecoveryModule = null;
//    for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
//      if (recoveryModule instanceof XARecoveryModule) {
//        xaRecoveryModule = (XARecoveryModule) recoveryModule;
//        break;
//      }
//    }
//
//    if (xaRecoveryModule == null) {
//      throw new IllegalStateException("Failed to retrieve the XARecoveryModule");
//    }
//
//    xaRecoveryModule.addSerializableXAResourceDeserializer(serializableXAResourceDeserializer);
//  }
//
//  /**
//   * Unregister the XAResources used to recover crashed XA transactions.
//   *
//   * @param recoveryXAResources the recovery XAResources to unregister
//   */
//  @Override
//  public void removeRecoveryXAResources(RecoveryXAResources recoveryXAResources) {
//    XARecoveryModule xaRecoveryModule = null;
//    for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
//      if (recoveryModule instanceof XARecoveryModule) {
//        xaRecoveryModule = (XARecoveryModule) recoveryModule;
//        break;
//      }
//    }
//
//    if (xaRecoveryModule == null) {
//      throw new IllegalStateException("Failed to retrieve the XARecoveryModule");
//    }
//
//    xaRecoveryModule.removeXAResourceRecoveryHelper(
//        new XAResourceRecoveryHelperImpl(recoveryXAResources.getXAResources()));
//  }
//
//  public void resume() {
//    recoveryManager.resume();
//  }
//
//  public void start() {
//    recoveryManager.initialize();
//    recoveryManager.startRecoveryManagerThread();
//  }
//
//  public void suspend() {
//    recoveryManager.suspend(false);
//  }
//
//  protected void shutdown() {
//    recoveryManager.terminate();
//  }
//}
