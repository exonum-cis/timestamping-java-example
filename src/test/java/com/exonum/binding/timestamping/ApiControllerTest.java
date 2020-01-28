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

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.timestamping.ApiController.TimestampingPaths.TIME_PATH;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.json.JsonSerializer;
import com.exonum.binding.core.blockchain.serialization.CoreTypeAdapterFactory;
import com.exonum.binding.testkit.TestKitExtension;
import com.exonum.binding.timestamping.models.TimeDto;
import com.google.gson.Gson;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Integration
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
// Execute the tests sequentially, as each of them creates a Vertx instance with its
// own thread pool, which drives the delays up.
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("WeakerAccess")
class ApiControllerTest {

  @RegisterExtension
  TestKitExtension testKitExtension = new TestKitExtension(
      TimestampingArtifactInfo.createQaServiceTestkit()
  );

  private static final String HOST = "0.0.0.0";

  private static final HashCode EXPECTED_TX_HASH = sha256().hashInt(1);

  private static final HashCode HASH_1 = HashCode.fromInt(0x00);

  private static final Gson JSON_SERIALIZER = JsonSerializer.builder()
      .registerTypeAdapterFactory(CoreTypeAdapterFactory.create())
      .create();

  @Mock
  TimestampingService timestampingService;

  HttpServer httpServer;

  WebClient webClient;

  volatile int port = -1;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext context) {
    httpServer = vertx.createHttpServer();
    webClient = WebClient.create(vertx);

    Router router = Router.router(vertx);
    new ApiController(timestampingService).mountApi(router);

    httpServer.requestHandler(router)
        .listen(0, context.succeeding(result -> {

          // Set the actual server port.
          port = result.actualPort();

          context.completeNow();
        }));
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    webClient.close();
    httpServer.close(context.succeeding(r -> context.completeNow()));
  }

  @Test
  @DisplayName("failureHandler converts unexpected exceptions to HTTP_INTERNAL_ERROR")
  void failureHandlerUnexpectedException(VertxTestContext context) {
    HashCode id = HASH_1;
    String message = "Boom";
    // This test is not specific to any service method — what matters is the exception type:
    // RuntimeException.
    when(timestampingService.getTimestamp(id))
        .thenThrow(new RuntimeException(message));
    String getCounterUri = getTimestampUri(id);

    get(getCounterUri)
        .send(context.succeeding(response -> context.verify(() -> {
          assertAll(
              () -> assertThat(response.statusCode()).isEqualTo(HTTP_INTERNAL_ERROR),
              () -> assertThat(response.bodyAsString()).contains(message));
          context.completeNow();
        })));
  }

  @Test
  void getTime(VertxTestContext context) {
    ZonedDateTime time = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    when(timestampingService.getTime()).thenReturn(Optional.of(time));

    get(TIME_PATH)
        .send(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode())
              .isEqualTo(HTTP_OK);

          String body = response.bodyAsString();
          TimeDto actualTime = JSON_SERIALIZER
              .fromJson(body, TimeDto.class);
          assertThat(actualTime.getTime()).isEqualTo(time);

          context.completeNow();
        })));
  }

  private HttpRequest<Buffer> post(String requestPath) {
    return webClient.post(port, HOST, requestPath);
  }

  private HttpRequest<Buffer> get(String requestPath) {
    return webClient.get(port, HOST, requestPath);
  }

  private String getTimestampUri(HashCode id) {
    return getTimestampUri(String.valueOf(id));
  }

  private String getTimestampUri(String id) {
    try {
      return "/timestamp/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported", e);
    }
  }
}
