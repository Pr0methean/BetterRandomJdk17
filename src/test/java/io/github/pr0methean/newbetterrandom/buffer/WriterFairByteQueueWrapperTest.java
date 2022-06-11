package io.github.pr0methean.newbetterrandom.buffer;

public class WriterFairByteQueueWrapperTest extends ByteQueueTest {
  @Override
  protected ByteQueue createBuffer(int size) {
    return new WriterFairByteQueueWrapper(new AtomicByteRingBufferUsingByteBuffer(size), 16, 2);
  }
}
