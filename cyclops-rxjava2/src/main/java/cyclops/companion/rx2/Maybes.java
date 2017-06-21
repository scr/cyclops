package cyclops.companion.rx2;


import com.aol.cyclops.rx2.hkt.MaybeKind;
import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.react.Status;
import com.aol.cyclops2.types.MonadicValue;
import com.aol.cyclops2.types.Value;
import com.aol.cyclops2.types.anyM.AnyMValue;
import cyclops.async.Future;
import cyclops.collections.mutable.ListX;
import cyclops.control.Eval;

import cyclops.control.lazy.Either;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.AnyM;
import cyclops.monads.Rx2Witness;
import cyclops.monads.Rx2Witness.maybe;
import cyclops.monads.WitnessType;
import cyclops.monads.transformers.rx2.MaybeT;
import cyclops.typeclasses.Pure;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Companion class for working with Reactor Maybe types
 * 
 * @author johnmcclean
 *
 */
@UtilityClass
public class Maybes {
    public static <T> Maybe<T> fromPublisher(Publisher<T> maybe){
        return Single.fromPublisher(maybe).toMaybe();
    }

    public static <T> Maybe<T> raw(AnyM<maybe,T> anyM){
        return Rx2Witness.maybe(anyM);
    }


    public static <W extends WitnessType<W>,T> MaybeT<W,T> liftM(AnyM<W,Maybe<T>> nested){
        return MaybeT.of(nested);
    }

    public static <T> Future[] futures(Maybe<T>... futures){

        Future[] array = new Future[futures.length];
        for(int i=0;i<array.length;i++){
            array[i]=future(futures[i]);
        }
        return array;
    }
    public static <T> cyclops.control.Maybe<T> toMaybe(Maybe<T> future){
        return cyclops.control.Maybe.fromPublisher(future.toFlowable());
    }
    public static <T> Maybe<T> fromMaybe(cyclops.control.Maybe<T> future){
        return Single.fromPublisher(future).toMaybe();

    }
    public static <T> Maybe<T> fromValue(MonadicValue<T> future){
        return Single.fromPublisher(future).toMaybe();

    }
    public static <T> Future<T> future(Maybe<T> future){
        return Future.fromPublisher(future.toFlowable());
    }

    public static <R> Either<Throwable,R> either(Maybe<R> either){
        return Either.fromFuture(future(either));

    }

    public static <T> cyclops.control.Maybe<T> maybe(Maybe<T> opt){
        return cyclops.control.Maybe.fromFuture(future(opt));
    }
    public static <T> Eval<T> eval(Maybe<T> opt){
        return Eval.fromFuture(future(opt));
    }
    
    /**
     * Construct an AnyM type from a Maybe. This allows the Maybe to be manipulated according to a standard interface
     * along with a vast array of other Java Monad implementations
     * 
     * <pre>
     * {@code 
     *    
     *    AnyMSeq<Integer> maybe = Fluxs.anyM(Maybe.just(1,2,3));
     *    AnyMSeq<Integer> transformedMaybe = myGenericOperation(maybe);
     *    
     *    public AnyMSeq<Integer> myGenericOperation(AnyMSeq<Integer> monad);
     * }
     * </pre>
     * 
     * @param maybe To wrap inside an AnyM
     * @return AnyMSeq wrapping a Maybe
     */
    public static <T> AnyMValue<maybe,T> anyM(Maybe<T> maybe) {
        return AnyM.ofValue(maybe, Rx2Witness.maybe.INSTANCE);
    }



    /**
     * Select the first Maybe to complete
     *
     * @see CompletableFuture#anyOf(CompletableFuture...)
     * @param fts Maybes to race
     * @return First Maybe to complete
     */
    public static <T> Maybe<T> anyOf(Maybe<T>... fts) {


        return Single.fromPublisher(Future.anyOf(futures(fts))).toMaybe();

    }
    /**
     * Wait until all the provided Future's to complete
     *
     * @see CompletableFuture#allOf(CompletableFuture...)
     *
     * @param fts Maybes to  wait on
     * @return Maybe that completes when all the provided Futures Complete. Empty Future result, or holds an Exception
     *         from a provided Future that failed.
     */
    public static <T> Maybe<T> allOf(Maybe<T>... fts) {

        return Single.fromPublisher(Future.allOf(futures(fts))).toMaybe();
    }
    /**
     * Block until a Quorum of results have returned as determined by the provided Predicate
     *
     * <pre>
     * {@code
     *
     * Maybe<ListX<Integer>> strings = Maybes.quorum(status -> status.getCompleted() >0, Maybe.deferred(()->1),Maybe.empty(),Maybe.empty());


    strings.get().size()
    //1
     *
     * }
     * </pre>
     *
     *
     * @param breakout Predicate that determines whether the block should be
     *            continued or removed
     * @param fts FutureWs to  wait on results from
     * @param errorHandler Consumer to handle any exceptions thrown
     * @return Future which will be populated with a Quorum of results
     */
    @SafeVarargs
    public static <T> Maybe<ListX<T>> quorum(Predicate<Status<T>> breakout, Consumer<Throwable> errorHandler, Maybe<T>... fts) {

        return Single.fromPublisher(Future.quorum(breakout,errorHandler,futures(fts))).toMaybe();


    }
    /**
     * Block until a Quorum of results have returned as determined by the provided Predicate
     *
     * <pre>
     * {@code
     *
     * Maybe<ListX<Integer>> strings = Maybes.quorum(status -> status.getCompleted() >0, Maybe.deferred(()->1),Maybe.empty(),Maybe.empty());


    strings.get().size()
    //1
     *
     * }
     * </pre>
     *
     *
     * @param breakout Predicate that determines whether the block should be
     *            continued or removed
     * @param fts Maybes to  wait on results from
     * @return Maybe which will be populated with a Quorum of results
     */
    @SafeVarargs
    public static <T> Maybe<ListX<T>> quorum(Predicate<Status<T>> breakout, Maybe<T>... fts) {

        return Single.fromPublisher(Future.quorum(breakout,futures(fts))).toMaybe();


    }
    /**
     * Select the first Future to return with a successful result
     *
     * <pre>
     * {@code
     * Maybe<Integer> ft = Maybe.empty();
      Maybe<Integer> result = Maybes.firstSuccess(Maybe.deferred(()->1),ft);

    ft.complete(10);
    result.get() //1
     * }
     * </pre>
     *
     * @param fts Maybes to race
     * @return First Maybe to return with a result
     */
    @SafeVarargs
    public static <T> Maybe<T> firstSuccess(Maybe<T>... fts) {
        return Single.fromPublisher(Future.firstSuccess(futures(fts))).toMaybe();

    }

    /**
     * Perform a For Comprehension over a Maybe, accepting 3 generating functions. 
     * This results in a four level nested internal iteration over the provided Maybes.
     * 
     *  <pre>
     * {@code
     *    
     *   import static cyclops.companion.reactor.Maybes.forEach4;
     *    
          forEach4(Maybe.just(1), 
                  a-> Maybe.just(a+1),
                  (a,b) -> Maybe.<Integer>just(a+b),
                  (a,b,c) -> Maybe.<Integer>just(a+b+c),
                  Tuple::tuple)
     * 
     * }
     * </pre>
     * 
     * @param value1 top level Maybe
     * @param value2 Nested Maybe
     * @param value3 Nested Maybe
     * @param value4 Nested Maybe
     * @param yieldingFunction Generates a result per combination
     * @return Maybe with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Maybe<R> forEach4(Maybe<? extends T1> value1,
            Function<? super T1, ? extends Maybe<R1>> value2,
            BiFunction<? super T1, ? super R1, ? extends Maybe<R2>> value3,
            Fn3<? super T1, ? super R1, ? super R2, ? extends Maybe<R3>> value4,
            Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        Maybe<? extends R> res = value1.flatMap(in -> {

            Maybe<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Maybe<R2> b = value3.apply(in, ina);
                return b.flatMap(inb -> {
                    Maybe<R3> c = value4.apply(in, ina, inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
        return  narrow(res);
    }


    /**
     * Perform a For Comprehension over a Maybe, accepting 2 generating functions. 
     * This results in a three level nested internal iteration over the provided Maybes.
     * 
     *  <pre>
     * {@code
     *    
     *   import static cyclops.companion.reactor.Maybes.forEach3;
     *    
          forEach3(Maybe.just(1), 
                  a-> Maybe.just(a+1),
                  (a,b) -> Maybe.<Integer>just(a+b),
                  Tuple::tuple)
     * 
     * }
     * </pre>
     * 
     * @param value1 top level Maybe
     * @param value2 Nested Maybe
     * @param value3 Nested Maybe
     * @param yieldingFunction Generates a result per combination
     * @return Maybe with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Maybe<R> forEach3(Maybe<? extends T1> value1,
            Function<? super T1, ? extends Maybe<R1>> value2,
            BiFunction<? super T1, ? super R1, ? extends Maybe<R2>> value3,
            Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {


        Maybe<? extends R> res = value1.flatMap(in -> {

            Maybe<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Maybe<R2> b = value3.apply(in, ina);


                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));


            });

        });
        return narrow(res);

    }



    /**
     * Perform a For Comprehension over a Maybe, accepting a generating function. 
     * This results in a two level nested internal iteration over the provided Maybes.
     * 
     *  <pre>
     * {@code
     *    
     *   import static cyclops.companion.reactor.Maybes.forEach;
     *    
          forEach(Maybe.just(1), 
                  a-> Maybe.just(a+1),
                  Tuple::tuple)
     * 
     * }
     * </pre>
     * 
     * @param value1 top level Maybe
     * @param value2 Nested Maybe
     * @param yieldingFunction Generates a result per combination
     * @return Maybe with a combined value generated by the yielding function
     */
    public static <T, R1, R> Maybe<R> forEach(Maybe<? extends T> value1,
                                             Function<? super T, Maybe<R1>> value2,
                                             BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        Maybe<R> res = value1.flatMap(in -> {

            Maybe<R1> a = value2.apply(in);
            return a.map(ina -> yieldingFunction.apply(in, ina));


        });


        return narrow(res);

    }



    /**
     * Lazily combine this Maybe with the supplied value via the supplied BiFunction
     * 
     * @param maybe Maybe to combine with another value
     * @param app Value to combine with supplied maybe
     * @param fn Combiner function
     * @return Combined Maybe
     */
    public static <T1, T2, R> Maybe<R> combine(Maybe<? extends T1> maybe, Value<? extends T2> app,
            BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Single.fromPublisher(Future.fromPublisher(maybe.toFlowable())
                                .combine(app, fn)).toMaybe());
    }

    /**
     * Lazily combine this Maybe with the supplied Maybe via the supplied BiFunction
     * 
     * @param maybe Maybe to combine with another value
     * @param app Maybe to combine with supplied maybe
     * @param fn Combiner function
     * @return Combined Maybe
     */
    public static <T1, T2, R> Maybe<R> combine(Maybe<? extends T1> maybe, Maybe<? extends T2> app,
            BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Single.fromPublisher(Future.fromPublisher(maybe.toFlowable())
                                .combine(Future.fromPublisher(app.toFlowable()), fn)).toMaybe());
    }

    /**
     * Combine the provided Maybe with the first element (if present) in the provided Iterable using the provided BiFunction
     * 
     * @param maybe Maybe to combine with an Iterable
     * @param app Iterable to combine with a Maybe
     * @param fn Combining function
     * @return Combined Maybe
     */
    public static <T1, T2, R> Maybe<R> zip(Maybe<? extends T1> maybe, Iterable<? extends T2> app,
            BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(Single.fromPublisher(Future.fromPublisher(maybe.toFlowable())
                                .zip(app, fn)).toMaybe());
    }

    /**
     * Combine the provided Maybe with the first element (if present) in the provided Publisher using the provided BiFunction
     * 
     * @param maybe  Maybe to combine with a Publisher
     * @param fn Publisher to combine with a Maybe
     * @param app Combining function
     * @return Combined Maybe
     */
    public static <T1, T2, R> Maybe<R> zip(Maybe<? extends T1> maybe, BiFunction<? super T1, ? super T2, ? extends R> fn,
            Publisher<? extends T2> app) {
        Maybe<R> res = narrow(Single.fromPublisher(Future.fromPublisher(maybe.toFlowable()).zipP(app, fn)).toMaybe());
        return res;
    }

    /**
     * Test if value is equal to the value inside this Maybe
     * 
     * @param maybe Maybe to test
     * @param test Value to test
     * @return true if equal
     */
    public static <T> boolean test(Maybe<T> maybe, T test) {
        return Future.fromPublisher(maybe.toFlowable())
                      .test(test);
    }

    /**
     * Construct a Maybe from Iterable by taking the first value from Iterable
     * 
     * @param t Iterable to populate Maybe from
     * @return Maybe containing first element from Iterable (or empty Maybe)
     */
    public static <T> Maybe<T> fromIterable(Iterable<T> t) {
        return narrow(Single.fromPublisher(Future.fromIterable(t)).toMaybe());
    }

    /**
     * Get an Iterator for the value (if any) in the provided Maybe
     * 
     * @param pub Maybe to get Iterator for
     * @return Iterator over Maybe value
     */
    public static <T> Iterator<T> iterator(Maybe<T> pub) {
        return pub.toFlowable().blockingIterable().iterator();

    }

    public static <R> Maybe<R> narrow(Maybe<? extends R> apply) {
        return (Maybe<R>)apply;
    }

    /**
     * Companion class for creating Type Class instances for working with Maybes
     * @author johnmcclean
     *
     */
    @UtilityClass
    public static class Instances {


        /**
         *
         * Transform a Maybe, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  MaybeKind<Integer> future = Maybes.functor().map(i->i*2, MaybeKind.widen(Maybe.just(3));
         *
         *  //[6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Maybes
         * <pre>
         * {@code
         *   MaybeKind<Integer> ft = Maybes.unit()
        .unit("hello")
        .then(h->Maybes.functor().map((String v) ->v.length(), h))
        .convert(MaybeKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Maybes
         */
        public static <T,R>Functor<MaybeKind.µ> functor(){
            BiFunction<MaybeKind<T>,Function<? super T, ? extends R>,MaybeKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * MaybeKind<String> ft = Maybes.unit()
        .unit("hello")
        .convert(MaybeKind::narrowK);

        //Maybe["hello"]
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Maybes
         */
        public static <T> Pure<MaybeKind.µ> unit(){
            return General.<MaybeKind.µ,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.MaybeKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Arrays.asMaybe;
         *
        Maybes.applicative()
        .ap(widen(asMaybe(l1(this::multiplyByTwo))),widen(Maybe.just(3)));
         *
         * //[6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * MaybeKind<Function<Integer,Integer>> ftFn =Maybes.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(MaybeKind::narrowK);

        MaybeKind<Integer> ft = Maybes.unit()
        .unit("hello")
        .then(h->Maybes.functor().map((String v) ->v.length(), h))
        .then(h->Maybes.applicative().ap(ftFn, h))
        .convert(MaybeKind::narrowK);

        //Maybe.just("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Maybes
         */
        public static <T,R> Applicative<MaybeKind.µ> applicative(){
            BiFunction<MaybeKind< Function<T, R>>,MaybeKind<T>,MaybeKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.MaybeKind.widen;
         * MaybeKind<Integer> ft  = Maybes.monad()
        .flatMap(i->widen(Maybe.just(i)), widen(Maybe.just(3)))
        .convert(MaybeKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    MaybeKind<Integer> ft = Maybes.unit()
        .unit("hello")
        .then(h->Maybes.monad().flatMap((String v) ->Maybes.unit().unit(v.length()), h))
        .convert(MaybeKind::narrowK);

        //Maybe.just("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Maybes
         */
        public static <T,R> Monad<MaybeKind.µ> monad(){

            BiFunction<Higher<MaybeKind.µ,T>,Function<? super T, ? extends Higher<MaybeKind.µ,R>>,Higher<MaybeKind.µ,R>> flatMap = Instances::flatMap;
            return General.monad(applicative(), flatMap);
        }
        /**
         *
         * <pre>
         * {@code
         *  MaybeKind<String> ft = Maybes.unit()
        .unit("hello")
        .then(h->Maybes.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(MaybeKind::narrowK);

        //Maybe.just("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<MaybeKind.µ> monadZero(){

            return General.monadZero(monad(), MaybeKind.empty());
        }
        /**
         * Combines Maybes by selecting the first result returned
         *
         * <pre>
         * {@code
         *  MaybeKind<Integer> ft = Maybes.<Integer>monadPlus()
        .plus(MaybeKind.widen(Maybe.empty()), MaybeKind.widen(Maybe.just(10)))
        .convert(MaybeKind::narrowK);
        //Maybe.empty()
         *
         * }
         * </pre>
         * @return Type class for combining Maybes by concatenation
         */
        public static <T> MonadPlus<MaybeKind.µ> monadPlus(){


            Monoid<MaybeKind<T>> m = Monoid.of(MaybeKind.<T>widen(Maybe.empty()),
                    (f,g)-> MaybeKind.widen(Maybe.ambArray(f.narrow(),g.narrow())));

            Monoid<Higher<MaybeKind.µ,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<MaybeKind<Integer>> m = Monoid.of(MaybeKind.widen(Arrays.asMaybe()), (a,b)->a.isEmpty() ? b : a);
        MaybeKind<Integer> ft = Maybes.<Integer>monadPlus(m)
        .plus(MaybeKind.widen(Arrays.asMaybe(5)), MaybeKind.widen(Arrays.asMaybe(10)))
        .convert(MaybeKind::narrowK);
        //Arrays.asMaybe(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining Maybes
         * @return Type class for combining Maybes
         */
        public static <T> MonadPlus<MaybeKind.µ> monadPlus(Monoid<MaybeKind<T>> m){
            Monoid<Higher<MaybeKind.µ,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<MaybeKind.µ> traverse(){

            return General.traverseByTraverse(applicative(), Instances::traverseA);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Maybes.foldable()
        .foldLeft(0, (a,b)->a+b, MaybeKind.widen(Arrays.asMaybe(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<MaybeKind.µ> foldable(){
            BiFunction<Monoid<T>,Higher<MaybeKind.µ,T>,T> foldRightFn =  (m, l)-> m.apply(m.zero(), MaybeKind.narrow(l).blockingGet());
            BiFunction<Monoid<T>,Higher<MaybeKind.µ,T>,T> foldLeftFn = (m, l)->  m.apply(m.zero(), MaybeKind.narrow(l).blockingGet());
            return General.foldable(foldRightFn, foldLeftFn);
        }
        public static <T> Comonad<MaybeKind.µ> comonad(){
            Function<? super Higher<MaybeKind.µ, T>, ? extends T> extractFn = maybe -> maybe.convert(MaybeKind::narrow).blockingGet();
            return General.comonad(functor(), unit(), extractFn);
        }

        private static <T> MaybeKind<T> of(T value){
            return MaybeKind.widen(Maybe.just(value));
        }
        private static <T,R> MaybeKind<R> ap(MaybeKind<Function< T, R>> lt, MaybeKind<T> list){


            return MaybeKind.widen(Maybes.combine(lt.narrow(),list.narrow(), (a, b)->a.apply(b)));

        }
        private static <T,R> Higher<MaybeKind.µ,R> flatMap(Higher<MaybeKind.µ,T> lt, Function<? super T, ? extends  Higher<MaybeKind.µ,R>> fn){
            return MaybeKind.widen(MaybeKind.narrow(lt).flatMap(Functions.rxFunction(fn.andThen(MaybeKind::narrow))));
        }
        private static <T,R> MaybeKind<R> map(MaybeKind<T> lt, Function<? super T, ? extends R> fn){
            return MaybeKind.widen(lt.narrow().map(Functions.rxFunction(fn)));
        }


        private static <C2,T,R> Higher<C2, Higher<MaybeKind.µ, R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn,
                                                                            Higher<MaybeKind.µ, T> ds){
            Maybe<T> future = MaybeKind.narrow(ds);
            return applicative.map(MaybeKind::just, fn.apply(future.blockingGet()));
        }

    }

}