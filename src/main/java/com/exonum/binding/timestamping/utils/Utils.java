package com.exonum.binding.timestamping.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class Utils {
  /**
   * Converts protobuf Timestamp to java.time.ZonedDataTime
   */
  public static Timestamp zdtToTimestamp(ZonedDateTime value) {
    checkArgument(value.getZone() == ZoneOffset.UTC,
        "ZonedDateTime value should be in UTC, but was %s", value.getZone());
    return Timestamp.newBuilder()
        .setSeconds(value.toEpochSecond())
        .setNanos(value.getNano())
        .build();
  }

  /**
   * Converts java.time.ZonedDataTime to protobuf Timestamp
   */
  public static ZonedDateTime timestampToZdt(Timestamp value) {
    checkNotNull(value);
    Instant instant = Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
    return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private Utils() {
  }
}
