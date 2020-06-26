/**
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
package java.util.stream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code U}.
 * 
 * @param <P_IN> type of elements in the upstream source
 * @param <P_OUT> type of elements in produced by this stage
 * 
 * @since 1.8
 */
abstract class ReferencePipeline<P_IN, P_OUT> extends AbstractPipeline<, , > implements Stream<> {
  /**
   * Constructor for the head of a stream pipeline.
   * 
   * @param source {@code Supplier<Spliterator>} describing the stream source
   * @param sourceFlags the source flags for the stream source, described in
   *        {@link StreamOpFlag}
   * @param parallel {@code true} if the pipeline is parallel
   */
  ReferencePipeline(java.util.function.Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
  }

  /**
   * Constructor for the head of a stream pipeline.
   * 
   * @param source {@code Spliterator} describing the stream source
   * @param sourceFlags The source flags for the stream source, described in
   *        {@link StreamOpFlag}
   * @param parallel {@code true} if the pipeline is parallel
   */
  ReferencePipeline(java.util.Spliterator<?> source, int sourceFlags, boolean parallel) {
        super(source, sourceFlags, parallel);
  }

  /**
   * Constructor for appending an intermediate operation onto an existing
   * pipeline.
   * 
   * @param upstream the upstream element source.
   */
  ReferencePipeline(AbstractPipeline<?, P_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
  }

  /**
   *  Shape-specific methods
   */
  @Override
  final StreamShape getOutputShape() {
        return StreamShape.REFERENCE;
  }

  @Override
  final <P_IN> Node<P_OUT> evaluateToNode(PipelineHelper<P_OUT> helper, java.util.Spliterator<P_IN> spliterator, boolean flattenTree, java.util.function.IntFunction<P_OUT[]> generator) {
        return Nodes.collect(helper, spliterator, flattenTree, generator);
  }

  @Override
  final <P_IN> java.util.Spliterator<P_OUT> wrap(PipelineHelper<P_OUT> ph, java.util.function.Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new StreamSpliterators.WrappingSpliterator<>(ph, supplier, isParallel);
  }

  @Override
  final java.util.Spliterator<P_OUT> lazySpliterator(java.util.function.Supplier<? extends Spliterator<P_OUT>> supplier) {
        return new StreamSpliterators.DelegatingSpliterator<>(supplier);
  }

  @Override
  final void forEachWithCancel(java.util.Spliterator<P_OUT> spliterator, Sink<P_OUT> sink) {
        do { } while (!sink.cancellationRequested() && spliterator.tryAdvance(sink));
  }

  @Override
  final Node.Builder<P_OUT> makeNodeBuilder(long exactSizeIfKnown, java.util.function.IntFunction<P_OUT[]> generator) {
        return Nodes.builder(exactSizeIfKnown, generator);
  }

  /**
   *  BaseStream
   */
  @Override
  public final java.util.Iterator<P_OUT> iterator() {
        return Spliterators.iterator(spliterator());
  }

  /**
   *  Stream
   *  Stateless intermediate operations from Stream
   */
  @Override
  public Stream<P_OUT> unordered() {
        if (!isOrdered())
            return this;
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_ORDERED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                return sink;
            }
        };
  }

  @Override
  public final Stream<P_OUT> filter(java.util.function.Predicate<? super P_OUT> predicate) {
        Objects.requireNonNull(predicate);
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE,
                                     StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                return new Sink.ChainedReference<P_OUT, P_OUT>(sink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(P_OUT u) {
                        if (predicate.test(u))
                            downstream.accept(u);
                    }
                };
            }
        };
  }

  @Override
  @SuppressWarnings("unchecked")
  public final <R> Stream<R> map(java.util.function.Function<? super P_OUT, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return new StatelessOp<P_OUT, R>(this, StreamShape.REFERENCE,
                                     StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> sink) {
                return new Sink.ChainedReference<P_OUT, R>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.apply(u));
                    }
                };
            }
        };
  }

  @Override
  public final IntStream mapToInt(java.util.function.ToIntFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        return new IntPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                              StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedReference<P_OUT, Integer>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsInt(u));
                    }
                };
            }
        };
  }

  @Override
  public final LongStream mapToLong(java.util.function.ToLongFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        return new LongPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                      StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedReference<P_OUT, Long>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsLong(u));
                    }
                };
            }
        };
  }

  @Override
  public final DoubleStream mapToDouble(java.util.function.ToDoubleFunction<? super P_OUT> mapper) {
        Objects.requireNonNull(mapper);
        return new DoublePipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                        StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedReference<P_OUT, Double>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        downstream.accept(mapper.applyAsDouble(u));
                    }
                };
            }
        };
  }

  @Override
  public final <R> Stream<R> flatMap(java.util.function.Function<? super P_OUT, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(mapper);
        // We can do better than this, by polling cancellationRequested when stream is infinite
        return new StatelessOp<P_OUT, R>(this, StreamShape.REFERENCE,
                                     StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<R> sink) {
                return new Sink.ChainedReference<P_OUT, R>(sink) {
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(P_OUT u) {
                        try (Stream<? extends R> result = mapper.apply(u)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(downstream);
                        }
                    }
                };
            }
        };
  }

  @Override
  public final IntStream flatMapToInt(java.util.function.Function<? super P_OUT, ? extends IntStream> mapper) {
        Objects.requireNonNull(mapper);
        // We can do better than this, by polling cancellationRequested when stream is infinite
        return new IntPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                              StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Integer> sink) {
                return new Sink.ChainedReference<P_OUT, Integer>(sink) {
                    IntConsumer downstreamAsInt = downstream::accept;
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(P_OUT u) {
                        try (IntStream result = mapper.apply(u)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(downstreamAsInt);
                        }
                    }
                };
            }
        };
  }

  @Override
  public final DoubleStream flatMapToDouble(java.util.function.Function<? super P_OUT, ? extends DoubleStream> mapper) {
        Objects.requireNonNull(mapper);
        // We can do better than this, by polling cancellationRequested when stream is infinite
        return new DoublePipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                                     StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Double> sink) {
                return new Sink.ChainedReference<P_OUT, Double>(sink) {
                    DoubleConsumer downstreamAsDouble = downstream::accept;
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(P_OUT u) {
                        try (DoubleStream result = mapper.apply(u)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(downstreamAsDouble);
                        }
                    }
                };
            }
        };
  }

  @Override
  public final LongStream flatMapToLong(java.util.function.Function<? super P_OUT, ? extends LongStream> mapper) {
        Objects.requireNonNull(mapper);
        // We can do better than this, by polling cancellationRequested when stream is infinite
        return new LongPipeline.StatelessOp<P_OUT>(this, StreamShape.REFERENCE,
                                                   StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT | StreamOpFlag.NOT_SIZED) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<Long> sink) {
                return new Sink.ChainedReference<P_OUT, Long>(sink) {
                    LongConsumer downstreamAsLong = downstream::accept;
                    @Override
                    public void begin(long size) {
                        downstream.begin(-1);
                    }

                    @Override
                    public void accept(P_OUT u) {
                        try (LongStream result = mapper.apply(u)) {
                            // We can do better that this too; optimize for depth=0 case and just grab spliterator and forEach it
                            if (result != null)
                                result.sequential().forEach(downstreamAsLong);
                        }
                    }
                };
            }
        };
  }

  @Override
  public final Stream<P_OUT> peek(java.util.function.Consumer<? super P_OUT> action) {
        Objects.requireNonNull(action);
        return new StatelessOp<P_OUT, P_OUT>(this, StreamShape.REFERENCE,
                                     0) {
            @Override
            Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
                return new Sink.ChainedReference<P_OUT, P_OUT>(sink) {
                    @Override
                    public void accept(P_OUT u) {
                        action.accept(u);
                        downstream.accept(u);
                    }
                };
            }
        };
  }

  /**
   *  Stateful intermediate operations from Stream
   */
  @Override
  public final Stream<P_OUT> distinct() {
        return DistinctOps.makeRef(this);
  }

  @Override
  public final Stream<P_OUT> sorted() {
        return SortedOps.makeRef(this);
  }

  @Override
  public final Stream<P_OUT> sorted(java.util.Comparator<? super P_OUT> comparator) {
        return SortedOps.makeRef(this, comparator);
  }

  @Override
  public final Stream<P_OUT> limit(long maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException(Long.toString(maxSize));
        return SliceOps.makeRef(this, 0, maxSize);
  }

  @Override
  public final Stream<P_OUT> skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException(Long.toString(n));
        if (n == 0)
            return this;
        else
            return SliceOps.makeRef(this, n, -1);
  }

  /**
   *  Terminal operations from Stream
   */
  @Override
  public void forEach(java.util.function.Consumer<? super P_OUT> action) {
        evaluate(ForEachOps.makeRef(action, false));
  }

  @Override
  public void forEachOrdered(java.util.function.Consumer<? super P_OUT> action) {
        evaluate(ForEachOps.makeRef(action, true));
  }

  @Override
  @SuppressWarnings("unchecked")
  public final <A> A[] toArray(java.util.function.IntFunction<A[]> generator) {
        // Since A has no relation to U (not possible to declare that A is an upper bound of U)
        // there will be no static type checking.
        // Therefore use a raw type and assume A == U rather than propagating the separation of A and U
        // throughout the code-base.
        // The runtime type of U is never checked for equality with the component type of the runtime type of A[].
        // Runtime checking will be performed when an element is stored in A[], thus if A is not a
        // super type of U an ArrayStoreException will be thrown.
        @SuppressWarnings("rawtypes")
        IntFunction rawGenerator = (IntFunction) generator;
        return (A[]) Nodes.flatten(evaluateToArrayNode(rawGenerator), rawGenerator)
                              .asArray(rawGenerator);
  }

  @Override
  public final Object[] toArray() {
        return toArray(Object[]::new);
  }

  @Override
  public final boolean anyMatch(java.util.function.Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ANY));
  }

  @Override
  public final boolean allMatch(java.util.function.Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.ALL));
  }

  @Override
  public final boolean noneMatch(java.util.function.Predicate<? super P_OUT> predicate) {
        return evaluate(MatchOps.makeRef(predicate, MatchOps.MatchKind.NONE));
  }

  @Override
  public final java.util.Optional<P_OUT> findFirst() {
        return evaluate(FindOps.makeRef(true));
  }

  @Override
  public final java.util.Optional<P_OUT> findAny() {
        return evaluate(FindOps.makeRef(false));
  }

  @Override
  public final P_OUT reduce(final P_OUT identity, final java.util.function.BinaryOperator<P_OUT> accumulator) {
        return evaluate(ReduceOps.makeRef(identity, accumulator, accumulator));
  }

  @Override
  public final java.util.Optional<P_OUT> reduce(java.util.function.BinaryOperator<P_OUT> accumulator) {
        return evaluate(ReduceOps.makeRef(accumulator));
  }

  @Override
  public final <R> R reduce(R identity, java.util.function.BiFunction<R, ? super P_OUT, R> accumulator, java.util.function.BinaryOperator<R> combiner) {
        return evaluate(ReduceOps.makeRef(identity, accumulator, combiner));
  }

  @Override
  @SuppressWarnings("unchecked")
  public final <R, A> R collect(Collector<? super P_OUT, A, R> collector) {
        A container;
        if (isParallel()
                && (collector.characteristics().contains(Collector.Characteristics.CONCURRENT))
                && (!isOrdered() || collector.characteristics().contains(Collector.Characteristics.UNORDERED))) {
            container = collector.supplier().get();
            BiConsumer<A, ? super P_OUT> accumulator = collector.accumulator();
            forEach(u -> accumulator.accept(container, u));
        }
        else {
            container = evaluate(ReduceOps.makeRef(collector));
        }
        return collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)
               ? (R) container
               : collector.finisher().apply(container);
  }

  @Override
  public final <R> R collect(java.util.function.Supplier<R> supplier, java.util.function.BiConsumer<R, ? super P_OUT> accumulator, java.util.function.BiConsumer<R, R> combiner) {
        return evaluate(ReduceOps.makeRef(supplier, accumulator, combiner));
  }

  @Override
  public final java.util.Optional<P_OUT> max(java.util.Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.maxBy(comparator));
  }

  @Override
  public final java.util.Optional<P_OUT> min(java.util.Comparator<? super P_OUT> comparator) {
        return reduce(BinaryOperator.minBy(comparator));

  }

  @Override
  public final long count() {
        return mapToLong(e -> 1L).sum();
  }

  static class Head<E_IN, E_OUT> extends ReferencePipeline<, > {
    /**
     * Constructor for the source stage of a Stream.
     * 
     * @param source {@code Supplier<Spliterator>} describing the stream
     *               source
     * @param sourceFlags the source flags for the stream source, described
     *                    in {@link StreamOpFlag}
     */
    Head(java.util.function.Supplier<? extends Spliterator<?>> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
    }

    /**
     * Constructor for the source stage of a Stream.
     * 
     * @param source {@code Spliterator} describing the stream source
     * @param sourceFlags the source flags for the stream source, described
     *                    in {@link StreamOpFlag}
     */
    Head(java.util.Spliterator<?> source, int sourceFlags, boolean parallel) {
            super(source, sourceFlags, parallel);
    }

    @Override
    final boolean opIsStateful() {
            throw new UnsupportedOperationException();
    }

    @Override
    final Sink<E_IN> opWrapSink(int flags, Sink<E_OUT> sink) {
            throw new UnsupportedOperationException();
    }

    /**
     *  Optimized sequential terminal operations for the head of the pipeline
     */
    @Override
    public void forEach(java.util.function.Consumer<? super E_OUT> action) {
            if (!isParallel()) {
                sourceStageSpliterator().forEachRemaining(action);
            }
            else {
                super.forEach(action);
            }
    }

    @Override
    public void forEachOrdered(java.util.function.Consumer<? super E_OUT> action) {
            if (!isParallel()) {
                sourceStageSpliterator().forEachRemaining(action);
            }
            else {
                super.forEachOrdered(action);
            }
    }

  }

  static abstract class StatelessOp<E_IN, E_OUT> extends ReferencePipeline<, > {
    /**
     * Construct a new Stream by appending a stateless intermediate
     * operation to an existing stream.
     * 
     * @param upstream The upstream pipeline stage
     * @param inputShape The stream shape for the upstream pipeline stage
     * @param opFlags Operation flags for the new stage
     */
    StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
    }

    @Override
    final boolean opIsStateful() {
            return false;
    }

  }

  static abstract class StatefulOp<E_IN, E_OUT> extends ReferencePipeline<, > {
    /**
     * Construct a new Stream by appending a stateful intermediate operation
     * to an existing stream.
     * @param upstream The upstream pipeline stage
     * @param inputShape The stream shape for the upstream pipeline stage
     * @param opFlags Operation flags for the new stage
     */
    StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
            assert upstream.getOutputShape() == inputShape;
    }

    @Override
    final boolean opIsStateful() {
            return true;
    }

    @Override
    abstract <P_IN> Node<E_OUT> opEvaluateParallel(PipelineHelper<E_OUT> helper, java.util.Spliterator<P_IN> spliterator, java.util.function.IntFunction<E_OUT[]> generator) ;

  }

}
