package cyclops.companion.vavr;

import cyclops.conversion.vavr.FromCyclopsReact;
import cyclops.monads.VavrWitness.stream;
import com.aol.cyclops.vavr.hkt.StreamKind;
import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.types.anyM.AnyMSeq;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.AnyM;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.Pure;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import io.vavr.collection.Stream;
import lombok.experimental.UtilityClass;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;


public class Streams {


   
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
    /**
     * Companion class for creating Type Class instances for working with Streams
     * @author johnmcclean
     *
     */
    @UtilityClass
    public static class Instances {


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

            return General.monadZero(monad(), StreamKind.widen(Stream.empty()));
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
            Monoid<StreamKind<T>> m = Monoid.of(StreamKind.widen(Stream.empty()), Instances::concat);
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
        public static <T> MonadPlus<stream> monadPlus(Monoid<StreamKind<T>> m){
            Monoid<Higher<stream,T>> m2= (Monoid)m;
            return General.monadPlus(monadZero(),m2);
        }

        /**
         * @return Type class for traversables with traverse / sequence operations
         */
        public static <C2,T> Traverse<stream> traverse(){

            BiFunction<Applicative<C2>,StreamKind<Higher<C2, T>>,Higher<C2, StreamKind<T>>> sequenceFn = (ap, list) -> {

                Higher<C2,StreamKind<T>> identity = ap.unit(StreamKind.widen(Stream.empty()));

                BiFunction<Higher<C2,StreamKind<T>>,Higher<C2,T>,Higher<C2,StreamKind<T>>> combineToStream =   (acc, next) -> ap.apBiFn(ap.unit((a, b) -> StreamKind.widen(StreamKind.narrow(a).append(b))),
                        acc,next);

                BinaryOperator<Higher<C2,StreamKind<T>>> combineStreams = (a, b)-> ap.apBiFn(ap.unit((l1, l2)-> StreamKind.widen(StreamKind.narrow(l1).appendAll(l2))),a,b); ;

                return ReactiveSeq.fromIterable(StreamKind.narrow(list))
                        .reduce(identity,
                                combineToStream,
                                combineStreams);


            };
            BiFunction<Applicative<C2>,Higher<stream,Higher<C2, T>>,Higher<C2, Higher<stream,T>>> sequenceNarrow  =
                    (a,b) -> StreamKind.widen2(sequenceFn.apply(a, StreamKind.narrowK(b)));
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
            BiFunction<Monoid<T>,Higher<stream,T>,T> foldRightFn =  (m, l)-> ReactiveSeq.fromIterable(StreamKind.narrow(l)).foldRight(m);
            BiFunction<Monoid<T>,Higher<stream,T>,T> foldLeftFn = (m, l)-> ReactiveSeq.fromIterable(StreamKind.narrow(l)).reduce(m);
            return General.foldable(foldRightFn, foldLeftFn);
        }

        private static  <T> StreamKind<T> concat(StreamKind<T> l1, StreamKind<T> l2){

            return StreamKind.widen(l1.appendAll(StreamKind.narrow(l2)));

        }

        private static <T,R> StreamKind<R> ap(StreamKind<Function< T, R>> lt, StreamKind<T> list){
            return StreamKind.widen(FromCyclopsReact.fromStream(ReactiveSeq.fromIterable(lt).zip(list, (a, b)->a.apply(b))).toStream());
        }
        private static <T,R> Higher<stream,R> flatMap(Higher<stream,T> lt, Function<? super T, ? extends  Higher<stream,R>> fn){
            return StreamKind.widen(StreamKind.narrow(lt).flatMap(fn.andThen(StreamKind::narrow)));
        }
        private static <T,R> StreamKind<R> map(StreamKind<T> lt, Function<? super T, ? extends R> fn){
            return StreamKind.widen(StreamKind.narrow(lt).map(in->fn.apply(in)));
        }
    }



}
