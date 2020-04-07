package com.jauntsdn.rsocket.lease.showcase.service;

@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
@com.jauntsdn.rsocket.rpc.annotations.internal.Generated(
    type = com.jauntsdn.rsocket.rpc.annotations.internal.ResourceType.CLIENT,
    idlClass = Service.class)
public final class ServiceClient implements Service {
  private final com.jauntsdn.rsocket.RSocket rSocket;
  private final io.netty.buffer.ByteBufAllocator allocator;
  private final java.util.function.Supplier<java.util.Map<String, String>> traceMap;
  private final java.util.function.Function<? super org.reactivestreams.Publisher<com.jauntsdn.rsocket.lease.showcase.service.Response>, ? extends org.reactivestreams.Publisher<com.jauntsdn.rsocket.lease.showcase.service.Response>> responseMetrics;
  private final java.util.function.Function<java.util.Map<String, String>, java.util.function.Function<? super org.reactivestreams.Publisher<com.jauntsdn.rsocket.lease.showcase.service.Response>, ? extends org.reactivestreams.Publisher<com.jauntsdn.rsocket.lease.showcase.service.Response>>> responseTrace;

  public ServiceClient(com.jauntsdn.rsocket.RSocket rSocket, java.util.Optional<io.netty.buffer.ByteBufAllocator> allocator, java.util.Optional<io.micrometer.core.instrument.MeterRegistry> registry, java.util.Optional<io.opentracing.Tracer> tracer) {
    this.rSocket = rSocket;
    this.allocator = allocator.orElse(io.netty.buffer.ByteBufAllocator.DEFAULT);
    if (!registry.isPresent()) {
      this.responseMetrics = java.util.function.Function.identity();
    } else {
      io.micrometer.core.instrument.MeterRegistry r = registry.get();
      this.responseMetrics = com.jauntsdn.rsocket.rpc.metrics.Metrics.timed(r, "rsocket.client", "service", Service.SERVICE, "method", Service.METHOD_RESPONSE);
    }

    if (!tracer.isPresent()) {
      this.traceMap = java.util.Collections::emptyMap;
      this.responseTrace = com.jauntsdn.rsocket.rpc.tracing.Tracing.trace();
    } else {
      this.traceMap = java.util.HashMap::new;
      io.opentracing.Tracer t = tracer.get();
      this.responseTrace = com.jauntsdn.rsocket.rpc.tracing.Tracing.trace(t, Service.METHOD_RESPONSE, com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.service", Service.SERVICE), com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.rpc.role", "client"), com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.rpc.version", ""));
    }

  }

  @com.jauntsdn.rsocket.rpc.annotations.internal.GeneratedMethod(returnTypeClass = com.jauntsdn.rsocket.lease.showcase.service.Response.class)
  public reactor.core.publisher.Mono<com.jauntsdn.rsocket.lease.showcase.service.Response> response(com.jauntsdn.rsocket.lease.showcase.service.Request message) {
    return response(message, io.netty.buffer.Unpooled.EMPTY_BUFFER);
  }

  @java.lang.Override
  @com.jauntsdn.rsocket.rpc.annotations.internal.GeneratedMethod(returnTypeClass = com.jauntsdn.rsocket.lease.showcase.service.Response.class)
  public reactor.core.publisher.Mono<com.jauntsdn.rsocket.lease.showcase.service.Response> response(com.jauntsdn.rsocket.lease.showcase.service.Request message, io.netty.buffer.ByteBuf metadata) {
  java.util.Map<String, String> map = this.traceMap.get();
  io.netty.buffer.ByteBufAllocator allocator = this.allocator;
    return reactor.core.publisher.Mono.defer(new java.util.function.Supplier<reactor.core.publisher.Mono<com.jauntsdn.rsocket.Payload>>() {
      @java.lang.Override
      public reactor.core.publisher.Mono<com.jauntsdn.rsocket.Payload> get() {
        final io.netty.buffer.ByteBuf data = serialize(message);
        final io.netty.buffer.ByteBuf tracing = com.jauntsdn.rsocket.rpc.tracing.Tracing.mapToByteBuf(allocator, map);
        final io.netty.buffer.ByteBuf metadataBuf = com.jauntsdn.rsocket.rpc.frames.Metadata.encode(allocator, Service.SERVICE, Service.METHOD_RESPONSE, tracing, metadata);
        tracing.release();
        metadata.release();
        return rSocket.requestResponse(com.jauntsdn.rsocket.util.ByteBufPayload.create(data, metadataBuf));
      }
    }).map(deserializer(com.jauntsdn.rsocket.lease.showcase.service.Response.parser())).transform(responseMetrics).transform(responseTrace.apply(map));
  }

  private io.netty.buffer.ByteBuf serialize(final com.google.protobuf.MessageLite message) {
    int length = message.getSerializedSize();
    io.netty.buffer.ByteBuf byteBuf = this.allocator.buffer(length);
    try {
      message.writeTo(com.google.protobuf.CodedOutputStream.newInstance(byteBuf.internalNioBuffer(0, length)));
      byteBuf.writerIndex(length);
      return byteBuf;
    } catch (Throwable t) {
      byteBuf.release();
      throw new RuntimeException(t);
    }
  }

  private static <T> java.util.function.Function<com.jauntsdn.rsocket.Payload, T> deserializer(final com.google.protobuf.Parser<T> parser) {
    return new java.util.function.Function<com.jauntsdn.rsocket.Payload, T>() {
      @java.lang.Override
      public T apply(com.jauntsdn.rsocket.Payload payload) {
        try {
          com.google.protobuf.CodedInputStream is = com.google.protobuf.CodedInputStream.newInstance(payload.getData());
          return parser.parseFrom(is);
        } catch (Throwable t) {
          throw new RuntimeException(t);
        } finally {
          payload.release();
        }
      }
    };
  }
}
