/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.hdfs.server.common.AutoCloseLock;
import org.apache.hadoop.hdfs.server.common.LockManager;
import org.apache.hadoop.hdfs.server.common.LockManager.LockLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestDataSetLockManager {
  DataSetLockManager manager;

  @Before
  public void init() {
    manager = new DataSetLockManager();
  }

  @Test(timeout = 5000)
  public void testBaseFunc() {
    manager.addLock(LockLevel.BLOCK_POOl, "BPtest");
    manager.addLock(LockLevel.VOLUME, "BPtest", "Volumetest");

    AutoCloseLock lock = manager.writeLock(LockLevel.BLOCK_POOl, "BPtest");
    AutoCloseLock lock1 = manager.readLock(LockLevel.BLOCK_POOl, "BPtest");
    lock1.close();
    lock.close();

    manager.lockLeakCheck();
    assertNull(manager.getLastException());

    AutoCloseLock lock2 = manager.writeLock(LockLevel.VOLUME, "BPtest", "Volumetest");
    AutoCloseLock lock3 = manager.readLock(LockLevel.VOLUME, "BPtest", "Volumetest");
    lock3.close();
    lock2.close();

    manager.lockLeakCheck();
    assertNull(manager.getLastException());

    AutoCloseLock lock4 = manager.writeLock(LockLevel.BLOCK_POOl, "BPtest");
    AutoCloseLock lock5 = manager.readLock(LockLevel.VOLUME, "BPtest", "Volumetest");
    lock5.close();
    lock4.close();

    manager.lockLeakCheck();
    assertNull(manager.getLastException());

    manager.writeLock(LockLevel.VOLUME, "BPtest", "Volumetest");
    manager.lockLeakCheck();

    Exception lastException = manager.getLastException();
    assertEquals(lastException.getMessage(), "lock Leak");
  }

  @Test(timeout = 5000)
  public void testAcquireWriteLockError() throws InterruptedException {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        manager.readLock(LockManager.LockLevel.BLOCK_POOl, "test");
        manager.writeLock(LockLevel.BLOCK_POOl, "test");
      }
    });
    t.start();
    Thread.sleep(1000);
    manager.lockLeakCheck();
    Exception lastException = manager.getLastException();
    t.stop();
    assertEquals(lastException.getMessage(), "lock Leak");
  }

  @Test(timeout = 5000)
  public void testLockLeakCheck() {
    manager.writeLock(LockLevel.BLOCK_POOl, "test");
    manager.lockLeakCheck();
    Exception lastException = manager.getLastException();
    assertEquals(lastException.getMessage(), "lock Leak");
  }
}
