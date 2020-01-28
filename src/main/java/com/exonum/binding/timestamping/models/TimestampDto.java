package com.exonum.binding.timestamping.models;

import com.exonum.binding.common.hash.HashCode;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

/**
 * DTO Timestamp class for JSON view.
 */
@Value
@Builder
public class TimestampDto {
  @SerializedName("content_hash")
  HashCode contentHash;
  String metadata;
}
