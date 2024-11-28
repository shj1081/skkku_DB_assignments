package simpledb.tx.concurrency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.file.BlockId;

/**
 * The lock table implementing the Wait-Die deadlock prevention scheme.
 * Key features:
 * - Uses transaction IDs to determine lock priority (lower ID = older
 * transaction)
 * - Younger transactions abort when requesting locks held by older ones
 * - Older transactions wait for younger ones to release locks
 * - Supports both shared (S) and exclusive (X) locks
 * - Represents X-locks with negative transaction IDs in the lock list
 */
class LockTable {

    /**
     * Maps blocks to their current lock holders.
     * The List<Integer> contains transaction IDs where:
     * - Positive ID (e.g., 5): Transaction 5 holds a shared (S) lock
     * - Negative ID (e.g., -5): Transaction 5 holds an exclusive (X) lock
     */
    private Map<BlockId, List<Integer>> locks = new HashMap<>();

    /**
     * Grants a shared (S) lock following Wait-Die protocol.
     * Younger transactions abort when conflicting with older ones' X-locks.
     * 
     * @param blk  the block to lock
     * @param txId the ID of requesting transaction
     * @throws LockAbortException if Wait-Die requires this transaction to abort
     */
    public synchronized void sLock(BlockId blk, int txId) {
        while (true) {
            List<Integer> txList = locks.get(blk);

            if (txList == null) {
                // No existing locks - create new list and grant S-lock
                txList = new ArrayList<>();
                locks.put(blk, txList);
                txList.add(txId);
                return;
            }

            // Already holds either type of lock - no need to wait
            if (txList.contains(txId) || txList.contains(-txId)) {
                return;
            }

            boolean shouldWait = false;
            // Check for conflicts with X-locks
            for (int tid : txList) {
                if (tid < 0) { // Found an X-lock
                    // Wait-Die check: abort if we're younger than lock holder
                    if (txId > -tid) {
                        throw new LockAbortException();
                    }
                    shouldWait = true;
                    break;
                }
            }

            if (!shouldWait) {
                // No X-locks found, safe to grant S-lock
                txList.add(txId);
                return;
            }

            // Must wait - older transaction has X-lock
            try {
                wait();
            } catch (InterruptedException e) {
                throw new LockAbortException();
            }
        }
    }

    /**
     * Grants an exclusive (X) lock following Wait-Die protocol.
     * Younger transactions abort when conflicting with any older transaction's
     * lock.
     * 
     * @param blk  the block to lock
     * @param txId the ID of requesting transaction
     * @throws LockAbortException if Wait-Die requires this transaction to abort
     */
    synchronized void xLock(BlockId blk, int txId) {
        while (true) {
            List<Integer> txList = locks.get(blk);

            if (txList == null) {
                // No existing locks - create new list and grant X-lock
                txList = new ArrayList<>();
                locks.put(blk, txList);
                txList.add(-txId); // Negative ID indicates X-lock
                return;
            }

            // Already holds this X-lock
            if (txList.contains(-txId)) {
                return;
            }

            boolean shouldWait = false;
            // Check for conflicts with other transactions' locks
            for (int tid : txList) {
                if (tid != txId) { // Skip our own S-lock if we have one
                    int holderTx = (tid < 0) ? -tid : tid;
                    // Wait-Die check: abort if we're younger than any lock holder
                    if (txId > holderTx) {
                        throw new LockAbortException();
                    }
                    shouldWait = true;
                    break;
                }
            }

            if (!shouldWait) {
                // No conflicts, safe to grant X-lock
                txList.remove(Integer.valueOf(txId)); // Remove S-lock if exists
                txList.add(-txId);
                return;
            }

            // Must wait - other transactions have locks
            try {
                wait();
            } catch (InterruptedException e) {
                throw new LockAbortException();
            }
        }
    }

    /**
     * Releases both S and X locks held by the transaction on the specified block.
     * Notifies all waiting transactions to check if they can now acquire their
     * locks.
     * 
     * @param blk  the block whose locks should be released
     * @param txId the ID of transaction releasing its locks
     */
    synchronized void unlock(BlockId blk, int txId) {
        List<Integer> txList = locks.get(blk);
        if (txList == null)
            return;

        // Remove both types of locks (if they exist)
        txList.remove(Integer.valueOf(txId)); // Remove S-lock
        txList.remove(Integer.valueOf(-txId)); // Remove X-lock

        // Remove block entry if no more locks
        if (txList.isEmpty()) {
            locks.remove(blk);
        }
        // Wake up all waiting transactions
        notifyAll();
    }
}
