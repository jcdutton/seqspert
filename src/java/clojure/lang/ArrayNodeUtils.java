package clojure.lang;

import clojure.lang.PersistentHashMap.ArrayNode;
import clojure.lang.PersistentHashMap.BitmapIndexedNode;
import clojure.lang.PersistentHashMap.HashCollisionNode;
import clojure.lang.PersistentHashMap.INode;

public class ArrayNodeUtils {

    // TODO: reorder/rename parameters
    public static INode[] promoteAndSet(int shift, int bitmap, int hash, Object[] bitIndexedArray, int index, INode newNode) {
        final INode[] newArray = new INode[32];
        final int newShift = shift + 5;
        int j = 0;
        for (int i = 0; i < 32 ; i++) {
            if ((bitmap & (1 << i)) != 0) {
                final Object key = bitIndexedArray[j++];
                final Object value = bitIndexedArray[j++];                
                newArray[i] = promote(newShift, key, value); // TODO: inline
            }
        }
        newArray[index] = newNode;
        return newArray;
    }

    public static INode[] cloneAndSetNode(INode[] oldArray, int index, INode node) {
        final INode[] newArray = oldArray.clone();
        newArray[index] = node;
        return newArray;
    }
    
    // promote a KVP for which you know the hash code i.e. return a BIN containing the KVP
    public static INode promote(int shift, int hash, Object key, Object value) {
        return BitmapIndexedNodeUtils.create(ArrayNodeUtils.partition(hash, shift), key, value);
    }
    
    // promote an entry that could be a KVP or a Node - if a Node, just return it, if a KVP,
    // lok up its hash and promote it
    public static INode promote(int shift, Object key, Object value) {
        return (key == null) ? (INode) value : promote(shift, BitmapIndexedNodeUtils.hash(key), key, value);
    }

    public static int partition(int hash, int shift) {
        return PersistentHashMap.mask(hash, shift);
    }

    public static INode makeArrayNode(int count, INode[] nodes) {
        return new ArrayNode(null, count, nodes);
    }

}
