package cyclops.collections.clojure;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyPOrderedSetX;
import cyclops.collections.immutable.OrderedSetX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.POrderedSet;



import clojure.lang.PersistentList;
import clojure.lang.PersistentTreeSet;
import clojure.lang.PersistentVector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClojureTreeSetX<T> extends AbstractSet<T>implements POrderedSet<T> {

    public static <T> OrderedSetX<T> copyFromCollection(CollectionX<T> vec, Comparator<T> comp) {

        return ClojureTreeSetX.empty(comp)
                .plusAll(vec);

    }
    /**
     * Create a LazyPOrderedSetX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPOrderedSetX
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> fromStream(Stream<T> stream) {
        Reducer<POrderedSet<T>> r = ClojureTreeSetX.<T>toPOrderedSet(Comparator.naturalOrder());
        return new LazyPOrderedSetX<T>(null, ReactiveSeq.fromStream(stream),
                                      r);
    }

    /**
     * Create a LazyPOrderedSetX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range SetX
     */
    public static LazyPOrderedSetX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyPOrderedSetX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range SetX
     */
    public static LazyPOrderedSetX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a SetX
     * 
     * <pre>
     * {@code 
     *  LazyPOrderedSetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return SetX generated by unfolder function
     */
    public static <U, T extends Comparable<? super T>> LazyPOrderedSetX<T> unfold(U seed,
            Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPOrderedSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate SetX elements
     * @return SetX generated from the provided Supplier
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                     .limit(limit));
    }

    /**
     * Create a LazyPOrderedSetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return SetX generated by iterative application
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> iterate(long limit, final T seed,
            final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                     .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * POrderedSet<Integer> q = JSPOrderedSet.<Integer>toPOrderedSet()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for POrderedSet
     */
    public static <T> Reducer<POrderedSet<T>> toPOrderedSet(Comparator<T> ordering) {
        return Reducer.<POrderedSet<T>> of(ClojureTreeSetX.emptyPOrderedSet(ordering),
                                           (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                           (final T x) -> ClojureTreeSetX.singleton(ordering, x));
    }
    /**
     * <pre>
     * {@code 
     * POrderedSet<Integer> q = ClojurePOrderedSet.<Integer>toPOrderedSet()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for POrderedSet
     */
    public static <T extends Comparable<? super T>> Reducer<POrderedSet<T>> toPOrderedSet() {
        return Reducer.<POrderedSet<T>> of(ClojureTreeSetX.emptyPOrderedSet(Comparator.<T>naturalOrder()),
                                           (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                           (final T x) -> ClojureTreeSetX.singleton(Comparator.<T>naturalOrder(), x));
    }

    public static <T> ClojureTreeSetX<T> fromSet(PersistentTreeSet set) {
        return new ClojureTreeSetX<>(
                                            set);
    }

   
    public static <T> ClojureTreeSetX<T> emptyPOrderedSet(Comparator<T> comp) {
        return new ClojureTreeSetX<>(
                PersistentTreeSet.create(comp,
                                         PersistentVector.create()
                                                         .seq()));
    }

   

    public static <T> LazyPOrderedSetX<T> empty(Comparator<T> comp) {
       
        return of(comp);
    }
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> empty() {
        
        return of();
    }

   
    public static <T> LazyPOrderedSetX<T> singleton(Comparator<T> comp, T t) {
        return of(comp, t);
    }
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> singleton(T t) {
        return of( t);
    }

    public static <T> LazyPOrderedSetX<T> of(Comparator<T> comp, T... t) {
        return fromPOrderedSet(new ClojureTreeSetX<>(
                                                                             PersistentTreeSet.create(comp,
                                                                                                      PersistentList.create(Arrays.asList(t)).seq())),
                                                toPOrderedSet(comp));
    }
    private static <T> LazyPOrderedSetX<T> fromPOrderedSet(POrderedSet<T> ordered, Reducer<POrderedSet<T>> reducer) {
        return  new LazyPOrderedSetX<T>(ordered,null,reducer);
    }
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> of(T... t) {

        return of(Comparator.naturalOrder(), t);
    }

    public static <T> LazyPOrderedSetX<T> POrderedSet(PersistentTreeSet q) {
        return fromPOrderedSet(new ClojureTreeSetX<T>(
                                                                              q),
                                                toPOrderedSet(q.comparator()));
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> POrderedSet(Comparator<T> comp,T... elements) {
        return fromPOrderedSet(of(elements), toPOrderedSet(comp));
    }

    @Wither
    private final PersistentTreeSet set;

    @Override
    public ClojureTreeSetX<T> plus(T e) {

        return withSet((PersistentTreeSet) set.cons(e));
    }

    @Override
    public ClojureTreeSetX<T> plusAll(Collection<? extends T> l) {

        PersistentTreeSet vec = set;
        for (T next : l) {
            vec = (PersistentTreeSet) vec.cons(next);
        }

        return withSet(vec);

    }

    @Override
    public POrderedSet<T> minus(Object e) {
        return withSet((PersistentTreeSet)set.disjoin((T) e));

    }

    @Override
    public POrderedSet<T> minusAll(Collection<?> s) {
        PersistentTreeSet use = set;
        for (Object next : s)
            use = (PersistentTreeSet) use.disjoin(next);
        return withSet(use);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public T get(int index) {
        return (T)set.get(index);

    }

    @Override
    public int indexOf(Object o) {
        Iterable<T> it = set;
        return ReactiveSeq.fromIterable(it)
                          .zipWithIndex()
                          .filter(t->t.v1.equals(o))
                          .map(t->t.v2.intValue())
                          .findFirst()
                          .get();
                          
    }

    

    

}
