package clojure.lang;

import clojure.lang.PersistentHashMap.HashCollisionNode;
import clojure.lang.PersistentHashMap.INode;

class KeyValuePairAndHashCollisionNodeSplicer implements Splicer {

    public INode splice(int shift, Counts counts,
                        Object leftKey, Object leftValue,
                        int _, Object rightKey, Object rightValue) {

        final HashCollisionNode rightNode = (HashCollisionNode) rightValue;

        final int leftHash = NodeUtils.hash(leftKey);
        final int rightHash = rightNode.hash;

        System.out.println("[3]HERE!: " + rightHash);
        if (leftHash == rightHash) {
            System.out.println("[4]HERE!: " + rightHash);
            final Object[] rightArray = rightNode.array;
            final int rightLength = rightNode.count * 2;
            final int keyIndex = HashCollisionNodeUtils.keyIndex(rightArray, rightLength, leftKey);
            if (keyIndex == -1) {
                final INode newNode = new HashCollisionNode(null,
                                                            rightHash,
                                                            rightNode.count + 1,
                                                            // since KVP is from LHS, insert at front of HCN
                                                            NodeUtils.cloneAndInsert(rightArray, rightLength,
                                                                                     0, leftKey, leftValue));
                System.out.println("[5]HERE!: " + rightHash);
                //return BitmapIndexedNodeUtils
                //    .create(PersistentHashMap.mask(rightNode.hash, shift), null, newNode);
                return newNode;
            } else {
                counts.sameKey++;
                System.out.println("[6]HERE!: " + rightHash);
                if (keyIndex == 1) {
                    return rightNode;
                } else {
                    // strictly speaking the left KVP should be first
                    // in the HCN - not efficient, but then I would
                    // imagine that this does not happen very often.
                    final Object[] newArray = rightArray.clone();
                    newArray[0] = leftKey;
                    newArray[1] = rightArray[keyIndex + 1];
                    System.arraycopy(rightArray, 0, newArray, 2, keyIndex);
                    System.arraycopy(rightArray, keyIndex + 2, newArray, keyIndex, rightLength - keyIndex - 2);
                    return new HashCollisionNode(null, rightHash, rightNode.count, newArray);
                    //return BitmapIndexedNodeUtils
                    //.create(PersistentHashMap.mask(rightNode.hash, shift), null, rightNode);
                }
            }
            
        } else {
            System.out.println("[7]HERE!: " + rightHash);
            return BitmapIndexedNodeUtils
                .create(PersistentHashMap.mask(leftHash, shift), leftKey, leftValue,
                        PersistentHashMap.mask(rightHash, shift), null, rightNode);
        }

    }

}
