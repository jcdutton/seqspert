package clojure.lang;

import clojure.lang.PersistentHashMap.ArrayNode;
import clojure.lang.PersistentHashMap.INode;

class ArrayNodeAndKeyValuePairSplicer implements Splicer {

    public INode splice(int shift, Counts counts,
                        boolean leftHaveHash, int leftHash, Object leftKey, Object leftValue,
                        boolean rightHaveHash, int rightHashCode, Object rightKey, Object rightValue) {

        final ArrayNode leftNode = (ArrayNode) leftValue;

        final int rightHash = rightHaveHash ? rightHashCode : BitmapIndexedNodeUtils.hash(rightKey);
        final int index = PersistentHashMap.mask(rightHash, shift);

        final INode[] leftArray = leftNode.array;
        final INode subNode = leftArray[index];

        if (subNode == null) {
            return new ArrayNode(null,
                                 leftNode.count + 1,
                                 ArrayNodeUtils.cloneAndSetNode(leftArray, index,
                                                           BitmapIndexedNodeUtils.create(PersistentHashMap.mask(rightHash, shift + 5), rightKey, rightValue)));
        } else {
            final INode newNode =
                Seqspert.splice(shift + 5, counts, false, 0, null, subNode, true, rightHash, rightKey, rightValue);
            
            return newNode == subNode ? 
                leftNode :
                new ArrayNode(null,
                              leftNode.count,
                              ArrayNodeUtils.cloneAndSetNode(leftArray, index, newNode));
        }
    }

}
