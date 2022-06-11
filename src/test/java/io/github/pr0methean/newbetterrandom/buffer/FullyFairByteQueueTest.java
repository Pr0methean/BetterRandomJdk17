package io.github.pr0methean.newbetterrandom.buffer;

import java.util.concurrent.atomic.AtomicLongArray;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FullyFairByteQueueTest extends ByteQueueTest {
  @Override
  protected ByteQueue createBuffer(int size) {
    return FullyFairByteQueue.create(new AtomicByteRingBufferUsingByteBuffer(size), 16, 2, 2);
  }

  @Override
  protected void validateBenchmarks(AtomicLongArray bytesFinishedReading, long bytesPerReader) {
    for (int i = 0; i < bytesFinishedReading.length(); i++) {
      assertTrue(bytesFinishedReading.get(i) >= bytesPerReader,
          () -> "Too few bytes read on a thread: expected " + bytesPerReader + ", actual: " + bytesFinishedReading);
    }
  }
}
