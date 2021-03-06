package com.aol.cyclops.functionaljava.adapter;

import cyclops.monads.FJWitness;
import cyclops.monads.FJWitness.list;
import com.aol.cyclops2.types.extensability.AbstractFunctionalAdapter;
import cyclops.monads.AnyM;
import cyclops.stream.ReactiveSeq;
import fj.data.List;

import java.util.function.Function;
import java.util.function.Predicate;



public class ListAdapter extends AbstractFunctionalAdapter<list> {



    @Override
    public <T> Iterable<T> toIterable(AnyM<list, T> t) {
        return ()->stream(t).iterator();
    }

    @Override
    public <T, R> AnyM<list, R> ap(AnyM<list,? extends Function<? super T,? extends R>> fn, AnyM<list, T> apply) {
        List<T> f = stream(apply);
        List<? extends Function<? super T, ? extends R>> fnF = stream(fn);
        return unitIterable(ReactiveSeq.fromIterable(fnF).zip(f, (a, b) -> a.apply(b)));

    }

    @Override
    public <T> AnyM<list, T> filter(AnyM<list, T> t, Predicate<? super T> fn) {
        return anyM(stream(t).filter(a->fn.test(a)));
    }

    <T> List<T> stream(AnyM<list,T> anyM){
        return anyM.unwrap();
    }

    @Override
    public <T> AnyM<list, T> empty() {
        return anyM(List.nil());
    }

    private <T> AnyM<list,T> anyM(List<T> t){
        return AnyM.ofSeq(t, list.INSTANCE);
    }



    @Override
    public <T, R> AnyM<list, R> flatMap(AnyM<list, T> t,
                                     Function<? super T, ? extends AnyM<list, ? extends R>> fn) {
        return anyM(stream(t).bind(a->stream((AnyM<list,R>)fn.apply(a))));

    }

    @Override
    public <T> AnyM<list, T> unitIterable(Iterable<T> it)  {
        return anyM(List.fromIterator(it.iterator()));
    }

    @Override
    public <T> AnyM<list, T> unit(T o) {
        return anyM(List.list(o));
    }


    @Override
    public <T, R> AnyM<list, R> map(AnyM<list, T> t, Function<? super T, ? extends R> fn) {
        return anyM(stream(t).map(x->fn.apply(x)));
    }
}
