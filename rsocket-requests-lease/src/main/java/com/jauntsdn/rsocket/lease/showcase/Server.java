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

import static java.time.Duration.*;

import com.jauntsdn.rsocket.Leases;
import com.jauntsdn.rsocket.RSocket;
import com.jauntsdn.rsocket.RSocketFactory;
import com.jauntsdn.rsocket.exceptions.RejectedException;
import com.jauntsdn.rsocket.frame.FrameType;
import com.jauntsdn.rsocket.frame.decoder.PayloadDecoder;
import com.jauntsdn.rsocket.lease.Lease;
import com.jauntsdn.rsocket.lease.showcase.service.ServiceServer;
import com.jauntsdn.rsocket.rpc.frames.Metadata;
import com.jauntsdn.rsocket.rpc.rsocket.RequestHandlingRSocket;
import com.jauntsdn.rsocket.transport.netty.server.CloseableChannel;
import com.jauntsdn.rsocket.transport.netty.server.TcpServerTransport;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.HdrHistogram.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Scheduler;
import reactor.netty.tcp.TcpServer;

public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  private static final String divider = repeat("=", 80);

  public static void main(String... args) {
    Integer leaseAllowedRequests = Integer.getInteger("ALLOWED_REQUESTS", 200);
    String concurrencyDelay =
        System.getProperty("CONCURRENCY_DELAY", "10 => 2; 50 => 5; 120 => 20; => 5000");
    String host = System.getProperty("HOST", "localhost");
    Integer port = Integer.getInteger("PORT", 8309);
    InetSocketAddress address = new InetSocketAddress(host, port);

    logger.info("Server bind address is {}:{}", host, port);
    logger.info("Lease allowed requests per second: {}", leaseAllowedRequests);

    Duration leaseTimeToLive = Duration.ofSeconds(1);
    Leases.ServerConfigurer leases =
        scheduler ->
            Leases.<ServiceStatsRecorder>create()
                .sender(
                    new StaticLeaseSender(
                        scheduler, address, leaseTimeToLive, leaseAllowedRequests))
                .stats(new ServiceStatsRecorder());

    RSocketFactory.receive()
        .lease(leases)
        .frameDecoder(PayloadDecoder.ZERO_COPY)
        .errorConsumer(errorConsumer())
        .acceptor((setup, requesterRSocket) -> serverAcceptor(address, concurrencyDelay))
        .transport(TcpServerTransport.create(TcpServer.create().addressSupplier(() -> address)))
        .start()
        .flatMap(CloseableChannel::onClose)
        .block();
  }

  private static Mono<RSocket> serverAcceptor(InetSocketAddress address, String concurrencyDelay) {
    return Mono.just(
        new RequestHandlingRSocket(
            new ServiceServer(
                new SaturableService(address, concurrencyDelay),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())));
  }

  private static class StaticLeaseSender
      implements Function<Optional<ServiceStatsRecorder>, Flux<Lease>> {
    private final Scheduler scheduler;
    private final String address;
    private final int ttlMillis;
    private final Duration ttl;
    private final int allowedRequests;

    public StaticLeaseSender(
        Scheduler scheduler, InetSocketAddress address, Duration ttl, int allowedRequests) {
      this.scheduler = scheduler;
      this.address = String.format("%s:%d", address.getHostName(), address.getPort());
      this.ttl = ttl;
      this.ttlMillis = (int) ttl.toMillis();
      this.allowedRequests = allowedRequests;
    }

    @Override
    public Flux<Lease> apply(Optional<ServiceStatsRecorder> serviceStatsRecorder) {
      if (serviceStatsRecorder.isPresent()) {
        ServiceStats stats = serviceStatsRecorder.get();
        logger.info("Service stats is {}", stats.getClass().getSimpleName());
        return Flux.interval(ofSeconds(0), ttl, scheduler)
            .takeUntilOther(stats.onClose())
            .map(
                ignored -> {
                  logger.info(divider);
                  ServiceStats.Counters counters = stats.counters();
                  int accepted = counters.acceptedRequests();
                  if (accepted > 0) {
                    logger.info("service {} accepted {} requests", address, accepted);
                  }
                  int rejected = counters.rejectedRequests();
                  if (rejected > 0) {
                    logger.info("service {} rejected {} requests", address, rejected);
                  }
                  Map<String, Long> latencies = stats.latencies();
                  latencies.forEach(
                      (request, latency) ->
                          logger.info("service call {} latency is {} millis", request, latency));
                  logger.info(
                      "responder sends new lease, allowed requests is {}, time-to-live is {} millis",
                      allowedRequests,
                      ttlMillis);
                  return Lease.create(ttlMillis, allowedRequests);
                });
      } else {
        return Flux.never();
      }
    }
  }

  private interface ServiceStats {

    Counters counters();

    Map<String, Long> latencies();

    Mono<Void> onClose();

    class Counters {
      private final int accepted;
      private final int rejected;

      public Counters(int accepted, int rejected) {
        this.accepted = accepted;
        this.rejected = rejected;
      }

      public int acceptedRequests() {
        return accepted;
      }

      public int rejectedRequests() {
        return rejected;
      }
    }
  }

  private static class ServiceStatsRecorder implements Leases.StatsRecorder<String>, ServiceStats {
    private final AtomicInteger acceptedCounter = new AtomicInteger();
    private final AtomicInteger rejectedCounter = new AtomicInteger();
    private final ConcurrentMap<String, Recorder> histograms = new ConcurrentHashMap<>();

    private final MonoProcessor<Void> onClose = MonoProcessor.create();

    @Override
    public Counters counters() {
      int accepts = acceptedCounter.getAndSet(0);
      int rejects = rejectedCounter.getAndSet(0);

      return new Counters(accepts, rejects);
    }

    @Override
    public Map<String, Long> latencies() {
      return histograms
          .entrySet()
          .stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  entry -> {
                    long latencyNanos =
                        entry.getValue().getIntervalHistogram().getValueAtPercentile(99.0);
                    return TimeUnit.NANOSECONDS.toMillis(latencyNanos);
                  }));
    }

    @Override
    public Mono<Void> onClose() {
      return onClose;
    }

    @Override
    public String onRequestStarted(FrameType requestType, ByteBuf metadata) {
      String svc = Metadata.getService(metadata);
      String service = svc.substring(svc.lastIndexOf(".") + 1);
      String method = Metadata.getMethod(metadata);
      return String.format("%s/%s", service, method);
    }

    @Override
    public void onResponseStarted(
        FrameType requestType, String request, Signal<Void> firstSignal, long latencyNanos) {
      Recorder recorder = histograms.computeIfAbsent(request, r -> new Recorder(3600000000000L, 3));
      recorder.recordValue(latencyNanos);
    }

    @Override
    public void onResponseTerminated(
        FrameType requestType, String request, Signal<Void> lastSignal) {
      if (lastSignal.isOnError() && lastSignal.getThrowable() instanceof RejectedException) {
        rejectedCounter.incrementAndGet();
      } else if (lastSignal.isOnComplete()) {
        acceptedCounter.incrementAndGet();
      }
    }

    @Override
    public void onRSocketClosed() {
      onClose.onComplete();
    }
  }

  private static Consumer<Throwable> errorConsumer() {
    return err -> {
      if (err instanceof IOException) {
        return;
      }
      logger.error("Unexpected server error", err);
    };
  }

  private static String repeat(String str, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }
    return sb.toString();
  }
}
