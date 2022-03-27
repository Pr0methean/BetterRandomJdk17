package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;
import java.security.SecureRandom;

public class SecureRandomSeedProvider extends AbstractSeedProvider {
  private final SecureRandom secureRandom;

  public SecureRandomSeedProvider(AtomicSeedByteRingBuffer destBuffer, int sourceReadSize,
                                  final SecureRandom secureRandom) {
    super(destBuffer, sourceReadSize);
    this.secureRandom = secureRandom;
  }

  @Override protected byte[] getSeedBytes() {
    return secureRandom.generateSeed(sourceReadSize);
  }
}
