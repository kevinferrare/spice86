package spice86.emulator.interrupthandlers.dos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;

public class DosMemoryManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(DosMemoryManager.class);

  private Memory memory;
  private DosMemoryControlBlock start;
  private int pspSegment;

  public DosMemoryManager(Memory memory) {
    this.memory = memory;
  }

  public void init(int pspSegment, int lastFreeSegment) {
    int startSegment = pspSegment - 1;
    this.pspSegment = pspSegment;
    int size = lastFreeSegment - startSegment;
    start = getDosMemoryControlBlockFromSegment(startSegment);
    // size -1 because the mcb itself takes 16 bytes which is 1 paragraph
    start.setSize(size - 1);
    start.setFree();
    start.setLast();
  }

  public int getPspSegment() {
    return pspSegment;
  }

  public boolean modifyBlock(int blockSegment, int requestedSize) {
    DosMemoryControlBlock block = getDosMemoryControlBlockFromSegment(blockSegment);
    if (!checkValidOrLogError(block)) {
      return false;
    }
    // Make the block the biggest it can get
    if (!joinBlocks(block, false)) {
      LOGGER.error("Could not join MCB {}.", block);
      return false;
    }
    if (block.getSize() < requestedSize - 1) {
      LOGGER.error("MCB {} is too small for requested size {}.", block, requestedSize);
      return false;
    }
    if (block.getSize() > requestedSize) {
      splitBlock(block, requestedSize);
    }
    block.setPspSegment(pspSegment);
    return true;
  }

  /**
   * Split the block:
   * <ul>
   * <li>If size is more than the block size => error, returns false</li>
   * <li>If size matches the block size => nothing to do</li>
   * <li>If size is less the block size => splits the block by creating a new free mcb at the end of the block</li>
   * </ul>
   * 
   * @param block
   * @param size
   * @return
   */
  private boolean splitBlock(DosMemoryControlBlock block, int size) {
    int blockSize = block.getSize();
    if (blockSize == size) {
      // nothing to do
      return true;
    }
    int nextBlockSize = blockSize - size - 1;
    if (size < 0) {
      LOGGER.error("Cannot split block {} with size {} because it is too small.", block, size);
      return false;
    }
    block.setSize(size);
    DosMemoryControlBlock next = block.next();
    // if it was last propagate it
    next.setType(block.getType());
    // we are non last now for sure
    block.setNonLast();
    // next is free
    next.setFree();
    next.setSize(nextBlockSize);
    return true;
  }

  private boolean joinBlocks(DosMemoryControlBlock block, boolean onlyIfFree) {
    if (onlyIfFree && !block.isFree()) {
      // Do not touch blocks in use
      return true;
    }
    while (block.isNonLast()) {
      DosMemoryControlBlock next = block.next();
      if (!next.isFree()) {
        // end of the free blocks reached
        break;
      }
      if (!checkValidOrLogError(next)) {
        LOGGER.error("MCB {} is not valid.", next);
        return false;
      }
      joinContiguousBlocks(block, next);
    }
    return true;
  }

  private void joinContiguousBlocks(DosMemoryControlBlock destination, DosMemoryControlBlock next) {
    destination.setType(next.getType());
    // +1 because next block metadata is going to free space
    destination.setSize(destination.getSize() + next.getSize() + 1);
  }

  public DosMemoryControlBlock allocateMemoryBlock(int requestedSize) {
    List<DosMemoryControlBlock> candidates = findCandidatesForAllocation(requestedSize);
    // take the smallest
    Optional<DosMemoryControlBlock> blockOptional = candidates.stream()
        .sorted(Comparator.comparing(DosMemoryControlBlock::getSize))
        .findFirst();
    if (blockOptional.isEmpty()) {
      // Nothing found
      LOGGER.error("Could not find any MCB to fit {}.", requestedSize);
      return null;
    }
    DosMemoryControlBlock block = blockOptional.get();
    if (!splitBlock(block, requestedSize)) {
      // An issue occurred while splitting the block
      LOGGER.error("Could not spit block {}.", block);
      return null;
    }
    block.setPspSegment(pspSegment);
    return block;
  }

  public DosMemoryControlBlock findLargestFree() {
    DosMemoryControlBlock current = start;
    DosMemoryControlBlock largest = null;
    while (true) {
      if (current.isFree() && (largest == null || current.getSize() > largest.getSize())) {
        largest = current;
      }
      if (current.isLast()) {
        return largest;
      }
      current = current.next();
    }
  }

  private List<DosMemoryControlBlock> findCandidatesForAllocation(int requestedSize) {
    DosMemoryControlBlock current = start;
    List<DosMemoryControlBlock> candidates = new ArrayList<>();
    while (true) {
      if (!checkValidOrLogError(current)) {
        return Collections.emptyList();
      }
      joinBlocks(current, true);
      if (current.isFree() && current.getSize() >= requestedSize) {
        candidates.add(current);
      }
      if ((current.isLast())) {
        return candidates;
      }
      current = current.next();
    }
  }

  public boolean freeMemoryBlock(int blockSegment) {
    DosMemoryControlBlock block = getDosMemoryControlBlockFromSegment(blockSegment);
    if (!checkValidOrLogError(block)) {
      return false;
    }
    block.setFree();
    return joinBlocks(block, true);
  }

  private boolean checkValidOrLogError(DosMemoryControlBlock block) {
    if (!block.isValid()) {
      LOGGER.error("MCB {} is invalid.", block);
      return false;
    }
    return true;
  }

  private DosMemoryControlBlock getDosMemoryControlBlockFromSegment(int blockSegment) {
    return new DosMemoryControlBlock(memory, MemoryUtils.toPhysicalAddress(blockSegment, 0));
  }
}
