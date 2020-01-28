package com.exonum.binding.timestamping.models;

import com.exonum.binding.common.hash.HashCode;
import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Value;

/**
 * DTO TimestampEntry class for JSON view.
 */
@Value
@Builder
public class TimestampEntryDto {
  TimestampDto timestamp;
  @SerializedName("tx_hash")
  HashCode transactionHash;
  ZonedDateTime time;
}
