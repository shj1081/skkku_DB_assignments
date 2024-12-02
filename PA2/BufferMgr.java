package simpledb.buffer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;

/**
 * Manages the pinning and unpinning of buffers to blocks using an LRU
 * replacement strategy.
 * 
 *
 */
public class BufferMgr {
    private Map<BlockId, Buffer> blockBufferMap; // Maps blocks to their assigned buffers
    private LinkedList<Buffer> unpinnedBuffers; // LRU list: least recently used buffers at front
    private Buffer[] allBuffers; // Array containing all buffer objects
    private int numAvailable;
    private static final long MAX_TIME = 10000;

    /**
     * Constructor: Creates a buffer manager having the specified
     * number of buffer slots. Initializes the buffer pool and LRU list.
     * This constructor depends on a {@link FileMgr} and
     * {@link simpledb.log.LogMgr LogMgr} object.
     * 
     * @param numbuffs the number of buffer slots to allocate
     */
    public BufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {

        // initialize blockBufferMap, unpinnedBuffers, buffers, and numAvailable
        blockBufferMap = new HashMap<>();
        unpinnedBuffers = new LinkedList<>();
        allBuffers = new Buffer[numbuffs];
        numAvailable = numbuffs;

        // add buffers to all buffers array and unpinnedBuffers
        for (int i = 0; i < numbuffs; i++) {
            allBuffers[i] = new Buffer(fm, lm, i);
            unpinnedBuffers.add(allBuffers[i]);
        }
    }

    /**
     * Returns the number of available (i.e. unpinned) buffers.
     * 
     * @return the number of available buffers
     */
    public synchronized int available() {
        return numAvailable;
    }

    /**
     * Flushes the dirty buffers modified by the specified transaction.
     * 
     * @param txId the transaction's id number
     */
    public synchronized void flushAll(int txId) {
        for (Buffer buff : allBuffers)
            if (buff.modifyingTx() == txId)
                buff.flush();
    }

    /**
     * Unpins the specified data buffer. If its pin count
     * goes to zero, then adds it to the end of the LRU list (most recently used
     * position)
     * and notifies any waiting threads.
     * 
     * @param buff the buffer to be unpinned
     */
    public synchronized void unpin(Buffer buff) {

        // if the buffer.pin is 0, then just return
        if (!buff.isPinned())
            return;

        buff.unpin();
        if (!buff.isPinned()) {
            numAvailable++;
            unpinnedBuffers.addLast(buff); // add to the end of the LRU list
            notifyAll();
        }
    }

    /**
     * Pins a buffer to the specified block, potentially
     * waiting until a buffer becomes available.
     * If no buffer becomes available within a fixed
     * time period, then a {@link BufferAbortException} is thrown.
     * 
     * @param blk a reference to a disk block
     * @return the buffer pinned to that block
     */
    public synchronized Buffer pin(BlockId blk) {
        try {
            long timestamp = System.currentTimeMillis();
            Buffer buff = tryToPin(blk);
            while (buff == null && !waitingTooLong(timestamp)) {
                wait(MAX_TIME);
                buff = tryToPin(blk);
            }
            if (buff == null)
                throw new BufferAbortException();
            return buff;
        } catch (InterruptedException e) {
            throw new BufferAbortException();
        }
    }

    /**
     * Returns true if starttime is older than 10 seconds
     * 
     * @param starttime timestamp
     * @return true if waited for more than 10 seconds
     */
    private boolean waitingTooLong(long starttime) {
        return System.currentTimeMillis() - starttime > MAX_TIME;
    }

    /**
     * Tries to pin a buffer to the specified block.
     * If there's already a buffer assigned to the block in blockBufferMap, that
     * buffer is used.
     * Otherwise, selects the least recently used unpinned buffer from the front
     * of the unpinnedBuffers list. Returns null if no unpinned buffers are
     * available.
     * 
     * @param blk a reference to a disk block
     * @return the pinned buffer or null if no buffers available
     */
    private Buffer tryToPin(BlockId blk) {
        Buffer buff = blockBufferMap.get(blk); // get the buffer assigned to the block
        if (buff == null) {
            buff = chooseUnpinnedBuffer(); // choose an unpinned buffer from unpinnedBuffers
            if (buff == null)
                return null;
            if (buff.block() != null)
                blockBufferMap.remove(buff.block()); // remove the buffer from blockBufferMap
            buff.assignToBlock(blk);
            blockBufferMap.put(blk, buff); // add the buffer to blockBufferMap with new blockId
        }
        if (!buff.isPinned()) {
            numAvailable--;
            unpinnedBuffers.remove(buff); // remove the buffer from unpinnedBuffers
        }
        buff.pin();
        return buff;
    }

    /**
     * Returns an unpinned buffer from the front of the unpinnedBuffers list
     * (least recently used position). Returns null if no unpinned buffers
     * are available.
     * 
     * @return the least recently used unpinned buffer, or null if none available
     */
    private Buffer chooseUnpinnedBuffer() {
        if (unpinnedBuffers.isEmpty())
            return null;
        return unpinnedBuffers.removeFirst(); // remove from the front of the LRU list
    }

    /**
     * Prints the status of the buffers
     */
    public void printStatus() {
        System.out.println("Allocated Buffers:");
        for (Buffer buff : allBuffers) {
            BlockId blk = buff.block();
            if (blk != null) {
                System.out.printf("Buffer %d: %s %s%n",
                        buff.getId(),
                        blk.toString(),
                        buff.isPinned() ? "pinned" : "unpinned");
            }
        }

        System.out.print("Unpinned Buffers in LRU order:");
        for (Buffer buff : unpinnedBuffers) {
            System.out.print(" " + buff.getId());
        }
        System.out.println();
    }
}
