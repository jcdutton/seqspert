package clojure.lang;

import static org.junit.Assert.*;
import static clojure.lang.TestUtils.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import clojure.lang.PersistentHashMap.HashCollisionNode;

public class HashCollisionNodeAndHashCollisionNodeSplicerTest implements SplicerTestInterface {

    @Test
    @Override
    public void testNoCollision() {
	// TODO - I guess we create a BIN and put the two HCNs inside it...
    }
    
    final int shift = 0;
    final int hashCode = 2;
    final Object key0 = new HashCodeKey("key0", hashCode);
    final Object key1 = new HashCodeKey("key1", hashCode);
    final Object key2 = new HashCodeKey("key2", hashCode);
    final Object key3 = new HashCodeKey("key3", hashCode);
    final Object value0 = "value0";
    final Object value1 = "value1";
    final Object value2 = "value2";
    final Object value3 = "value3";

    public void test(Object key0, Object value0, Object key1, Object value1,
		     Object key2, Object value2, Object key3, Object value3,
		     int expectedDuplications, boolean same) {
	final HashCollisionNode leftNode   = new HashCollisionNode(null, hashCode, 2, new Object[]{key0, value0, key1, value1});
	final HashCollisionNode rightNode =  new HashCollisionNode(null, hashCode, 2, new Object[]{key1, value1, key2, value2});
	
	final AtomicReference<Thread> edit = new AtomicReference<Thread>();
	final Box addedLeaf = new Box(null);
	final HashCollisionNode expected = (HashCollisionNode) leftNode.
	    assoc(edit, shift, hashCode, key1, value1, addedLeaf).
	    assoc(edit, shift, hashCode, key2, value2, addedLeaf);
	
	final Duplications duplications = new Duplications(0);
	final HashCollisionNode actual =  (HashCollisionNode) NodeUtils.splice(shift, duplications, null, leftNode, 0, null, rightNode);
	//assertEquals(expectedDuplications, duplications.duplications);
	assertHashCollisionNodesEqual(expected, actual);
	if (same) assertSame(expected, actual);
    }
    
    @Test
    @Override
    public void testCollision() {
	// differing keys all have same hashcode but values are different...
	test(key0, value0, key1, value1, key2, value2, key3, value3, 0, false);
    }

    @Test
    @Override
    public void testDuplication() {
	// as above, but one pair of keys is identical...
	final Object leftValue1 = "left-" + (String) value1;
	final Object rightValue1 = "right-" + (String) value1;
	test(key0, value0, key1, leftValue1, key1, rightValue1, key2, value2, 1, false);
    }

    @Test
    //@Override
    public void testSomeIdentical() {
	// as above but one pair of values is also identical...
	test(key0, value0, key1, value1, key1, value1, key2, value2, 1, false);
    }

    @Test
    //@Override
    public void testAllIdentical() {
	// all keys and values is also identical...
	test(key0, value0, key1, value1, key0, value0, key1, value1, 2, true);
    }

}