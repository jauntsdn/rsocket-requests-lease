package com.jauntsdn.rsocket.lease.showcase.service;

/**
 */
@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler (version 0.9.5)",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
public interface BlockingService {
  String SERVICE_ID = "com.jauntsdn.rsocket.lease.showcase.service.Service";
  String METHOD_RESPONSE = "response";

  /**
   */
  com.jauntsdn.rsocket.lease.showcase.service.Response response(com.jauntsdn.rsocket.lease.showcase.service.Request message, io.netty.buffer.ByteBuf metadata);
}
