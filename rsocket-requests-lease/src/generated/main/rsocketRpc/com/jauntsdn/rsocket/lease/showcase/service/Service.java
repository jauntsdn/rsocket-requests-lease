package com.jauntsdn.rsocket.lease.showcase.service;

/**
 */
@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
public interface Service {
  String SERVICE = "com.jauntsdn.rsocket.lease.showcase.service.Service";
  String METHOD_RESPONSE = "response";

  /**
   */
  reactor.core.publisher.Mono<com.jauntsdn.rsocket.lease.showcase.service.Response> response(com.jauntsdn.rsocket.lease.showcase.service.Request message, io.netty.buffer.ByteBuf metadata);
}
