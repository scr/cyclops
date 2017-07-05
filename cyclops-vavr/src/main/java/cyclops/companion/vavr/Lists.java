package cyclops.companion.vavr;

import com.aol.cyclops.vavr.hkt.*;
import cyclops.collections.immutable.VectorX;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Optionals;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Reader;
import cyclops.control.Xor;
import cyclops.monads.*;
import cyclops.monads.VavrWitness.*;
import cyclops.collections.vavr.VavrListX;
import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.types.anyM.AnyMSeq;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.VavrWitness.either;
import cyclops.monads.VavrWitness.future;
import cyclops.monads.VavrWitness.list;
import cyclops.monads.VavrWitness.tryType;
import cyclops.monads.Witness.*;
import cyclops.monads.transformers.ListT;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.*;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.Lazy;
import io.vavr.collection.*;
import io.vavr.concurrent.Future;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import static com.aol.cyclops.vavr.hkt.ListKind.widen;


public class Lists {
    public static <T,W extends WitnessType<W>> ListT<W, T> liftM(List<T> opt, W witness) {
        return ListT.ofList(witness.adapter().unit(VavrListX.ofAll(opt)));
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,list,T> xorM(List<T> type){
        return XorM.right(anyM(type));
    }
    public static <T> AnyMSeq<list,T> anyM(List<T> option) {
        return AnyM.ofSeq(option, list.INSTANCE);
    }
    /**
     * Perform a For Comprehension over a List, accepting 3 generating functions.
     * This results in a four level nested internal iteration over the provided Publishers.
     *
     *  <pre>
     * {@code
     *
     *   import static cyclops.Lists.forEach4;
     *
    forEach4(IntList.range(1,10).boxed(),
    a-> List.iterate(a,i->i+1).limit(10),
    (a,b) -> List.<Integer>of(a+b),
    (a,b,c) -> List.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level List
     * @param value2 Nested List
     * @param value3 Nested List
     * @param value4 Nested List
     * @param yieldingFunction  Generates a result per combination
     * @return List with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> List<R> forEach4(List<? extends T1> value1,
                                                               Function<? super T1, ? extends List<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends List<R2>> value3,
                                                               Fn3<? super T1, ? super R1, ? super R2, ? extends List<R3>> value4,
                                                               Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                List<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    List<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a List, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Listes.forEach4;
     *
     *  forEach4(IntList.range(1,10).boxed(),
    a-> List.iterate(a,i->i+1).limit(10),
    (a,b) -> List.<Integer>just(a+b),
    (a,b,c) -> List.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level List
     * @param value2 Nested List
     * @param value3 Nested List
     * @param value4 Nested List
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return List with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> List<R> forEach4(List<? extends T1> value1,
                                                                 Function<? super T1, ? extends List<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends List<R2>> value3,
                                                                 Fn3<? super T1, ? super R1, ? super R2, ? extends List<R3>> value4,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                List<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    List<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }

    /**
     * Perform a For Comprehension over a List, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     * import static Lists.forEach3;
     *
     * forEach(IntList.range(1,10).boxed(),
    a-> List.iterate(a,i->i+1).limit(10),
    (a,b) -> List.<Integer>of(a+b),
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     *
     * @param value1 top level List
     * @param value2 Nested List
     * @param value3 Nested List
     * @param yieldingFunction Generates a result per combination
     * @return List with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> List<R> forEach3(List<? extends T1> value1,
                                                         Function<? super T1, ? extends List<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends List<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                List<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });


    }

    /**
     * Perform a For Comprehension over a List, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     * import static Lists.forEach;
     *
     * forEach(IntList.range(1,10).boxed(),
    a-> List.iterate(a,i->i+1).limit(10),
    (a,b) -> List.<Integer>of(a+b),
    (a,b,c) ->a+b+c<10,
    Tuple::tuple)
    .toListX();
     * }
     * </pre>
     *
     * @param value1 top level List
     * @param value2 Nested publisher
     * @param value3 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T1, T2, R1, R2, R> List<R> forEach3(List<? extends T1> value1,
                                                         Function<? super T1, ? extends List<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends List<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                List<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });
    }

    /**
     * Perform a For Comprehension over a List, accepting an additonal generating function.
     * This results in a two level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     *  import static Lists.forEach2;
     *  forEach(IntList.range(1, 10).boxed(),
     *          i -> List.range(i, 10), Tuple::tuple)
    .forEach(System.out::println);

    //(1, 1)
    (1, 2)
    (1, 3)
    (1, 4)
    ...
     *
     * }</pre>
     *
     * @param value1 top level List
     * @param value2 Nested publisher
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> List<R> forEach2(List<? extends T> value1,
                                                Function<? super T, List<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });

    }

    /**
     *
     * <pre>
     * {@code
     *
     *   import static Lists.forEach2;
     *
     *   forEach(IntList.range(1, 10).boxed(),
     *           i -> List.range(i, 10),
     *           (a,b) -> a>2 && b<10,
     *           Tuple::tuple)
    .forEach(System.out::println);

    //(3, 3)
    (3, 4)
    (3, 5)
    (3, 6)
    (3, 7)
    (3, 8)
    (3, 9)
    ...

     *
     * }</pre>
     *
     *
     * @param value1 top level List
     * @param value2 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> List<R> forEach2(List<? extends T> value1,
                                                Function<? super T, ? extends List<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            List<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });
    }

    public static <T> Active<list,T> allTypeclasses(List<T> array){
        return Active.of(widen(array), Lists.Instances.definitions());
    }
    public static <T,W2,R> Nested<list,W2,R> mapM(List<T> array, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        List<Higher<W2, R>> e = array.map(fn);
        ListKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, Lists.Instances.definitions(), defs);
    }
    /**
     * Companion class for creating Type Class instances for working with Lists
     *
     */
    @UtilityClass
    public static class Instances {

        public static InstanceDefinitions<list> definitions() {
            return new InstanceDefinitions<list>() {

                @Override
                public <T, R> Functor<list> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<list> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<list> applicative() {
                    return Instances.zippingApplicative();
                }

                @Override
                public <T, R> Monad<list> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<list>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<list>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> Maybe<MonadPlus<list>> monadPlus(Monoid<Higher<list, T>> m) {
                    return Maybe.just(Instances.monadPlus(m));
                }

                @Override
                public <C2, T> Maybe<Traverse<list>> traverse() {
                    return Maybe.just(Instances.traverse());
                }

                @Override
                public <T> Maybe<Foldable<list>> foldable() {
                    return Maybe.just(Instances.foldable());
                }

                @Override
                public <T> Maybe<Comonad<list>> comonad() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<Unfoldable<list>> unfoldable() {
                    return Maybe.just(Instances.unfoldable());
                }
            };
        }
        /**
         *
         * Transform a list, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  ListKind<Integer> list = Lists.functor().map(i->i*2, ListKind.widen(List.of(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Lists
         * <pre>
         * {@code
         *   ListKind<Integer> list = Lists.unit()
        .unit("hello")
        .then(h->Lists.functor().map((String v) ->v.length(), h))
        .convert(ListKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Lists
         */
        public static <T,R>Functor<list> functor(){
            BiFunction<ListKind<T>,Function<? super T, ? extends R>,ListKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * ListKind<String> list = Lists.unit()
        .unit("hello")
        .convert(ListKind::narrowK);

        //List.of("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Lists
         */
        public static <T> Pure<list> unit(){
            return General.<list,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.ListKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         *
        Lists.zippingApplicative()
        .ap(widen(List.of(l1(this::multiplyByTwo))),widen(List.of(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * ListKind<Function<Integer,Integer>> listFn =Lists.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(ListKind::narrowK);

        ListKind<Integer> list = Lists.unit()
        .unit("hello")
        .then(h->Lists.functor().map((String v) ->v.length(), h))
        .then(h->Lists.zippingApplicative().ap(listFn, h))
        .convert(ListKind::narrowK);

        //List.of("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Lists
         */
        public static <T,R> Applicative<list> zippingApplicative(){
            BiFunction<ListKind< Function<T, R>>,ListKind<T>,ListKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.ListKind.widen;
         * ListKind<Integer> list  = Lists.monad()
        .flatMap(i->widen(ListX.range(0,i)), widen(List.of(1,2,3)))
        .convert(ListKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    ListKind<Integer> list = Lists.unit()
        .unit("hello")
        .then(h->Lists.monad().flatMap((String v) ->Lists.unit().unit(v.length()), h))
        .convert(ListKind::narrowK);

        //List.of("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Lists
         */
        public static <T,R> Monad<list> monad(){

            BiFunction<Higher<list,T>,Function<? super T, ? extends Higher<list,R>>,Higher<list,R>> flatMap = Instances::flatMap;
            return General.monad(zippingApplicative(), flatMap);
        }
        /**
         *
         * <pre>
         * {@code
         *  ListKind<String> list = Lists.unit()
        .unit("hello")
        .then(h->Lists.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(ListKind::narrowK);

        //List.of("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<list> monadZero(){
            BiFunction<Higher<list,T>,Predicate<? super T>,Higher<list,T>> filter = Instances::filter;
            Supplier<Higher<list, T>> zero = ()-> widen(List.empty());
            return General.<list,T,R>monadZero(monad(), zero,filter);
        }
        /**
         * <pre>
         * {@code
         *  ListKind<Integer> list = Lists.<Integer>monadPlus()
        .plus(ListKind.widen(List.of()), ListKind.widen(List.of(10)))
        .convert(ListKind::narrowK);
        //List.of(10))
         *
         * }
         * </pre>
         * @return Type class for combining Lists by concatenation
         */
        public static <T> MonadPlus<list> monadPlus(){
            Monoid<ListKind<T>> m = Monoid.of(widen(List.<T>empty()), Instances::concat);
            Monoid<Higher<list,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<ListKind<Integer>> m = Monoid.of(ListKind.widen(List.of()), (a,b)->a.isEmpty() ? b : a);
        ListKind<Integer> list = Lists.<Integer>monadPlus(m)
        .plus(ListKind.widen(List.of(5)), ListKind.widen(List.of(10)))
        .convert(ListKind::narrowK);
        //List.of(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining Lists
         * @return Type class for combining Lists
         */
        public static <T> MonadPlus<list> monadPlus(Monoid<Higher<list,T>> m){
            Monoid<Higher<list,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        public static <T> MonadPlus<list> monadPlusK(Monoid<ListKind<T>> m){
            Monoid<Higher<list,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<list> traverse(){
            BiFunction<Applicative<C2>,ListKind<Higher<C2, T>>,Higher<C2, ListKind<T>>> sequenceFn = (ap, list) -> {

                Higher<C2,ListKind<T>> identity = ap.unit(widen(List.empty()));

                BiFunction<Higher<C2,ListKind<T>>,Higher<C2,T>,Higher<C2,ListKind<T>>> combineToList =   (acc, next) -> ap.apBiFn(ap.unit((a, b) -> concat(a, ListKind.just(b))),acc,next);

                BinaryOperator<Higher<C2,ListKind<T>>> combineLists = (a, b)-> ap.apBiFn(ap.unit((l1, l2)-> { return concat(l1,l2);}),a,b); ;

                return ReactiveSeq.fromIterable(list).reduce(identity,
                        combineToList,
                        combineLists);


            };
            BiFunction<Applicative<C2>,Higher<list,Higher<C2, T>>,Higher<C2, Higher<list,T>>> sequenceNarrow  =
                    (a,b) -> ListKind.widen2(sequenceFn.apply(a, ListKind.narrowK(b)));
            return General.traverse(zippingApplicative(), sequenceNarrow);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Lists.foldable()
        .foldLeft(0, (a,b)->a+b, ListKind.widen(List.of(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<list> foldable(){
            BiFunction<Monoid<T>,Higher<list,T>,T> foldRightFn =  (m, l)-> ReactiveSeq.fromIterable(ListKind.narrow(l)).foldRight(m);
            BiFunction<Monoid<T>,Higher<list,T>,T> foldLeftFn = (m, l)-> ReactiveSeq.fromIterable(ListKind.narrow(l)).reduce(m);
            return General.foldable(foldRightFn, foldLeftFn);
        }

        private static  <T> ListKind<T> concat(ListKind<T> l1, ListKind<T> l2){
            return widen(l1.appendAll(l2));
        }
        private <T> ListKind<T> of(T value){
            return widen(List.of(value));
        }
        private static <T,R> ListKind<R> ap(ListKind<Function< T, R>> lt, ListKind<T> list){
            return widen(lt.toReactiveSeq().zip(list,(a, b)->a.apply(b)));
        }
        private static <T,R> Higher<list,R> flatMap(Higher<list,T> lt, Function<? super T, ? extends  Higher<list,R>> fn){
            return widen(ListKind.narrowK(lt).flatMap(fn.andThen(ListKind::narrowK)));
        }
        private static <T,R> ListKind<R> map(ListKind<T> lt, Function<? super T, ? extends R> fn){
            return widen(lt.map(fn));
        }
        private static <T> ListKind<T> filter(Higher<list,T> lt, Predicate<? super T> fn){
            return widen(ListKind.narrow(lt).filter(fn));
        }
        public static Unfoldable<list> unfoldable(){
            return new Unfoldable<list>() {
                @Override
                public <R, T> Higher<list, R> unfold(T b, Function<? super T, Optional<Tuple2<R, T>>> fn) {
                    return widen(ReactiveSeq.unfold(b,fn).collect(List.collector()));

                }
            };
        }
    }

    public static  <W1,T> Coproduct<W1,list,T> coproduct(List<T> list, InstanceDefinitions<W1> def1){
        return Coproduct.of(Xor.primary(ListKind.widen(list)),def1, Instances.definitions());
    }

    public static interface Nesteds{


        public static <T> Nested<list,option,T> option(List<Option<T>> type){
            return Nested.of(widen(type.map(OptionKind::widen)),Instances.definitions(),Options.Instances.definitions());
        }
        public static <T> Nested<list,tryType,T> listTry(List<Try<T>> type){
            return Nested.of(widen(type.map(TryKind::widen)),Instances.definitions(),Trys.Instances.definitions());
        }
        public static <T> Nested<list,future,T> future(List<Future<T>> type){
            return Nested.of(widen(type.map(FutureKind::widen)),Instances.definitions(),Futures.Instances.definitions());
        }
        public static <T> Nested<list,lazy,T> lazy(List<Lazy<T>> nested){
            return Nested.of(widen(nested.map(LazyKind::widen)),Instances.definitions(),Lazys.Instances.definitions());
        }
        public static <L, R> Nested<list,Higher<either,L>, R> either(List<Either<L, R>> nested){
            return Nested.of(widen(nested.map(EitherKind::widen)),Instances.definitions(),Eithers.Instances.definitions());
        }
        public static <T> Nested<list,VavrWitness.stream,T> stream(List<Stream<T>> nested){
            return Nested.of(widen(nested.map(StreamKind::widen)),Instances.definitions(),Streams.Instances.definitions());
        }
        public static <T> Nested<list,VavrWitness.queue,T> queue(List<Queue<T>> nested){
            return Nested.of(widen(nested.map(QueueKind::widen)),Instances.definitions(),Queues.Instances.definitions());
        }
        public static <T> Nested<list,list,T> list(List<List<T>> nested){
            return Nested.of(widen(nested.map(ListKind::widen)),Instances.definitions(),Lists.Instances.definitions());
        }
        public static <T> Nested<list,array,T> array(List<Array<T>> nested){
            return Nested.of(widen(nested.map(ArrayKind::widen)),Instances.definitions(),Arrays.Instances.definitions());
        }
        public static <T> Nested<list,vector,T> vector(List<Vector<T>> nested){
            return Nested.of(widen(nested.map(VectorKind::widen)),Instances.definitions(),Vectors.Instances.definitions());
        }
        public static <T> Nested<list,VavrWitness.set,T> set(List<HashSet<T>> nested){
            return Nested.of(widen(nested.map(SetKind::widen)),Instances.definitions(),Sets.Instances.definitions());
        }

        public static <T> Nested<list,reactiveSeq,T> reactiveSeq(List<ReactiveSeq<T>> nested){
            ListKind<ReactiveSeq<T>> x = widen(nested);
            ListKind<Higher<reactiveSeq,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<list,maybe,T> maybe(List<Maybe<T>> nested){
            ListKind<Maybe<T>> x = widen(nested);
            ListKind<Higher<maybe,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<list,eval,T> eval(List<Eval<T>> nested){
            ListKind<Eval<T>> x = widen(nested);
            ListKind<Higher<eval,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<list,Witness.future,T> cyclopsFuture(List<cyclops.async.Future<T>> nested){
            ListKind<cyclops.async.Future<T>> x = widen(nested);
            ListKind<Higher<Witness.future,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<list,Higher<xor,S>, P> xor(List<Xor<S, P>> nested){
            ListKind<Xor<S, P>> x = widen(nested);
            ListKind<Higher<Higher<xor,S>, P>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),Xor.Instances.definitions());
        }
        public static <S,T> Nested<list,Higher<reader,S>, T> reader(List<Reader<S, T>> nested){
            ListKind<Reader<S, T>> x = widen(nested);
            ListKind<Higher<Higher<reader,S>, T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<list,Higher<Witness.tryType,S>, P> cyclopsTry(List<cyclops.control.Try<P, S>> nested){
            ListKind<cyclops.control.Try<P, S>> x = widen(nested);
            ListKind<Higher<Higher<Witness.tryType,S>, P>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<list,optional,T> optional(List<Optional<T>> nested){
            ListKind<Optional<T>> x = widen(nested);
            ListKind<Higher<optional,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(), Optionals.Instances.definitions());
        }
        public static <T> Nested<list,completableFuture,T> completableFuture(List<CompletableFuture<T>> nested){
            ListKind<CompletableFuture<T>> x = widen(nested);
            ListKind<Higher<completableFuture,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<list,Witness.stream,T> javaStream(List<java.util.stream.Stream<T>> nested){
            ListKind<java.util.stream.Stream<T>> x = widen(nested);
            ListKind<Higher<Witness.stream,T>> y = (ListKind)x;
            return Nested.of(y,Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }

        public static interface Reversed{
            public static <T> Nested<reactiveSeq,list,T> reactiveSeq(ReactiveSeq<List<T>> nested){
                ReactiveSeq<Higher<list,T>> x = nested.map(ListKind::widenK);
                return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
            }

            public static <T> Nested<maybe,list,T> maybe(Maybe<List<T>> nested){
                Maybe<Higher<list,T>> x = nested.map(ListKind::widenK);

                return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
            }
            public static <T> Nested<eval,list,T> eval(Eval<List<T>> nested){
                Eval<Higher<list,T>> x = nested.map(ListKind::widenK);

                return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
            }
            public static <T> Nested<Witness.future,list,T> cyclopsFuture(cyclops.async.Future<List<T>> nested){
                cyclops.async.Future<Higher<list,T>> x = nested.map(ListKind::widenK);

                return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
            }
            public static <S, P> Nested<Higher<xor,S>,list, P> xor(Xor<S, List<P>> nested){
                Xor<S, Higher<list,P>> x = nested.map(ListKind::widenK);

                return Nested.of(x,Xor.Instances.definitions(),Instances.definitions());
            }
            public static <S,T> Nested<Higher<reader,S>,list, T> reader(Reader<S, List<T>> nested){

                Reader<S, Higher<list, T>>  x = nested.map(ListKind::widenK);

                return Nested.of(x,Reader.Instances.definitions(),Instances.definitions());
            }
            public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,list, P> cyclopsTry(cyclops.control.Try<List<P>, S> nested){
                cyclops.control.Try<Higher<list,P>, S> x = nested.map(ListKind::widenK);

                return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
            }
            public static <T> Nested<optional,list,T> optional(Optional<List<T>> nested){
                Optional<Higher<list,T>> x = nested.map(ListKind::widenK);

                return  Nested.of(Optionals.OptionalKind.widen(x), Optionals.Instances.definitions(), Instances.definitions());
            }
            public static <T> Nested<completableFuture,list,T> completableFuture(CompletableFuture<List<T>> nested){
                CompletableFuture<Higher<list,T>> x = nested.thenApply(ListKind::widenK);

                return Nested.of(CompletableFutures.CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
            }
            public static <T> Nested<Witness.stream,list,T> javaStream(java.util.stream.Stream<List<T>> nested){
                java.util.stream.Stream<Higher<list,T>> x = nested.map(ListKind::widenK);

                return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
            }
        }


    }


}
