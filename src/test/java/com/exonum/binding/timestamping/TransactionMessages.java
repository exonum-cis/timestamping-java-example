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

import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.timestamping.transactions.ServiceTransaction.*;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.Chain;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampTxBody;
import com.google.protobuf.ByteString;

public final class TransactionMessages {

  private static final KeyPair TEST_KEY_PAIR = ed25519().generateKeyPair();

  /**
   * Returns a TimestampingTx transaction message with
   * the given arguments and signed with the given key pair.
   */
  public static TransactionMessage createTimestampTx(String metadata, HashCode contentHash,
                                                     int qaServiceId, KeyPair keyPair) {
    return TransactionMessage.builder()
        .chain(Chain.DEFAULT)
        .serviceId(qaServiceId)
        .transactionId(CREATE_TIMESTAMP.id())
        .payload(TimestampTxBody.newBuilder()
            .setContentHash(ByteString.copyFrom(contentHash.asBytes()))
            .setMetadata(metadata)
            .build())
        .sign(keyPair);
  }

  /**
   * Returns a TimestampTx transaction message with the given arguments and signed with the test key
   * pair.
   */
  public static TransactionMessage createTimestampTx(String metadata, HashCode contentHash,
                                                     int qaServiceId) {
    return createTimestampTx(metadata, contentHash, qaServiceId, TEST_KEY_PAIR);
  }

  private TransactionMessages() {
  }
}
