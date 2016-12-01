package com.aol.cyclops.scala.collections.extension;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jooq.lambda.tuple.Tuple2;
import org.junit.Test;

import com.aol.cyclops.data.collections.extensions.FluentCollectionX;
import com.aol.cyclops.data.collections.extensions.persistent.PBagX;
import com.aol.cyclops.data.collections.extensions.persistent.PVectorX;
import com.aol.cyclops.reactor.collections.extensions.AbstractOrderDependentCollectionXTest;
import com.aol.cyclops.reactor.collections.extensions.base.LazyFluentCollectionX;
import com.aol.cyclops.reactor.collections.extensions.persistent.LazyPVectorX;
import com.aol.cyclops.scala.collections.ScalaPVector;

import reactor.core.publisher.Flux;

public class LazyPVectorXTest extends AbstractOrderDependentCollectionXTest  {

    @Override
    public <T> LazyFluentCollectionX<T> of(T... values) {
        LazyPVectorX<T> list = ScalaPVector.empty();
        for (T next : values) {
            list = list.plus(list.size(), next);
        }
        System.out.println("List " + list);
        return list;

    }

    @Test
    public void onEmptySwitch() {
        assertThat(ScalaPVector.empty()
                          .onEmptySwitch(() -> LazyPVectorX.of(1, 2, 3)),
                   equalTo(PVectorX.of(1, 2, 3)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.functions.collections.extensions.AbstractCollectionXTest#
     * empty()
     */
    @Override
    public <T> FluentCollectionX<T> empty() {
        return ScalaPVector.empty();
    }

    

    @Test
    public void remove() {

        ScalaPVector.of(1, 2, 3)
               .minusAll(PBagX.of(2, 3))
               .flatMapPublisher(i -> Flux.just(10 + i, 20 + i, 30 + i));

    }

    @Override
    public FluentCollectionX<Integer> range(int start, int end) {
        return ScalaPVector.range(start, end);
    }

    @Override
    public FluentCollectionX<Long> rangeLong(long start, long end) {
        return ScalaPVector.rangeLong(start, end);
    }

    @Override
    public <T> FluentCollectionX<T> iterate(int times, T seed, UnaryOperator<T> fn) {
        return ScalaPVector.iterate(times, seed, fn);
    }

    @Override
    public <T> FluentCollectionX<T> generate(int times, Supplier<T> fn) {
        return ScalaPVector.generate(times, fn);
    }

    @Override
    public <U, T> FluentCollectionX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return ScalaPVector.unfold(seed, unfolder);
    }
}