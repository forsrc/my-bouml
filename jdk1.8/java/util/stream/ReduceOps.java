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

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
/**
 * Factory for creating instances of {@code TerminalOp} that implement
 * reductions.
 * 
 * @since 1.8
 */
final class ReduceOps {
  private ReduceOps() {
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * reference values.
   * 
   * @param <T> the type of the input elements
   * @param <U> the type of the result
   * @param seed the identity element for the reduction
   * @param reducer the accumulating function that incorporates an additional
   *        input element into the result
   * @param combiner the combining function that combines two intermediate
   *        results
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static <T, U> TerminalOp<T, U> makeRef(U seed, java.util.function.BiFunction<U, ? super T, U> reducer, java.util.function.BinaryOperator<U> combiner)
  {
        Objects.requireNonNull(reducer);
        Objects.requireNonNull(combiner);
        class ReducingSink extends Box<U> implements AccumulatingSink<T, U, ReducingSink> {
            @Override
            public void begin(long size) {
                state = seed;
            }

            @Override
            public void accept(T t) {
                state = reducer.apply(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        return new ReduceOp<T, U, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * reference values producing an optional reference result.
   * 
   * @param <T> The type of the input elements, and the type of the result
   * @param operator The reducing function
   * @return A {@code TerminalOp} implementing the reduction
   */
  public static <T> TerminalOp<T, Optional<T>> makeRef(java.util.function.BinaryOperator<T> operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<T, Optional<T>, ReducingSink> {
            private boolean empty;
            private T state;

            public void begin(long size) {
                empty = true;
                state = null;
            }

            @Override
            public void accept(T t) {
                if (empty) {
                    empty = false;
                    state = t;
                } else {
                    state = operator.apply(state, t);
                }
            }

            @Override
            public Optional<T> get() {
                return empty ? Optional.empty() : Optional.of(state);
            }

            @Override
            public void combine(ReducingSink other) {
                if (!other.empty)
                    accept(other.state);
            }
        }
        return new ReduceOp<T, Optional<T>, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a mutable reduce on
   * reference values.
   * 
   * @param <T> the type of the input elements
   * @param <I> the type of the intermediate reduction result
   * @param collector a {@code Collector} defining the reduction
   * @return a {@code ReduceOp} implementing the reduction
   */
  public static <T, I> TerminalOp<T, I> makeRef(Collector<? super T, I, ?> collector)
  {
        Supplier<I> supplier = Objects.requireNonNull(collector).supplier();
        BiConsumer<I, ? super T> accumulator = collector.accumulator();
        BinaryOperator<I> combiner = collector.combiner();
        class ReducingSink extends Box<I>
                implements AccumulatingSink<T, I, ReducingSink> {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }

            @Override
            public void accept(T t) {
                accumulator.accept(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        return new ReduceOp<T, I, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }

            @Override
            public int getOpFlags() {
                return collector.characteristics().contains(Collector.Characteristics.UNORDERED)
                       ? StreamOpFlag.NOT_ORDERED
                       : 0;
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a mutable reduce on
   * reference values.
   * 
   * @param <T> the type of the input elements
   * @param <R> the type of the result
   * @param seedFactory a factory to produce a new base accumulator
   * @param accumulator a function to incorporate an element into an
   *        accumulator
   * @param reducer a function to combine an accumulator into another
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static <T, R> TerminalOp<T, R> makeRef(java.util.function.Supplier<R> seedFactory, java.util.function.BiConsumer<R, ? super T> accumulator, java.util.function.BiConsumer<R,R> reducer)
  {
        Objects.requireNonNull(seedFactory);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(reducer);
        class ReducingSink extends Box<R>
                implements AccumulatingSink<T, R, ReducingSink> {
            @Override
            public void begin(long size) {
                state = seedFactory.get();
            }

            @Override
            public void accept(T t) {
                accumulator.accept(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                reducer.accept(state, other.state);
            }
        }
        return new ReduceOp<T, R, ReducingSink>(StreamShape.REFERENCE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code int} values.
   * 
   * @param identity the identity for the combining function
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Integer, Integer> makeInt(int identity, java.util.function.IntBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Integer, Integer, ReducingSink>, Sink.OfInt {
            private int state;

            @Override
            public void begin(long size) {
                state = identity;
            }

            @Override
            public void accept(int t) {
                state = operator.applyAsInt(state, t);
            }

            @Override
            public Integer get() {
                return state;
            }

            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        return new ReduceOp<Integer, Integer, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code int} values, producing an optional integer result.
   * 
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Integer, OptionalInt> makeInt(java.util.function.IntBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Integer, OptionalInt, ReducingSink>, Sink.OfInt {
            private boolean empty;
            private int state;

            public void begin(long size) {
                empty = true;
                state = 0;
            }

            @Override
            public void accept(int t) {
                if (empty) {
                    empty = false;
                    state = t;
                }
                else {
                    state = operator.applyAsInt(state, t);
                }
            }

            @Override
            public OptionalInt get() {
                return empty ? OptionalInt.empty() : OptionalInt.of(state);
            }

            @Override
            public void combine(ReducingSink other) {
                if (!other.empty)
                    accept(other.state);
            }
        }
        return new ReduceOp<Integer, OptionalInt, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a mutable reduce on
   * {@code int} values.
   * 
   * @param <R> The type of the result
   * @param supplier a factory to produce a new accumulator of the result type
   * @param accumulator a function to incorporate an int into an
   *        accumulator
   * @param combiner a function to combine an accumulator into another
   * @return A {@code ReduceOp} implementing the reduction
   */
  public static <R> TerminalOp<Integer, R> makeInt(java.util.function.Supplier<R> supplier, java.util.function.ObjIntConsumer<R> accumulator, java.util.function.BinaryOperator<R> combiner)
  {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        class ReducingSink extends Box<R>
                implements AccumulatingSink<Integer, R, ReducingSink>, Sink.OfInt {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }

            @Override
            public void accept(int t) {
                accumulator.accept(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        return new ReduceOp<Integer, R, ReducingSink>(StreamShape.INT_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code long} values.
   * 
   * @param identity the identity for the combining function
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Long, Long> makeLong(long identity, java.util.function.LongBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Long, Long, ReducingSink>, Sink.OfLong {
            private long state;

            @Override
            public void begin(long size) {
                state = identity;
            }

            @Override
            public void accept(long t) {
                state = operator.applyAsLong(state, t);
            }

            @Override
            public Long get() {
                return state;
            }

            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        return new ReduceOp<Long, Long, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code long} values, producing an optional long result.
   * 
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Long, OptionalLong> makeLong(java.util.function.LongBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Long, OptionalLong, ReducingSink>, Sink.OfLong {
            private boolean empty;
            private long state;

            public void begin(long size) {
                empty = true;
                state = 0;
            }

            @Override
            public void accept(long t) {
                if (empty) {
                    empty = false;
                    state = t;
                }
                else {
                    state = operator.applyAsLong(state, t);
                }
            }

            @Override
            public OptionalLong get() {
                return empty ? OptionalLong.empty() : OptionalLong.of(state);
            }

            @Override
            public void combine(ReducingSink other) {
                if (!other.empty)
                    accept(other.state);
            }
        }
        return new ReduceOp<Long, OptionalLong, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a mutable reduce on
   * {@code long} values.
   * 
   * @param <R> the type of the result
   * @param supplier a factory to produce a new accumulator of the result type
   * @param accumulator a function to incorporate an int into an
   *        accumulator
   * @param combiner a function to combine an accumulator into another
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static <R> TerminalOp<Long, R> makeLong(java.util.function.Supplier<R> supplier, java.util.function.ObjLongConsumer<R> accumulator, java.util.function.BinaryOperator<R> combiner)
  {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        class ReducingSink extends Box<R>
                implements AccumulatingSink<Long, R, ReducingSink>, Sink.OfLong {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }

            @Override
            public void accept(long t) {
                accumulator.accept(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        return new ReduceOp<Long, R, ReducingSink>(StreamShape.LONG_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code double} values.
   * 
   * @param identity the identity for the combining function
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Double, Double> makeDouble(double identity, java.util.function.DoubleBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Double, Double, ReducingSink>, Sink.OfDouble {
            private double state;

            @Override
            public void begin(long size) {
                state = identity;
            }

            @Override
            public void accept(double t) {
                state = operator.applyAsDouble(state, t);
            }

            @Override
            public Double get() {
                return state;
            }

            @Override
            public void combine(ReducingSink other) {
                accept(other.state);
            }
        }
        return new ReduceOp<Double, Double, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a functional reduce on
   * {@code double} values, producing an optional double result.
   * 
   * @param operator the combining function
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static TerminalOp<Double, OptionalDouble> makeDouble(java.util.function.DoubleBinaryOperator operator)
  {
        Objects.requireNonNull(operator);
        class ReducingSink
                implements AccumulatingSink<Double, OptionalDouble, ReducingSink>, Sink.OfDouble {
            private boolean empty;
            private double state;

            public void begin(long size) {
                empty = true;
                state = 0;
            }

            @Override
            public void accept(double t) {
                if (empty) {
                    empty = false;
                    state = t;
                }
                else {
                    state = operator.applyAsDouble(state, t);
                }
            }

            @Override
            public OptionalDouble get() {
                return empty ? OptionalDouble.empty() : OptionalDouble.of(state);
            }

            @Override
            public void combine(ReducingSink other) {
                if (!other.empty)
                    accept(other.state);
            }
        }
        return new ReduceOp<Double, OptionalDouble, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  /**
   * Constructs a {@code TerminalOp} that implements a mutable reduce on
   * {@code double} values.
   * 
   * @param <R> the type of the result
   * @param supplier a factory to produce a new accumulator of the result type
   * @param accumulator a function to incorporate an int into an
   *        accumulator
   * @param combiner a function to combine an accumulator into another
   * @return a {@code TerminalOp} implementing the reduction
   */
  public static <R> TerminalOp<Double, R> makeDouble(java.util.function.Supplier<R> supplier, java.util.function.ObjDoubleConsumer<R> accumulator, java.util.function.BinaryOperator<R> combiner)
  {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        class ReducingSink extends Box<R>
                implements AccumulatingSink<Double, R, ReducingSink>, Sink.OfDouble {
            @Override
            public void begin(long size) {
                state = supplier.get();
            }

            @Override
            public void accept(double t) {
                accumulator.accept(state, t);
            }

            @Override
            public void combine(ReducingSink other) {
                state = combiner.apply(state, other.state);
            }
        }
        return new ReduceOp<Double, R, ReducingSink>(StreamShape.DOUBLE_VALUE) {
            @Override
            public ReducingSink makeSink() {
                return new ReducingSink();
            }
        };
  }

  private interface AccumulatingSink<T, R, K extends AccumulatingSink<T, R, K>> extends TerminalSink<, > {
    void combine(K other) ;

  }

  private static abstract class Box<U> {
    U state;

    Box() {
    }

    /**
     *  Avoid creation of special accessor
     */
    public U get() {
            return state;
    }

  }

  private static abstract class ReduceOp<T, R, S extends AccumulatingSink<T, R, S>> implements TerminalOp<, > {
    private final StreamShape inputShape;

    /**
     * Create a {@code ReduceOp} of the specified stream shape which uses
     * the specified {@code Supplier} to create accumulating sinks.
     * 
     * @param shape The shape of the stream pipeline
     */
    ReduceOp(StreamShape shape) {
            inputShape = shape;
    }

    public abstract S makeSink() ;

    @Override
    public StreamShape inputShape() {
            return inputShape;
    }

    @Override
    public <P_IN> R evaluateSequential(PipelineHelper<T> helper, java.util.Spliterator<P_IN> spliterator) {
            return helper.wrapAndCopyInto(makeSink(), spliterator).get();
    }

    @Override
    public <P_IN> R evaluateParallel(PipelineHelper<T> helper, java.util.Spliterator<P_IN> spliterator) {
            return new ReduceTask<>(this, helper, spliterator).invoke().get();
    }

  }

  @SuppressWarnings("serial")
  private static final class ReduceTask<P_IN, P_OUT, R, S extends AccumulatingSink<P_OUT, R, S>> extends AbstractTask<, , , > {
    private final ReduceOps.ReduceOp<P_OUT, R, S> op;

    ReduceTask(ReduceOps.ReduceOp<P_OUT, R, S> op, PipelineHelper<P_OUT> helper, java.util.Spliterator<P_IN> spliterator) {
            super(helper, spliterator);
            this.op = op;
    }

    ReduceTask(ReduceOps.ReduceTask<P_IN, P_OUT, R, S> parent, java.util.Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
    }

    @Override
    protected ReduceOps.ReduceTask<P_IN, P_OUT, R, S> makeChild(java.util.Spliterator<P_IN> spliterator) {
            return new ReduceTask<>(this, spliterator);
    }

    @Override
    protected S doLeaf() {
            return helper.wrapAndCopyInto(op.makeSink(), spliterator);
    }

    @Override
    public void onCompletion(java.util.concurrent.CountedCompleter<?> caller) {
            if (!isLeaf()) {
                S leftResult = leftChild.getLocalResult();
                leftResult.combine(rightChild.getLocalResult());
                setLocalResult(leftResult);
            }
            // GC spliterator, left and right child
            super.onCompletion(caller);
    }

  }

}
