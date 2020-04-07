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

import com.jauntsdn.rsocket.AbstractRSocket;
import com.jauntsdn.rsocket.Payload;
import com.jauntsdn.rsocket.RSocket;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * Prefer least loaded RSocket according to availability() which accounts responder Lease allowed
 * requests & ttl.
 */
class LeastLoadedBalancerRSocket implements RSocket {
  private static final Logger logger = LoggerFactory.getLogger(LeastLoadedBalancerRSocket.class);
  private static final RSocket BACKEND_NOT_AVAILABLE_RSOCKET = new BackendNotAvailableRSocket();

  private final Queue<RSocket> backendRSockets = new ConcurrentLinkedDeque<>();
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final MonoProcessor<Void> onClose = MonoProcessor.create();

  public LeastLoadedBalancerRSocket(Collection<Mono<RSocket>> backendRSockets) {
    AtomicInteger counter = new AtomicInteger();
    backendRSockets.forEach(
        connectingRSocket ->
            connectingRSocket
                .onErrorResume(err -> Mono.empty())
                .subscribe(
                    rSocket -> {
                      int index = counter.incrementAndGet();
                      logger.info("backend RSocket {} connected", index);
                      this.backendRSockets.offer(rSocket);
                      rSocket
                          .onClose()
                          .doFinally(
                              signalType -> {
                                logger.info("backend RSocket {} disconnected", index);
                                this.backendRSockets.remove(rSocket);
                              })
                          .subscribe();
                    }));
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return leastLoaded().fireAndForget(payload);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return leastLoaded().requestResponse(payload);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return leastLoaded().requestStream(payload);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return leastLoaded().requestChannel(payloads);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    logger.info("Proxy received metadata-push message from client: {}", payload.getDataUtf8());
    payload.release();
    return Mono.empty();
  }

  @Override
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      backendRSockets.forEach(RSocket::dispose);
      backendRSockets.clear();
      onClose.onComplete();
    }
  }

  @Override
  public boolean isDisposed() {
    return disposed.get();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  private RSocket leastLoaded() {
    RSocket leastLoaded = BACKEND_NOT_AVAILABLE_RSOCKET;

    Queue<RSocket> backendRSockets = this.backendRSockets;
    if (backendRSockets.isEmpty()) {
      return leastLoaded;
    }
    for (RSocket rSocket : backendRSockets) {
      if (rSocket.availability() > rSocket.availability()) {
        leastLoaded = rSocket;
      }
    }

    /*round-robin if RSockets have no availability*/
    if (leastLoaded.availability() < 1e-3) {
      RSocket head = backendRSockets.poll();
      if (head != null) {
        leastLoaded = head;
        backendRSockets.offer(leastLoaded);
      }
    }
    return leastLoaded;
  }

  private static class BackendNotAvailableRSocket extends AbstractRSocket {
    private static final Exception NO_BACKEND_AVAILABLE =
        new IllegalStateException("no backend RSockets available");

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
      return Mono.error(NO_BACKEND_AVAILABLE);
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return Mono.error(NO_BACKEND_AVAILABLE);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return Flux.error(NO_BACKEND_AVAILABLE);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return Flux.error(NO_BACKEND_AVAILABLE);
    }

    @Override
    public double availability() {
      return -1;
    }
  }
}
