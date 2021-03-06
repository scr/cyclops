package cyclops.companion.vavr;

import cyclops.monads.VavrWitness.*;
import cyclops.monads.VavrWitness.list;
import cyclops.monads.VavrWitness.stream;
import cyclops.monads.VavrWitness.tryType;
import io.vavr.collection.*;
import io.vavr.concurrent.Future;
import io.vavr.control.*;
import com.aol.cyclops.vavr.hkt.*;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Optionals;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Reader;
import cyclops.control.Xor;
import cyclops.conversion.vavr.FromCyclopsReact;
import cyclops.monads.*;
import com.aol.cyclops2.hkt.Higher;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.Witness.*;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.*;

import com.aol.cyclops.vavr.hkt.EitherKind;
import cyclops.conversion.vavr.ToCyclopsReact;
import com.aol.cyclops.vavr.hkt.LazyKind;
import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.types.Value;
import cyclops.collections.mutable.ListX;
import cyclops.function.Reducer;
import cyclops.monads.transformers.EvalT;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.Lazy;
import io.vavr.control.Either;
import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


import static com.aol.cyclops.vavr.hkt.LazyKind.narrowK;
import static com.aol.cyclops.vavr.hkt.LazyKind.widen;

public class Lazys {

    public static  <W1,T> Coproduct<W1,lazy,T> coproduct(Lazy<T> type, InstanceDefinitions<W1> def1){
        return Coproduct.of(Xor.primary(widen(type)),def1, Instances.definitions());
    }
    public static  <W1,T> Coproduct<W1,lazy,T> coproduct(Supplier<T> type, InstanceDefinitions<W1> def1){
        return coproduct(Lazy.of(type),def1);
    }

    public static <L, T, R> Lazy<R> tailRec(T initial, Function<? super T, ? extends Lazy<? extends Either<T, R>>> fn) {
        Lazy<? extends Either<T, R>> next[] = new Lazy[1];
        next[0] = Lazy.of(()->Either.left(initial));
        boolean cont = true;
        do {
            cont = next[0].map(p -> p.fold(s -> {
                next[0] = fn.apply(s);
                return true;
            }, pr -> false)).getOrElse(false);
        } while (cont);
        return next[0].map(Either::get);
    }
    public static <T, R> Lazy< R> tailRecXor(T initial, Function<? super T, ? extends Lazy<? extends Xor<T, R>>> fn) {
        Lazy<? extends Xor<T, R>> next[] = new Lazy[1];
        next[0] = Lazy.of(()->Xor.secondary(initial));
        boolean cont = true;
        do {
            cont = next[0].map(p -> p.visit(s -> {
                next[0] = fn.apply(s);
                return true;
            }, pr -> false)).getOrElse(false);
        } while (cont);
        return next[0].map(Xor::get);
    }


    /**
     * Lifts a vavr Lazy into a cyclops LazyT monad transformer (involves an observables conversion to
     * cyclops Lazy types)
     *
     */
    public static <T,W extends WitnessType<W>> EvalT<W, T> liftM(Lazy<T> opt, W witness) {
        return EvalT.of(witness.adapter().unit(ToCyclopsReact.eval(opt)));
    }
    /**
     * Perform a For Comprehension over a Lazy, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Lazys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Lazys.forEach4;
     *
    forEach4(Lazy.just(1),
    a-> Lazy.just(a+1),
    (a,b) -> Lazy.<Integer>just(a+b),
    a                  (a,b,c) -> Lazy.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Lazy
     * @param value2 Nested Lazy
     * @param value3 Nested Lazy
     * @param value4 Nested Lazy
     * @param yieldingFunction Generates a result per combination
     * @return Lazy with a combined value generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Lazy<R> forEach4(Lazy<? extends T1> value1,
                                                               Function<? super T1, ? extends Lazy<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends Lazy<R2>> value3,
                                                               Fn3<? super T1, ? super R1, ? super R2, ? extends Lazy<R3>> value4,
                                                               Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {

        Eval<R> res = ToCyclopsReact.eval(value1).flatMap(in -> {

            Lazy<R1> a = value2.apply(in);
            return ToCyclopsReact.eval(a).flatMap(ina -> {
                Lazy<R2> b = value3.apply(in, ina);
                return ToCyclopsReact.eval(b).flatMap(inb -> {
                    Lazy<R3> c = value4.apply(in, ina, inb);
                    return ToCyclopsReact.eval(c).map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
        return FromCyclopsReact.eval(res);
    }



    /**
     * Perform a For Comprehension over a Lazy, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Lazys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Lazys.forEach3;
     *
    forEach3(Lazy.just(1),
    a-> Lazy.just(a+1),
    (a,b) -> Lazy.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Lazy
     * @param value2 Nested Lazy
     * @param value3 Nested Lazy
     * @param yieldingFunction Generates a result per combination
     * @return Lazy with a combined value generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Lazy<R> forEach3(Lazy<? extends T1> value1,
                                                       Function<? super T1, ? extends Lazy<R1>> value2,
                                                       BiFunction<? super T1, ? super R1, ? extends Lazy<R2>> value3,
                                                       Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        Eval<? extends R> res = ToCyclopsReact.eval(value1).flatMap(in -> {

            Lazy<R1> a = value2.apply(in);
            return ToCyclopsReact.eval(a).flatMap(ina -> {
                Lazy<R2> b = value3.apply(in, ina);
                return ToCyclopsReact.eval(b).map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });

        return FromCyclopsReact.eval(Eval.narrow(res));

    }


    /**
     * Perform a For Comprehension over a Lazy, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Lazys.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Lazys.forEach;
     *
    forEach(Lazy.just(1),
    a-> Lazy.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Lazy
     * @param value2 Nested Lazy
     * @param yieldingFunction Generates a result per combination
     * @return Lazy with a combined value generated by the yielding function
     */
    public static <T, R1, R> Lazy<R> forEach2(Lazy<? extends T> value1, Function<? super T, Lazy<R1>> value2,
                                              BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {

        Eval<? extends R> res = ToCyclopsReact.eval(value1).flatMap(in -> {

            Lazy<R1> a = value2.apply(in);
            return ToCyclopsReact.eval(a).map(in2 -> yieldingFunction.apply(in, in2));
        });


        return FromCyclopsReact.eval(Eval.narrow(res));
    }




    /**
     * Sequence operation, take a Collection of Lazys and turn it into a Lazy with a Collection
     * By constrast with {@link Lazys#sequencePresent(CollectionX)}, if any Lazys are empty the result
     * is an empty Lazy
     *
     * <pre>
     * {@code
     *
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();
     *
     *  Lazy<ListX<Integer>> opts = Lazys.sequence(ListX.of(just, none, Lazy.of(1)));
    //Lazy.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Maybe with a List of values
     */
    public static <T> Lazy<ListX<T>> sequence(final CollectionX<Lazy<T>> opts) {
        return sequence(opts.stream()).map(s -> s.toListX());

    }
    /**
     * Sequence operation, take a Collection of Lazys and turn it into a Lazy with a Collection
     * Only successes are retained. By constrast with {@link Lazys#sequence(CollectionX)} Lazy#empty types are
     * tolerated and ignored.
     *
     * <pre>
     * {@code
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();
     *
     * Lazy<ListX<Integer>> maybes = Lazys.sequencePresent(ListX.of(just, none, Lazy.of(1)));
    //Lazy.of(ListX.of(10, 1));
     * }
     * </pre>
     *
     * @param opts Lazys to Sequence
     * @return Lazy with a List of values
     */
    public static <T> Lazy<ListX<T>> sequencePresent(final CollectionX<Lazy<T>> opts) {
        return sequence(opts.stream().filter(Lazy::isEvaluated)).map(s->s.toListX());
    }
    /**
     * Sequence operation, take a Collection of Lazys and turn it into a Lazy with a Collection
     * By constrast with {@link Lazys#sequencePresent(CollectionX)} if any Lazy types are empty
     * the return type will be an empty Lazy
     *
     * <pre>
     * {@code
     *
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();
     *
     *  Lazy<ListX<Integer>> maybes = Lazys.sequence(ListX.of(just, none, Lazy.of(1)));
    //Lazy.empty();
     *
     * }
     * </pre>
     *
     *
     * @param opts Maybes to Sequence
     * @return  Lazy with a List of values
     */
    public static <T> Lazy<ReactiveSeq<T>> sequence(final java.util.stream.Stream<Lazy<T>> opts) {
        return FromCyclopsReact.eval(AnyM.sequence(opts.map(ToCyclopsReact::eval).map(AnyM::fromEval), Witness.eval.INSTANCE)
                .map(ReactiveSeq::fromStream)
                .to(Witness::eval));

    }
    /**
     * Accummulating operation using the supplied Reducer (@see cyclops2.Reducers). A typical use case is to accumulate into a Persistent Collection type.
     * Accumulates the present results, ignores empty Lazys.
     *
     * <pre>
     * {@code
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();

     * Lazy<PersistentSetX<Integer>> opts = Lazy.accumulateJust(ListX.of(just, none, Lazy.of(1)), Reducers.toPersistentSetX());
    //Lazy.of(PersistentSetX.of(10, 1)));
     *
     * }
     * </pre>
     *
     * @param futureals Lazys to accumulate
     * @param reducer Reducer to accumulate values with
     * @return Lazy with reduced value
     */
    public static <T, R> Lazy<R> accumulatePresent(final CollectionX<Lazy<T>> futureals, final Reducer<R> reducer) {
        return sequencePresent(futureals).map(s -> s.mapReduce(reducer));
    }
    /**
     * Accumulate the results only from those Lazys which have a value present, using the supplied mapping function to
     * convert the data from each Lazy before reducing them using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();

     *  Lazy<String> opts = Lazy.accumulateJust(ListX.of(just, none, Lazy.of(1)), i -> "" + i,
    Monoids.stringConcat);
    //Lazy.of("101")
     *
     * }
     * </pre>
     *
     * @param futureals Lazys to accumulate
     * @param mapper Mapping function to be applied to the result of each Lazy
     * @param reducer Monoid to combine values from each Lazy
     * @return Lazy with reduced value
     */
    public static <T, R> Lazy<R> accumulatePresent(final CollectionX<Lazy<T>> futureals, final Function<? super T, R> mapper,
                                                   final Monoid<R> reducer) {
        return sequencePresent(futureals).map(s -> s.map(mapper)
                .reduce(reducer));
    }
    /**
     * Accumulate the results only from those Lazys which have a value present, using the
     * supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see cyclops2.Monoids }.
     *
     * <pre>
     * {@code
     *  Lazy<Integer> just = Lazy.of(10);
    Lazy<Integer> none = Lazy.empty();

     *  Lazy<String> opts = Lazy.accumulateJust(Monoids.stringConcat,ListX.of(just, none, Lazy.of(1)),
    );
    //Lazy.of("101")
     *
     * }
     * </pre>
     *
     * @param futureals Lazys to accumulate
     * @param reducer Monoid to combine values from each Lazy
     * @return Lazy with reduced value
     */
    public static <T> Lazy<T> accumulatePresent(final Monoid<T> reducer, final CollectionX<Lazy<T>> futureals) {
        return sequencePresent(futureals).map(s -> s
                .reduce(reducer));
    }

    /**
     * Combine an Lazy with the provided value using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Lazys.combine(Lazy.of(10),Maybe.just(20), this::add)
     *  //Lazy[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Lazy to combine with a value
     * @param v Value to combine
     * @param fn Combining function
     * @return Lazy combined with supplied value
     */
    public static <T1, T2, R> Lazy<R> combine(final Lazy<? extends T1> f, final Value<? extends T2> v,
                                              final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.eval(ToCyclopsReact.eval(f)
                .combine(v, fn)));
    }
    /**
     * Combine an Lazy with the provided Lazy using the supplied BiFunction
     *
     * <pre>
     * {@code
     *  Lazys.combine(Lazy.of(10),Lazy.of(20), this::add)
     *  //Lazy[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param f Lazy to combine with a value
     * @param v Lazy to combine
     * @param fn Combining function
     * @return Lazy combined with supplied value, or empty Lazy if no value present
     */
    public static <T1, T2, R> Lazy<R> combine(final Lazy<? extends T1> f, final Lazy<? extends T2> v,
                                              final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return combine(f,ToCyclopsReact.eval(v),fn);
    }

    /**
     * Combine an Lazy with the provided Iterable (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Lazys.zip(Lazy.of(10),Arrays.asList(20), this::add)
     *  //Lazy[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     * @param f Lazy to combine with first element in Iterable (if present)
     * @param v Iterable to combine
     * @param fn Combining function
     * @return Lazy combined with supplied Iterable, or empty Lazy if no value present
     */
    public static <T1, T2, R> Lazy<R> zip(final Lazy<? extends T1> f, final Iterable<? extends T2> v,
                                          final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.eval(ToCyclopsReact.eval(f)
                .zip(v, fn)));
    }

    /**
     * Combine an Lazy with the provided Publisher (selecting one element if present) using the supplied BiFunction
     * <pre>
     * {@code
     *  Lazys.zip(Flux.just(10),Lazy.of(10), this::add)
     *  //Lazy[30]
     *
     *  private int add(int a, int b) {
    return a + b;
    }
     *
     * }
     * </pre>
     *
     * @param p Publisher to combine
     * @param f  Lazy to combine with
     * @param fn Combining function
     * @return Lazy combined with supplied Publisher, or empty Lazy if no value present
     */
    public static <T1, T2, R> Lazy<R> zip(final Publisher<? extends T2> p, final Lazy<? extends T1> f,
                                          final BiFunction<? super T1, ? super T2, ? extends R> fn) {
        return narrow(FromCyclopsReact.eval(ToCyclopsReact.eval(f)
                .zipP(p, fn)));
    }
    /**
     * Narrow covariant type parameter
     *
     * @param futureal Lazy with covariant type parameter
     * @return Narrowed Lazy
     */
    public static <T> Lazy<T> narrow(final Lazy<? extends T> futureal) {
        return (Lazy<T>) futureal;
    }
    public static <T> Active<lazy,T> allTypeclasses(Lazy<T> lazy){
        return Active.of(widen(lazy), Lazys.Instances.definitions());
    }
    public static <T,W2,R> Nested<lazy,W2,R> mapM(Lazy<T> lazy, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        Lazy<Higher<W2, R>> e = lazy.map(fn);
        LazyKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, Lazys.Instances.definitions(), defs);
    }

    @UtilityClass
    public static class Instances {

        public static  InstanceDefinitions<lazy> definitions() {
            return new InstanceDefinitions<lazy>() {

                @Override
                public <T, R> Functor<lazy> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<lazy> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<lazy> applicative() {
                    return Instances.applicative();
                }

                @Override
                public <T, R> Monad<lazy> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<lazy>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<lazy>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> MonadRec<lazy> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<lazy>> monadPlus(Monoid<Higher<lazy, T>> m) {
                    return Maybe.just(Instances.monadPlus(m));
                }

                @Override
                public <C2, T> Traverse<lazy> traverse() {
                    return Instances.traverse();
                }

                @Override
                public <T> Foldable<lazy> foldable() {
                    return Instances.foldable();
                }

                @Override
                public <T> Maybe<Comonad<lazy>> comonad() {
                    return Maybe.just(Instances.comonad());
                }

                @Override
                public <T> Maybe<Unfoldable<lazy>> unfoldable() {
                    return Maybe.none();
                }
            };
        };
        /**
         *
         * Transform a lazy, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  LazyKind<Integer> lazy = Instances.functor().map(i->i*2, LazyKind.widen(Lazy.of(()->1));
         *
         *  //[2]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Lazys
         * <pre>
         * {@code
         *   LazyKind<Integer> lazy = Lazys.unit()
        .unit("hello")
        .then(h->Lazys.functor().map((String v) ->v.length(), h))
        .convert(LazyKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Lazys
         */
        public static <T,R>Functor<lazy> functor(){
            BiFunction<LazyKind<T>,Function<? super T, ? extends R>,LazyKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * LazyKind<String> lazy = Lazys.unit()
        .unit("hello")
        .convert(LazyKind::narrowK);

        //Lazy.just("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Lazys
         */
        public static <T> Pure<lazy> unit(){
            return General.<lazy,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.LazyKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Lazy.just;
         *
        Lazys.zippingApplicative()
        .ap(widen(asLazy(l1(this::multiplyByTwo))),widen(asLazy(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * LazyKind<Function<Integer,Integer>> lazyFn =Lazys.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(LazyKind::narrowK);

        LazyKind<Integer> lazy = Lazys.unit()
        .unit("hello")
        .then(h->Lazys.functor().map((String v) ->v.length(), h))
        .then(h->Lazys.applicative().ap(lazyFn, h))
        .convert(LazyKind::narrowK);

        //Lazy.just("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Lazys
         */
        public static <T,R> Applicative<lazy> applicative(){
            BiFunction<LazyKind< Function<T, R>>,LazyKind<T>,LazyKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.LazyKind.widen;
         * LazyKind<Integer> lazy  = Lazys.monad()
        .flatMap(i->widen(LazyX.range(0,i)), widen(Lazy.just(1,2,3)))
        .convert(LazyKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    LazyKind<Integer> lazy = Lazys.unit()
        .unit("hello")
        .then(h->Lazys.monad().flatMap((String v) ->Lazys.unit().unit(v.length()), h))
        .convert(LazyKind::narrowK);

        //Lazy.just("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Lazys
         */
        public static <T,R> Monad<lazy> monad(){

            BiFunction<Higher<lazy,T>,Function<? super T, ? extends Higher<lazy,R>>,Higher<lazy,R>> flatMap = Instances::flatMap;
            return General.monad(applicative(), flatMap);
        }
        /**
         *
         * <pre>
         * {@code
         *  LazyKind<String> lazy = Lazys.unit()
        .unit("hello")
        .then(h->Lazys.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(LazyKind::narrowK);

        //Lazy.just("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<lazy> monadZero(){

            return General.monadZero(monad(), LazyKind.of(()->null));
        }
        /**
         * <pre>
         * {@code
         *  LazyKind<Integer> lazy = Lazys.<Integer>monadPlus()
        .plus(LazyKind.widen(Lazy.just()), LazyKind.widen(Lazy.just(10)))
        .convert(LazyKind::narrowK);
        //Lazy.just(10))
         *
         * }
         * </pre>
         * @return Type class for combining Lazys by concatenation
         */
        public static <T> MonadPlus<lazy> monadPlus(){
            Monoid<LazyKind<T>> m = Monoid.of( LazyKind.of(()->null),
                    (a,b)-> a.get()==null? b: a);
            Monoid<Higher<lazy,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<LazyKind<Integer>> m = Monoid.of(LazyKind.widen(Lazy.just()), (a,b)->a.isEmpty() ? b : a);
        LazyKind<Integer> lazy = Lazys.<Integer>monadPlus(m)
        .plus(LazyKind.widen(Lazy.just(5)), LazyKind.widen(Lazy.just(10)))
        .convert(LazyKind::narrowK);
        //Lazy[5]
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining Lazys
         * @return Type class for combining Lazys
         */
        public static <T> MonadPlus<lazy> monadPlus(Monoid<Higher<lazy,T>> m){
            Monoid<Higher<lazy,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        public static <T> MonadPlus<lazy> monadPlusK(Monoid<LazyKind<T>> m){
            return monadPlus((Monoid)m);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<lazy> traverse(){

            return General.traverseByTraverse(applicative(), Instances::traverseA);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Lazys.foldable()
        .foldLeft(0, (a,b)->a+b, LazyKind.widen(Lazy.just(1)));

        //1
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<lazy> foldable(){
            return new Foldable<lazy>() {
                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<lazy, T> ds) {
                    return narrowK(ds).narrow().getOrElse(monoid.zero());
                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<lazy, T> ds) {
                    return narrowK(ds).narrow().getOrElse(monoid.zero());
                }

                @Override
                public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<lazy, T> nestedA) {
                    return narrowK(nestedA).narrow().<R>map(fn).getOrElse(mb.zero());
                }
            };

        }
        public static <T> MonadRec<lazy> monadRec(){
            return new MonadRec<lazy>() {
                @Override
                public <T, R> Higher<lazy, R> tailRec(T initial, Function<? super T, ? extends Higher<lazy, ? extends Xor<T, R>>> fn) {
                    return widen(Lazys.tailRecXor(initial, fn.andThen(l ->  narrowK(l).narrow())));

                }
            };
        }
        public static <T> Comonad<lazy> comonad(){
            Function<? super Higher<lazy, T>, ? extends T> extractFn = maybe -> maybe.convert(LazyKind::narrow).get();
            return General.comonad(functor(), unit(), extractFn);
        }

        private <T> LazyKind<T> of(T value){
            return widen(Lazy.of(()->value));
        }
        private static <T,R> LazyKind<R> ap(LazyKind<Function< T, R>> lt, LazyKind<T> lazy){
            return widen(FromCyclopsReact.lazy(ToCyclopsReact.eval(lt.narrow()).combine(ToCyclopsReact.eval(lazy.narrow()), (a, b)->a.apply(b))));

        }
        private static <T,R> Higher<lazy,R> flatMap(Higher<lazy,T> lt, Function<? super T, ? extends  Higher<lazy,R>> fn){
            return widen(LazyKind.narrowEval(lt).flatMap(fn.andThen(LazyKind::narrowEval)));
        }
        private static <T,R> LazyKind<R> map(LazyKind<T> lt, Function<? super T, ? extends R> fn){
            return widen(LazyKind.narrow(lt).map(fn));
        }


        private static <C2,T,R> Higher<C2, Higher<lazy, R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn,
                                                                            Higher<lazy, T> ds){

            Lazy<T> eval = LazyKind.narrow(ds);
            Higher<C2, R> ds2 = fn.apply(eval.get());
            return applicative.map(v-> LazyKind.of(()->v), ds2);

        }

    }
    public static interface LazyNested{


        public static <T> Nested<lazy,option,T> option(Lazy<Option<T>> type){
            return Nested.of(widen(type.map(OptionKind::widen)),Instances.definitions(),Options.Instances.definitions());
        }
        public static <T> Nested<lazy,tryType,T> lazyTry(Lazy<Try<T>> type){
            return Nested.of(widen(type.map(TryKind::widen)),Instances.definitions(),Trys.Instances.definitions());
        }
        public static <T> Nested<lazy,VavrWitness.future,T> future(Lazy<Future<T>> type){
            return Nested.of(widen(type.map(FutureKind::widen)),Instances.definitions(),Futures.Instances.definitions());
        }
        public static <T> Nested<lazy,lazy,T> lazy(Lazy<Lazy<T>> nested){
            return Nested.of(widen(nested.map(LazyKind::widen)),Instances.definitions(),Lazys.Instances.definitions());
        }
        public static <L, R> Nested<lazy,Higher<VavrWitness.either,L>, R> either(Lazy<Either<L, R>> nested){
            return Nested.of(widen(nested.map(EitherKind::widen)),Instances.definitions(),Eithers.Instances.definitions());
        }
        public static <T> Nested<lazy,VavrWitness.queue,T> queue(Lazy<Queue<T>> nested){
            return Nested.of(widen(nested.map(QueueKind::widen)), Instances.definitions(),Queues.Instances.definitions());
        }
        public static <T> Nested<lazy,stream,T> stream(Lazy<Stream<T>> nested){
            return Nested.of(widen(nested.map(StreamKind::widen)),Instances.definitions(),Streams.Instances.definitions());
        }
        public static <T> Nested<lazy,list,T> list(Lazy<List<T>> nested){
            return Nested.of(widen(nested.map(ListKind::widen)), Instances.definitions(),Lists.Instances.definitions());
        }
        public static <T> Nested<lazy,array,T> array(Lazy<Array<T>> nested){
            return Nested.of(widen(nested.map(ArrayKind::widen)),Instances.definitions(),Arrays.Instances.definitions());
        }
        public static <T> Nested<lazy,vector,T> vector(Lazy<Vector<T>> nested){
            return Nested.of(widen(nested.map(VectorKind::widen)),Instances.definitions(),Vectors.Instances.definitions());
        }
        public static <T> Nested<lazy,hashSet,T> set(Lazy<HashSet<T>> nested){
            return Nested.of(widen(nested.map(HashSetKind::widen)),Instances.definitions(), HashSets.Instances.definitions());
        }

        public static <T> Nested<lazy,reactiveSeq,T> reactiveSeq(Lazy<ReactiveSeq<T>> nested){
            LazyKind<ReactiveSeq<T>> x = widen(nested);
            LazyKind<Higher<reactiveSeq,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<lazy,maybe,T> maybe(Lazy<Maybe<T>> nested){
            LazyKind<Maybe<T>> x = widen(nested);
            LazyKind<Higher<maybe,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<lazy,eval,T> eval(Lazy<Eval<T>> nested){
            LazyKind<Eval<T>> x = widen(nested);
            LazyKind<Higher<eval,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<lazy,Witness.future,T> cyclopsFuture(Lazy<cyclops.async.Future<T>> nested){
            LazyKind<cyclops.async.Future<T>> x = widen(nested);
            LazyKind<Higher<Witness.future,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<lazy,Higher<xor,S>, P> xor(Lazy<Xor<S, P>> nested){
            LazyKind<Xor<S, P>> x = widen(nested);
            LazyKind<Higher<Higher<xor,S>, P>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),Xor.Instances.definitions());
        }
        public static <S,T> Nested<lazy,Higher<reader,S>, T> reader(Lazy<Reader<S, T>> nested,S defaultValue){
            LazyKind<Reader<S, T>> x = widen(nested);
            LazyKind<Higher<Higher<reader,S>, T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions(defaultValue));
        }
        public static <S extends Throwable, P> Nested<lazy,Higher<Witness.tryType,S>, P> cyclopsTry(Lazy<cyclops.control.Try<P, S>> nested){
            LazyKind<cyclops.control.Try<P, S>> x = widen(nested);
            LazyKind<Higher<Higher<Witness.tryType,S>, P>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<lazy,optional,T> optional(Lazy<Optional<T>> nested){
            LazyKind<Optional<T>> x = widen(nested);
            LazyKind<Higher<optional,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(), Optionals.Instances.definitions());
        }
        public static <T> Nested<lazy,completableFuture,T> completableLazy(Lazy<CompletableFuture<T>> nested){
            LazyKind<CompletableFuture<T>> x = widen(nested);
            LazyKind<Higher<completableFuture,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<lazy,Witness.stream,T> javaStream(Lazy<java.util.stream.Stream<T>> nested){
            LazyKind<java.util.stream.Stream<T>> x = widen(nested);
            LazyKind<Higher<Witness.stream,T>> y = (LazyKind)x;
            return Nested.of(y,Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }




    }

    public static interface NestedLazy{
        public static <T> Nested<reactiveSeq,lazy,T> reactiveSeq(ReactiveSeq<Lazy<T>> nested){
            ReactiveSeq<Higher<lazy,T>> x = nested.map(LazyKind::widenK);
            return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
        }

        public static <T> Nested<maybe,lazy,T> maybe(Maybe<Lazy<T>> nested){
            Maybe<Higher<lazy,T>> x = nested.map(LazyKind::widenK);

            return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<eval,lazy,T> eval(Eval<Lazy<T>> nested){
            Eval<Higher<lazy,T>> x = nested.map(LazyKind::widenK);

            return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.future,lazy,T> cyclopsFuture(cyclops.async.Future<Lazy<T>> nested){
            cyclops.async.Future<Higher<lazy,T>> x = nested.map(LazyKind::widenK);

            return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
        }
        public static <S, P> Nested<Higher<xor,S>,lazy, P> xor(Xor<S, Lazy<P>> nested){
            Xor<S, Higher<lazy,P>> x = nested.map(LazyKind::widenK);

            return Nested.of(x,Xor.Instances.definitions(),Instances.definitions());
        }
        public static <S,T> Nested<Higher<reader,S>,lazy, T> reader(Reader<S, Lazy<T>> nested,S defaultValue){

            Reader<S, Higher<lazy, T>>  x = nested.map(LazyKind::widenK);

            return Nested.of(x,Reader.Instances.definitions(defaultValue),Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,lazy, P> cyclopsTry(cyclops.control.Try<Lazy<P>, S> nested){
            cyclops.control.Try<Higher<lazy,P>, S> x = nested.map(LazyKind::widenK);

            return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<optional,lazy,T> optional(Optional<Lazy<T>> nested){
            Optional<Higher<lazy,T>> x = nested.map(LazyKind::widenK);

            return  Nested.of(Optionals.OptionalKind.widen(x), Optionals.Instances.definitions(), Instances.definitions());
        }
        public static <T> Nested<completableFuture,lazy,T> completableLazy(CompletableFuture<Lazy<T>> nested){
            CompletableFuture<Higher<lazy,T>> x = nested.thenApply(LazyKind::widenK);

            return Nested.of(CompletableFutures.CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.stream,lazy,T> javaStream(java.util.stream.Stream<Lazy<T>> nested){
            java.util.stream.Stream<Higher<lazy,T>> x = nested.map(LazyKind::widenK);

            return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
        }
    }


}
