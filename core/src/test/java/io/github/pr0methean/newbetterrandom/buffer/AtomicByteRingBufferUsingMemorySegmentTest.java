package io.github.pr0methean.newbetterrandom.buffer;

public class AtomicByteRingBufferUsingMemorySegmentTest extends ByteQueueTest {
  @Override
  protected AtomicByteRingBuffer createBuffer(int size) {
    return new AtomicByteRingBufferUsingMemorySegment(size);
  }
}
