package clojure.lang;

import clojure.lang.PersistentHashMap.BitmapIndexedNode;
import clojure.lang.PersistentHashMap.INode;

// TODO: this is pretty much identical to BINAndHCNSplicer - can we reuse the same code ?

class BitmapIndexedNodeAndKeyValuePairSplicer implements Splicer {

    public INode splice(int shift, Counts counts, 
                        Object leftKey, Object leftValue,
                        Object rightKey, Object rightValue) {
        final BitmapIndexedNode leftNode = (BitmapIndexedNode) leftValue;

        final int rightHash = NodeUtils.hash(rightKey);
        final int bit = BitmapIndexedNodeUtils.bitpos(rightHash, shift);
        final int index = leftNode.index(bit);
        final int keyIndex = index * 2;
        final int valueIndex = keyIndex + 1;
        if((leftNode.bitmap & bit) == 0) {
            // left hand side unoccupied
            // TODO: BIN or AN ?
            return new BitmapIndexedNode(null,
                                         leftNode.bitmap | bit,
                                         NodeUtils.cloneAndInsert(leftNode.array,
                                                                  Integer.bitCount(leftNode.bitmap) * 2,
                                                                  keyIndex,
                                                                  rightKey,
                                                                  rightValue));

        } else {
            // left hand side already occupied...
            final Object subKey = leftNode.array[keyIndex];
            final Object subVal = leftNode.array[valueIndex];
            final INode spliced = NodeUtils.splice(shift + 5, counts, subKey, subVal, rightHash, rightKey, rightValue);
            if (spliced == null) {
                if (Util.equiv(subVal, rightValue)) {
                    return leftNode;
                } else {
                    return new BitmapIndexedNode(null, leftNode.bitmap, NodeUtils.cloneAndSet(leftNode.array, valueIndex, rightValue));
                }
            } else {
                return new BitmapIndexedNode(null, leftNode.bitmap, NodeUtils.cloneAndSetNode(leftNode.array, valueIndex, spliced));
            }
        }
    }

}
