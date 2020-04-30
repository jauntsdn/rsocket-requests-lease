package com.jauntsdn.rsocket.lease.showcase.service;

@javax.annotation.Generated(
    value = "by RSocket RPC proto compiler (version 0.9.5)",
    comments = "Source: com/jauntsdn/rsocket/lease/showcase/service/leaseservice.proto")
@com.jauntsdn.rsocket.rpc.annotations.internal.Generated(
    type = com.jauntsdn.rsocket.rpc.annotations.internal.ResourceType.SERVICE,
    idlClass = BlockingService.class)
@javax.inject.Named(
    value ="BlockingServiceServer")
public final class BlockingServiceServer extends com.jauntsdn.rsocket.rpc.AbstractRSocketService {
  private final BlockingService service;
  private final io.netty.buffer.ByteBufAllocator allocator;
  private final reactor.core.scheduler.Scheduler scheduler;
  private final java.util.function.Function<? super org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>, ? extends org.reactivestreams.Publisher<com.jauntsdn.rsocket.Payload>> responseMetrics;
  @javax.inject.Inject
  public BlockingServiceServer(BlockingService service, java.util.Optional<io.netty.buffer.ByteBufAllocator> allocator, java.util.Optional<reactor.core.scheduler.Scheduler> scheduler, java.util.Optional<io.micrometer.core.instrument.MeterRegistry> registry) {
    this.scheduler = scheduler.orElse(reactor.core.scheduler.Schedulers.elastic());
    this.service = service;
    this.allocator = allocator.orElse(io.netty.buffer.ByteBufAllocator.DEFAULT);
    if (!registry.isPresent()) {
      this.responseMetrics = java.util.function.Function.identity();
    } else {
      this.responseMetrics = com.jauntsdn.rsocket.rpc.metrics.Metrics.timed(registry.get(), "rsocket.server", "service", BlockingService.SERVICE_ID, "method", BlockingService.METHOD_RESPONSE);
    }

  }

  @java.lang.Override
  public String getService() {
    return BlockingService.SERVICE_ID;
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
      switch(com.jauntsdn.rsocket.rpc.frames.Metadata.getMethod(metadata)) {
        case Service.METHOD_RESPONSE: {
          com.google.protobuf.CodedInputStream is = com.google.protobuf.CodedInputStream.newInstance(payload.getData());
          com.jauntsdn.rsocket.lease.showcase.service.Request message = com.jauntsdn.rsocket.lease.showcase.service.Request.parseFrom(is);
          return reactor.core.publisher.Mono.fromSupplier(() -> service.response(message, metadata)).map(serializer).transform(responseMetrics).subscribeOn(scheduler);
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
        io.netty.buffer.ByteBuf byteBuf = BlockingServiceServer.this.allocator.buffer(length);
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
