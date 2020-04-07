package com.jauntsdn.rsocket.lease.showcase.service;

@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
@com.jauntsdn.rsocket.rpc.annotations.internal.Generated(
    type = com.jauntsdn.rsocket.rpc.annotations.internal.ResourceType.SERVICE,
    idlClass = Service.class)
@javax.inject.Named(
    value ="ServiceServer")
public final class ServiceServer extends com.jauntsdn.rsocket.rpc.AbstractRSocketService {
  private final Service service;
  private final io.netty.buffer.ByteBufAllocator allocator;
  private final io.opentracing.Tracer tracer;
  private final java.util.function.Function<? super org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>, ? extends org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>> responseMetrics;
  private final java.util.function.Function<io.opentracing.SpanContext, java.util.function.Function<? super org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>, ? extends org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>>> responseTrace;
  @javax.inject.Inject
  public ServiceServer(Service service, java.util.Optional<io.netty.buffer.ByteBufAllocator> allocator, java.util.Optional<io.micrometer.core.instrument.MeterRegistry> registry, java.util.Optional<io.opentracing.Tracer> tracer) {
    this.service = service;
    this.allocator = allocator.orElse(io.netty.buffer.ByteBufAllocator.DEFAULT);
    if (!registry.isPresent()) {
      this.responseMetrics = java.util.function.Function.identity();
    } else {
      io.micrometer.core.instrument.MeterRegistry r = registry.get();
      this.responseMetrics = com.jauntsdn.rsocket.rpc.metrics.Metrics.timed(r, "rsocket.server", "service", Service.SERVICE, "method", Service.METHOD_RESPONSE);
    }

    if (!tracer.isPresent()) {
      this.tracer = null;
      this.responseTrace = (ignored) -> java.util.function.Function.identity();
    } else {
      io.opentracing.Tracer t = tracer.get();
      this.tracer = t;
      this.responseTrace = com.jauntsdn.rsocket.rpc.tracing.Tracing.traceAsChild(t, Service.METHOD_RESPONSE, com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.service", Service.SERVICE), com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.rpc.role", "server"), com.jauntsdn.rsocket.rpc.tracing.Tag.of("rsocket.rpc.version", ""));
    }

  }

  @java.lang.Override
  public String getService() {
    return Service.SERVICE;
  }

  @java.lang.Override
  public Class<?> getServiceClass() {
    return service.getClass();
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> fireAndForget(com.jauntsdn.rsocket.Payload payload) {
    return reactor.core.publisher.Mono.error(new UnsupportedOperationException("Fire and forget not implemented."));
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<com.jauntsdn.rsocket.Payload> requestResponse(com.jauntsdn.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = payload.sliceMetadata();
      io.opentracing.SpanContext spanContext = com.jauntsdn.rsocket.rpc.tracing.Tracing.deserializeTracingMetadata(tracer, metadata);
      switch(com.jauntsdn.rsocket.rpc.frames.Metadata.getMethod(metadata)) {
        case Service.METHOD_RESPONSE: {
          com.google.protobuf.CodedInputStream is = com.google.protobuf.CodedInputStream.newInstance(payload.getData());
          return service.response(com.jauntsdn.rsocket.lease.showcase.service.Request.parseFrom(is), metadata).map(serializer).transform(responseMetrics).transform(responseTrace.apply(spanContext));
        }
        default: {
          return reactor.core.publisher.Mono.error(new UnsupportedOperationException());
        }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Mono.error(t);
    } finally {
      payload.release();
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<com.jauntsdn.rsocket.Payload> requestStream(com.jauntsdn.rsocket.Payload payload) {
    return reactor.core.publisher.Flux.error(new UnsupportedOperationException("Request-Stream not implemented."));
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<com.jauntsdn.rsocket.Payload> requestChannel(com.jauntsdn.rsocket.Payload payload, reactor.core.publisher.Flux<com.jauntsdn.rsocket.Payload> publisher) {
    return reactor.core.publisher.Flux.error(new UnsupportedOperationException("Request-Channel not implemented."));
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<com.jauntsdn.rsocket.Payload> requestChannel(org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload> payloads) {
    return reactor.core.publisher.Flux.error(new UnsupportedOperationException("Request-Channel not implemented."));
  }

  private final java.util.function.Function<com.google.protobuf.MessageLite, com.jauntsdn.rsocket.Payload> serializer =
    new java.util.function.Function<com.google.protobuf.MessageLite, com.jauntsdn.rsocket.Payload>() {
      @java.lang.Override
      public com.jauntsdn.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        int length = message.getSerializedSize();
        io.netty.buffer.ByteBuf byteBuf = ServiceServer.this.allocator.buffer(length);
        try {
          message.writeTo(com.google.protobuf.CodedOutputStream.newInstance(byteBuf.internalNioBuffer(0, length)));
          byteBuf.writerIndex(length);
          return com.jauntsdn.rsocket.util.ByteBufPayload.create(byteBuf);
        } catch (Throwable t) {
          byteBuf.release();
          throw new RuntimeException(t);
        }
      }
    };

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
