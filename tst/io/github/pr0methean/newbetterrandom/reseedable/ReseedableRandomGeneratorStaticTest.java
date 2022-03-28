package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReseedableRandomGeneratorStaticTest {
  @Test
  public void testBytesToLongExpanding() {
    TreeSet<Long> uniqueValues = new TreeSet<>();
    for (int b1 = Byte.MIN_VALUE; b1 <= Byte.MAX_VALUE; b1++) {
      uniqueValues.add(ReseedableRandomGenerator.bytesToLong(new byte[]{(byte)b1}, 64));
      for (int b2 = Byte.MIN_VALUE; b2 <= Byte.MAX_VALUE; b2++) {
        uniqueValues.add(ReseedableRandomGenerator.bytesToLong(new byte[]{(byte)b1, (byte)b2}, 64));
      }
    }
    assertEquals((1 << Byte.SIZE) * (1 + (1 << Byte.SIZE)), uniqueValues.size());
  }
}
