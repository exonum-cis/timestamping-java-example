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

import com.exonum.binding.core.service.AbstractServiceModule;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.timestamping.transactions.ServiceTransactionConverter;
import com.google.inject.Singleton;
import org.pf4j.Extension;

/**
 * A module of the Timestamping service.
 */
@Extension
public final class TimestampingServiceModule extends AbstractServiceModule {

  @Override
  protected void configure() {
    bind(Service.class).to(TimestampingServiceImpl.class);
    bind(TimestampingService.class).to(TimestampingServiceImpl.class);
    // Make sure Service remains a singleton.
    bind(TimestampingServiceImpl.class).in(Singleton.class);

    bind(com.exonum.binding.core.service.TransactionConverter.class)
        .to(ServiceTransactionConverter.class);
  }
}
