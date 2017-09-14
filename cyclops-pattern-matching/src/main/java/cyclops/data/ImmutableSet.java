package cyclops.data;


import com.aol.cyclops2.types.Filters;
import com.aol.cyclops2.types.foldable.Evaluation;
import com.aol.cyclops2.types.foldable.Folds;
import com.aol.cyclops2.types.functor.Transformable;
import com.aol.cyclops2.types.recoverable.OnEmpty;
import com.aol.cyclops2.types.recoverable.OnEmptySwitch;
import com.aol.cyclops2.util.ExceptionSoftener;
import cyclops.collections.immutable.PersistentSetX;
import cyclops.control.Trampoline;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.stream.ReactiveSeq;

import java.util.concurrent.TimeUnit;
import java.util.function.*;

public interface ImmutableSet<T> extends Folds<T>,
                                         Filters<T>,
                                         Transformable<T>,
                                         OnEmpty<T>,
                                         OnEmptySwitch<ImmutableSet<T>,ImmutableSet<T>>,
                                         Iterable<T> {
    @Override
    default <U> ImmutableSet<U> ofType(Class<? extends U> type) {
        return (ImmutableSet<U>)Filters.super.ofType(type);
    }

    @Override
    default ImmutableSet<T> filterNot(Predicate<? super T> predicate) {
        return (ImmutableSet<T>)Filters.super.filterNot(predicate);
    }

    @Override
    default ImmutableSet<T> notNull() {
        return (ImmutableSet<T>)Filters.super.notNull();
    }

    @Override
    default <U> ImmutableSet<U> cast(Class<? extends U> type) {
        return (ImmutableSet<U>)Transformable.super.cast(type);
    }

    @Override
    default ImmutableSet<T> peek(Consumer<? super T> c) {
        return (ImmutableSet<T>)Transformable.super.peek(c);
    }

    @Override
    default <R> ImmutableSet<R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (ImmutableSet<R>)Transformable.super.trampoline(mapper);
    }

    @Override
    default <R> ImmutableSet<R> retry(Function<? super T, ? extends R> fn) {
        return (ImmutableSet<R>)Transformable.super.retry(fn);
    }

    @Override
    default <R> ImmutableSet<R> retry(Function<? super T, ? extends R> fn, int retries, long delay, TimeUnit timeUnit) {
        return (ImmutableSet<R>)Transformable.super.retry(fn,retries,delay,timeUnit);
    }

    default PersistentSetX<T> persistentSetX(){
        return stream().to().persistentSetX(Evaluation.LAZY);
    }
    boolean contains(T value);
    int size();
    ImmutableSet<T> add(T value);
    ImmutableSet<T> remove(T value);
    boolean isEmpty();

    <R> ImmutableSet<R> map(Function<? super T,? extends R> fn);
    <R> ImmutableSet<R> flatMap(Function<? super T,? extends ImmutableSet<? extends R>> fn);
    <R> ImmutableSet<R> flatMapI(Function<? super T,? extends Iterable<? extends R>> fn);

    ImmutableSet<T> filter(Predicate<? super T> predicate);

    default <R1, R2, R3, R> ImmutableSet<R> forEach4(Function<? super T, ? extends Iterable<R1>> iterable1,
                                                      BiFunction<? super T, ? super R1, ? extends Iterable<R2>> iterable2,
                                                      Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> iterable3,
                                                      Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return this.flatMapI(in -> {

            ReactiveSeq<R1> a = ReactiveSeq.fromIterable(iterable1.apply(in));
            return a.flatMap(ina -> {
                ReactiveSeq<R2> b = ReactiveSeq.fromIterable(iterable2.apply(in, ina));
                return b.flatMap(inb -> {
                    ReactiveSeq<R3> c = ReactiveSeq.fromIterable(iterable3.apply(in, ina, inb));
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }

    default <R1, R2, R3, R> ImmutableSet<R> forEach4(Function<? super T, ? extends Iterable<R1>> iterable1,
                                                      BiFunction<? super T, ? super R1, ? extends Iterable<R2>> iterable2,
                                                      Fn3<? super T, ? super R1, ? super R2, ? extends Iterable<R3>> iterable3,
                                                      Fn4<? super T, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                      Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        return this.flatMapI(in -> {

            ReactiveSeq<R1> a = ReactiveSeq.fromIterable(iterable1.apply(in));
            return a.flatMap(ina -> {
                ReactiveSeq<R2> b = ReactiveSeq.fromIterable(iterable2.apply(in, ina));
                return b.flatMap(inb -> {
                    ReactiveSeq<R3> c = ReactiveSeq.fromIterable(iterable3.apply(in, ina, inb));
                    return c.filter(in2 -> filterFunction.apply(in, ina, inb, in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }
    default <R1, R2, R> ImmutableSet<R> forEach3(Function<? super T, ? extends Iterable<R1>> iterable1,
                                                  BiFunction<? super T, ? super R1, ? extends Iterable<R2>> iterable2,
                                                  Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return this.flatMapI(in -> {

            Iterable<R1> a = iterable1.apply(in);
            return ReactiveSeq.fromIterable(a)
                    .flatMap(ina -> {
                        ReactiveSeq<R2> b = ReactiveSeq.fromIterable(iterable2.apply(in, ina));
                        return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
                    });

        });
    }


    default <R1, R2, R> ImmutableSet<R> forEach3(Function<? super T, ? extends Iterable<R1>> iterable1,
                                                  BiFunction<? super T, ? super R1, ? extends Iterable<R2>> iterable2,
                                                  Fn3<? super T, ? super R1, ? super R2, Boolean> filterFunction,
                                                  Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return this.flatMapI(in -> {

            Iterable<R1> a = iterable1.apply(in);
            return ReactiveSeq.fromIterable(a)
                    .flatMap(ina -> {
                        ReactiveSeq<R2> b = ReactiveSeq.fromIterable(iterable2.apply(in, ina));
                        return b.filter(in2 -> filterFunction.apply(in, ina, in2))
                                .map(in2 -> yieldingFunction.apply(in, ina, in2));
                    });

        });
    }


    default <R1, R> ImmutableSet<R> forEach2(Function<? super T, ? extends Iterable<R1>> iterable1,
                                              BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return this.flatMapI(in-> {

            Iterable<? extends R1> b = iterable1.apply(in);
            return ReactiveSeq.fromIterable(b)
                    .map(in2->yieldingFunction.apply(in, in2));
        });
    }


    default <R1, R> ImmutableSet<R> forEach2(Function<? super T, ? extends Iterable<R1>> iterable1,
                                              BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                              BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        return this.flatMapI(in-> {

            Iterable<? extends R1> b = iterable1.apply(in);
            return ReactiveSeq.fromIterable(b)
                    .filter(in2-> filterFunction.apply(in,in2))
                    .map(in2->yieldingFunction.apply(in, in2));
        });
    }


    @Override
    default ImmutableSet<T> onEmpty(T value){
        if(size()==0){
            return add(value);
        }
        return this;
    }

    @Override
    default ImmutableSet<T> onEmptyGet(Supplier<? extends T> supplier){
        return onEmpty(supplier.get());
    }

    @Override
    default <X extends Throwable> ImmutableSet<T> onEmptyThrow(Supplier<? extends X> supplier){
        if(size()==0)
            throw ExceptionSoftener.throwSoftenedException(supplier.get());
        return this;
    }

    @Override
    default OnEmptySwitch<ImmutableSet<T>, ImmutableSet<T>> onEmptySwitch(Supplier<? extends ImmutableSet<T>> supplier){
        if(size()==0)
            return supplier.get();
        return this;
    }
}
