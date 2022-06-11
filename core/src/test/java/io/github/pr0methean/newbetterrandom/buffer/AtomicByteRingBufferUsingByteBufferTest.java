package io.github.pr0methean.newbetterrandom.buffer;

public class AtomicByteRingBufferUsingByteBufferTest extends ByteQueueTest {
  @Override
  protected AtomicByteRingBuffer createBuffer(int size) {
    return new AtomicByteRingBufferUsingByteBuffer(size);
  }
}
