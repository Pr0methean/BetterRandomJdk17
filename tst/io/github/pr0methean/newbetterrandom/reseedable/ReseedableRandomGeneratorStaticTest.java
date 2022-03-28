package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReseedableRandomGeneratorStaticTest {
  @Test
  public void testBytesToLongExpanding() {
    TreeSet<Long> uniqueValues = new TreeSet<>();
    for (int b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
      byte[] bytes = {(byte) b};
      uniqueValues.add(ReseedableRandomGenerator.bytesToLong(bytes, 64));
    }
    assertEquals(1 << Byte.SIZE, uniqueValues.size());
  }
}
