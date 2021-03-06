package cyclops.companion.functionaljava;

import com.aol.cyclops.functionaljava.hkt.*;
import cyclops.collections.mutable.ListX;
import cyclops.companion.CompletableFutures;
import cyclops.companion.CompletableFutures.CompletableFutureKind;
import cyclops.companion.Optionals;
import cyclops.companion.Optionals.OptionalKind;
import cyclops.companion.functionaljava.functionaljava.NonEmptyListSupplier;
import cyclops.control.Eval;
import cyclops.control.Maybe;
import cyclops.control.Reader;
import cyclops.control.Xor;
import cyclops.conversion.functionaljava.FromJDK;
import cyclops.conversion.functionaljava.FromJooqLambda;
import cyclops.monads.*;
import cyclops.monads.FJWitness.list;
import cyclops.monads.FJWitness.nonEmptyList;
import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.types.anyM.AnyMSeq;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.FJWitness.option;
import cyclops.monads.Witness.*;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.*;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.instances.General;
import cyclops.typeclasses.monad.*;
import fj.F;
import fj.P2;
import fj.data.Either;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Option;
import io.vavr.collection.Array;
import lombok.experimental.UtilityClass;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static com.aol.cyclops.functionaljava.hkt.NonEmptyListKind.narrow;
import static com.aol.cyclops.functionaljava.hkt.NonEmptyListKind.narrowK;
import static com.aol.cyclops.functionaljava.hkt.NonEmptyListKind.widen;
import static javafx.scene.input.KeyCode.G;


public class NonEmptyLists {

    public static  <W1,T> Coproduct<W1,nonEmptyList,T> coproduct(NonEmptyList<T> list, InstanceDefinitions<W1> def1){
        return Coproduct.of(Xor.primary(widen(list)),def1, Instances.definitions());
    }

    public static  <W1 extends WitnessType<W1>,T> XorM<W1,nonEmptyList,T> xorM(NonEmptyList<T> type){
        return XorM.right(anyM(type));
    }
    public static <T> AnyMSeq<nonEmptyList,T> anyM(NonEmptyList<T> option) {
        return AnyM.ofSeq(option, nonEmptyList.INSTANCE);
    }
    public static  <T,R> NonEmptyList<R> tailRec(NonEmptyList<R> defaultValue,T initial, Function<? super T, ? extends NonEmptyList<? extends Either<T, R>>> fn) {
        NonEmptyList<Either<T, R>> next = NonEmptyList.nel(Either.left(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.bind(e -> e.either(s -> {
                        newValue[0]=true;
                        return (NonEmptyList<Either<T,R>>)fn.apply(s);
                    },
                    p -> {
                        newValue[0]=false;
                        return NonEmptyList.nel(e);
                    }));
            if(!newValue[0])
                break;

        }


        return NonEmptyList.fromList(next.toList().filter(Either::isRight).map(e -> e.right()
                .iterator()
                .next())).orSome(defaultValue);
    }
    public static  <T,R> NonEmptyList<R> tailRecXor(NonEmptyList<R> defaultValue, T initial, Function<? super T, ? extends NonEmptyList<? extends Xor<T, R>>> fn) {
        NonEmptyList<Xor<T, R>> next = NonEmptyList.nel(Xor.secondary(initial));

        boolean newValue[] = {true};
        for(;;){

            next = next.bind(e -> e.visit(s -> {

                        newValue[0]=true;
                        return (NonEmptyList<Xor<T,R>>)fn.apply(s);


                    },
                    p -> {
                        Xor<T, R> x = e;
                        newValue[0]=false;
                        return NonEmptyList.nel(x);
                    }));
            if(!newValue[0])
                break;

        }

        return  NonEmptyList.fromList(next.toList().filter(Xor::isPrimary).map(Xor::get)).orSome(defaultValue);
    }
    /**
     * Perform a For Comprehension over a NonEmptyList, accepting 3 generating functions.
     * This results in a four level nested internal iteration over the provided Publishers.
     *
     *  <pre>
     * {@code
     *
     *   import static cyclops.NonEmptyLists.forEach4;
     *
    forEach4(IntNonEmptyList.range(1,10).boxed(),
    a-> NonEmptyList.iterate(a,i->i+1).limit(10),
    (a,b) -> NonEmptyList.<Integer>of(a+b),
    (a,b,c) -> NonEmptyList.<Integer>just(a+b+c),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level NonEmptyList
     * @param value2 Nested NonEmptyList
     * @param value3 Nested NonEmptyList
     * @param value4 Nested NonEmptyList
     * @param yieldingFunction  Generates a result per combination
     * @return NonEmptyList with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, T3, R1, R2, R3, R> NonEmptyList<R> forEach4(NonEmptyList<? extends T1> value1,
                                                               Function<? super T1, ? extends NonEmptyList<R1>> value2,
                                                               BiFunction<? super T1, ? super R1, ? extends NonEmptyList<R2>> value3,
                                                               Fn3<? super T1, ? super R1, ? super R2, ? extends NonEmptyList<R3>> value4,
                                                               Fn4<? super T1, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {


        return value1.bind(in -> {

            NonEmptyList<R1> a = value2.apply(in);
            return a.bind(ina -> {
                NonEmptyList<R2> b = value3.apply(in,ina);
                return b.bind(inb -> {
                    NonEmptyList<R3> c = value4.apply(in,ina,inb);
                    return c.map(in2 -> yieldingFunction.apply(in, ina, inb, in2));
                });

            });

        });

    }

  

    /**
     * Perform a For Comprehension over a NonEmptyList, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     * import static NonEmptyLists.forEach3;
     *
     * forEach(IntNonEmptyList.range(1,10).boxed(),
    a-> NonEmptyList.iterate(a,i->i+1).limit(10),
    (a,b) -> NonEmptyList.<Integer>of(a+b),
    Tuple::tuple);
     *
     * }
     * </pre>
     *
     *
     * @param value1 top level NonEmptyList
     * @param value2 Nested NonEmptyList
     * @param value3 Nested NonEmptyList
     * @param yieldingFunction Generates a result per combination
     * @return NonEmptyList with an element per combination of nested publishers generated by the yielding function
     */
    public static <T1, T2, R1, R2, R> NonEmptyList<R> forEach3(NonEmptyList<? extends T1> value1,
                                                         Function<? super T1, ? extends NonEmptyList<R1>> value2,
                                                         BiFunction<? super T1, ? super R1, ? extends NonEmptyList<R2>> value3,
                                                         Fn3<? super T1, ? super R1, ? super R2, ? extends R> yieldingFunction) {

        return value1.bind(in -> {

            NonEmptyList<R1> a = value2.apply(in);
            return a.bind(ina -> {
                NonEmptyList<R2> b = value3.apply(in,ina);
                return b.map(in2 -> yieldingFunction.apply(in, ina, in2));
            });


        });


    }

   

    /**
     * Perform a For Comprehension over a NonEmptyList, accepting an additonal generating function.
     * This results in a two level nested internal iteration over the provided Publishers.
     *
     * <pre>
     * {@code
     *
     *  import static NonEmptyLists.forEach2;
     *  forEach(IntNonEmptyList.range(1, 10).boxed(),
     *          i -> NonEmptyList.range(i, 10), Tuple::tuple)
    .forEach(System.out::println);

    //(1, 1)
    (1, 2)
    (1, 3)
    (1, 4)
    ...
     *
     * }</pre>
     *
     * @param value1 top level NonEmptyList
     * @param value2 Nested publisher
     * @param yieldingFunction Generates a result per combination
     * @return
     */
    public static <T, R1, R> NonEmptyList<R> forEach2(NonEmptyList<? extends T> value1,
                                                Function<? super T, NonEmptyList<R1>> value2,
                                                BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {


        return value1.bind(in -> {

            NonEmptyList<R1> a = value2.apply(in);
            return a.map(in2 -> yieldingFunction.apply(in,  in2));
        });

    }

    public static <T> Active<nonEmptyList,T> allTypeclasses(NonEmptyList<T> array){
        return Active.of(widen(array), NonEmptyLists.Instances.definitions());
    }
    public static <T,W2,R> Nested<nonEmptyList,W2,R> mapM(NonEmptyList<T> array, Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        NonEmptyList<Higher<W2, R>> e = array.map(i->fn.apply(i));
        NonEmptyListKind<Higher<W2, R>> lk = widen(e);
        return Nested.of(lk, Instances.definitions(), defs);
    }
    /**
     * Companion class for creating Type Class instances for working with NonEmptyLists
     * @author johnmcclean
     *
     */
    @UtilityClass
    public static class Instances {

        public static InstanceDefinitions<nonEmptyList> definitions() {
            return new InstanceDefinitions<nonEmptyList>() {

                @Override
                public <T, R> Functor<nonEmptyList> functor() {
                    return Instances.functor();
                }

                @Override
                public <T> Pure<nonEmptyList> unit() {
                    return Instances.unit();
                }

                @Override
                public <T, R> Applicative<nonEmptyList> applicative() {
                    return Instances.zippingApplicative();
                }

                @Override
                public <T, R> Monad<nonEmptyList> monad() {
                    return Instances.monad();
                }

                @Override
                public <T, R> Maybe<MonadZero<nonEmptyList>> monadZero() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<MonadPlus<nonEmptyList>> monadPlus() {
                    return Maybe.none();
                }

                @Override
                public <T> MonadRec<nonEmptyList> monadRec() {
                    return Instances.monadRec();
                }

                @Override
                public <T> Maybe<MonadPlus<nonEmptyList>> monadPlus(Monoid<Higher<nonEmptyList, T>> m) {
                    return Maybe.none();
                }

                @Override
                public <C2, T> Traverse<nonEmptyList> traverse() {
                    return Instances.traverse();
                }

                @Override
                public <T> Foldable<nonEmptyList> foldable() {
                    return Instances.foldable();
                }

                @Override
                public <T> Maybe<Comonad<nonEmptyList>> comonad() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<Unfoldable<nonEmptyList>> unfoldable() {
                    return Maybe.none();
                }
            };
        }
        /**
         *
         * Transform a list, mulitplying every element by 2
         *
         * <pre>
         * {@code
         *  NonEmptyListKind<Integer> list = NonEmptyLists.functor().map(i->i*2, NonEmptyListKind.widen(Arrays.asNonEmptyList(1,2,3));
         *
         *  //[2,4,6]
         *
         *
         * }
         * </pre>
         *
         * An example fluent api working with NonEmptyLists
         * <pre>
         * {@code
         *   NonEmptyListKind<Integer> list = NonEmptyLists.unit()
        .unit("hello")
        .then(h->NonEmptyLists.functor().map((String v) ->v.length(), h))
        .convert(NonEmptyListKind::narrowK);
         *
         * }
         * </pre>
         *
         *
         * @return A functor for NonEmptyLists
         */
        public static <T,R>Functor<nonEmptyList> functor(){
            BiFunction<NonEmptyListKind<T>,Function<? super T, ? extends R>,NonEmptyListKind<R>> map = Instances::map;
            return General.functor(map);
        }
        /**
         * <pre>
         * {@code
         * NonEmptyListKind<String> list = NonEmptyLists.unit()
        .unit("hello")
        .convert(NonEmptyListKind::narrowK);

        //Arrays.asNonEmptyList("hello"))
         *
         * }
         * </pre>
         *
         *
         * @return A factory for NonEmptyLists
         */
        public static <T> Pure<nonEmptyList> unit(){
            return General.<nonEmptyList,T>unit(Instances::of);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.NonEmptyListKind.widen;
         * import static com.aol.cyclops.util.function.Lambda.l1;
         * import static java.util.Arrays.asNonEmptyList;
         *
        NonEmptyLists.zippingApplicative()
        .ap(widen(asNonEmptyList(l1(this::multiplyByTwo))),widen(asNonEmptyList(1,2,3)));
         *
         * //[2,4,6]
         * }
         * </pre>
         *
         *
         * Example fluent API
         * <pre>
         * {@code
         * NonEmptyListKind<Function<Integer,Integer>> listFn =NonEmptyLists.unit()
         *                                                  .unit(Lambda.l1((Integer i) ->i*2))
         *                                                  .convert(NonEmptyListKind::narrowK);

        NonEmptyListKind<Integer> list = NonEmptyLists.unit()
        .unit("hello")
        .then(h->NonEmptyLists.functor().map((String v) ->v.length(), h))
        .then(h->NonEmptyLists.zippingApplicative().ap(listFn, h))
        .convert(NonEmptyListKind::narrowK);

        //Arrays.asNonEmptyList("hello".length()*2))
         *
         * }
         * </pre>
         *
         *
         * @return A zipper for NonEmptyLists
         */
        public static <T,R> Applicative<nonEmptyList> zippingApplicative(){
            BiFunction<NonEmptyListKind< Function<T, R>>,NonEmptyListKind<T>,NonEmptyListKind<R>> ap = Instances::ap;
            return General.applicative(functor(), unit(), ap);
        }
        /**
         *
         * <pre>
         * {@code
         * import static com.aol.cyclops.hkt.jdk.NonEmptyListKind.widen;
         * NonEmptyListKind<Integer> list  = NonEmptyLists.monad()
        .flatMap(i->widen(NonEmptyListX.range(0,i)), widen(Arrays.asNonEmptyList(1,2,3)))
        .convert(NonEmptyListKind::narrowK);
         * }
         * </pre>
         *
         * Example fluent API
         * <pre>
         * {@code
         *    NonEmptyListKind<Integer> list = NonEmptyLists.unit()
        .unit("hello")
        .then(h->NonEmptyLists.monad().flatMap((String v) ->NonEmptyLists.unit().unit(v.length()), h))
        .convert(NonEmptyListKind::narrowK);

        //Arrays.asNonEmptyList("hello".length())
         *
         * }
         * </pre>
         *
         * @return Type class with monad functions for NonEmptyLists
         */
        public static <T,R> Monad<nonEmptyList> monad(){

            BiFunction<Higher<nonEmptyList,T>,Function<? super T, ? extends Higher<nonEmptyList,R>>,Higher<nonEmptyList,R>> flatMap = Instances::flatMap;
            return General.monad(zippingApplicative(), flatMap);
        }




        public static <T> MonadRec<nonEmptyList> monadRec(){
            return new MonadRec<nonEmptyList>() {
                @Override
                public <T, R> Higher<nonEmptyList, R> tailRec(T initial, Function<? super T, ? extends Higher<nonEmptyList, ? extends Xor<T, R>>> fn) {
                    return widen(NonEmptyLists.tailRecXor(null, initial, a->{
                        Higher<nonEmptyList, ? extends Xor<T, R>> r = fn.apply(a);
                        NonEmptyList<? extends Xor<T, R>> x = r.convert(NonEmptyListKind::narrowK).narrow();
                        return x;
                    }));

                };
            };
        }

        public static <T> Traverse<nonEmptyList> traverse(){
            return new Traverse<nonEmptyList>() {
                @Override
                public <C2, T, R> Higher<C2, Higher<nonEmptyList, R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn, Higher<nonEmptyList, T> ds) {

                    NonEmptyListKind<T> nel = narrowK(ds);
                    Traverse<list> traverseList = Lists.Instances.traverse();

                    Eval<Higher<C2, Higher<list, R>>> x = Eval.always(() -> traverseList.traverseA(applicative, fn, ListKind.widen(nel.tail())));

                   return applicative.<R,Higher<list, R>,Higher<nonEmptyList,R>>lazyZip(fn.apply(nel.head()), x,
                                                (a,b) ->  widen(NonEmptyList.nel(a,  ListKind.narrow(b)))).get();

                }

                @Override
                public <C2, T> Higher<C2, Higher<nonEmptyList, T>> sequenceA(Applicative<C2> applicative, Higher<nonEmptyList, Higher<C2, T>> ds) {
                    return traverseA(applicative,Function.identity(),ds);
                }

                @Override
                public <T, R> Higher<nonEmptyList, R> ap(Higher<nonEmptyList, ? extends Function<T, R>> fn, Higher<nonEmptyList, T> apply) {
                    return Instances.zippingApplicative().ap(fn,apply);
                }

                @Override
                public <T> Higher<nonEmptyList, T> unit(T value) {
                    return Instances.unit().unit(value);
                }

                @Override
                public <T, R> Higher<nonEmptyList, R> map(Function<? super T, ? extends R> fn, Higher<nonEmptyList, T> ds) {
                    return Instances.functor().map(fn,ds);
                }
            };
        }
        /**
         *
         * <pre>
         * {@code
         * int sum  = NonEmptyLists.foldable()
        .foldLeft(0, (a,b)->a+b, NonEmptyListKind.widen(Arrays.asNonEmptyList(1,2,3,4)));

        //10
         *
         * }
         * </pre>
         *
         *
         * @return Type class for folding / reduction operations
         */
        public static <T> Foldable<nonEmptyList> foldable(){
            return new Foldable<nonEmptyList>() {
                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<nonEmptyList, T> ds) {
                    return narrow(ds).foldRight1((a,b)->monoid.apply(a,b));
                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<nonEmptyList, T> ds) {
                    return narrow(ds).foldLeft1((a,b)->monoid.apply(a,b));
                }

                @Override
                public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<nonEmptyList, T> nestedA) {
                    NonEmptyList<R> nel = narrow(nestedA).map(a -> fn.apply(a));
                   return nel.foldLeft1((a,b)->mb.apply(a,b));
                }
            };

        }


        private <T> NonEmptyListKind<T> of(T value){
            return NonEmptyListKind.of(value);
        }
        private static <T,R> NonEmptyListKind<R> ap(NonEmptyListKind<Function< T, R>> lt, NonEmptyListKind<T> list){

            return NonEmptyListKind.widen(lt.zipWith(list.narrow().toList(),(a, b)->a.apply(b)));
        }
        private static <T,R> Higher<nonEmptyList,R> flatMap(Higher<nonEmptyList,T> lt, Function<? super T, ? extends  Higher<nonEmptyList,R>> fn){
            return NonEmptyListKind.widen(NonEmptyListKind.narrow(lt).bind(in->fn.andThen(NonEmptyListKind::narrow).apply(in)));
        }
        private static <T,R> NonEmptyListKind<R> map(NonEmptyListKind<T> lt, Function<? super T, ? extends R> fn){
            return NonEmptyListKind.widen(NonEmptyListKind.narrow(lt).map(in->fn.apply(in)));
        }

    }
    public static interface NonEmptyListNested {
        public static <T> Nested<nonEmptyList,nonEmptyList,T> nonEmptyList(NonEmptyList<NonEmptyList<T>> nested){
            NonEmptyList<NonEmptyListKind<T>> f = nested.map(NonEmptyListKind::widen);
            NonEmptyListKind<NonEmptyListKind<T>> x = widen(f);
            NonEmptyListKind<Higher<nonEmptyList,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(), NonEmptyLists.Instances.definitions());
        }
        public static <L,T> Nested<nonEmptyList,Higher<FJWitness.either,L>,T> either(NonEmptyList<Either<L,T>> nested){
            NonEmptyList<EitherKind<L,T>> f = nested.map(EitherKind::widen);
            NonEmptyListKind<EitherKind<L,T>> x = widen(f);
            NonEmptyListKind<Higher<Higher<FJWitness.either,L>,T>> y = (NonEmptyListKind)x;

            return Nested.of(y,Instances.definitions(), Eithers.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,option,T> option(NonEmptyList<Option<T>> nested){
            NonEmptyList<OptionKind<T>> f = nested.map(OptionKind::widen);
            NonEmptyListKind<OptionKind<T>> x = widen(f);
            NonEmptyListKind<Higher<option,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Options.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,FJWitness.stream,T> stream(NonEmptyList<fj.data.Stream<T>> nested){
            NonEmptyList<StreamKind<T>> f = nested.map(StreamKind::widen);
            NonEmptyListKind<StreamKind<T>> x = widen(f);
            NonEmptyListKind<Higher<FJWitness.stream,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.companion.functionaljava.Streams.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,list,T> list(NonEmptyList<List<T>> nested){
            NonEmptyList<ListKind<T>> f = nested.map(ListKind::widen);
            NonEmptyListKind<ListKind<T>> x = widen(f);
            NonEmptyListKind<Higher<list,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Lists.Instances.definitions());
        }

        public static <T> Nested<nonEmptyList,reactiveSeq,T> reactiveSeq(NonEmptyList<ReactiveSeq<T>> nested){
            NonEmptyListKind<ReactiveSeq<T>> x = widen(nested);
            NonEmptyListKind<Higher<reactiveSeq,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),ReactiveSeq.Instances.definitions());
        }

        public static <T> Nested<nonEmptyList,Witness.maybe,T> maybe(NonEmptyList<Maybe<T>> nested){
            NonEmptyListKind<Maybe<T>> x = widen(nested);
            NonEmptyListKind<Higher<Witness.maybe,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Maybe.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,Witness.eval,T> eval(NonEmptyList<Eval<T>> nested){
            NonEmptyListKind<Eval<T>> x = widen(nested);
            NonEmptyListKind<Higher<Witness.eval,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Eval.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,Witness.future,T> future(NonEmptyList<cyclops.async.Future<T>> nested){
            NonEmptyListKind<cyclops.async.Future<T>> x = widen(nested);
            NonEmptyListKind<Higher<Witness.future,T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.async.Future.Instances.definitions());
        }
        public static <S, P> Nested<nonEmptyList,Higher<xor,S>, P> xor(NonEmptyList<Xor<S, P>> nested){
            NonEmptyListKind<Xor<S, P>> x = widen(nested);
            NonEmptyListKind<Higher<Higher<xor,S>, P>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Xor.Instances.definitions());
        }
        public static <S,T> Nested<nonEmptyList,Higher<reader,S>, T> reader(NonEmptyList<Reader<S, T>> nested, S defaultValue){
            NonEmptyListKind<Reader<S, T>> x = widen(nested);
            NonEmptyListKind<Higher<Higher<reader,S>, T>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),Reader.Instances.definitions(defaultValue));
        }
        public static <S extends Throwable, P> Nested<nonEmptyList,Higher<Witness.tryType,S>, P> cyclopsTry(NonEmptyList<cyclops.control.Try<P, S>> nested){
            NonEmptyListKind<cyclops.control.Try<P, S>> x = widen(nested);
            NonEmptyListKind<Higher<Higher<Witness.tryType,S>, P>> y = (NonEmptyListKind)x;
            return Nested.of(y,Instances.definitions(),cyclops.control.Try.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,optional,T> javaOptional(NonEmptyList<Optional<T>> nested){
            NonEmptyList<OptionalKind<T>> f = nested.map(o -> OptionalKind.widen(o));
            NonEmptyListKind<OptionalKind<T>> x = NonEmptyListKind.widen(f);

            NonEmptyListKind<Higher<optional,T>> y = (NonEmptyListKind)x;
            return Nested.of(y, Instances.definitions(), cyclops.companion.Optionals.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,completableFuture,T> javaCompletableFuture(NonEmptyList<CompletableFuture<T>> nested){
            NonEmptyList<CompletableFutureKind<T>> f = nested.map(o -> CompletableFutureKind.widen(o));
            NonEmptyListKind<CompletableFutureKind<T>> x = NonEmptyListKind.widen(f);
            NonEmptyListKind<Higher<completableFuture,T>> y = (NonEmptyListKind)x;
            return Nested.of(y, Instances.definitions(), CompletableFutures.Instances.definitions());
        }
        public static <T> Nested<nonEmptyList,Witness.stream,T> javaStream(NonEmptyList<java.util.stream.Stream<T>> nested){
            NonEmptyList<cyclops.companion.Streams.StreamKind<T>> f = nested.map(o -> cyclops.companion.Streams.StreamKind.widen(o));
            NonEmptyListKind<cyclops.companion.Streams.StreamKind<T>> x = NonEmptyListKind.widen(f);
            NonEmptyListKind<Higher<Witness.stream,T>> y = (NonEmptyListKind)x;
            return Nested.of(y, Instances.definitions(), cyclops.companion.Streams.Instances.definitions());
        }

    }

    public static interface NestedNonEmptyList{
        public static <T> Nested<reactiveSeq,nonEmptyList,T> reactiveSeq(ReactiveSeq<NonEmptyList<T>> nested){
            ReactiveSeq<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);
            return Nested.of(x,ReactiveSeq.Instances.definitions(),Instances.definitions());
        }

        public static <T> Nested<maybe,nonEmptyList,T> maybe(Maybe<NonEmptyList<T>> nested){
            Maybe<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,Maybe.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<eval,nonEmptyList,T> eval(Eval<NonEmptyList<T>> nested){
            Eval<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,Eval.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.future,nonEmptyList,T> future(cyclops.async.Future<NonEmptyList<T>> nested){
            cyclops.async.Future<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,cyclops.async.Future.Instances.definitions(),Instances.definitions());
        }
        public static <S, P> Nested<Higher<xor,S>,nonEmptyList, P> xor(Xor<S, NonEmptyList<P>> nested){
            Xor<S, Higher<nonEmptyList,P>> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,Xor.Instances.definitions(),Instances.definitions());
        }
        public static <S,T> Nested<Higher<reader,S>,nonEmptyList, T> reader(Reader<S, NonEmptyList<T>> nested,S defaultValue){

            Reader<S, Higher<nonEmptyList, T>>  x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,Reader.Instances.definitions(defaultValue),Instances.definitions());
        }
        public static <S extends Throwable, P> Nested<Higher<Witness.tryType,S>,nonEmptyList, P> cyclopsTry(cyclops.control.Try<NonEmptyList<P>, S> nested){
            cyclops.control.Try<Higher<nonEmptyList,P>, S> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(x,cyclops.control.Try.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<optional,nonEmptyList,T> javaNonEmptyListal(Optional<NonEmptyList<T>> nested){
            Optional<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);

            return  Nested.of(OptionalKind.widen(x), cyclops.companion.Optionals.Instances.definitions(), Instances.definitions());
        }
        public static <T> Nested<completableFuture,nonEmptyList,T> javaCompletableFuture(CompletableFuture<NonEmptyList<T>> nested){
            CompletableFuture<Higher<nonEmptyList,T>> x = nested.thenApply(NonEmptyListKind::widenK);

            return Nested.of(CompletableFutureKind.widen(x), CompletableFutures.Instances.definitions(),Instances.definitions());
        }
        public static <T> Nested<Witness.stream,nonEmptyList,T> javaStream(java.util.stream.Stream<NonEmptyList<T>> nested){
            java.util.stream.Stream<Higher<nonEmptyList,T>> x = nested.map(NonEmptyListKind::widenK);

            return Nested.of(cyclops.companion.Streams.StreamKind.widen(x), cyclops.companion.Streams.Instances.definitions(),Instances.definitions());
        }
    }


}
