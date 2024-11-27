package simpledb.buffer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * 
 * @author Edward Sciore
 *
 */
public class BufferMgr {
    private Map<BlockId, Buffer> bufferMap; // blockid-buffer mapping
    private LinkedList<Buffer> unpinnedBuffers; // LRU list (unpinned buffers)
    private Buffer[] buffers; // all buffers array
    private int numAvailable;
    private static final long MAX_TIME = 10000;

    /**
     * Constructor: Creates a buffer manager having the specified
     * number of buffer slots.
     * This constructor depends on a {@link FileMgr} and
     * {@link simpledb.log.LogMgr LogMgr} object.
     * 
     * @param numbuffs the number of buffer slots to allocate
     */
    public BufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {

        // initialize bufferMap, unpinnedBuffers, buffers, and numAvailable
        bufferMap = new HashMap<>();
        unpinnedBuffers = new LinkedList<>();
        buffers = new Buffer[numbuffs];
        numAvailable = numbuffs;

        // add buffers to all buffers array and unpinnedBuffers
        for (int i = 0; i < numbuffs; i++) {
            buffers[i] = new Buffer(fm, lm, i);
            unpinnedBuffers.add(buffers[i]);
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
     * @param txnum the transaction's id number
     */
    public synchronized void flushAll(int txnum) {
        for (Buffer buff : buffers)
            if (buff.modifyingTx() == txnum)
                buff.flush();
    }

    /**
     * Unpins the specified data buffer. If its pin count
     * goes to zero, then notify any waiting threads.
     * 
     * @param buff the buffer to be unpinned
     */
    public synchronized void unpin(Buffer buff) {
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
     * If the buffer is already assigned to the block, then that buffer is used.
     * Otherwise, an unpinned buffer is chosen from unpinnedBuffers.
     * Returns a null value if there are no available buffers.
     * 
     * @param blk a reference to a disk block
     * @return the pinned buffer
     */
    private Buffer tryToPin(BlockId blk) {
        Buffer buff = bufferMap.get(blk); // get the buffer assigned to the block
        if (buff == null) {
            buff = chooseUnpinnedBuffer(); // choose an unpinned buffer from unpinnedBuffers
            if (buff == null)
                return null;
            if (buff.block() != null)
                bufferMap.remove(buff.block()); // remove the buffer from bufferMap
            buff.assignToBlock(blk);
            bufferMap.put(blk, buff); // add the buffer to bufferMap with new blockId
        }
        if (!buff.isPinned()) {
            numAvailable--;
            unpinnedBuffers.remove(buff); // remove the buffer from unpinnedBuffers
        }
        buff.pin();
        return buff;
    }

    /**
     * Find and return an unpinned buffer .
     * 
     * @return the unpinned buffer
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
        for (Buffer buff : buffers) {
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
