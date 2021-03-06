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

import static com.exonum.binding.timestamping.TimestampingArtifactInfo.TIMESTAMPING_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class TimestampingSchemaTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TimestampingArtifactInfo.createQaServiceTestkit());

  @Test
  void getStateHashesEmptyDb(TestKit testKit) {
    Snapshot view = testKit.getSnapshot();
    TimestampingSchema schema = new TimestampingSchema(view, TIMESTAMPING_SERVICE_NAME);

    List<HashCode> stateHashes = schema.getStateHashes();

    assertThat(stateHashes).hasSize(1);

    HashCode timestampsRootHash = schema.timestamps().getIndexHash();
    assertThat(stateHashes.get(0)).isEqualTo(timestampsRootHash);
  }
}
