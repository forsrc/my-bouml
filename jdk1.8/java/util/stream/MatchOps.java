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
import java.util.Spliterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
/**
 * Factory for instances of a short-circuiting {@code TerminalOp} that implement
 * quantified predicate matching on the elements of a stream. Supported variants
 * include match-all, match-any, and match-none.
 * 
 * @since 1.8
 */
final class MatchOps {
  private MatchOps() {
  }

  enum MatchKind {
    ANY(true, true),/**
     *  Do all elements match the predicate? 
     */

    ALL(false, false),/**
     *  Do any elements match the predicate? 
     */

    NONE(true, false),/**
     *  Do no elements match the predicate? 
     */
;
    private final boolean stopOnPredicateMatches;
    private final boolean shortCircuitResult;
    private MatchKind(boolean stopOnPredicateMatches, boolean shortCircuitResult) {
            this.stopOnPredicateMatches = stopOnPredicateMatches;
            this.shortCircuitResult = shortCircuitResult;
    }

  }

  /**
   * Constructs a quantified predicate matcher for a Stream.
   * 
   * @param <T> the type of stream elements
   * @param predicate the {@code Predicate} to apply to stream elements
   * @param matchKind the kind of quantified match (all, any, none)
   * @return a {@code TerminalOp} implementing the desired quantified match
   *         criteria
   */
  public static <T> TerminalOp<T, Boolean> makeRef(java.util.function.Predicate<? super T> predicate, MatchOps.MatchKind matchKind)
  {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        class MatchSink extends BooleanTerminalSink<T> {
            MatchSink() {
                super(matchKind);
            }

            @Override
            public void accept(T t) {
                if (!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }

        return new MatchOp<>(StreamShape.REFERENCE, matchKind, MatchSink::new);
  }

  /**
   * Constructs a quantified predicate matcher for an {@code IntStream}.
   * 
   * @param predicate the {@code Predicate} to apply to stream elements
   * @param matchKind the kind of quantified match (all, any, none)
   * @return a {@code TerminalOp} implementing the desired quantified match
   *         criteria
   */
  public static TerminalOp<Integer, Boolean> makeInt(java.util.function.IntPredicate predicate, MatchOps.MatchKind matchKind)
  {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        class MatchSink extends BooleanTerminalSink<Integer> implements Sink.OfInt {
            MatchSink() {
                super(matchKind);
            }

            @Override
            public void accept(int t) {
                if (!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }

        return new MatchOp<>(StreamShape.INT_VALUE, matchKind, MatchSink::new);
  }

  /**
   * Constructs a quantified predicate matcher for a {@code LongStream}.
   * 
   * @param predicate the {@code Predicate} to apply to stream elements
   * @param matchKind the kind of quantified match (all, any, none)
   * @return a {@code TerminalOp} implementing the desired quantified match
   *         criteria
   */
  public static TerminalOp<Long, Boolean> makeLong(java.util.function.LongPredicate predicate, MatchOps.MatchKind matchKind)
  {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        class MatchSink extends BooleanTerminalSink<Long> implements Sink.OfLong {

            MatchSink() {
                super(matchKind);
            }

            @Override
            public void accept(long t) {
                if (!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }

        return new MatchOp<>(StreamShape.LONG_VALUE, matchKind, MatchSink::new);
  }

  /**
   * Constructs a quantified predicate matcher for a {@code DoubleStream}.
   * 
   * @param predicate the {@code Predicate} to apply to stream elements
   * @param matchKind the kind of quantified match (all, any, none)
   * @return a {@code TerminalOp} implementing the desired quantified match
   *         criteria
   */
  public static TerminalOp<Double, Boolean> makeDouble(java.util.function.DoublePredicate predicate, MatchOps.MatchKind matchKind)
  {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(matchKind);
        class MatchSink extends BooleanTerminalSink<Double> implements Sink.OfDouble {

            MatchSink() {
                super(matchKind);
            }

            @Override
            public void accept(double t) {
                if (!stop && predicate.test(t) == matchKind.stopOnPredicateMatches) {
                    stop = true;
                    value = matchKind.shortCircuitResult;
                }
            }
        }

        return new MatchOp<>(StreamShape.DOUBLE_VALUE, matchKind, MatchSink::new);
  }

  private static final class MatchOp<T> implements TerminalOp<, > {
    private final StreamShape inputShape;

    final MatchOps.MatchKind matchKind;

    final java.util.function.Supplier<BooleanTerminalSink<T>> sinkSupplier;

    /**
     * Constructs a {@code MatchOp}.
     * 
     * @param shape the output shape of the stream pipeline
     * @param matchKind the kind of quantified match (all, any, none)
     * @param sinkSupplier {@code Supplier} for a {@code Sink} of the
     *        appropriate shape which implements the matching operation
     */
    MatchOp(StreamShape shape, MatchOps.MatchKind matchKind, java.util.function.Supplier<BooleanTerminalSink<T>> sinkSupplier) {
            this.inputShape = shape;
            this.matchKind = matchKind;
            this.sinkSupplier = sinkSupplier;
    }

    @Override
    public int getOpFlags() {
            return StreamOpFlag.IS_SHORT_CIRCUIT | StreamOpFlag.NOT_ORDERED;
    }

    @Override
    public StreamShape inputShape() {
            return inputShape;
    }

    @Override
    public <S> Boolean evaluateSequential(PipelineHelper<T> helper, java.util.Spliterator<S> spliterator) {
            return helper.wrapAndCopyInto(sinkSupplier.get(), spliterator).getAndClearState();
    }

    @Override
    public <S> Boolean evaluateParallel(PipelineHelper<T> helper, java.util.Spliterator<S> spliterator) {
            // Approach for parallel implementation:
            // - Decompose as per usual
            // - run match on leaf chunks, call result "b"
            // - if b == matchKind.shortCircuitOn, complete early and return b
            // - else if we complete normally, return !shortCircuitOn

            return new MatchTask<>(this, helper, spliterator).invoke();
    }

  }

  private static abstract class BooleanTerminalSink<T> implements Sink<> {
    boolean stop;

    boolean value;

    BooleanTerminalSink(MatchOps.MatchKind matchKind) {
            value = !matchKind.shortCircuitResult;
    }

    public boolean getAndClearState() {
            return value;
    }

    @Override
    public boolean cancellationRequested() {
            return stop;
    }

  }

  @SuppressWarnings("serial")
  private static final class MatchTask<P_IN, P_OUT> extends AbstractShortCircuitTask<, , , > {
    private final MatchOps.MatchOp<P_OUT> op;

    /**
     * Constructor for root node
     */
    MatchTask(MatchOps.MatchOp<P_OUT> op, PipelineHelper<P_OUT> helper, java.util.Spliterator<P_IN> spliterator) {
            super(helper, spliterator);
            this.op = op;
    }

    /**
     * Constructor for non-root node
     */
    MatchTask(MatchOps.MatchTask<P_IN, P_OUT> parent, java.util.Spliterator<P_IN> spliterator) {
            super(parent, spliterator);
            this.op = parent.op;
    }

    @Override
    protected MatchOps.MatchTask<P_IN, P_OUT> makeChild(java.util.Spliterator<P_IN> spliterator) {
            return new MatchTask<>(this, spliterator);
    }

    @Override
    protected Boolean doLeaf() {
            boolean b = helper.wrapAndCopyInto(op.sinkSupplier.get(), spliterator).getAndClearState();
            if (b == op.matchKind.shortCircuitResult)
                shortCircuit(b);
            return null;
    }

    @Override
    protected Boolean getEmptyResult() {
            return !op.matchKind.shortCircuitResult;
    }

  }

}
