package com.aol.cyclops.reactor.adapter;

import com.aol.cyclops2.internal.stream.ReactiveStreamX;
import com.aol.cyclops2.internal.stream.spliterators.push.PublisherToOperator;
import com.aol.cyclops2.types.anyM.AnyMSeq;
import com.aol.cyclops2.types.stream.HeadAndTail;
import com.aol.cyclops2.types.traversable.FoldableTraversable;
import com.aol.cyclops2.types.traversable.Traversable;
import cyclops.async.Future;
import cyclops.async.adapters.QueueFactory;
import cyclops.collections.immutable.VectorX;
import cyclops.collections.mutable.ListX;
import cyclops.companion.*;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Try;
import cyclops.control.lazy.Either;
import cyclops.function.Monoid;
import cyclops.function.Reducer;
import cyclops.monads.AnyM;
import cyclops.monads.Witness;
import cyclops.monads.Witness.reactiveSeq;
import cyclops.monads.Witness.stream;
import cyclops.monads.transformers.ListT;
import cyclops.stream.ReactiveSeq;
import cyclops.stream.Spouts;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSource;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.*;




@AllArgsConstructor
public class FluxReactiveSeq<T> implements ReactiveSeq<T> {
    @Wither
    @Getter
    Flux<T> flux;

    public <R> FluxReactiveSeq<R> flux(Flux<R> flux){
        return new FluxReactiveSeq<>(flux);
    }
    public <R> FluxReactiveSeq<R> flux(ReactiveSeq<R> flux){
        if(flux instanceof FluxReactiveSeq){
            return  (FluxReactiveSeq)flux;
        }
        return new FluxReactiveSeq<>(Flux.from(flux));
    }

    @Override
    public <R> ReactiveSeq<R> coflatMap(Function<? super ReactiveSeq<T>, ? extends R> fn) {
        return flux(Flux.just(fn.apply(this)));
    }

    @Override
    public <T1> ReactiveSeq<T1> unit(T1 unit) {
        return flux(Flux.just(unit));
    }

    @Override
    public <U> U foldRight(U identity, BiFunction<? super T, ? super U, ? extends U> accumulator) {
        return flux.reduce(identity,(a,b)->accumulator.apply(b,a)).block();
    }

    @Override
    public <U, R> ReactiveSeq<R> zipS(Stream<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        if(other instanceof Publisher){
            return zipP((Publisher<U>)other,zipper);
        }
        return flux(flux.zipWithIterable(ReactiveSeq.fromStream((Stream<U>)other),zipper));
    }

    @Override
    public <U, R> ReactiveSeq<R> zipLatest(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return flux(Flux.combineLatest(flux,other,zipper));
    }

    @Override
    public <U, R> ReactiveSeq<R> zipP(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return flux(flux.zipWith(other,zipper));
    }

    @Override
    public <U> ReactiveSeq<Tuple2<T, U>> zipP(Publisher<? extends U> other) {
        return flux(flux.zipWith(other,Tuple::tuple));
    }

    @Override
    public ReactiveSeq<T> cycle() {
        return flux(flux.repeat());
    }

    @Override
    public Tuple2<ReactiveSeq<T>, ReactiveSeq<T>> duplicate() {
        return Spouts.from(flux).duplicate().map((s1,s2)->Tuple.tuple(flux(s1),flux(s2)));
    }

    @Override
    public Tuple2<ReactiveSeq<T>, ReactiveSeq<T>> duplicate(Supplier<Deque<T>> bufferFactory) {
        return Spouts.from(flux).duplicate(bufferFactory).map((s1,s2)->Tuple.tuple(flux(s1),flux(s2)));
    }

    @Override
    public Tuple3<ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>> triplicate() {
        return Spouts.from(flux).triplicate().map((s1,s2,s3)->Tuple.tuple(flux(s1),flux(s2),flux(s3)));
    }

    @Override
    public Tuple3<ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>> triplicate(Supplier<Deque<T>> bufferFactory) {
        return Spouts.from(flux).triplicate(bufferFactory).map((s1,s2,s3)->Tuple.tuple(flux(s1),flux(s2),flux(s3)));
    }

    @Override
    public Tuple4<ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>> quadruplicate() {
        return Spouts.from(flux).quadruplicate().map((s1,s2,s3,s4)->Tuple.tuple(flux(s1),flux(s2),flux(s3),flux(s4)));
    }

    @Override
    public Tuple4<ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>, ReactiveSeq<T>> quadruplicate(Supplier<Deque<T>> bufferFactory) {
        return Spouts.from(flux).quadruplicate(bufferFactory).map((s1,s2,s3,s4)->Tuple.tuple(flux(s1),flux(s2),flux(s3),flux(s4)));
    }

    @Override
    public Tuple2<Optional<T>, ReactiveSeq<T>> splitAtHead() {
        return Spouts.from(flux).splitAtHead().map((s1,s2)->Tuple.tuple(s1,flux(s2)));
    }

    @Override
    public Tuple2<ReactiveSeq<T>, ReactiveSeq<T>> splitAt(int where) {
        return Spouts.from(flux).splitAt(where).map((s1,s2)->Tuple.tuple(flux(s1),flux(s2)));
    }

    @Override
    public Tuple2<ReactiveSeq<T>, ReactiveSeq<T>> splitBy(Predicate<T> splitter) {
        return Spouts.from(flux).splitBy(splitter).map((s1,s2)->Tuple.tuple(flux(s1),flux(s2)));
    }

    @Override
    public Tuple2<ReactiveSeq<T>, ReactiveSeq<T>> partition(Predicate<? super T> splitter) {
        return Spouts.from(flux).partition(splitter).map((s1,s2)->Tuple.tuple(flux(s1),flux(s2)));
    }

    @Override
    public <U> ReactiveSeq<Tuple2<T, U>> zipS(Stream<? extends U> other) {
        if(other instanceof Publisher){
            return zipP((Publisher<U>)other);
        }
        return zipS(other,Tuple::tuple);
    }

    @Override
    public <S, U> ReactiveSeq<Tuple3<T, S, U>> zip3(Iterable<? extends S> second, Iterable<? extends U> third) {
        return zip(second,Tuple::tuple).zip(third,(a,b)->Tuple.tuple(a.v1,a.v2,b));
    }

    @Override
    public <T2, T3, T4> ReactiveSeq<Tuple4<T, T2, T3, T4>> zip4(Iterable<? extends T2> second, Iterable<? extends T3> third, Iterable<? extends T4> fourth) {
        return zip(second,Tuple::tuple).zip(third,(a,b)->Tuple.tuple(a.v1,a.v2,b))
                .zip(fourth,(a,b)->(Tuple4<T,T2,T3,T4>)Tuple.tuple(a.v1,a.v2,a.v3,b));
    }

    @Override
    public ReactiveSeq<VectorX<T>> sliding(int windowSize, int increment) {
        return flux(Spouts.from(flux).sliding(windowSize,increment));
    }

    @Override
    public ReactiveSeq<ListX<T>> grouped(int groupSize) {
        return flux(flux.buffer(groupSize).map(ListX::fromIterable));
    }

    @Override
    public ReactiveSeq<ListX<T>> groupedStatefullyUntil(BiPredicate<ListX<? super T>, ? super T> predicate) {
        return flux(Spouts.from(flux).groupedStatefullyUntil(predicate));
    }

    @Override
    public <C extends Collection<T>, R> ReactiveSeq<R> groupedStatefullyUntil(BiPredicate<C, ? super T> predicate, Supplier<C> factory, Function<? super C, ? extends R> finalizer) {
        return flux(Spouts.from(flux).groupedStatefullyUntil(predicate,factory,finalizer));
    }

    @Override
    public ReactiveSeq<ListX<T>> groupedStatefullyWhile(BiPredicate<ListX<? super T>, ? super T> predicate) {
        return flux(Spouts.from(flux).groupedStatefullyWhile(predicate));
    }

    @Override
    public <C extends Collection<T>, R> ReactiveSeq<R> groupedStatefullyWhile(BiPredicate<C, ? super T> predicate, Supplier<C> factory, Function<? super C, ? extends R> finalizer) {
        return flux(Spouts.from(flux).groupedStatefullyWhile(predicate,factory,finalizer));
    }

    @Override
    public ReactiveSeq<ListX<T>> groupedBySizeAndTime(int size, long time, TimeUnit t) {
        return flux(Spouts.from(flux).groupedBySizeAndTime(size, time, t));
    }

    @Override
    public <C extends Collection<? super T>> ReactiveSeq<C> groupedBySizeAndTime(int size, long time, TimeUnit unit, Supplier<C> factory) {
        return flux(Spouts.from(flux).groupedBySizeAndTime(size,time,unit,factory));
    }

    @Override
    public <C extends Collection<? super T>, R> ReactiveSeq<R> groupedBySizeAndTime(int size, long time, TimeUnit unit, Supplier<C> factory, Function<? super C, ? extends R> finalizer) {
        return flux(Spouts.from(flux).groupedBySizeAndTime(size,time,unit,factory,finalizer));
    }

    @Override
    public <C extends Collection<? super T>, R> ReactiveSeq<R> groupedByTime(long time, TimeUnit unit, Supplier<C> factory, Function<? super C, ? extends R> finalizer) {
        return groupedBySizeAndTime(Integer.MAX_VALUE,time,unit,factory,finalizer);
    }

    @Override
    public ReactiveSeq<ListX<T>> groupedByTime(long time, TimeUnit t) {
        return flux(Spouts.from(flux).groupedByTime(time, t));
    }

    @Override
    public <C extends Collection<? super T>> ReactiveSeq<C> groupedByTime(long time, TimeUnit unit, Supplier<C> factory) {
        return flux(Spouts.from(flux).groupedByTime(time, unit, factory));
    }

    @Override
    public <C extends Collection<? super T>> ReactiveSeq<C> grouped(int size, Supplier<C> supplier) {
        return flux(flux.buffer(size,supplier));
    }

    @Override
    public ReactiveSeq<ListX<T>> groupedWhile(Predicate<? super T> predicate) {
        return flux(Spouts.from(flux).groupedWhile(predicate));
    }

    @Override
    public <C extends Collection<? super T>> ReactiveSeq<C> groupedWhile(Predicate<? super T> predicate, Supplier<C> factory) {
        return flux(Spouts.from(flux).groupedWhile(predicate,factory));
    }

    @Override
    public ReactiveSeq<T> distinct() {
        return flux(flux.distinct());
    }

    @Override
    public <U> ReactiveSeq<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return flux(flux.scan(seed,(a,b)->function.apply(a,b)));
    }

    @Override
    public ReactiveSeq<T> sorted() {
        return flux(flux.sort());
    }

    @Override
    public ReactiveSeq<T> skip(long num) {
        return flux(flux.skip(num));
    }


    @Override
    public void forEach(Consumer<? super T> action) {
        Spouts.from(flux).forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        Spouts.from(flux).forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return Spouts.from(flux).toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return Spouts.from(flux).toArray(generator);
    }

    @Override
    public ReactiveSeq<T> skipWhile(Predicate<? super T> p) {
        return flux(flux.skipWhile(p));
    }

    @Override
    public ReactiveSeq<T> limit(long num) {
        return flux(flux.take(num));
    }

    @Override
    public ReactiveSeq<T> limitWhile(Predicate<? super T> p) {
        return flux(Spouts.from(flux).takeWhile(p));
    }
    @Override
    public ReactiveSeq<T> limitWhileClosed(Predicate<? super T> p) {
        return flux(flux.takeWhile(p));
    }

    @Override
    public ReactiveSeq<T> limitUntil(Predicate<? super T> p) {
       return flux(Spouts.from(flux).limitUntil(p));
    }

    @Override
    public ReactiveSeq<T> limitUntilClosed(Predicate<? super T> p) {
        return flux(flux.takeUntil(p));
    }

    @Override
    public ReactiveSeq<T> parallel() {
        return this;
    }

    @Override
    public boolean allMatch(Predicate<? super T> c) {
        return Spouts.from(flux).allMatch(c);
    }

    @Override
    public boolean anyMatch(Predicate<? super T> c) {
        return Spouts.from(flux).anyMatch(c);
    }

    @Override
    public boolean xMatch(int num, Predicate<? super T> c) {
        return Spouts.from(flux).xMatch(num,c);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> c) {
        return Spouts.from(flux).noneMatch(c);
    }

    @Override
    public String join() {
        return Spouts.from(flux).join();
    }

    @Override
    public String join(String sep) {
        return Spouts.from(flux).join(sep);
    }

    @Override
    public String join(String sep, String start, String end) {
        return Spouts.from(flux).join(sep,start,end);
    }

    @Override
    public HeadAndTail<T> headAndTail() {
        return Spouts.from(flux).headAndTail();
    }

    @Override
    public Optional<T> findFirst() {
        return Spouts.from(flux).findFirst();
    }

    @Override
    public Maybe<T> findOne() {
        return Spouts.from(flux).findOne();
    }

    @Override
    public Either<Throwable, T> findFirstOrError() {
        return Spouts.from(flux).findFirstOrError();
    }

    @Override
    public Optional<T> findAny() {
        return Spouts.from(flux).findAny();
    }

    @Override
    public <R> R mapReduce(Reducer<R> reducer) {
        return Spouts.from(flux).mapReduce(reducer);
    }

    @Override
    public <R> R mapReduce(Function<? super T, ? extends R> mapper, Monoid<R> reducer) {
        return Spouts.from(flux).mapReduce(mapper,reducer);
    }

    @Override
    public T reduce(Monoid<T> reducer) {
        return Spouts.from(flux).reduce(reducer);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return Spouts.from(flux).reduce(accumulator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return Spouts.from(flux).reduce(identity,accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return Spouts.from(flux).reduce(identity, accumulator, combiner);
    }

    @Override
    public ListX<T> reduce(Stream<? extends Monoid<T>> reducers) {
        return Spouts.from(flux).reduce(reducers);
    }

    @Override
    public ListX<T> reduce(Iterable<? extends Monoid<T>> reducers) {
        return Spouts.from(flux).reduce(reducers);
    }

    @Override
    public T foldRight(Monoid<T> reducer) {
        return Spouts.from(flux).foldRight(reducer);
    }

    @Override
    public T foldRight(T identity, BinaryOperator<T> accumulator) {
        return Spouts.from(flux).foldRight(identity,accumulator);
    }

    @Override
    public <T1> T1 foldRightMapToType(Reducer<T1> reducer) {
        return Spouts.from(flux).foldRightMapToType(reducer);
    }

    @Override
    public ReactiveSeq<T> stream() {
        return Spouts.from(flux);
    }

    @Override
    public <U> Traversable<U> unitIterator(Iterator<U> U) {
        return new FluxReactiveSeq<>(Flux.fromIterable(()->U));
    }

    @Override
    public boolean startsWithIterable(Iterable<T> iterable) {
        return Spouts.from(flux).startsWithIterable(iterable);
    }

    @Override
    public boolean startsWith(Stream<T> stream) {
        return Spouts.from(flux).startsWith(stream);
    }

    @Override
    public AnyMSeq<reactiveSeq, T> anyM() {
        return AnyM.fromStream(this);
    }

    @Override
    public <R> ReactiveSeq<R> map(Function<? super T, ? extends R> fn) {
        return flux(flux.map(fn));
    }

    @Override
    public <R> ReactiveSeq<R> flatMap(Function<? super T, ? extends Stream<? extends R>> fn) {
        return flux(flux.flatMap(s->ReactiveSeq.fromStream(fn.apply(s))));
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return Spouts.from(flux).flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return Spouts.from(flux).flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return Spouts.from(flux).flatMapToDouble(mapper);
    }

    @Override
    public <R> ReactiveSeq<R> flatMapAnyM(Function<? super T, AnyM<stream, ? extends R>> fn) {
        return flux(flux.flatMap(fn));
    }

    @Override
    public <R> ReactiveSeq<R> flatMapI(Function<? super T, ? extends Iterable<? extends R>> fn) {
        return flux(flux.flatMapIterable(fn));
    }



    @Override
    public <R> ReactiveSeq<R> flatMapP(Function<? super T, ? extends Publisher<? extends R>> fn) {
        return flux(flux.flatMap(fn));
    }

    @Override
    public <R> ReactiveSeq<R> flatMapP(int maxConcurrency, Function<? super T, ? extends Publisher<? extends R>> fn) {
        return flux(flux.flatMap(fn,maxConcurrency));
    }

    @Override
    public <R> ReactiveSeq<R> flatMapStream(Function<? super T, BaseStream<? extends R, ?>> fn) {
        
        return this.<R>flux((Flux)flux.flatMap(fn.andThen(s->{
            ReactiveSeq<R> res = s instanceof ReactiveSeq ? (ReactiveSeq) s : (ReactiveSeq) ReactiveSeq.fromSpliterator(s.spliterator());
           return res;
                }
            
        )));
    }

    @Override
    public ReactiveSeq<T> filter(Predicate<? super T> fn) {
        return flux(flux.filter(fn));
    }

    @Override
    public Iterator<T> iterator() {
        return flux.toIterable().iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return flux.toIterable().spliterator();
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public ReactiveSeq<T> sequential() {
        return this;
    }

    @Override
    public ReactiveSeq<T> unordered() {
        return this;
    }

    @Override
    public ReactiveSeq<T> reverse() {
        return flux(Spouts.from(flux).reverse());
    }

    @Override
    public ReactiveSeq<T> onClose(Runnable closeHandler) {
        return flux(flux.doOnComplete(closeHandler));
    }

    @Override
    public void close() {
        
    }

    @Override
    public ReactiveSeq<T> prependS(Stream<? extends T> stream) {
        return flux(Spouts.from(flux).prependS(stream));
    }

    @Override
    public ReactiveSeq<T> append(T... values) {
        return flux(Spouts.from(flux).append(values));
    }

    @Override
    public ReactiveSeq<T> append(T value) {
        return flux(Spouts.from(flux).append(value));
    }

    @Override
    public ReactiveSeq<T> prepend(T value) {
        return flux(Spouts.from(flux).prepend(value));
    }

    @Override
    public ReactiveSeq<T> prepend(T... values) {
        return flux(Spouts.from(flux).prepend(values));
    }

    @Override
    public boolean endsWithIterable(Iterable<T> iterable) {
        return Spouts.from(flux).endsWithIterable(iterable);
    }

    @Override
    public boolean endsWith(Stream<T> stream) {
        return Spouts.from(flux).endsWith(stream);
    }

    @Override
    public ReactiveSeq<T> skip(long time, TimeUnit unit) {
        return flux(flux.skip(Duration.ofNanos(unit.toNanos(time))));
    }

    @Override
    public ReactiveSeq<T> limit(long time, TimeUnit unit) {
        return flux(flux.take(Duration.ofNanos(unit.toNanos(time))));
    }

    @Override
    public ReactiveSeq<T> skipLast(int num) {
        return flux(flux.skipLast(num));
    }

    @Override
    public ReactiveSeq<T> limitLast(int num) {
        return flux(flux.takeLast(num));
    }

    @Override
    public T firstValue() {
        return flux.blockFirst();
    }

    @Override
    public ReactiveSeq<T> onEmptySwitch(Supplier<? extends Stream<T>> switchTo) {
        return flux(Spouts.from(flux).onEmptySwitch(switchTo));
    }

    @Override
    public ReactiveSeq<T> onEmptyGet(Supplier<? extends T> supplier) {
        return flux(Spouts.from(flux).onEmptyGet(supplier));
    }

    @Override
    public <X extends Throwable> ReactiveSeq<T> onEmptyThrow(Supplier<? extends X> supplier) {
        return flux(Spouts.from(flux).onEmptyThrow(supplier));
    }

    @Override
    public <U> ReactiveSeq<T> distinct(Function<? super T, ? extends U> keyExtractor) {
        return flux(flux.distinct(keyExtractor));
    }

    @Override
    public ReactiveSeq<T> xPer(int x, long time, TimeUnit t) {
        return flux(Spouts.from(flux).xPer(x,time,t));
    }

    @Override
    public ReactiveSeq<T> onePer(long time, TimeUnit t) {
        return flux(Spouts.from(flux).onePer(time,t));
    }

    @Override
    public ReactiveSeq<T> debounce(long time, TimeUnit t) {
        return flux(Spouts.from(flux).debounce(time,t));
    }

    @Override
    public ReactiveSeq<T> fixedDelay(long l, TimeUnit unit) {
        return flux(Spouts.from(flux).fixedDelay(l,unit));
    }

    @Override
    public ReactiveSeq<T> jitter(long maxJitterPeriodInNanos) {
        return flux(Spouts.from(flux).jitter(maxJitterPeriodInNanos));
    }

    @Override
    public ReactiveSeq<T> complete(Runnable fn) {
        return flux(flux.doOnComplete(fn));
    }

    @Override
    public ReactiveSeq<T> recover(Function<? super Throwable, ? extends T> fn) {
        return flux(Spouts.from(flux).recover(fn));
    }

    @Override
    public <EX extends Throwable> ReactiveSeq<T> recover(Class<EX> exceptionClass, Function<? super EX, ? extends T> fn) {
        return flux(Spouts.from(flux).recover(exceptionClass,fn));
    }

    @Override
    public long count() {
        return Spouts.from(flux).count();
    }

    @Override
    public ReactiveSeq<T> appendS(Stream<? extends T> other) {
        return flux(Spouts.from(flux).appendS(other));
    }

    @Override
    public ReactiveSeq<T> append(Iterable<? extends T> other) {
        return  flux(Spouts.from(flux).append(other));
    }

    @Override
    public ReactiveSeq<T> prepend(Iterable<? extends T> other) {
        return flux(Spouts.from(flux).prepend(other));
    }

    @Override
    public ReactiveSeq<T> cycle(long times) {
        return flux(flux.repeat(times));
    }

    @Override
    public ReactiveSeq<T> skipWhileClosed(Predicate<? super T> predicate) {
        return flux(Spouts.from(flux).skipWhileClosed(predicate));
    }


    @Override
    public String format() {
        return Spouts.from(flux).format();
    }

    @Override
    public ReactiveSeq<T> changes() {
        return flux(Spouts.from(flux).changes());
    }



    @Override
    public <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer) {
        return Spouts.from(flux).forEachSubscribe(consumer);
    }

    @Override
    public <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer, Consumer<? super Throwable> consumerError) {
        return Spouts.from(flux).forEachSubscribe(consumer, consumerError);
    }

    @Override
    public <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer, Consumer<? super Throwable> consumerError, Runnable onComplete) {
        return Spouts.from(flux).forEachSubscribe(consumer, consumerError,onComplete);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {

        return flux.toStream().collect(supplier,accumulator,combiner);
    }

    @Override
    public <R, A> ReactiveSeq<R> collectStream(Collector<? super T, A, R> collector) {
        return flux(Flux.from(flux.collect((Collector<T,A,R>)collector)));
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return flux.collect((Collector<T,A,R>)collector).block();
    }

    @Override
    public void forEachAsync(Consumer<? super T> action) {
        flux.subscribe(action);
    }

    @Override
    public T singleUnsafe() {
        return single().get();
    }

    @Override
    public Maybe<T> single(Predicate<? super T> predicate) {
        return filter(predicate).single();
    }

    @Override
    public Maybe<T> single() {
        Maybe.CompletableMaybe<T,T> maybe = Maybe.<T>maybe();
        flux.subscribe(new Subscriber<T>() {
            T value = null;
            Subscription sub;
            @Override
            public void onSubscribe(Subscription s) {
                this.sub=s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                if(value==null)
                    value = t;
                else {
                    maybe.complete(null);
                    sub.cancel();
                }
            }

            @Override
            public void onError(Throwable t) {
                maybe.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                maybe.complete(value);
            }
        });
        return maybe;
    }
    /**
    @Override
   public <R> ReactiveSeq<R> fanOut(Function<? super ReactiveSeq<T>, ? extends ReactiveSeq<? extends R>> path1,
                                      Function<? super ReactiveSeq<T>, ? extends ReactiveSeq<? extends R>> path2,
                                      Function<? super ReactiveSeq<T>, ? extends ReactiveSeq<? extends R>> path3){

        Flux<T> co = flux.doOnSubscribe(s -> System.out.println("subscribed to source")).publish().autoConnect(3);
        ListX<ReactiveSeq<T>> list = multicast(3);
        Publisher<R> pub2 = (Publisher<R>)path2.apply(list.get(1));
        Publisher<R> pub3 = (Publisher<R>)path3.apply(list.get(2));
        ReactiveSeq<R> seq = (ReactiveSeq<R>)path1.apply(list.get(0));
        return  flux(Flux.merge(pub2,pub3,seq));



    }
    @Override
    public ListX<ReactiveSeq<T>> multicast(int num) {
        ListX<ReactiveSeq<T>> result = ListX.empty();
         for(int i=0;i<num;i++) {

            result.add(flux(FluxSource.wrap()));
        }

        return result;
    }
     **/

    @Override
    public void subscribe(Subscriber<? super T> s) {
        flux.subscribe(s);
    }

    @Override
    public <R> R visit(Function<? super ReactiveSeq<T>,? extends R> sync,Function<? super ReactiveSeq<T>,? extends R> reactiveStreams,
                         Function<? super ReactiveSeq<T>,? extends R> asyncNoBackPressure){
        return reactiveStreams.apply(this);
    }

    @Override
    public ListT<reactiveSeq, T> groupedT(int groupSize) {
        return ListT.fromStream(grouped(groupSize));
    }

    @Override
    public ListT<reactiveSeq, T> slidingT(int windowSize, int increment) {
        return ListT.fromStream(sliding(windowSize,increment));
    }

    @Override
    public ListT<reactiveSeq, T> slidingT(int windowSize) {
        return ListT.fromStream(sliding(windowSize));
    }

    @Override
    public ListT<reactiveSeq, T> groupedUntilT(Predicate<? super T> predicate) {
        return ListT.fromStream(groupedUntil(predicate));
    }

    @Override
    public ListT<reactiveSeq, T> groupedStatefullyUntilT(BiPredicate<ListX<? super T>, ? super T> predicate) {
        return ListT.fromStream(groupedStatefullyWhile(predicate));
    }

    @Override
    public ListT<reactiveSeq, T> groupedWhileT(Predicate<? super T> predicate) {
        return ListT.fromStream(groupedWhile(predicate));
    }



}
