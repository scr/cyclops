package com.aol.cyclops.guava.hkt.typeclasses.instances;
import static com.aol.cyclops.guava.hkt.FluentIterableKind.widen;

import static cyclops.function.Lambda.l1;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import cyclops.collections.mutable.ListX;
import cyclops.companion.Monoids;
import cyclops.companion.guava.FluentIterables;
import com.aol.cyclops.guava.hkt.FluentIterableKind;
import com.aol.cyclops2.hkt.Higher;
import cyclops.control.Maybe;
import cyclops.function.Fn1;
import cyclops.function.Lambda;
import cyclops.monads.GuavaWitness.fluentIterable;
import cyclops.stream.ReactiveSeq;
import org.junit.Test;

import com.google.common.collect.FluentIterable;

public class FluentIterablesTest {

    @Test
    public void optional(){
        Higher<fluentIterable, Integer> res = FluentIterables.FluentIterableNested
                .optional(FluentIterable.of(com.google.common.base.Optional.of(1)))
                .map(i -> i * 20)
                .foldLeft(Monoids.intMax);
        FluentIterable<Integer> fi = FluentIterableKind.narrow(res);
        assertThat(fi.get(0),equalTo(20));
    }
    @Test
    public void nestedOptionalJava(){
        Higher<fluentIterable, Integer> res = FluentIterables.FluentIterableNested
                                                            .javaOptional(FluentIterable.of(Optional.of(1)))
                                                            .map(i -> i * 20)
                                                            .foldLeft(Monoids.intMax);
        FluentIterable<Integer> fi = FluentIterableKind.narrow(res);
        assertThat(fi.get(0),equalTo(20));
    }
    @Test
    public void nestedStream(){
        Higher<fluentIterable, Integer> res = FluentIterables.FluentIterableNested
                .javaStream(FluentIterable.of(Stream.of(1)))
                .map(i -> i * 20)
                .foldLeft(Monoids.intMax);
        FluentIterable<Integer> fi = FluentIterableKind.narrow(res);
        assertThat(fi.get(0),equalTo(20));
    }
    @Test
    public void nestedCompletableFuture(){
        Higher<fluentIterable, Integer> res = FluentIterables.FluentIterableNested
                .javaCompletableFuture(FluentIterable.of(CompletableFuture.completedFuture(1)))
                .map(i -> i * 20)
                .foldLeft(Monoids.intMax);
        FluentIterable<Integer> fi = FluentIterableKind.narrow(res);
        assertThat(fi.get(0),equalTo(20));
    }

    @Test
    public void unit(){
        
        FluentIterableKind<String> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList("hello")));
    }
    @Test
    public void functor(){
        
        FluentIterableKind<Integer> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> FluentIterables.Instances.functor().map((String v) ->v.length(), h))
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList("hello".length())));
    }
    @Test
    public void apSimple(){
        FluentIterables.Instances.zippingApplicative()
            .ap(widen(FluentIterable.of(l1(this::multiplyByTwo))),widen(FluentIterable.of(1,2,3)));
    }
    private int multiplyByTwo(int x){
        return x*2;
    }
    @Test
    public void applicative(){
        
        FluentIterableKind<Fn1<Integer,Integer>> listFn = FluentIterables.Instances.unit().unit(Lambda.l1((Integer i) ->i*2)).convert(FluentIterableKind::narrowK);
        
        FluentIterableKind<Integer> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> FluentIterables.Instances.functor().map((String v) ->v.length(), h))
                                     .applyHKT(h-> FluentIterables.Instances.zippingApplicative().ap(listFn, h))
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList("hello".length()*2)));
    }
    @Test
    public void monadSimple(){
       FluentIterableKind<Integer> list  = FluentIterables.Instances.monad()
                                      .flatMap(i->widen(ReactiveSeq.range(0,i)), widen(FluentIterable.of(1,2,3)))
                                      .convert(FluentIterableKind::narrowK);
    }
    @Test
    public void monad(){
        
        FluentIterableKind<Integer> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> FluentIterables.Instances.monad().flatMap((String v) -> FluentIterables.Instances.unit().unit(v.length()), h))
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList("hello".length())));
    }
    @Test
    public void monadZeroFilter(){
        
        FluentIterableKind<String> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> FluentIterables.Instances.monadZero().filter((String t)->t.startsWith("he"), h))
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList("hello")));
    }
    @Test
    public void monadZeroFilterOut(){
        
        FluentIterableKind<String> list = FluentIterables.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> FluentIterables.Instances.monadZero().filter((String t)->!t.startsWith("he"), h))
                                     .convert(FluentIterableKind::narrowK);
        
        assertThat(list.toList(),equalTo(Arrays.asList()));
    }
    
    @Test
    public void monadPlus(){
        FluentIterableKind<Integer> list = FluentIterables.Instances.<Integer>monadPlus()
                                      .plus(FluentIterableKind.widen(FluentIterable.of()), FluentIterableKind.widen(FluentIterable.of(10)))
                                      .convert(FluentIterableKind::narrowK);
        assertThat(list.toList(),equalTo(Arrays.asList(10)));
    }

    @Test
    public void  foldLeft(){
        int sum  = FluentIterables.Instances.foldable()
                        .foldLeft(0, (a,b)->a+b, FluentIterableKind.widen(FluentIterable.of(1,2,3,4)));
        
        assertThat(sum,equalTo(10));
    }
    @Test
    public void  foldRight(){
        int sum  = FluentIterables.Instances.foldable()
                        .foldRight(0, (a,b)->a+b, FluentIterableKind.widen(FluentIterable.of(1,2,3,4)));
        
        assertThat(sum,equalTo(10));
    }
    @Test
    public void traverse(){
       Maybe<Higher<fluentIterable, Integer>> res = FluentIterables.Instances.traverse()
                                                         .traverseA(Maybe.Instances.applicative(), (Integer a)->Maybe.just(a*2), FluentIterableKind.just(1,2,3))
                                                         .convert(Maybe::narrowK);
       
       
       assertThat(res.map(i->i.convert(FluentIterableKind::narrowK).toList()),
                  equalTo(Maybe.just(ListX.of(2,4,6))));
    }
    
}
