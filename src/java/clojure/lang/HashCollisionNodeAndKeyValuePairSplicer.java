package clojure.lang;

import static clojure.lang.HashCollisionNodeUtils.maybeAdd;
import clojure.lang.PersistentHashMap.HashCollisionNode;
import clojure.lang.PersistentHashMap.INode;

public class HashCollisionNodeAndKeyValuePairSplicer implements Splicer {

    @Override
    public INode splice(int shift, Counts counts,
                        boolean leftHaveHash, int leftHashCode, Object leftKey, Object leftValue,
                        boolean rightHaveHash, int rightHashCode, Object rightKey, Object rightValue) {
        final HashCollisionNode leftNode = (HashCollisionNode) leftValue;
        final int leftHash = leftNode.hash;
        final int rightHash = BitmapIndexedNodeUtils.hash(rightHaveHash, rightHashCode, rightKey);
        if (rightHash == leftHash) {
            final int leftCount = leftNode.count;
            final Object[] leftArray = leftNode.array;
            final int oldSameKey = counts.sameKey;
            final Object[] newArray = maybeAdd(leftArray, leftCount * 2, rightKey, rightValue, counts);
            final int newSameKey = counts.sameKey;
            final boolean added = oldSameKey == newSameKey;
            return (leftArray == newArray) ?
                leftNode : new HashCollisionNode(null, leftHash, leftCount + (added ? 1 : 0), newArray);
        } else {
            return BitmapIndexedNodeUtils.
                recurse(shift, leftHash, null, leftNode, rightHash, rightKey, rightValue);
        }
    }

}
