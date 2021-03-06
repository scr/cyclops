package cyclops.companion.vavr;

import cyclops.control.*;
import cyclops.monads.VavrWitness.queue;
import cyclops.monads.VavrWitness.tryType;
import io.vavr.Lazy;
import io.vavr.collection.*;
import io.vavr.concurrent.Future;
import io.vavr.control.*;
import com.aol.cyclops.vavr.hkt.*;
import cyclops.companion.CompletableFutures;
import cyclops.companion.Optionals;
import cyclops.conversion.vavr.FromCyclopsReact;
import cyclops.monads.*;
import cyclops.monads.VavrWitness.*;
import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.types.anyM.AnyMSeq;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.Witness.*;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.*;
import com.aol.cyclops.vavr.hkt.ListKind;
import cyclops.monads.VavrWitness;
import cyclops.monads.VavrWitness.stream;
import com.aol.cyclops.vavr.hkt.StreamKind;
import cyclops.monads.AnyM;
import cyclops.monads.WitnessType;
import cyclops.monads.XorM;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static com.aol.cyclops.vavr.hkt.StreamKind.narrowK;
import static com.aol.cyclops.vavr.hkt.StreamKind.widen;


public class Streams {
    public static  <W1,T> Coproduct<W1,stream,T> coproduct(Stream<T> type, InstanceDefinitions<W1> def1){
        return Coproduct.of(Xor.primary(widen(type)),def1, Instances.definitions());
    }
    public static  <W1,T> Coproduct<W1,stream,T> coproduct(InstanceDefinitions<W1> def1,T... values){
        return coproduct(Stream.of(values),def1);
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,stream,T> xorM(Stream<T> type){
        return XorM.right(anyM(type));
    }
    public static  <W1 extends WitnessType<W1>,T> XorM<W1,stream,T> xorM(T... values){
        return xorM(Stream.of(values));
    }

    public static  <T,R> Stream<R> tailRec(T initial, Function<? super T, ? extends Stream<? extends Either<T, R>>> fn) {
        Stream<Either<T, R>> next = Stream.of(Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            Stream<Either<T, R>> rebuild = Stream.of();
            for(Either<T,R> e : next){
                Stream<? extends Either<T, R>> r = e.fold(s -> {
                            newValue[0] = true;

                            return fn.apply(s);
                        },
                        p -> {
                            newValue[0] = false;
                            return Stream.of(e);
                        });
                rebuild = Stream.concat(rebuild,r);
            }
            next = rebuild;
            if(!newValue[0])
                break;

        }

        return next.filter(Either::isRight).map(Either::get);
    }
    public static  <T,R> Stream<R> tailRecXor(T initial, Function<? super T, ? extends Stream<? extends Xor<T, R>>> fn) {
        Stream<Xor<T, R>> next = Stream.of(Xor.secondary(initial));

        boolean newValue[] = {true};
        for(;;){

            Stream<Xor<T, R>> rebuild = Stream.of();
            for(Xor<T,R> e : next){
                Stream<? extends Xor<T, R>> r = e.visit(s -> {
                            newValue[0] = true;

                            return fn.apply(s);
                        },
                        p -> {
                            newValue[0] = false;
                            return Stream.of(e);
                        });
                rebuild = Stream.concat(rebuild,r);
            }

            next = rebuild;
            if(!newValue[0])
                break;

        }

        return next.filter(Xor::isPrimary).map(Xor::get);
    }
    //not tail-recursive, may blow stack, but have better performance
    public static <T,R> R foldRightUnsafe(Stream<T> stream,R identity, BiFunction<? super T, ? super R, ? extends R> fn){
        if(stream.isEmpty())
            return identity;
        else
            return fn.apply(stream.head(),foldRight(stream.tail(),identity,fn));
    }

    public static <T,R> R foldRight(Stream<T> stream,R identity, BiFunction<? super T, ? super R, ? extends R> p){
        class Step{
            public Trampoline<R> loop(Stream<T> s, Function<? super R, ? extends Trampoline<R>> fn){
                   if (s.isEmpty())
                       return fn.apply(identity);
                   return Trampoline.more(()->loop(s.tail(), rem -> Trampoline.more(() -> fn.apply(p.apply(s.head(), rem)))));

            }
        }
        return new Step().loop(stream,i->Trampoline.done(i)).result();
    }



    public static <T> AnyMSeq<stream,T> anyM(Stream<T> option) {
        return AnyM.ofSeq(option, stream.INSTANCE);
    }
    /**
     * Perform a For Comprehension over a Stream, accepting 3 generating functions.
     * This results in a four level nested internal iteration over the provided Publishers.
     *
     *  <pre>
     * {@code
     *
     *   import static cyclops.Streams.forEach4;
     *
    forEach4(IntStream.range(1,10).boxed(),
    a-> Stream.iterate(a,i->i+1).limit(10),
    (a,b) -> Stream.<Integer>of(a+b),
    (a,b,c) -> Stream.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Stream
     * @param value2 Nested Stream
     * @param value3 Nested Stream
     * @param value4 Nested Stream
     * @param yieldingFunction  Generates a result per combination
     * @return Stream with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Stream<R> forEach4(Stream<? extends T1> value1,
                                                               Function<? super T1, ? extends Stream<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends Stream<R2>> value3,
                                                               Fn3<? super T1, ? super R1, ? super R2, ? extends Stream<R3>> value4,
                                                               Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Stream<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Stream<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

    /**
     * Perform a For Comprehension over a Stream, accepting 3 generating function.
     * This results in a four level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     *  import static com.aol.cyclops2.reactor.Streames.forEach4;
     *
     *  forEach4(IntStream.range(1,10).boxed(),
    a-> Stream.iterate(a,i->i+1).limit(10),
    (a,b) -> Stream.<Integer>just(a+b),
    (a,b,c) -> Stream.<Integer>just(a+b+c),
    (a,b,c,d) -> a+b+c+d <100,
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     * @param value1 top level Stream
     * @param value2 Nested Stream
     * @param value3 Nested Stream
     * @param value4 Nested Stream
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return Stream with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> Stream<R> forEach4(Stream<? extends T1> value1,
                                                                 Function<? super T1, ? extends Stream<R1>> value2,
                                                                 BiFunction<? super T1, ? super R1, ? extends Stream<R2>> value3,
                                                                 Fn3<? super T1, ? super R1, ? super R2, ? extends Stream<R3>> value4,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
                                                                 Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Stream<R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {
                    Stream<R3> c = value4.apply(in,ina,inb);
                    return c.filter(in2->filterFunction.apply(in,ina,inb,in2))
                            .map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });
    }

    /**
     * Perform a For Comprehension over a Stream, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     * import static Streams.forEach3;
     *
     * forEach(IntStream.range(1,10).boxed(),
    a-> Stream.iterate(a,i->i+1).limit(10),
    (a,b) -> Stream.<Integer>of(a+b),
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     *
     * @param value1 top level Stream
     * @param value2 Nested Stream
     * @param value3 Nested Stream
     * @param yieldingFunction Generates a result per combination
     * @return Stream with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> Stream<R> forEach3(Stream<? extends T1> value1,
                                                         Function<? super T1, ? extends Stream<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Stream<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Stream<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });


    }

    /**
     * Perform a For Comprehension over a Stream, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     * <pre>
     * {@code
     *
     * import static Streams.forEach;
     *
     * forEach(IntStream.range(1,10).boxed(),
    a-> Stream.iterate(a,i->i+1).limit(10),
    (a,b) -> Stream.<Integer>of(a+b),
    (a,b,c) ->a+b+c<10,
    Tuple::tuple)
    .toStreamX();
     * }
     * </pre>
     *
     * @param value1 top level Stream
     * @param value2 Nested publisher
     * @param value3 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T1, T2, R1, R2, R> Stream<R> forEach3(Stream<? extends T1> value1,
                                                         Function<? super T1, ? extends Stream<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends Stream<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, Boolean> filterFunction,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Stream<R2> b = value3.apply(in,ina);
                return b.filter(in2->filterFunction.apply(in,ina,in2))
                        .map(in2 -> yieldingFunction.apply(in, ina, in2));
            });



        });
    }

    /**
     * Perform a For Comprehension over a Stream, accepting an additonal generating function.
     * This results in a two level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     *  import static Streams.forEach2;
     *  forEach(IntStream.range(1, 10).boxed(),
     *          i -> Stream.range(i, 10), Tuple::tuple)
    .forEach(System.out::println);

    //(1, 1)
    (1, 2)
    (1, 3)
    (1, 4)
    ...
     *
     * }</pre>
     *
     * @param value1 top level Stream
     * @param value2 Nested publisher
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Stream<R> forEach2(Stream<? extends T> value1,
                                                Function<? super T, Stream<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });

    }

    /**
     *
     * <pre>
     * {@code
     *
     *   import static Streams.forEach2;
     *
     *   forEach(IntStream.range(1, 10).boxed(),
     *           i -> Stream.range(i, 10),
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
     * @param value1 top level Stream
     * @param value2 Nested publisher
     * @param filterFunction A filtering function, keeps values where the predicate holds
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> Stream<R> forEach2(Stream<? extends T> value1,
                                                Function<? super T, ? extends Stream<R1>> value2,
                                                BiFunction<? super T, ? super R1, Boolean> filterFunction,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.flatMap(in -> {

            Stream<R1> a = value2.apply(in);
            return a.filter(in2->filterFunction.apply(in,in2))
                    .map(in2 -> yieldingFunction.apply(in,  in2));
        });
    }
    public static <T> Active<stream,T> allTypeclasses(Stream<T> array){
        return Active.of(widen(array), Streams.Instances.definitions());
    }
    public static <T,W2,R> Nested<stream,W2,R> mapM(Stream<T> array, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        Stream<Higher<W2, R>> e = array.map(fn);
        StreamKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, Streams.Instances.definitions(), defs);
    }
    /**
     * Companion class for creating Type Class instances for working with Streams
     * @author johnmcclean
     *
     */
    @UtilityClass
    public static class Instances {

        public static InstanceDefinitions<stream> definitions() {
            return new InstanceDefinitions<stream>() {

                @Override
                public <T, R> Functor<stream> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<stream> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<stream> applicative() {
                    return Instances.zippingApplicative();
                }

                @Override
                public <T, R> Monad<stream> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<stream>> monadZero() {
                    return Maybe.just(Instances.monadZero());
                }

                @Override
                public <T> Maybe<MonadPlus<stream>> monadPlus() {
                    return Maybe.just(Instances.monadPlus());
                }

                @Override
                public <T> MonadRec<stream> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<stream>> monadPlus(Monoid<Higher<stream, T>> m) {
                    return Maybe.just(Instances.monadPlus(m));
                }

                @Override
                public <C2, T> Traverse<stream> traverse() {
                    return Instances.traverse();
                }

                @Override
                public <T> Foldable<stream> foldable() {
                    return Instances.foldable();
                }

                @Override
                public <T> Maybe<Comonad<stream>> comonad() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<Unfoldable<stream>> unfoldable() {
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
         *  StreamKind<Integer> list = Streams.functor().map(i->i*2, StreamKind.widen(Arrays.asStream(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with Streams
         * <pre>
         * {@code
         *   StreamKind<Integer> list = Streams.unit()
        .unit("hello")
        .then(h->Streams.functor().map((String v) ->v.length(), h))
        .convert(StreamKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for Streams
         */
        public static <T,R>Functor<stream> functor(){
            BiFunction<StreamKind<T>,Function<? super T, ? extends R>,StreamKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * StreamKind<String> list = Streams.unit()
        .unit("hello")
        .convert(StreamKind::narrowK);

        //Arrays.asStream("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for Streams
         */
        public static <T> Pure<stream> unit(){
            return General.<stream,T>unit(StreamKind::just);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.StreamKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Arrays.asStream;
         *
        Streams.zippingApplicative()
        .ap(widen(asStream(l1(this::multiplyByTwo))),widen(asStream(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * StreamKind<Function<Integer,Integer>> listFn =Streams.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(StreamKind::narrowK);

        StreamKind<Integer> list = Streams.unit()
        .unit("hello")
        .then(h->Streams.functor().map((String v) ->v.length(), h))
        .then(h->Streams.zippingApplicative().ap(listFn, h))
        .convert(StreamKind::narrowK);

        //Arrays.asStream("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for Streams
         */
        public static <T,R> Applicative<stream> zippingApplicative(){
            BiFunction<StreamKind< Function<T, R>>,StreamKind<T>,StreamKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.StreamKind.widen;
         * StreamKind<Integer> list  = Streams.monad()
        .flatMap(i->widen(StreamX.range(0,i)), widen(Arrays.asStream(1,2,3)))
        .convert(StreamKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    StreamKind<Integer> list = Streams.unit()
        .unit("hello")
        .then(h->Streams.monad().flatMap((String v) ->Streams.unit().unit(v.length()), h))
        .convert(StreamKind::narrowK);

        //Arrays.asStream("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for Streams
         */
        public static <T,R> Monad<stream> monad(){

            BiFunction<Higher<stream,T>,Function<? super T, ? extends Higher<stream,R>>,Higher<stream,R>> flatMap = Instances::flatMap;
            return General.monad(zippingApplicative(), flatMap);
        }
        /**
         *
         * <pre>
         * {@code
         *  StreamKind<String> list = Streams.unit()
        .unit("hello")
        .then(h->Streams.monadZero().filter((String t)->t.startsWith("he"), h))
        .convert(StreamKind::narrowK);

        //Arrays.asStream("hello"));
         *
         * }
         * </pre>
         *
         *
         * @return A filterable monad (with default value)
         */
        public static <T,R> MonadZero<stream> monadZero(){

            return General.monadZero(monad(), widen(Stream.empty()));
        }
        /**
         * <pre>
         * {@code
         *  StreamKind<Integer> list = Streams.<Integer>monadPlus()
        .plus(StreamKind.widen(Arrays.asStream()), StreamKind.widen(Arrays.asStream(10)))
        .convert(StreamKind::narrowK);
        //Arrays.asStream(10))
         *
         * }
         * </pre>
         * @return Type class for combining Streams by concatenation
         */
        public static <T> MonadPlus<stream> monadPlus(){
            Monoid<StreamKind<T>> m = Monoid.of(widen(Stream.empty()), Instances::concat);
            Monoid<Higher<stream,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        /**
         *
         * <pre>
         * {@code
         *  Monoid<StreamKind<Integer>> m = Monoid.of(StreamKind.widen(Arrays.asStream()), (a,b)->a.isEmpty() ? b : a);
        StreamKind<Integer> list = Streams.<Integer>monadPlus(m)
        .plus(StreamKind.widen(Arrays.asStream(5)), StreamKind.widen(Arrays.asStream(10)))
        .convert(StreamKind::narrowK);
        //Arrays.asStream(5))
         *
         * }
         * </pre>
         *
         * @param m Monoid to use for combining Streams
         * @return Type class for combining Streams
         */
        public static <T> MonadPlus<stream> monadPlus(Monoid<Higher<stream,T>> m){
            Monoid<Higher<stream,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }
        public static <T> MonadPlus<stream> monadPlusK(Monoid<StreamKind<T>> m){
            Monoid<Higher<stream,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<stream> traverse(){

            BiFunction<Applicative<C2>,StreamKind<Higher<C2, T>>,Higher<C2, StreamKind<T>>> sequenceFn = (ap, list) -> {

                Higher<C2,StreamKind<T>> identity = ap.unit(widen(Stream.empty()));

                BiFunction<Higher<C2,StreamKind<T>>,Higher<C2,T>,Higher<C2,StreamKind<T>>> combineToStream =   (acc, next) -> ap.apBiFn(ap.unit((a, b) -> widen(StreamKind.narrow(a).append(b))),
                        acc,next);

                BinaryOperator<Higher<C2,StreamKind<T>>> combineStreams = (a, b)-> ap.apBiFn(ap.unit((l1, l2)-> widen(StreamKind.narrow(l1).appendAll(l2))),a,b); ;

                return ReactiveSeq.fromIterable(StreamKind.narrow(list))
                        .reduce(identity,
                                combineToStream,
                                combineStreams);


            };
            BiFunction<Applicative<C2>,Higher<stream,Higher<C2, T>>,Higher<C2, Higher<stream,T>>> sequenceNarrow  =
                    (a,b) -> StreamKind.widen2(sequenceFn.apply(a, narrowK(b)));
            return General.traverse(zippingApplicative(), sequenceNarrow);
        }

        /**
         *
         * <pre>
         * {@code
         * int sum  = Streams.foldable()
        .foldLeft(0, (a,b)->a+b, StreamKind.widen(Arrays.asStream(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<stream> foldable(){
            return new Foldable<stream>() {
                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<stream, T> ds) {
                    return Streams.foldRight(narrowK(ds),monoid.zero(),monoid);
                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<stream, T> ds) {
                    return narrowK(ds).foldLeft(monoid.zero(),monoid);
                }

                @Override
                public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<stream, T> nestedA) {
                    return Streams.foldRight(narrowK(nestedA),mb.zero(),(a,b)->mb.apply(b,fn.apply(a)));
                }
            };
        }

        public static <T> MonadRec<stream> monadRec(){
            return new MonadRec<stream>(){

                @Override
                public <T, R> Higher<stream, R> tailRec(T initial, Function<? super T, ? extends Higher<stream, ? extends Xor<T, R>>> fn) {
                    return widen(Streams.tailRecXor(initial,fn.andThen(StreamKind::narrowK)));
                }
            };
        }

        private static  <T> StreamKind<T> concat(StreamKind<T> l1, StreamKind<T> l2){

            return widen(l1.appendAll(StreamKind.narrow(l2)));

        }

        private static <T,R> StreamKind<R> ap(StreamKind<Function< T, R>> lt, StreamKind<T> list){
            return widen(FromCyclopsReact.fromStream(ReactiveSeq.fromIterable(lt).zip(list, (a, b)->a.apply(b))).toStream());
        }
        private static <T,R> Higher<stream,R> flatMap(Higher<stream,T> lt, Function<? super T, ? extends  Higher<stream,R>> fn){
            return widen(StreamKind.narrow(lt).flatMap(fn.andThen(StreamKind::narrow)));
        }
        private static <T,R> StreamKind<R> map(StreamKind<T> lt, Function<? super T, ? extends R> fn){
            return widen(StreamKind.narrow(lt).map(in->fn.apply(in)));
        }
        public static Unfoldable<stream> unfoldable(){
            return new Unfoldable<stream>() {
                @Override
                public <R, T> Higher<stream, R> unfold(T b, Function<? super T, Optional<Tuple2<R, T>>> fn) {
                    return widen(Stream.ofAll((Iterable)ReactiveSeq.unfold(b,fn)));

                }
            };
        }
    }

    public static interface StreamNested{


        public static <T> Nested<stream,lazy,T> lazy(Stream<Lazy<T>> type){
            return Nested.of(widen(type.map(LazyKind::widen)),Instances.definitions(),Lazys.Instances.definitions());
        }
        public static <T> Nested<stream,tryType,T> streamTry(Stream<Try<T>> type){
            return Nested.of(widen(type.map(TryKind::widen)),Instances.definitions(),Trys.Instances.definitions());
        }
        public static <T> Nested<stream,VavrWitness.future,T> future(Stream<Future<T>> type){
            return Nested.of(widen(type.map(FutureKind::widen)),Instances.definitions(),Futures.Instances.definitions());
        }
        public static <T> Nested<stream,queue,T> queue(Stream<Queue<T>> nested){
            return Nested.of(widen(nested.map(QueueKind::widen)),Instances.definitions(),Queues.Instances.definitions());
        }
        public static <L, R> Nested<stream,Higher<VavrWitness.either,L>, R> either(Stream<Either<L, R>> nested){
            return Nested.of(widen(nested.map(EitherKind::widen)),Instances.definitions(),Eithers.Instances.definitions());
        }
        public static <T> Nested<stream,VavrWitness.stream,T> stream(Stream<Stream<T>> nested){
            return Nested.of(widen(nested.map(StreamKind::widen)),Instances.definitions(),Streams.Instances.definitions());
        }
        public static <T> Nested<stream,VavrWitness.list,T> list(Stream<List<T>> nested){
            return Nested.of(widen(nested.map(ListKind::widen)), Instances.definitions(),Lists.Instances.definitions());
        }
        public static <T> Nested<stream,array,T> array(Stream<Array<T>> nested){
            return Nested.of(widen(nested.map(ArrayKind::widen)),Instances.definitions(),Arrays.Instances.definitions());
        }
        public static <T> Nested<stream,vector,T> vector(Stream<Vector<T>> nested){
            return Nested.of(widen(nested.map(VectorKind::widen)),Instances.definitions(),Vectors.Instances.definitions());
        }
        public static <T> Nested<stream,hashSet,T> set(Stream<HashSet<T>> nested){
            return Nested.of(widen(nested.map(HashSetKind::widen)),Instances.definitions(), HashSets.Instances.definitions());
        }

        public static <T> Nested<stream,reactiveSeq,T> reactiveSeq(Stream<ReactiveSeq<T>> nested){
            StreamKind<ReactiveSeq<T>> x = widen(nested);
            StreamKind<Higher<reactiveSeq,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<stream,maybe,T> maybe(Stream<Maybe<T>> nested){
            StreamKind<Maybe<T>> x = widen(nested);
            StreamKind<Higher<maybe,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<stream,eval,T> eval(Stream<Eval<T>> nested){
            StreamKind<Eval<T>> x = widen(nested);
            StreamKind<Higher<eval,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<stream,Witness.future,T> cyclopsFuture(Stream<cyclops.async.Future<T>> nested){
            StreamKind<cyclops.async.Future<T>> x = widen(nested);
            StreamKind<Higher<Witness.future,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<stream,Higher<xor,S>, P> xor(Stream<Xor<S, P>> nested){
            StreamKind<Xor<S, P>> x = widen(nested);
            StreamKind<Higher<Higher<xor,S>, P>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),Xor.Instances.definitions());
        }
        public static <S,T> Nested<stream,Higher<reader,S>, T> reader(Stream<Reader<S, T>> nested, S defaultValue){
            StreamKind<Reader<S, T>> x = widen(nested);
            StreamKind<Higher<Higher<reader,S>, T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions(defaultValue));
        }
        public static <S extends Throwable, P> Nested<stream,Higher<Witness.tryType,S>, P> cyclopsTry(Stream<cyclops.control.Try<P, S>> nested){
            StreamKind<cyclops.control.Try<P, S>> x = widen(nested);
            StreamKind<Higher<Higher<Witness.tryType,S>, P>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<stream,optional,T> streamal(Stream<Optional<T>> nested){
            StreamKind<Optional<T>> x = widen(nested);
            StreamKind<Higher<optional,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(), Optionals.Instances.definitions());
        }
        public static <T> Nested<stream,completableFuture,T> completableStream(Stream<CompletableFuture<T>> nested){
            StreamKind<CompletableFuture<T>> x = widen(nested);
            StreamKind<Higher<completableFuture,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<stream,Witness.stream,T> javaStream(Stream<java.util.stream.Stream<T>> nested){
            StreamKind<java.util.stream.Stream<T>> x = widen(nested);
            StreamKind<Higher<Witness.stream,T>> y = (StreamKind)x;
            return Nested.of(y,Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }




    }

    public static interface NestedStream{
        public static <T> Nested<reactiveSeq,stream,T> reactiveSeq(ReactiveSeq<Stream<T>> nested){
            ReactiveSeq<Higher<stream,T>> x = nested.map(StreamKind::widenK);
            return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
        }

        public static <T> Nested<maybe,stream,T> maybe(Maybe<Stream<T>> nested){
            Maybe<Higher<stream,T>> x = nested.map(StreamKind::widenK);

            return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<eval,stream,T> eval(Eval<Stream<T>> nested){
            Eval<Higher<stream,T>> x = nested.map(StreamKind::widenK);

            return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.future,stream,T> cyclopsFuture(cyclops.async.Future<Stream<T>> nested){
            cyclops.async.Future<Higher<stream,T>> x = nested.map(StreamKind::widenK);

            return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
        }
        public static <S, P> Nested<Higher<xor,S>,stream, P> xor(Xor<S, Stream<P>> nested){
            Xor<S, Higher<stream,P>> x = nested.map(StreamKind::widenK);

            return Nested.of(x,Xor.Instances.definitions(),Instances.definitions());
        }
        public static <S,T> Nested<Higher<reader,S>,stream, T> reader(Reader<S, Stream<T>> nested, S defaultValue){

            Reader<S, Higher<stream, T>>  x = nested.map(StreamKind::widenK);

            return Nested.of(x,Reader.Instances.definitions(defaultValue),Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,stream, P> cyclopsTry(cyclops.control.Try<Stream<P>, S> nested){
            cyclops.control.Try<Higher<stream,P>, S> x = nested.map(StreamKind::widenK);

            return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<optional,stream,T> streamal(Optional<Stream<T>> nested){
            Optional<Higher<stream,T>> x = nested.map(StreamKind::widenK);

            return  Nested.of(Optionals.OptionalKind.widen(x), Optionals.Instances.definitions(), Instances.definitions());
        }
        public static <T> Nested<completableFuture,stream,T> completableStream(CompletableFuture<Stream<T>> nested){
            CompletableFuture<Higher<stream,T>> x = nested.thenApply(StreamKind::widenK);

            return Nested.of(CompletableFutures.CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.stream,stream,T> javaStream(java.util.stream.Stream<Stream<T>> nested){
            java.util.stream.Stream<Higher<stream,T>> x = nested.map(StreamKind::widenK);

            return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
        }
    }
}
