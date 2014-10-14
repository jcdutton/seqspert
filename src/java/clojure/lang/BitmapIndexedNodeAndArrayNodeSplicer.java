package clojure.lang;

import clojure.lang.PersistentHashMap.ArrayNode;
import clojure.lang.PersistentHashMap.BitmapIndexedNode;
import clojure.lang.PersistentHashMap.INode;

class BitmapIndexedNodeAndArrayNodeSplicer implements Splicer {

    public INode splice(int shift, Counts counts,
                        Object leftKey, Object leftValue,
                        Object rightKey, Object rightValue) {

        final BitmapIndexedNode leftNode = (BitmapIndexedNode) leftValue;
        final ArrayNode rightNode = (ArrayNode) rightValue;

        final Object[] leftArray = leftNode.array;
        final INode[] rightArray = rightNode.array;

        final INode[] newArray = new INode[32];

        int differences = 0; // can we just return RHS ?
        int count = 0;
        int leftIndex = 0;
        for (int i = 0; i < 32; i++) {
            final int mask = 1 << i;
            final boolean hasLeft = ((leftNode.bitmap & mask) != 0);
			final INode rightSubNode = rightArray[i];
            final boolean hasRight = rightSubNode != null;

            if (hasLeft) {
                count++;
				final Object leftSubKey = leftArray[leftIndex++];
                final Object leftSubValue = leftArray[leftIndex++];

                if (hasRight) {
                    // both sides present - merge them...
                    final INode newSubNode = NodeUtils.splice(shift + 5, counts, leftSubKey, leftSubValue, null, rightSubNode);
					newArray[i] = newSubNode;
					differences += (newSubNode == rightSubNode) ? 0 : 1;
                } else {
                    newArray[i] = (leftSubKey == null) ? (INode) leftSubValue : NodeUtils.create(shift + 5, leftSubKey, leftSubValue);
					differences++;
                }
            } else { // not lb
                if (hasRight) {
                    count++;
                    // only rhs present - copy over
                    newArray[i] = rightSubNode;
                } else {
                    // do nothing...
                }
            }
        }

        return differences == 0 ? rightNode : new ArrayNode(null, count, newArray);
    }
}
