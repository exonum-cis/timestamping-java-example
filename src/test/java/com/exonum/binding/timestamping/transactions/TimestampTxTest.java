package com.exonum.binding.timestamping.transactions;

import static com.exonum.binding.common.blockchain.ExecutionStatuses.serviceError;
import static com.exonum.binding.common.blockchain.ExecutionStatuses.success;
import static com.exonum.binding.common.crypto.CryptoFunctions.ed25519;
import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.timestamping.TimestampingArtifactInfo.TIMESTAMPING_SERVICE_ID;
import static com.exonum.binding.timestamping.TimestampingArtifactInfo.TIMESTAMPING_SERVICE_NAME;
import static com.exonum.binding.timestamping.TransactionMessages.createTimestampTx;
import static com.exonum.binding.timestamping.transactions.TransactionError.TIMESTAMP_ALREADY_EXISTS;
import static com.exonum.binding.timestamping.utils.Utils.zdtToTimestamp;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.testkit.FakeTimeProvider;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.timestamping.Integration;
import com.exonum.binding.timestamping.TimestampingArtifactInfo;
import com.exonum.binding.timestamping.TimestampingSchema;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;
import com.exonum.core.messages.Runtime;
import com.google.protobuf.ByteString;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Integration
class TimestampTxTest {

  private static final ZonedDateTime INITIAL_TIME = ZonedDateTime.now(ZoneOffset.UTC);

  private final FakeTimeProvider timeProvider = FakeTimeProvider.create(INITIAL_TIME);

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TimestampingArtifactInfo.createQaServiceTestkit(timeProvider)
  );

  @BeforeEach
  void setUpConsolidatedTime(TestKit testKit) {
    // Commit two blocks for time oracle to update consolidated time. Two blocks are needed as
    // after the first block time transactions are generated and after the second one they are
    // processed
    testKit.createBlock();
    testKit.createBlock();
  }

  @Test
  void executeTimestamp(TestKit testKit) {
    Snapshot view = testKit.getSnapshot();
    TimestampingSchema schema = new TimestampingSchema(view, TIMESTAMPING_SERVICE_NAME);
    TimeSchema timeOracle = schema.timeSchema();
    Optional<ZonedDateTime> currentTime = timeOracle.getTime().toOptional();

    String metadata = "metadata";
    HashCode contentHash = sha256().hashString(metadata, UTF_8);
    TransactionMessage tx = createTimestampTx(metadata, contentHash, TIMESTAMPING_SERVICE_ID);
    testKit.createBlockWithTransactions(tx);

    view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<Runtime.ExecutionStatus> txResult = blockchain.getTxResult(tx.hash());
    assertThat(txResult).hasValue(success());

    view = testKit.getSnapshot();
    schema = new TimestampingSchema(view, TIMESTAMPING_SERVICE_NAME);
    MapIndex<HashCode, TimestampEntry> timestamps = schema.timestamps();
    TimestampEntry entry = timestamps.get(contentHash);

    TimestampEntry timestampEntry = TimestampEntry.newBuilder()
        .setTxHash(ByteString.copyFrom(tx.hash().asBytes()))
        .setTimestamp(TxMessageProtos.TimestampTxBody.newBuilder()
            .setContentHash(ByteString.copyFrom(contentHash.asBytes()))
            .setMetadata(metadata)
            .build())
        .setTime(zdtToTimestamp(currentTime.get()))
        .build();

    assertThat(entry).isEqualTo(timestampEntry);
  }

  @Test
  void executeAlreadyExistingCounter(TestKit testKit) {
    String metadata = "metadata";
    HashCode contentHash = sha256().hashString(metadata, UTF_8);

    KeyPair key1 = ed25519().generateKeyPair();
    KeyPair key2 = ed25519().generateKeyPair();
    TransactionMessage transactionMessage = createTimestampTx(metadata,
        contentHash, TIMESTAMPING_SERVICE_ID, key1);
    TransactionMessage transactionMessage2 = createTimestampTx(metadata,
        contentHash, TIMESTAMPING_SERVICE_ID, key2);
    testKit.createBlockWithTransactions(transactionMessage);
    testKit.createBlockWithTransactions(transactionMessage2);

    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    Optional<Runtime.ExecutionStatus> txResult = blockchain
        .getTxResult(transactionMessage2.hash());
    Runtime.ExecutionStatus expectedTransactionResult =
        serviceError(TIMESTAMP_ALREADY_EXISTS.code);
    assertThat(txResult).hasValue(expectedTransactionResult);
  }
}
