package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;
import java.security.SecureRandom;

public class SecureRandomSeedProvider extends AbstractSeedProvider {
  private final SecureRandom secureRandom;

  public SecureRandomSeedProvider(final int size, final int readConcurrency) {
    super(size, readConcurrency);
    this.secureRandom = new SecureRandom();
  }

  public SecureRandomSeedProvider(AtomicSeedByteRingBuffer destBuffer, int sourceReadSize,
                                  final SecureRandom secureRandom) {
    super(destBuffer, sourceReadSize);
    this.secureRandom = secureRandom;
  }

  @Override protected void fillSourceBuffer() {
    System.arraycopy(secureRandom.generateSeed(sourceReadSize), 0, sourceBuffer, 0, sourceReadSize);
  }
}
