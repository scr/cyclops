package com.aol.cyclops.functionaljava.hkt.typeclesses.instances;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import cyclops.companion.functionaljava.NonEmptyLists;
import com.aol.cyclops.functionaljava.hkt.NonEmptyListKind;
import org.junit.Test;

import cyclops.function.Fn1;
import cyclops.function.Lambda;

import fj.data.List;
import fj.data.NonEmptyList;

public class NonEmptyListsTest {

    @Test
    public void unit(){
        
        NonEmptyListKind<String> list = NonEmptyLists.Instances.unit()
                                     .unit("hello")
                                     .convert(NonEmptyListKind::narrowK);
        
        assertThat(list,equalTo(list("hello")));
    }
    @Test
    public void functor(){
        
        NonEmptyListKind<Integer> list = NonEmptyLists.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> NonEmptyLists.Instances.functor().map((String v) ->v.length(), h))
                                     .convert(NonEmptyListKind::narrowK);
        
        assertThat(list,equalTo(NonEmptyList.fromList(List.list("hello".length())).some()));
    }
    @Test
    public void apSimple(){
        NonEmptyLists.Instances.zippingApplicative()
            .ap(NonEmptyListKind.of(Lambda.<Integer,Integer>l1(this::multiplyByTwo)), NonEmptyListKind.of(1,2,3));
    }
    private int multiplyByTwo(int x){
        return x*2;
    }
    @Test
    public void applicative(){
        
        NonEmptyListKind<Fn1<Integer,Integer>> listFn = NonEmptyLists.Instances.unit().unit(Lambda.l1((Integer i) ->i*2)).convert(NonEmptyListKind::narrowK);
        
        NonEmptyListKind<Integer> list = NonEmptyLists.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> NonEmptyLists.Instances.functor().map((String v) ->v.length(), h))
                                     .applyHKT(h-> NonEmptyLists.Instances.zippingApplicative().ap(listFn, h))
                                     .convert(NonEmptyListKind::narrowK);
        
        assertThat(list,equalTo(list("hello".length()*2)));
    }
    private <T> NonEmptyList<T> list(T... values) {
        return NonEmptyList.fromList(List.list(values)).some();
                
    }
    private NonEmptyListKind<Integer> range(int start, int end){
        return NonEmptyListKind.widen(NonEmptyList.fromList(List.range(start,end)).some());
    }
    @Test
    public void monadSimple(){
       NonEmptyListKind<Integer> list  = NonEmptyLists.Instances.monad()
                                      .flatMap(i->range(0,i), NonEmptyListKind.of(1,2,3))
                                      .convert(NonEmptyListKind::narrowK);
    }
    @Test
    public void monad(){
        
        NonEmptyListKind<Integer> list = NonEmptyLists.Instances.unit()
                                     .unit("hello")
                                     .applyHKT(h-> NonEmptyLists.Instances.monad().flatMap((String v) -> NonEmptyLists.Instances.unit().unit(v.length()), h))
                                     .convert(NonEmptyListKind::narrowK);
        
        assertThat(list,equalTo(list("hello".length())));
    }

    @Test
    public void  foldLeft(){
        int sum  = NonEmptyLists.Instances.foldable()
                        .foldLeft(0, (a,b)->a+b, NonEmptyListKind.of(1,2,3,4));
        
        assertThat(sum,equalTo(10));
    }
    @Test
    public void  foldRight(){
        int sum  = NonEmptyLists.Instances.foldable()
                        .foldRight(0, (a,b)->a+b, NonEmptyListKind.of(1,2,3,4));
        
        assertThat(sum,equalTo(10));
    }
    
   
    
}
