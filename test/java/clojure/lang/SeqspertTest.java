package clojure.lang;

import static clojure.lang.TestUtils.assertNodeEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import clojure.lang.PersistentHashMap.ArrayNode;
import clojure.lang.PersistentHashMap.BitmapIndexedNode;
import clojure.lang.PersistentHashMap.HashCollisionNode;
import clojure.lang.PersistentHashMap.INode;
import clojure.lang.TestUtils.Hasher;

public class SeqspertTest {

    @Test
    public void testConstructor() {
        new Seqspert();
    }

    @Test
    public void testCreatePersistentVector() {
        Seqspert.createPersistentVector(0, 0, null, null);
    }

    @Test
    public void testCreatePersistentHashMap() {
        Seqspert.createPersistentHashMap(0, null);
    }

    @Test
    public void testCreatePersistentHashSet() {
        PersistentHashMap empty = PersistentHashMap.EMPTY;
        assertSame(empty, Seqspert.hashSetImpl(Seqspert.createPersistentHashSet(empty)));
    }

    @Test
    public void testAssoc() {
        Seqspert.assoc(BitmapIndexedNodeUtils.EMPTY, 0, 1, "key", "value", new Box(null));
    }

    @Test
    public void testSpliceHashMaps() {
        Seqspert.spliceHashMaps(PersistentHashMap.EMPTY, PersistentHashMap.EMPTY);
        Seqspert.spliceHashMaps(PersistentHashMap.create("key", "value"), PersistentHashMap.EMPTY);
        Seqspert.spliceHashMaps(PersistentHashMap.create("key", "value"), PersistentHashMap.create("key", "value"));
    }

    @Test
    public void testSpliceHashSets() {
        Seqspert.spliceHashSets(PersistentHashSet.EMPTY, PersistentHashSet.EMPTY);
        Seqspert.spliceHashSets(PersistentHashSet.create("value"), PersistentHashSet.EMPTY);
        Seqspert.spliceHashSets(PersistentHashSet.create("value"), PersistentHashSet.create("value"));
    }

    public static INode assocN(int shift, INode node, int start, int end, Counts counts) {
        for (int i = start; i < end; i++)
            node = TestUtils.assoc(shift, node , "key" + i, "value" + i, counts);
        return node;
    }
    
    public INode createN(int shift, int start, int end) {
        return assocN(shift, BitmapIndexedNodeUtils.EMPTY, start, end, new Counts());
    }

    public void test(int shift, int leftStart, int leftEnd, int rightStart, int rightEnd) {
        final INode leftNode = createN(shift, leftStart, leftEnd);
        final INode rightNode = createN(shift, rightStart, rightEnd);
        
        final Counts expectedCounts = new Counts();
        final INode expectedNode = assocN(shift, leftNode, rightStart, rightEnd, expectedCounts);
        
        final Counts actualCounts = new Counts();
        final INode actualNode = Seqspert.splice(shift, actualCounts, false, 0, null, leftNode, false, 0, null, rightNode);
        
        assertEquals(expectedCounts, actualCounts);
        assertNodeEquals(expectedNode, actualNode);
    }

    @Test
    public void testHashCollisionNodePromotion() {
        final Hasher hasher = new Hasher() {@Override
        public int hash(int i) { return ((i + 1) << 5) | i; }};

        // 1-16 inc singleton BINS
        final INode leftNode = TestUtils.create(0, hasher, 1, 17);
        assertEquals(Integer.bitCount(((BitmapIndexedNode)leftNode).bitmap), 16);

        // 1 extra KVP to force promotion
        // 2 further KVPs which should cause a BIN-HCN
        // if this had happened before promotion it would just have been an HCN
        final INode rightNode = TestUtils.create(0,
                                                 new HashCodeKey("key17", hasher.hash(17)), "value17",
                                                 new HashCodeKey("key18.1", hasher.hash(18)), "value18.1",
                                                 new HashCodeKey("key18.2", hasher.hash(18)), "value18.2"
                                                 );

        assertEquals(Integer.bitCount(((BitmapIndexedNode)rightNode).bitmap), 2);
        assertTrue(((BitmapIndexedNode)rightNode).array[3] instanceof HashCollisionNode);

        final int shift = 0;
        final Counts expectedCounts = new Counts();
        final INode expectedNode = TestUtils.assoc(shift, leftNode,
                                                   new HashCodeKey("key17", hasher.hash(17)), "value17",
                                                   new HashCodeKey("key18.1", hasher.hash(18)), "value18.1",
                                                   new HashCodeKey("key18.2", hasher.hash(18)), "value18.2",
                                                   expectedCounts);
        {
            assertTrue(expectedNode instanceof ArrayNode);
            // 18th child is a BIN whose only child is an HCN
            final ArrayNode parent = (ArrayNode) expectedNode;
            assertEquals(parent.count, 18);
            final BitmapIndexedNode child = (BitmapIndexedNode) parent.array[18];
            assertEquals(Integer.bitCount(child.bitmap), 1);
            final HashCollisionNode grandchild = (HashCollisionNode) child.array[1];
            assertEquals(grandchild.hash, hasher.hash(18));
        }       
        
        final Counts actualCounts = new Counts();
        final INode actualNode = new BitmapIndexedNodeAndBitmapIndexedNodeSplicer()
            .splice(shift, actualCounts,
                    false, 0, null, leftNode,
                    false, 0, null, rightNode);
        
        assertEquals(expectedCounts, actualCounts);
        assertNodeEquals(expectedNode, actualNode);
    }
    
    @Test
    public void testHashCollisionNodeUniqueness() {
        // it seems to be possible to get duplicate KVPs in a single HCN !
        final int shift = 0;
        final int hash = 1;
        HashCollisionNode node = new HashCollisionNode(null, hash, 0, new Object[]{});
        Box box = new Box(null);
        node = (HashCollisionNode) node.assoc(shift, hash, new HashCodeKey("key1.1", hash), "value1.1", box);
        assertEquals(box, box.val);
        assertEquals(1, node.count);
        box.val = null;
        node = (HashCollisionNode) node.assoc(shift, hash, new HashCodeKey("key1.2", hash), "value1.1", box);
        assertEquals(box, box.val);
        assertEquals(2, node.count);
        box.val = null;
        node = (HashCollisionNode) node.assoc(shift, hash, new HashCodeKey("key1.1", hash), "value1.1", box);
        assertEquals(null, box.val);
        assertEquals(2, node.count);
    }

}
