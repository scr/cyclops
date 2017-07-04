package cyclops.collections.clojure;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.FoldToList;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyLinkedListX;
import com.aol.cyclops2.types.Unwrapable;
import com.aol.cyclops2.types.foldable.Evaluation;
import cyclops.collections.immutable.LinkedListX;
import cyclops.collections.mutable.ListX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;


import clojure.lang.IPersistentList;
import clojure.lang.PersistentList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClojureListX<T> extends AbstractList<T>implements PStack<T>, Unwrapable {

    static final FoldToList gen = (it, i)-> ClojureListX.from(from(it,i));

    public static <T> LinkedListX<T> listX(ReactiveSeq<T> stream){
        return fromStream(stream);
    }
    public static <T> LinkedListX<T> copyFromCollection(CollectionX<T> vec) {
        return fromPStack(new ClojureListX<T>(from(vec.iterator(),0)),toPStack());

    }
    public static <T> LazyLinkedListX<T> from(IPersistentList q) {
        return fromPStack(new ClojureListX<>(q), toPStack());
    }

    private static <E> IPersistentList from(final Iterator<E> i, int depth) {

        if(!i.hasNext())
            return PersistentList.create(Arrays.asList());
        E e = i.next();
        return  (IPersistentList) from(i,depth++).cons(e);
    }
    @Override
    public <R> R unwrap() {
        return (R)list;
    }
    /**
     * Create a LazyLinkedListX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyLinkedListX
     */
    public static <T> LazyLinkedListX<T> fromStream(Stream<T> stream) {
        Reducer<PStack<T>> s = toPStack();
        return new LazyLinkedListX<T>(null,
                                  ReactiveSeq.fromStream(stream), s,gen, Evaluation.LAZY);
    }

    /**
     * Create a LazyLinkedListX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyLinkedListX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyLinkedListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyLinkedListX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyLinkedListX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyLinkedListX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                     .limit(limit));
    }

    /**
     * Create a LazyLinkedListX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyLinkedListX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                     .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * PStack<Integer> q = JSPStack.<Integer>toPStack()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for PStack
     */
    public static <T> Reducer<PStack<T>> toPStack() {
        return Reducer.<PStack<T>> of(ClojureListX.emptyPStack(), (final PStack<T> a) -> b -> a.plusAll(b),
                                      (final T x) -> ClojureListX.singleton(x));
    }

    public static <T> ClojureListX<T> fromList(List<T> list) {
        return new ClojureListX<T>(
                                    PersistentList.create(list));
    }

    public static <T> ClojureListX<T> emptyPStack() {

        return new ClojureListX<T>(
                                    PersistentList.create(Arrays.asList()));
    }

    public static <T> LazyLinkedListX<T> empty() {
        return of();
    }

    public static <T> LazyLinkedListX<T> singleton(T t) {
        return of(t);
    }

    public static <T> LazyLinkedListX<T> of(T... t) {

        return fromPStack(new ClojureListX<T>(
                                                           PersistentList.create(Arrays.asList(t))),
                                      toPStack());
    }

    public static <T> LazyLinkedListX<T> PStack(List<T> q) {
        return fromPStack(new ClojureListX<T>(
                                                           PersistentList.create(q)),
                                      toPStack());
    }

    private static <T> LazyLinkedListX<T> fromPStack(PStack<T> s, Reducer<PStack<T>> pStackReducer) {
        return new LazyLinkedListX<T>(s,null, pStackReducer, gen,Evaluation.LAZY);
    }

    @SafeVarargs
    public static <T> LazyLinkedListX<T> PStack(T... elements) {
        return fromPStack(of(elements), toPStack());
    }

    @Wither
    private final IPersistentList list;

    @Override
    public ClojureListX<T> plus(T e) {
        return withList((IPersistentList) list.cons(e));
    }

    @Override
    public ClojureListX<T> plusAll(Collection<? extends T> l) {

        IPersistentList vec = list;
        for (T next : l) {
            vec = (IPersistentList) vec.cons(next);
        }

        return withList(vec);
    }

    @Override
    public ClojureListX<T> with(int i, T e) {

        if (i < 0 || i > size())
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());

        PersistentList nel = (PersistentList) list;
        if (i == 0) {
            if (head().equals(e))
                return this;
            return withList((IPersistentList) tail().list.cons(e));

        }
        IPersistentList newRest = tail().with(i - 1, e).list;

        if (newRest == nel.next())
            return this;
        return withList((IPersistentList) newRest.cons(head()));

    }

    @Override
    public ClojureListX<T> plus(int i, T e) {
        if (i < 0 || i > size())
            throw new IndexOutOfBoundsException();
        
        if(size()==0){
            return withList((IPersistentList) PersistentList.create(Arrays.asList(e)));
        }
        if( tail().list==null){
            return withList((IPersistentList) PersistentList.create(Arrays.asList(e)).cons(head()));
        }
        if (i == 0) // insert at beginning
            return plus(e);
        return withList((IPersistentList) tail().plus(i - 1, e).list.cons(head()));

    }

    @Override
    public ClojureListX<T> plusAll(int i, Collection<? extends T> l) {

        if (i < 0 || i > size())
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());
        
        if(size()==0){
            return withList((IPersistentList) PersistentList.create(ListX.fromIterable(l)));
        }
        if( tail().list==null){
            return withList((IPersistentList) PersistentList.create(ListX.fromIterable(l)).cons(head()));
        }
        if (i == 0 || tail().list==null)
            return plusAll(l);
        return withList((IPersistentList) tail().plusAll(i - 1, l).list.cons(head()));

    }
    
    

    @Override
    public ClojureListX<T> minus(Object e) {
        if (size() == 0)
            return this;
        PersistentList nel = (PersistentList) list;
        if (head().equals(e)){
            
            IPersistentList res = (IPersistentList)nel.next();
            if(res==null)
                res = PersistentList.create(Arrays.asList());
            return withList(res);
        }

        IPersistentList newRest = tail().minus(e).list;

        if (newRest == ((PersistentList) list).next())
            return this;
       
        return withList((IPersistentList) newRest.cons(nel.first()));

    }
    
    @Override
    public ClojureListX<T> minusAll(Collection<?> l) {
        if (size() == 0)
            return this;
        PersistentList nel = (PersistentList) list;
        if (l.contains(head())){
            IPersistentList res1 = (IPersistentList)nel.next();
            if(res1==null)
                return withList(PersistentList.create(Arrays.asList()));
            return tail().minusAll(l);
        }
       
       
        IPersistentList newRest = tail().minusAll(l).list;
        if (newRest == nel.next())
            return this;
        return withList((IPersistentList) newRest.cons(nel.first()));
    }

    public ClojureListX<T> tail() {
        PersistentList nel = (PersistentList) list;
        return withList((IPersistentList) nel.next());
    }

    public T head() {
        PersistentList nel = (PersistentList) list;
        return (T) nel.first();
    }

    @Override
    public ClojureListX<T> minus(int i) {

        return minus(get(i));
    }

    @Override
    public ClojureListX<T> subList(int start, int end) {

        if (start < 0 || end > size() || start > end)
            throw new IndexOutOfBoundsException(
                                                "Index  is out of bounds - size : " + size());
        if (end == size())
            return subList(start);
        if (start == end)
            return withList(PersistentList.EMPTY);
        if (start == 0) {
            return withList((IPersistentList) tail().subList(0, end - 1).list.cons(head()));
        }

        return tail().subList(start - 1, end - 1);
    }

    @Override
    public T get(int index) {
        PersistentList nel = (PersistentList) list;
        return (T) nel.get(index);
    }

    @Override
    public int size() {
        if (list instanceof PersistentList) {

            PersistentList nel = (PersistentList) list;
            return nel.size();
        }
        return 0;
    }

    @Override
    public ClojureListX<T> subList(int start) {
        return subList(start, size());
    }

}
