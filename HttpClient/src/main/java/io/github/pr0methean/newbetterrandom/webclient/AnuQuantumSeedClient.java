package io.github.pr0methean.newbetterrandom.webclient;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.pr0methean.newbetterrandom.core.buffer.ByteQueue;
import io.github.pr0methean.newbetterrandom.core.producer.SeedException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HexFormat;

/**
 * API client for the Australian National University's <a href="https://qrng.anu.edu.au/">quantum
 * RNG</a>, which extracts randomness from quantum-vacuum fluctuations. Unlike random.org, this API
 * has no usage quotas; the generator produces 5.7 Gbps, so the output rate is limited only by
 * network bandwidth.
 */
public class AnuQuantumSeedClient extends WebSeedClient {

  private static final int MAX_STRINGS_PER_REQUEST = 1024;
  private static final int MAX_BYTES_PER_STRING = 1024;

  private static final String REQUEST_URL_FORMAT
      = "https://qrng.anu.edu.au/API/jsonI.php?length=%d&type=hex16&size=%d";
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  /**
   * @param buffer
   * @param sourceReadSize
   * @param webSeedClientConfiguration configuration
   */
  protected AnuQuantumSeedClient(ByteQueue buffer, int sourceReadSize,
                                 WebSeedClientConfiguration webSeedClientConfiguration) {
    super(buffer, sourceReadSize, webSeedClientConfiguration);
  }

  @Override protected int getMaxRequestSize() {
    return MAX_STRINGS_PER_REQUEST * MAX_BYTES_PER_STRING;
  }

  @Override protected URL getConnectionUrl(int numBytes) {
    int stringCount = divideRoundingUp(numBytes, MAX_BYTES_PER_STRING);
    int stringLength = (stringCount > 1) ? MAX_BYTES_PER_STRING : numBytes;
    try {
      return new URL(String.format(REQUEST_URL_FORMAT, stringCount, stringLength));
    } catch (MalformedURLException e) {
      throw new SeedException("Error creating URL", e);
    }
  }

  @Override protected void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      int length) throws IOException {
    final TreeNode response = parseJsonResponse(connection);
    final TreeNode byteStringsNode = response.get("data");
    if (!byteStringsNode.isArray()) {
      throw new SeedException("Wrong type of 'data' node in response");
    }
    final ArrayNode byteStrings = (ArrayNode) byteStringsNode;
    final int stringCount, stringLength, usedLengthOfLastString;
    if (length > MAX_BYTES_PER_STRING) {
      stringCount = divideRoundingUp(length, MAX_BYTES_PER_STRING);
      stringLength = MAX_BYTES_PER_STRING;
      usedLengthOfLastString = modRange1ToM(length, stringLength);
    } else {
      stringCount = 1;
      stringLength = length;
      usedLengthOfLastString = length;
    }
    if (stringCount != byteStrings.size()) {
      throw new SeedException(String.format("Wrong size response (expected %d byte arrays, got %d",
          stringCount, byteStrings.size()));
    }
    try {
      for (int stringIndex = 0; stringIndex < stringCount - 1; stringIndex++) {
        byte[] bytes = HEX_FORMAT.parseHex(getStringAndCheckLength(byteStrings, stringIndex, 2 * stringLength));
        System.arraycopy(bytes, 0, seed, offset + stringIndex * stringLength, stringLength);
      }
      byte[] lastBytes = HEX_FORMAT.parseHex(getStringAndCheckLength(byteStrings, stringCount - 1, 2 * usedLengthOfLastString));
      System.arraycopy(lastBytes, 0, seed, offset + stringLength * (stringCount - 1), usedLengthOfLastString);
      connection.disconnect();
    } catch (IllegalArgumentException e) {
      throw new SeedException("qrng.anu.edu.au returned malformed JSON", e);
    }
  }

  private static String getStringAndCheckLength(ArrayNode array, int index, int expectedLength) {
    String out = array.get(index).toString();
    int actualLength = out.length();
    if (actualLength != expectedLength) {
      throw new SeedException(String.format(
          "qrng.anu.edu.au sent string with wrong length (expected %d, was %d)",
          expectedLength, actualLength));
    }
    return out;
  }
}
