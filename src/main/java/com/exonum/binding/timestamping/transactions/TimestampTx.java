package com.exonum.binding.timestamping.transactions;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.timestamping.transactions.TransactionError.TIMESTAMP_ALREADY_EXISTS;
import static com.exonum.binding.timestamping.transactions.TransactionError.TIME_NOT_AVAILABLE;
import static com.exonum.binding.timestamping.utils.Utils.zdtToTimestamp;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.transaction.Transaction;
import com.exonum.binding.core.transaction.TransactionContext;
import com.exonum.binding.core.transaction.TransactionExecutionException;
import com.exonum.binding.time.TimeSchema;
import com.exonum.binding.timestamping.TimestampingSchema;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampTxBody;
import com.google.protobuf.ByteString;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A transaction creating a new timestamping.
 */
public final class TimestampTx implements Transaction {
  private static final int ID = ServiceTransaction.CREATE_TIMESTAMP.id();

  private static final Serializer<TxMessageProtos.TimestampTxBody> PROTO_SERIALIZER =
      protobuf(TxMessageProtos.TimestampTxBody.class);

  private final HashCode contentHash;
  private final String metadata;

  TimestampTx(HashCode contentHash, String metadata) {
    this.contentHash = checkNotNull(contentHash);
    this.metadata = checkNotNull(metadata);
  }

  static TimestampTx fromBytes(byte[] bytes) {
    TxMessageProtos.TimestampTxBody body = PROTO_SERIALIZER.fromBytes(bytes);
    return new TimestampTx(
        HashCode.fromBytes(body.getContentHash().toByteArray()),
        body.getMetadata()
    );
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    TimestampingSchema schema = new TimestampingSchema(context.getFork(), context.getServiceName());
    MapIndex<HashCode, TimestampEntry> timestamps = schema.timestamps();

    if (timestamps.containsKey(contentHash)) {
      throw new TransactionExecutionException(TIMESTAMP_ALREADY_EXISTS.code);
    }

    TimeSchema timeOracle = schema.timeSchema();
    Optional<ZonedDateTime> currentTime = timeOracle.getTime().toOptional();
    if (!currentTime.isPresent()) {
      throw new TransactionExecutionException(TIME_NOT_AVAILABLE.code);
    }

    TimestampEntry timestampEntry = createTimestampEntry(context.getTransactionMessageHash(),
        currentTime.get());
    timestamps.put(contentHash, timestampEntry);
  }

  private TimestampEntry createTimestampEntry(HashCode txHash, ZonedDateTime currentTime) {
    return TimestampEntry.newBuilder()
        .setTxHash(ByteString.copyFrom(txHash.asBytes()))
        .setTimestamp(TimestampTxBody.newBuilder()
            .setContentHash(ByteString.copyFrom(contentHash.asBytes()))
            .setMetadata(metadata)
            .build())
        .setTime(zdtToTimestamp(currentTime))
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimestampTx that = (TimestampTx) o;
    return Objects.equals(metadata, that.metadata)
        && Objects.equals(contentHash, that.contentHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentHash, metadata);
  }

}
