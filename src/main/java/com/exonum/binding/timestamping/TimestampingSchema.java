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

package com.exonum.binding.timestamping;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;

import java.util.Collections;
import java.util.List;

/**
 * A schema of the Timestamping service.
 */
public final class TimestampingSchema implements Schema {

  private final View view;
  /** A namespace of Timestamping service collections. */
  private final String namespace;

  public TimestampingSchema(View view, String serviceName) {
    this.view = checkNotNull(view);
    namespace = serviceName + ".";
  }

  /**
   * Returns state hash for the the service.
   */
  @Override
  public List<HashCode> getStateHashes() {
    return Collections.singletonList(timestamps().getIndexHash());
  }

  /**
   * Returns the index containing the name of the time oracle to use.
   */
  public EntryIndexProxy<String> timeOracleName() {
    String name = fullIndexName("time_oracle_name");
    return EntryIndexProxy.newInstance(name, view, StandardSerializers.string());
  }

  /**
   * Returns the time schema of the time oracle this timestamping service uses.
   * {@link #timeOracleName()} must be non-empty.
   */
  public TimeSchema timeSchema() {
    return TimeSchema.newInstance(view, timeOracleName().get());
  }

  /**
   * Returns a proof map of timestamps.
   */
  public ProofMapIndexProxy<HashCode, TimestampEntry> timestamps() {
    String name = fullIndexName("timestamps");
    return ProofMapIndexProxy.newInstanceNoKeyHashing(
            name, view, StandardSerializers.hash(),
            protobuf(TimestampEntry.class)
    );
  }

  private String fullIndexName(String name) {
    return namespace + name;
  }
}
