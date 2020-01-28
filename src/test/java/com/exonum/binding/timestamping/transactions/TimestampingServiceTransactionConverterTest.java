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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.timestamping.transactions.ServiceTransaction.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampTxBody;
import com.google.protobuf.ByteString;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class TimestampingServiceTransactionConverterTest {

  private ServiceTransactionConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ServiceTransactionConverter();
  }

  @ParameterizedTest
  @EnumSource(ServiceTransaction.class)
  void hasFactoriesForEachTransaction(ServiceTransaction tx) {
    // Check that the ServiceTransaction enum is kept in sync with the map of transaction factories,
    // i.e., each transaction type is mapped to the corresponding factory.
    int id = tx.id();

    assertThat(ServiceTransactionConverter.TRANSACTION_FACTORIES)
        .as("No entry for transaction %s with id=%d", tx, id)
        .containsKey(id);
  }

  @ParameterizedTest
  @MethodSource("transactions")
  void toTransaction(int txId, byte[] arguments, Transaction expectedTx) {
    Transaction transaction = converter.toTransaction(txId, arguments);

    assertThat(transaction).isEqualTo(expectedTx);
  }

  private static Collection<Arguments> transactions() {
    List<Arguments> transactionTemplates = asList(
        timestampArgs()
    );

    // Check that the test data includes all known transactions.
    assertThat(transactionTemplates).hasSameSizeAs(ServiceTransaction.values());

    return transactionTemplates;
  }

  private static Arguments timestampArgs() {
    String metadata = "metadata";
    HashCode hashCode = sha256().hashString(metadata, UTF_8);

    return arguments(CREATE_TIMESTAMP.id(),
        TimestampTxBody.newBuilder()
            .setContentHash(ByteString.copyFrom(hashCode.asBytes()))
            .setMetadata(metadata)
            .build()
            .toByteArray(),
        new TimestampTx(hashCode, metadata)
    );
  }
}
