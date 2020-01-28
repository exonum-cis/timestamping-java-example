/*
 * Copyright 2019 The Exonum Team
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.runtime.ServiceInstanceSpec;
import com.exonum.binding.core.service.AbstractService;
import com.exonum.binding.core.service.Configuration;
import com.exonum.binding.core.service.Node;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.core.storage.indices.EntryIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.timestamping.Config.ServiceConfiguration;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import jdk.internal.jline.internal.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple Timestamping service.
 */
public final class TimestampingServiceImpl extends AbstractService implements TimestampingService {

  private static final Logger logger = LogManager.getLogger(TimestampingService.class);

  @Nullable
  private Node node;

  @Inject
  public TimestampingServiceImpl(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  public TimestampingSchema createDataSchema(View view) {
    return new TimestampingSchema(view, getName());
  }

  @Override
  public List<HashCode> getStateHashes(Snapshot snapshot) {
    List<HashCode> stateHashes = super.getStateHashes(snapshot);
    // Log the state hashes, so that the values passed to the native part of the framework
    // are known.
    logger.info("state hashes: {}", stateHashes);
    return stateHashes;
  }

  @Override
  public void initialize(Fork fork, Configuration configuration) {
    // Init the time oracle
    updateTimeOracle(fork, configuration);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<TimestampEntry> getTimestamp(HashCode contentHash) {
    checkBlockchainInitialized();

    return node.withSnapshot((view) -> {
      TimestampingSchema schema = createDataSchema(view);
      MapIndex<HashCode, TimestampEntry> timestamps = schema.timestamps();
      if (!timestamps.containsKey(contentHash)) {
        return Optional.empty();
      }
      TimestampEntry timestampEntry = timestamps.get(contentHash);

      return Optional.ofNullable(timestampEntry);
    });
  }

  @Override
  @SuppressWarnings("ConstantConditions")  // Node is not null.
  public Optional<ZonedDateTime> getTime() {
    return node.withSnapshot(s -> {
      TimeSchema timeOracle = createDataSchema(s).timeSchema();
      EntryIndexProxy<ZonedDateTime> currentTime = timeOracle.getTime();
      return currentTime.toOptional();
    });
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }

  @Override
  public void verifyConfiguration(Fork fork, Configuration configuration) {
    ServiceConfiguration config = configuration.getAsMessage(ServiceConfiguration.class);
    checkConfiguration(config);
  }

  @Override
  public void applyConfiguration(Fork fork, Configuration configuration) {
    updateTimeOracle(fork, configuration);
  }

  private void checkConfiguration(ServiceConfiguration config) {
    String timeOracleName = config.getTimeOracleName();
    // Check the time oracle name is non-empty.
    // We do *not* check if the time oracle is active to (a) allow running this service with
    // reduced read functionality without time oracle; (b) testing time schema when it is not
    // active.
    checkArgument(!Strings.isNullOrEmpty(timeOracleName), "Empty time oracle name: %s",
        timeOracleName);
  }

  private void updateTimeOracle(Fork fork, Configuration configuration) {
    TimestampingSchema schema = createDataSchema(fork);
    ServiceConfiguration config = configuration.getAsMessage(ServiceConfiguration.class);

    // Verify the configuration
    checkConfiguration(config);

    // Save the configuration
    String timeOracleName = config.getTimeOracleName();
    schema.timeOracleName().set(timeOracleName);
  }
}
