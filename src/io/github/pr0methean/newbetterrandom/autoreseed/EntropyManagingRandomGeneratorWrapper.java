package io.github.pr0methean.newbetterrandom.autoreseed;

import java.util.Objects;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;
import io.github.pr0methean.newbetterrandom.reseedable.ReseedableRandomGenerator;

public class EntropyManagingRandomGeneratorWrapper extends EntropyCountingRandomGeneratorWrapper {
  private final long desiredEntropyBits;
  private final long minimumEntropyBits;
  private final AtomicSeedByteRingBuffer seedBuffer;
  private final int seedSizeBytes;
  private final byte[] seedHolder;
  private int availableSeedBytes = 0;

  public EntropyManagingRandomGeneratorWrapper(ReseedableRandomGenerator delegate, long desiredEntropyBits, long minimumEntropyBits, AtomicSeedByteRingBuffer seedBuffer) {
    this(delegate, delegate.seedEntropyBits(), desiredEntropyBits, minimumEntropyBits, seedBuffer);
  }

  public EntropyManagingRandomGeneratorWrapper(ReseedableRandomGenerator delegate, long initialEntropy, long desiredEntropyBits, long minimumEntropyBits, AtomicSeedByteRingBuffer seedBuffer) {
    super(delegate, initialEntropy);
    if (delegate.seedEntropyBits() - minimumEntropyBits < MAX_RANDOM_BITS_PER_CALL) {
      throw new IllegalArgumentException("Can't store enough entropy to stay above minimum");
    }
    this.desiredEntropyBits = desiredEntropyBits;
    this.minimumEntropyBits = minimumEntropyBits;
    this.seedBuffer = seedBuffer;
    seedSizeBytes = delegate.desiredSeedSizeBytes();
    seedHolder = new byte[seedSizeBytes];
  }

  @Override
  protected void debitEntropy(long amount) {
    super.debitEntropy(amount);
    if (amount < desiredEntropyBits) {
      if (amount < minimumEntropyBits) {
        forceReseed();
        entropy -= amount;
      } else {
        if (tryReseed()) {
          entropy -= amount;
        }
      }
    }
  }

  protected void forceReseed() {
    try {
      seedBuffer.read(seedHolder, availableSeedBytes, seedSizeBytes - availableSeedBytes);
      delegate.reseed(seedHolder);
      availableSeedBytes = 0;
      entropy = delegate.seedEntropyBits();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean tryReseed() {
    int readBytes;
    do {
      readBytes = seedBuffer.poll(seedHolder, availableSeedBytes, seedSizeBytes - availableSeedBytes);
      availableSeedBytes += readBytes;
    } while (readBytes > 0 && availableSeedBytes < seedSizeBytes);
    if (availableSeedBytes >= seedSizeBytes) {
      delegate.reseed(seedHolder);
      availableSeedBytes = 0;
      entropy = delegate.seedEntropyBits();
      return true;
    }
    return false;
  }

  protected int maxBytesToWrite() {
    return getBytesBeforeEntropyThreshold(desiredEntropyBits);
  }

  @Override
  public void nextBytes(byte[] bytes) {
    int writtenBytes = 0;
    while (writtenBytes < bytes.length) {
      int maxBytes = maxBytesToWrite();
      if (maxBytes <= 0) {
        if (getBytesBeforeEntropyThreshold(minimumEntropyBits) <= 0) {
          forceReseed();
          maxBytes = maxBytesToWrite();
        } else if (tryReseed()) {
          maxBytes = maxBytesToWrite();
        } else {
          maxBytes = 1;
        }
      }
      if (maxBytes >= bytes.length && writtenBytes == 0) {
        entropy -= (long) Byte.SIZE * bytes.length;
        delegate.nextBytes(bytes);
        return;
      } else {
        int bytesToWrite = (Math.min(maxBytes, bytes.length - writtenBytes));
        byte[] holder = new byte[bytesToWrite];
        entropy -= (long) Byte.SIZE * bytesToWrite;
        delegate.nextBytes(holder);
        System.arraycopy(holder, 0, bytes, writtenBytes, bytesToWrite);
        writtenBytes += bytesToWrite;
      }
    }
  }

  private int getBytesBeforeEntropyThreshold(long threshold) {
    long bytes = (entropy - threshold) / Byte.SIZE;
    return (int) Math.min(Math.max(0, bytes), Integer.MAX_VALUE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    return o instanceof EntropyManagingRandomGeneratorWrapper that
        && desiredEntropyBits == that.desiredEntropyBits
        && minimumEntropyBits == that.minimumEntropyBits
        && seedSizeBytes == that.seedSizeBytes
        && delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), desiredEntropyBits, minimumEntropyBits, delegate, seedSizeBytes);
  }
}
