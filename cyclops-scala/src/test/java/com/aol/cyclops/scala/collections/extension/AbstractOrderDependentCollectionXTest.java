package com.aol.cyclops.scala.collections.extension;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.types.stream.HeadAndTail;

import cyclops.collections.immutable.VectorX;
import cyclops.collections.mutable.ListX;
import cyclops.companion.Semigroups;
import cyclops.control.Trampoline;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public abstract class AbstractOrderDependentCollectionXTest extends AbstractCollectionXTest {
    
    @Test
    public void forEach2OD() {

        assertThat(of(1, 2, 3).forEach2(a -> Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), (a , b) -> a + b).toList(),
                equalTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 3, 4, 5, 6, 7, 8,
                        9, 10, 11, 12)));
    }

    @Test
    public void forEach2FilterOD() {

        assertThat(of(1, 2, 3).forEach2(a -> Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), (a , b) -> a > 2 && b < 8,
                (a ,b) -> a + b).toList(), equalTo(Arrays.asList(3, 4, 5, 6, 7, 8, 9, 10)));
    }
        
    @Test
    public void whenNilOrNotJoinWithFirstElement(){
        
        
        String res= of(1,2,3).visit((x,xs)-> xs.join(x>2? "hello" : "world"),()->"EMPTY");
        assertThat(res,equalTo("2world3"));
    }
    @Test
    public void sortedComparator() {
        assertThat(of(1,5,3,4,2).sorted((t1,t2) -> t2-t1).collect(Collectors.toList()),is(Arrays.asList(5,4,3,2,1)));
    }
    @Test
    public void takeRight(){
        assertThat(of(1,2,3).takeRight(1).toList(),hasItems(3));
    }
    @Test
    public void takeRightEmpty(){
        assertThat(of().takeRight(1).toList(),equalTo(Arrays.asList()));
    }
    
    @Test
    public void takeUntil(){
        assertThat(of(1,2,3,4,5).takeUntil(p->p==2).toList().size(),greaterThan(0));
    }
    @Test
    public void takeUntilEmpty(){
        assertThat(of().takeUntil(p->true).toList(),equalTo(Arrays.asList()));
    }
    @Test
    public void takeWhile(){
        assertThat(of(1,2,3,4,5).takeWhile(p->p<6).toList().size(),greaterThan(1));
    }
    @Test
    public void takeWhileEmpty(){
        assertThat(of().takeWhile(p->true).toList(),equalTo(Arrays.asList()));
    } 
    
    @Test
    public void testOnEmptyOrdered() throws X {
        assertEquals(asList(1), of().onEmpty(1).toListX());
        assertEquals(asList(1), of().onEmptyGet(() -> 1).toListX());

        assertEquals(asList(2), of(2).onEmpty(1).toListX());
        assertEquals(asList(2), of(2).onEmptyGet(() -> 1).toListX());
        assertEquals(asList(2), of(2).onEmptyThrow(() -> new X()).toListX());

        assertEquals(asList(2, 3), of(2, 3).onEmpty(1).toListX());
        assertEquals(asList(2, 3), of(2, 3).onEmptyGet(() -> 1).toListX());
        assertEquals(asList(2, 3), of(2, 3).onEmptyThrow(() -> new X()).toListX());
    }
    @Test
    public void testCycle() {
        assertEquals(asList(1, 2, 1, 2, 1, 2),of(1, 2).cycle(3).toListX());
        assertEquals(asList(1, 2, 3, 1, 2, 3), of(1, 2, 3).cycle(2).toListX());
    }
    @Test
    public void testCycleTimes() {
        assertEquals(asList(1, 2, 1, 2, 1, 2),of(1, 2).cycle(3).toListX());
       
    }

    int count =0;
    @Test
    public void testCycleWhile() {
        count =0;
        assertEquals(asList(1, 2,3, 1, 2,3),of(1, 2, 3).cycleWhile(next->count++<6).toListX());
       
    }
    @Test
    public void testCycleUntil() {
        count =0;
        assertEquals(asList(1, 2,3, 1, 2,3),of(1, 2, 3).cycleUntil(next->count++==6).toListX());
       
    }
    @Test
    public void sliding() {
        ListX<VectorX<Integer>> list = of(1, 2, 3, 4, 5, 6).sliding(2).toListX();

        System.out.println(list);
        assertThat(list.get(0), hasItems(1, 2));
        assertThat(list.get(1), hasItems(2, 3));
    }

    @Test
    public void slidingIncrement() {
        List<List<Integer>> list = of(1, 2, 3, 4, 5, 6).sliding(3, 2).collect(Collectors.toList());

        System.out.println(list);
        assertThat(list.get(0), hasItems(1, 2, 3));
        assertThat(list.get(1), hasItems(3, 4, 5));
    }

    @Test
    public void combine(){
        assertThat(of(1,1,2,3)
                   .combine((a, b)->a.equals(b), Semigroups.intSum)
                   .toListX(),equalTo(ListX.of(4,3)));
                   
    }
    @Test
    public void groupedFunction(){
        assertThat(of(1,2,3).grouped(f-> f<3? "a" : "b").count(),equalTo((2L)));
        assertThat(of(1,2,3).grouped(f-> f<3? "a" : "b").filter(t->t.v1.equals("a"))
                        .map(t->t.v2).map(s->s.toList()).singleUnsafe(),
                            equalTo((Arrays.asList(1,2))));
    }
    @Test
    public void groupedFunctionCollector(){
        assertThat(of(1,2,3).grouped(f-> f<3? "a" : "b",Collectors.toList()).count(),equalTo((2L)));
        assertThat(of(1,2,3).grouped(f-> f<3? "a" : "b",Collectors.toList()).filter(t->t.v1.equals("a"))
                .map(t->t.v2).singleUnsafe(),
                    equalTo((Arrays.asList(1,2))));
    }
	@Test
	public void zip3(){
		List<Tuple3<Integer,Integer,Character>> list =
				of(1,2,3,4,5,6).zip3(of(100,200,300,400).stream(),of('a','b','c').stream())
												.toListX();
		
		System.out.println(list);
		List<Integer> right = list.stream().map(t -> t.v2).collect(Collectors.toList());
		assertThat(right,hasItem(100));
		assertThat(right,hasItem(200));
		assertThat(right,hasItem(300));
		assertThat(right,not(hasItem(400)));
		
		List<Integer> left = list.stream().map(t -> t.v1).collect(Collectors.toList());
		assertThat(Arrays.asList(1,2,3,4,5,6),hasItem(left.get(0)));
		
		List<Character> three = list.stream().map(t -> t.v3).collect(Collectors.toList());
		assertThat(Arrays.asList('a','b','c'),hasItem(three.get(0)));
		
		
	}
	@Test
	public void zip4(){
		List<Tuple4<Integer,Integer,Character,String>> list =
				of(1,2,3,4,5,6).zip4(of(100,200,300,400).stream(),of('a','b','c').stream(),of("hello","world").stream())
												.toListX();
		System.out.println(list);
		List<Integer> right = list.stream().map(t -> t.v2).collect(Collectors.toList());
		assertThat(right,hasItem(100));
		assertThat(right,hasItem(200));
		assertThat(right,not(hasItem(300)));
		assertThat(right,not(hasItem(400)));
		
		List<Integer> left = list.stream().map(t -> t.v1).collect(Collectors.toList());
		assertThat(Arrays.asList(1,2,3,4,5,6),hasItem(left.get(0)));
		
		List<Character> three = list.stream().map(t -> t.v3).collect(Collectors.toList());
		assertThat(Arrays.asList('a','b','c'),hasItem(three.get(0)));
	
		List<String> four = list.stream().map(t -> t.v4).collect(Collectors.toList());
		assertThat(Arrays.asList("hello","world"),hasItem(four.get(0)));
		
		
	}
	
	@Test
	public void testIntersperse() {
		
		assertThat(((CollectionX<Integer>)of(1,2,3).intersperse(0)).toListX(),equalTo(Arrays.asList(1,0,2,0,3)));
	



	}


	
	@Test
	public void testOfType() {

		

		assertThat((((CollectionX<Integer>)of(1, "a", 2, "b", 3).ofType(Integer.class))).toListX(),containsInAnyOrder(1, 2, 3));

		assertThat((((CollectionX<Integer>)of(1, "a", 2, "b", 3).ofType(Integer.class))).toListX(),not(containsInAnyOrder("a", "b",null)));

		assertThat(((CollectionX<Serializable>)of(1, "a", 2, "b", 3)

				.ofType(Serializable.class)).toListX(),containsInAnyOrder(1, "a", 2, "b", 3));

	}

	
	private int addOne(Integer i){
		return i+1;
	}
	private int add(Integer a, Integer b){
		return a+b;
	}
	private String concat(String a, String b, String c){
		return a+b+c;
	}
	private String concat4(String a, String b, String c,String d){
		return a+b+c+d;
	}
	private String concat5(String a, String b, String c,String d,String e){
		return a+b+c+d+e;
	}
	


	@Test
    public void allCombinations3() {
        assertThat(of(1, 2, 3).combinations().map(s->s.toList()).toList(),equalTo(Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(2),
        		Arrays.asList(3), Arrays.asList(1, 2), Arrays.asList(1, 3), Arrays.asList(2, 3), Arrays.asList(1, 2, 3))));
    }

	@Test
	public void emptyAllCombinations() {
		assertThat(of().combinations().map(s -> s.toList()).toList(), equalTo(Arrays.asList(Arrays.asList())));
	}
	
	@Test
    public void emptyPermutations() {
        assertThat(of().permutations().map(s->s.toList()).toList(),equalTo(Arrays.asList()));
    }

    @Test
    public void permuations3() {
    	System.out.println(of(1, 2, 3).permutations().map(s->s.toList()).toList());
        assertThat(of(1, 2, 3).permutations().map(s->s.toList()).toList(),
        		equalTo(of(of(1, 2, 3),
        		of(1, 3, 2), of(2, 1, 3), of(2, 3, 1), of(3, 1, 2), of(3, 2, 1)).peek(i->System.out.println("peek - " + i)).map(s->s.toList()).toList()));
    }

	@Test
	public void emptyCombinations() {
		assertThat(of().combinations(2).map(s -> s.toList()).toList(), equalTo(Arrays.asList()));
	}
	   
	 @Test
	public void combinations2() {
	        assertThat(of(1, 2, 3).combinations(2).map(s->s.toList()).toList(),
	                equalTo(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(1, 3), Arrays.asList(2, 3))));
	    }	

    @Test
    public void whenGreaterThan2() {
        String res = of(5, 2, 3).visit((x, xs) -> xs.join(x > 2 ? "hello" : "world"), () -> "boo!");

        assertThat(res, equalTo("2hello3"));
    }

	@Test
	public void headTailReplay() {

		CollectionX<String> helloWorld = of("hello", "world", "last");
		HeadAndTail<String> headAndTail = helloWorld.headAndTail();
		String head = headAndTail.head();
		assertThat(head, equalTo("hello"));

		ReactiveSeq<String> tail = headAndTail.tail();
		assertThat(tail.headAndTail().head(), equalTo("world"));

	} 
	@Test
    public void testScanLeftStringConcat() {
        assertThat(of("a", "b", "c").scanLeft("", String::concat).toList().size(),
        		is(4));
    }
	@Test
	public void batchBySize(){
		System.out.println(of(1,2,3,4,5,6).grouped(3).collect(Collectors.toList()));
		assertThat(of(1,2,3,4,5,6).grouped(3).collect(Collectors.toList()).size(),is(2));
	}
	@Test
	public void testReverse() {
		
		assertThat(of(1, 2, 3).reverse().toList(), equalTo(asList(3, 2, 1)));
	}

	@Test
	public void testFoldRight() {
		Supplier<CollectionX<String>> s = () -> of("a", "b", "c");

		assertTrue(s.get().foldRight("", String::concat).contains("a"));
		assertTrue(s.get().foldRight("", String::concat).contains("b"));
		assertTrue(s.get().foldRight("", String::concat).contains("c"));
		assertEquals(3, (int) s.get().map(str -> str.length()).foldRight(0, (t, u) -> u + t));
	}

	@Test
	public void testFoldLeft() {
		for (int i = 0; i < 100; i++) {
			Supplier<CollectionX<String>> s = () -> of("a", "b", "c");

			assertTrue(s.get().reduce("", String::concat).contains("a"));
			assertTrue(s.get().reduce("", String::concat).contains("b"));
			assertTrue(s.get().reduce("", String::concat).contains("c"));

			assertEquals(3, (int) s.get().map(str -> str.length()).reduce(0, (u, t) -> u + t));

			assertEquals(3, (int) s.get().map(str -> str.length()).foldRight(0, (t, u) -> u + t));
		}
	}
	private Trampoline<Integer> sum(int times, int sum){
        return times ==0 ?  Trampoline.done(sum) : Trampoline.more(()->sum(times-1,sum+times));
    }
    @Test
    public void testTrampoline() {
        assertThat(of(10).trampoline(n ->sum(10,n)),hasItem(65));
    }

}
