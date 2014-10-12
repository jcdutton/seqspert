package clojure.lang;

import static clojure.lang.NodeUtils.cloneAndInsert;
import clojure.lang.PersistentHashMap.ArrayNode;
import clojure.lang.PersistentHashMap.BitmapIndexedNode;
import clojure.lang.PersistentHashMap.HashCollisionNode;
import clojure.lang.PersistentHashMap.INode;

class BitmapIndexedNodeAndHashCollisionNodeSplicer implements Splicer {

    public INode splice(int shift, Counts counts, 
                        Object leftKey, Object leftValue,
                        Object rightKey, Object rightValue) {

        final BitmapIndexedNode leftNode = (BitmapIndexedNode) leftValue;
        final HashCollisionNode rightNode = (HashCollisionNode) rightValue;
        final int rightHash = rightNode.hash;
        final int bit = BitmapIndexedNodeUtils.bitpos(rightHash, shift);
        final int index = leftNode.index(bit);
        final int keyIndex = index * 2;
        final int valueIndex = keyIndex + 1;
        if((leftNode.bitmap & bit) == 0) {
            final int leftBitCount = Integer.bitCount(leftNode.bitmap);
            if (leftBitCount == 16)
                return new ArrayNode(null,
                                     17,
                                     NodeUtils.promoteAndSet(shift + 5,
                                                             leftNode.bitmap,
                                                             leftNode.array,
                                                             keyIndex,
                                                             rightNode));
            else
                return new BitmapIndexedNode(null,
                                             leftNode.bitmap | bit,
                                             cloneAndInsert(leftNode.array,
                                                            leftBitCount * 2,
                                                            keyIndex,
                                                            rightNode));
            
        } else {
            // left hand side already occupied...
            final Object subKey = leftNode.array[keyIndex];
            final Object subVal = leftNode.array[valueIndex];
            return new BitmapIndexedNode(null,
                                         leftNode.bitmap,
                                         NodeUtils.cloneAndSetNode(leftNode.array,
                                                                   valueIndex,
                                                                   NodeUtils.splice(shift + 5, counts,
                                                                                    subKey, subVal,
                                                                                    rightKey, rightValue)
                                                                   ));

        }
    }

}
