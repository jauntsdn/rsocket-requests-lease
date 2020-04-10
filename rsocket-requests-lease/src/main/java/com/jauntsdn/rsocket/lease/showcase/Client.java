/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jauntsdn.rsocket.lease.showcase;

import com.jauntsdn.rsocket.RSocketFactory;
import com.jauntsdn.rsocket.frame.decoder.PayloadDecoder;
import com.jauntsdn.rsocket.lease.showcase.Client.Stats.Counters.Success;
import com.jauntsdn.rsocket.lease.showcase.service.Request;
import com.jauntsdn.rsocket.lease.showcase.service.Service;
import com.jauntsdn.rsocket.lease.showcase.service.ServiceClient;
import com.jauntsdn.rsocket.transport.netty.client.TcpClientTransport;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);
  private static final String divider = repeat("=", 80);

  public static void main(String... args) {
    String address = System.getProperty("ADDRESS", "localhost:8308");
    Duration duration = Duration.ofSeconds(Integer.getInteger("DURATION", 120));

    logger.info("Client connects proxy address {}", address);

    InetSocketAddress inetSocketAddress = address(address);

    Stats stats = new Stats();

    Flux.interval(Duration.ofSeconds(1))
        .onBackpressureDrop()
        .doOnNext(
            ignored -> {
              Stats.Counters counters = stats.counters();
              Collection<Success> successes = counters.successes();
              int errors = counters.errors();
              logger.info(divider);
              for (Stats.Counters.Success success : successes) {
                logger.info("Responses from {}: {}", success.getResponse(), success.getCount());
              }
              logger.info("Responses p99 latency millis: {}", counters.latencyMillis());
              logger.info("Rejected requests: {}", errors);
            })
        .subscribe();

    RSocketFactory.connect()
        .frameDecoder(PayloadDecoder.ZERO_COPY)
        .transport(
            TcpClientTransport.create(TcpClient.create().addressSupplier(() -> inetSocketAddress)))
        .start()
        .flatMapMany(
            rSocket -> {
              Service service =
                  new ServiceClient(rSocket, Optional.empty(), Optional.empty(), Optional.empty());
              return Flux.interval(Duration.ofMillis(1))
                  .onBackpressureDrop()
                  .flatMap(
                      ignored -> {
                        long requested = stats.request();

                        return service
                            .response(
                                Request.newBuilder().setMessage("message").build(),
                                Unpooled.EMPTY_BUFFER)
                            .doOnError(err -> stats.responseError())
                            .doOnNext(
                                response -> stats.responseSuccess(requested, response.getMessage()))
                            .onErrorResume(err -> Mono.empty());
                      },
                      Integer.MAX_VALUE);
            })
        .take(duration)
        .doFinally(signalType -> logger.info("Demo completed"))
        .blockLast();
  }

  static class Stats {
    private final Map<String, Integer> successCounts = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final Recorder histogram = new Recorder(3600000000000L, 3);

    public long request() {
      return System.nanoTime();
    }

    public void responseSuccess(long request, String response) {
      histogram.recordValue(System.nanoTime() - request);
      successCounts.compute(
          response,
          (resp, count) -> {
            if (count == null) {
              return 1;
            } else {
              return count + 1;
            }
          });
    }

    public void responseError() {
      errorCount.incrementAndGet();
    }

    public Counters counters() {
      int errors = errorCount.get();
      Set<Success> successes =
          successCounts
              .entrySet()
              .stream()
              .map(entry -> new Counters.Success(entry.getKey(), entry.getValue()))
              .collect(Collectors.toSet());

      errorCount.set(0);
      successCounts.clear();

      return new Counters(
          successes,
          errors,
          histogram.getIntervalHistogram().getValueAtPercentile(99.0) / 1_000_000);
    }

    static class Counters {
      private final Collection<Success> successes;
      private final int errors;
      private final long p99Millis;

      public Counters(Collection<Success> successes, int errors, long p99Millis) {
        this.successes = successes;
        this.errors = errors;
        this.p99Millis = p99Millis;
      }

      public Collection<Success> successes() {
        return successes;
      }

      public int errors() {
        return errors;
      }

      public long latencyMillis() {
        return p99Millis;
      }

      public static class Success {
        private final String response;
        private final int count;

        public Success(String response, int count) {
          this.response = response;
          this.count = count;
        }

        public String getResponse() {
          return response;
        }

        public int getCount() {
          return count;
        }
      }
    }
  }

  private static InetSocketAddress address(String address) {
    String[] hostPort = address.split(":");
    String host = hostPort[0];
    int port = Integer.parseInt(hostPort[1]);
    return new InetSocketAddress(host, port);
  }

  private static String repeat(String str, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }
    return sb.toString();
  }
}
