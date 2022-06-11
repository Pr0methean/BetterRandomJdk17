package io.github.pr0methean.newbetterrandom.buffer;

import java.nio.ByteBuffer;

/**
 * <p>Byte ring buffer designed to have allocation-free and lock-free offer, allocation-free and wait-free poll,
 * at-most-once delivery, and a single-byte unit of transmission, and to be thread-safe for multiple readers and
 * multiple writerss.</p>
 * <p>The intended use case is reseeding of pseudorandom number generators (PRNGs), to prevent
 * results of complex programs from being influenced by subtle patterns in the PRNG's output. For
 * example, in a simulation program, a handful of threads may read from truly-random sources such as
 * a Unix {@code /dev/random} and call {@link #write(byte[], int, int)} to deliver seed material,
 * while each thread running the simulation calls {@link #poll(byte[], int, int)} and, when it
 * succeeds in fetching enough bytes, either replaces its PRNG with a new one obtained by calling
 * {@link java.util.random.RandomGeneratorFactory#create(byte[])} or, in the case of a
 * {@link java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator} whose period far exceeds
 * the amount of output expected over the simulation's running time, jumping by a distance equal to
 * the seed.</p>
 */
public class AtomicByteRingBufferUsingByteBuffer extends AtomicByteRingBuffer {

  protected final ByteBuffer buffer;

  public AtomicByteRingBufferUsingByteBuffer(final int byteSize) {
    super(byteSize);
    this.buffer = ByteBuffer.allocateDirect(byteSize);
  }

  @Override
  protected void unsafeWrite(int destStartIndex, byte[] source, int start, int beforeWrap) {
    buffer.put(destStartIndex, source, start, beforeWrap);
  }

  @Override
  protected void unsafeRead(int readStartIndex, byte[] dest, int start, int lengthBeforeWrap) {
    buffer.get(readStartIndex, dest, start, lengthBeforeWrap);
  }
}
