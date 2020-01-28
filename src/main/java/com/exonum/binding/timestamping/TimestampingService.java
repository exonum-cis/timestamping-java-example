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

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Configurable;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A simple service for Timestamping.
 */
public interface TimestampingService extends Service, Configurable {
  Optional<TimestampEntry> getTimestamp(HashCode contentHash);

  Optional<ZonedDateTime> getTime();
}
