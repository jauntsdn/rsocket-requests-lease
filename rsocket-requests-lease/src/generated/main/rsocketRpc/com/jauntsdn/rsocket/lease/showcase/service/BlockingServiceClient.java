package com.jauntsdn.rsocket.lease.showcase.service;

@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler (version 0.9.4-feature-adaptive-lease-SNAPSHOT)",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
@com.jauntsdn.rsocket.rpc.annotations.internal.Generated(
    type = com.jauntsdn.rsocket.rpc.annotations.internal.ResourceType.CLIENT,
    idlClass = BlockingService.class)
public final class BlockingServiceClient implements BlockingService {
  private final com.jauntsdn.rsocket.lease.showcase.service.ServiceClient delegate;
  public BlockingServiceClient(com.jauntsdn.rsocket.RSocket rSocket, java.util.Optional<io.netty.buffer.ByteBufAllocator> allocator, java.util.Optional<io.micrometer.core.instrument.MeterRegistry> registry) {
    this.delegate = new com.jauntsdn.rsocket.lease.showcase.service.ServiceClient(rSocket, allocator, registry, java.util.Optional.empty());
  }

  @com.jauntsdn.rsocket.rpc.annotations.internal.GeneratedMethod(returnTypeClass = com.jauntsdn.rsocket.lease.showcase.service.Response.class)
  public com.jauntsdn.rsocket.lease.showcase.service.Response response(com.jauntsdn.rsocket.lease.showcase.service.Request message) {
    return response(message, io.netty.buffer.Unpooled.EMPTY_BUFFER);
  }

  @java.lang.Override
  @com.jauntsdn.rsocket.rpc.annotations.internal.GeneratedMethod(returnTypeClass = com.jauntsdn.rsocket.lease.showcase.service.Response.class)
  public com.jauntsdn.rsocket.lease.showcase.service.Response response(com.jauntsdn.rsocket.lease.showcase.service.Request message, io.netty.buffer.ByteBuf metadata) {
    return delegate.response(message, metadata).block();
  }

}

