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

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static com.exonum.binding.timestamping.ApiController.TimestampingPaths.*;
import static com.exonum.binding.timestamping.utils.Utils.timestampToZdt;
import static com.exonum.binding.timestamping.utils.Utils.zdtToTimestamp;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.timestamping.models.TimeDto;
import com.exonum.binding.timestamping.models.TimestampDto;
import com.exonum.binding.timestamping.models.TimestampEntryDto;
import com.exonum.binding.timestamping.transactions.TxMessageProtos;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampEntry;
import com.exonum.binding.timestamping.transactions.TxMessageProtos.TimestampTxBody;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApiController {

  private static final Logger logger = LogManager.getLogger(ApiController.class);

  private final TimestampingService service;

  ApiController(TimestampingService service) {
    this.service = service;
  }

  void mountApi(Router router) {
    // Mount the body handler to process bodies of some POST queries, and the handler of failures.
    router.route()
        .handler(BodyHandler.create())
        .failureHandler(this::failureHandler);

    // Mount the handlers of each request
    ImmutableMap<String, Handler<RoutingContext>> handlers =
        ImmutableMap.<String, Handler<RoutingContext>>builder()
            .put(GET_TIMESTAMP_PATH, this::getTimestamp)
            .put(TIME_PATH, this::getTime)
            .build();

    handlers.forEach((path, handler) ->
        router.route(path).handler(handler)
    );
  }

  private void getTimestamp(RoutingContext rc) {
    HashCode contentHash = getRequiredParameter(rc.request(), TIMESTAMP_HASH_PARAM,
        HashCode::fromString);
    Optional<TimestampEntry> timestampEntry = service.getTimestamp(contentHash);

    if (timestampEntry.isPresent()) {
      TimestampEntryDto response = timestampEntry.map(ApiController::toDto).get();
      rc.response()
          .putHeader("Content-Type", "application/json")
          .end(json().toJson(response));
    } else {
      rc.response()
          .setStatusCode(HTTP_NOT_FOUND)
          .end();
    }
  }

  public static TimestampEntryDto toDto(TimestampEntry entry) {
    TxMessageProtos.TimestampTxBody timestamp = entry.getTimestamp();

    TimestampDto timestampDto = TimestampDto.builder()
        .contentHash(HashCode.fromBytes(timestamp.getContentHash().toByteArray()))
        .metadata(timestamp.getMetadata())
        .build();

    return TimestampEntryDto.builder()
        .timestamp(timestampDto)
        .transactionHash(HashCode.fromBytes(entry.getTxHash().toByteArray()))
        .time(timestampToZdt(entry.getTime()))
        .build();
  }

  public static TimestampEntry fromDto(TimestampEntryDto dto) {
    TimestampDto timestampDto = dto.getTimestamp();
    HashCode contentHash = timestampDto.getContentHash();
    String metadata = timestampDto.getMetadata();

    HashCode txHash = dto.getTransactionHash();
    ZonedDateTime time = dto.getTime();

    return TimestampEntry.newBuilder()
        .setTxHash(ByteString.copyFrom(txHash.asBytes()))
        .setTimestamp(TimestampTxBody.newBuilder()
            .setContentHash(ByteString.copyFrom(contentHash.asBytes()))
            .setMetadata(metadata)
            .build())
        .setTime(zdtToTimestamp(time))
        .build();
  }

  private void getTime(RoutingContext rc) {
    Optional<TimeDto> time = service.getTime().map(TimeDto::new);
    respondWithJson(rc, time);
  }

  private static <T> T getRequiredParameter(HttpServerRequest request, String key,
                                            Function<String, T> converter) {
    return getRequiredParameter(request.params(), key, converter);
  }

  private static <T> T getRequiredParameter(MultiMap parameters, String key,
                                            Function<String, T> converter) {
    checkArgument(parameters.contains(key), "No required key (%s) in request parameters: %s",
        key, parameters);
    String parameter = parameters.get(key);
    try {
      return converter.apply(parameter);
    } catch (Exception e) {
      String message = String.format("Failed to convert parameter (%s): %s", key, e.getMessage());
      throw new IllegalArgumentException(message);
    }
  }

  private void failureHandler(RoutingContext rc) {
    logger.info("An error whilst processing request {}", rc.normalisedPath());

    Throwable requestFailure = rc.failure();
    if (requestFailure != null) {
      HttpServerResponse response = rc.response();
      if (isBadRequest(requestFailure)) {
        logger.info("Request error:", requestFailure);
        response.setStatusCode(HTTP_BAD_REQUEST);
      } else {
        logger.error("Internal error", requestFailure);
        response.setStatusCode(HTTP_INTERNAL_ERROR);
      }
      String description = Strings.nullToEmpty(requestFailure.getMessage());
      response.putHeader(CONTENT_TYPE, "text/plain")
          .end(description);
    } else {
      int failureStatusCode = rc.statusCode();
      rc.response()
          .setStatusCode(failureStatusCode)
          .end();
    }
  }

  /**
   * Returns true if the passed throwable corresponds to a bad request; false — otherwise.
   */
  private boolean isBadRequest(Throwable requestFailure) {
    // All IllegalArgumentExceptions (including NumberFormatException) and IndexOutOfBoundsException
    // are considered to be caused by a bad request. Other Throwables are considered internal
    // errors.
    return (requestFailure instanceof IllegalArgumentException
        || requestFailure instanceof IndexOutOfBoundsException);
  }

  private <T> void respondWithJson(RoutingContext rc, Optional<T> responseBody) {
    if (responseBody.isPresent()) {
      respondWithJson(rc, responseBody.get());
    } else {
      respondNotFound(rc);
    }
  }

  private void respondWithJson(RoutingContext rc, Object responseBody) {
    rc.response()
        .putHeader(CONTENT_TYPE, "application/json")
        .end(json().toJson(responseBody));
  }

  private void respondNotFound(RoutingContext rc) {
    rc.response()
        .setStatusCode(HTTP_NOT_FOUND)
        .end();
  }

  static class TimestampingPaths {
    @VisibleForTesting
    static final String TIMESTAMP_HASH_PARAM = "content_hash";
    static final String GET_TIMESTAMP_PATH = "/timestamp/:" + TIMESTAMP_HASH_PARAM;
    @VisibleForTesting
    static final String TIME_PATH = "/time";
  }

}
