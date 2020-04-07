package com.jauntsdn.rsocket.lease.showcase;
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

import com.jauntsdn.rsocket.lease.showcase.service.Request;
import com.jauntsdn.rsocket.lease.showcase.service.Response;
import com.jauntsdn.rsocket.lease.showcase.service.Service;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

class SaturableService implements Service {
  private final String address;
  private final ConcurrencyDelay concurrencyDelay;

  public SaturableService(InetSocketAddress address, String concurrencyDelays) {
    this.address = String.format("%s:%d", address.getHostName(), address.getPort());
    this.concurrencyDelay = new ConcurrencyDelay(concurrencyDelays);
  }

  @Override
  public Mono<Response> response(Request message, ByteBuf metadata) {
    return Mono.delay(Duration.ofMillis(concurrencyDelay.get()))
        .thenReturn(Response.newBuilder().setMessage(address).build());
  }

  static class ConcurrencyDelay {
    private static final long CONCURRENCY_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    private final NavigableMap<Integer, Integer> concurrencyToDelay = new TreeMap<>();
    private final Random random = new Random();
    private int concurrency;
    private long prevIntervalStart = System.currentTimeMillis();

    public ConcurrencyDelay(String concurrencyDelays) {
      String[] concurrencyDelaysArr = concurrencyDelays.split(";");
      int length = concurrencyDelaysArr.length;
      for (int i = 0; i < length; i++) {
        String concurrencyDelay = concurrencyDelaysArr[i];
        String[] concurrencyAndDelay = concurrencyDelay.split("=>");
        int concurrency =
            i == length - 1 ? Integer.MAX_VALUE : Integer.parseInt(concurrencyAndDelay[0].trim());
        int delay = Integer.parseInt(concurrencyAndDelay[1].trim());
        concurrencyToDelay.put(concurrency, delay);
      }
      concurrencyToDelay.put(0, 0);
    }

    public long get() {
      long now = System.currentTimeMillis();
      if (now - prevIntervalStart >= CONCURRENCY_INTERVAL) {
        prevIntervalStart = now;
        concurrency = 0;
      }
      int c = ++concurrency;
      Integer cur = concurrencyToDelay.ceilingEntry(c).getValue();
      Integer prev = concurrencyToDelay.lowerEntry(c).getValue();
      return prev + random.nextInt(cur - prev);
    }
  }
}
