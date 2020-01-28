/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.timestamping.transactions;

/**
 * All known Timestamping service transactions.
 *
 * @implNote Keep in sync with {@link ServiceTransactionConverter#TRANSACTION_FACTORIES}.
 */
public enum ServiceTransaction {
  // Create timestamp transaction.
  CREATE_TIMESTAMP(1);

  private final int id;

  ServiceTransaction(int id) {
    this.id = id;
  }

  /**
   * Returns the unique id of this transaction.
   */
  public int id() {
    return id;
  }
}
