package io.github.pr0methean.newbetterrandom.buffer;

import java.lang.ref.Cleaner;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

public class AtomicByteRingBufferUsingMemorySegment extends AtomicByteRingBuffer {

  private static final Cleaner CLEANER = Cleaner.create();

  private final MemorySegment segment;

  private final MemorySession session;

  public AtomicByteRingBufferUsingMemorySegment(int byteSize) {
    super(byteSize);
    session = MemorySession.openShared(CLEANER);
    segment = session.allocate(byteSize);
  }

  @Override
  protected void unsafeWrite(int destStart, byte[] source, int srcStart, int length) {
    MemorySegment.copy(source, srcStart, segment, ValueLayout.JAVA_BYTE, destStart, length);
  }

  @Override
  protected void unsafeRead(int srcStart, byte[] dest, int destStart, int length) {
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, srcStart, dest, destStart, length);
  }

  @Override
  public void close() {
    session.close();
  }
}
