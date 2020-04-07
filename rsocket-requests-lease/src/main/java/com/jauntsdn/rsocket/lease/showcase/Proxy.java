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

import com.jauntsdn.rsocket.RSocket;
import com.jauntsdn.rsocket.RSocketFactory;
import com.jauntsdn.rsocket.frame.decoder.PayloadDecoder;
import com.jauntsdn.rsocket.transport.netty.client.TcpClientTransport;
import com.jauntsdn.rsocket.transport.netty.server.CloseableChannel;
import com.jauntsdn.rsocket.transport.netty.server.TcpServerTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;

public class Proxy {
  private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

  public static void main(String[] args) {

    String host = System.getProperty("HOST", "localhost");
    Integer port = Integer.getInteger("PORT", 8308);
    String backendAddresses =
        System.getProperty("SERVERS", "localhost:8309,localhost:8310,localhost:8311");

    logger.info("Proxy bind address {}:{}", host, port);
    logger.info("Backend servers addresses {}", backendAddresses);

    InetSocketAddress address = new InetSocketAddress(host, port);
    TcpServer tcpServer = TcpServer.create().addressSupplier(() -> address);
    TcpServerTransport tcpServerTransport = TcpServerTransport.create(tcpServer);

    RSocket leastLoadedBalancerRSocket =
        new LeastLoadedBalancerRSocket(backendRSockets(backendAddresses));

    RSocketFactory.receive()
        .frameDecoder(PayloadDecoder.ZERO_COPY)
        .errorConsumer(errorConsumer())
        .acceptor((setup, requesterRSocket) -> Mono.just(leastLoadedBalancerRSocket))
        .transport(tcpServerTransport)
        .start()
        .flatMap(CloseableChannel::onClose)
        .block();
  }

  private static Collection<Mono<RSocket>> backendRSockets(String addresses) {
    return Arrays.stream(addresses.split(","))
        .map(
            hostPort -> {
              String[] hostAndPort = hostPort.split(":");
              String host = hostAndPort[0];
              int port = Integer.parseInt(hostAndPort[1]);
              return new InetSocketAddress(host, port);
            })
        .map(Proxy::connect)
        .collect(Collectors.toSet());
  }

  private static Mono<RSocket> connect(InetSocketAddress address) {
    return RSocketFactory.connect()
        .frameDecoder(PayloadDecoder.ZERO_COPY)
        .errorConsumer(errorConsumer())
        .singleSubscriberRequester()
        .lease()
        .transport(TcpClientTransport.create(address))
        .start();
  }

  private static Consumer<Throwable> errorConsumer() {
    return err -> {
      if (err instanceof IOException) {
        return;
      }
      logger.error("Unexpected server error", err);
    };
  }
}
