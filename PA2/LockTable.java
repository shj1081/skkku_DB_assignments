package simpledb.tx.concurrency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.file.BlockId;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * If a transaction requests a lock that causes a conflict with an
 * existing lock, then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * 
 * @author Edward Sciore
 */
class LockTable {

    private Map<BlockId, List<Integer>> locks = new HashMap<>();

    /**
     * Grant an SLock on the specified block.
     * 
     * @param blk   a reference to the disk block
     * @param txnum the transaction ID requesting the lock
     */
    public synchronized void sLock(BlockId blk, int txnum) {
        List<Integer> txList = locks.get(blk);

        // If the list is not empty
        if (txList != null) {
            // Create a copy of the list
            List<Integer> txListCopy = new ArrayList<>(txList);

            // Check if there's an Xlock
            for (int tid : txListCopy) {
                if (tid < 0) { // Negative ID indicates Xlock
                    int holderTx = -tid; // Get actual TX number
                    if (txnum > holderTx) { // If younger transaction
                        throw new LockAbortException(); // Die
                    }

                    // Older transaction waits
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new LockAbortException();
                    }
                }
            }
        }

        // If the list is empty
        else {
            txList = new ArrayList<>();
            locks.put(blk, txList);
        }

        // Get the list again in case it changed while waiting
        txList = locks.get(blk);
        if (txList == null) {
            txList = new ArrayList<>();
            locks.put(blk, txList);
        }
        txList.add(txnum);
    }

    /**
     * Grant an XLock on the specified block.
     * 
     * @param blk   a reference to the disk block
     * @param txnum the transaction ID requesting the lock
     */
    synchronized void xLock(BlockId blk, int txnum) {
        List<Integer> txList = locks.get(blk);

        // If the list is not empty
        if (txList != null) {
            // Create a copy of the list
            List<Integer> txListCopy = new ArrayList<>(txList);

            // Check existing locks
            for (int tid : txListCopy) {
                int holderTx = (tid < 0) ? -tid : tid;
                if (txnum > holderTx) { // If younger transaction
                    throw new LockAbortException(); // Die
                }

                // Older transaction waits
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new LockAbortException();
                }
            }
        }

        // If the list is empty
        else {
            txList = new ArrayList<>();
            locks.put(blk, txList);
        }

        // Get the list again in case it changed while waiting
        txList = locks.get(blk);
        if (txList == null) {
            txList = new ArrayList<>();
            locks.put(blk, txList);
        }
        txList.add(-txnum); // Negative indicates Xlock
    }

    /**
     * Release a lock on the specified block.
     * 
     * @param blk   a reference to the disk block
     * @param txnum the transaction ID releasing the lock
     */
    synchronized void unlock(BlockId blk, int txnum) {
        List<Integer> txList = locks.get(blk);
        if (txList == null)
            return;

        // Remove both potential lock types
        txList.remove(Integer.valueOf(txnum));
        txList.remove(Integer.valueOf(-txnum));

        if (txList.isEmpty()) {
            locks.remove(blk);
        }
        notifyAll();
    }
}
