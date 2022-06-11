package io.github.pr0methean.newbetterrandom.webclient;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.pr0methean.newbetterrandom.buffer.ByteQueue;
import io.github.pr0methean.newbetterrandom.producer.AbstractSeedProvider;
import io.github.pr0methean.newbetterrandom.producer.SeedException;

/**
 * An {@link AbstractSeedProvider} that is a client for a Web random-number service. Contains many methods
 * for parsing JSON responses.
 */
public abstract class WebSeedClient extends AbstractSeedProvider {
  /**
   * Measures the retry delay. A ten-second delay might become either nothing or an hour if we used
   * local time during the start or end of Daylight Saving Time, but it's fine if we occasionally
   * wait 9 or 11 seconds instead of 10 because of a leap-second adjustment. See <a
   * href="https://www.youtube.com/watch?v=-5wpm-gesOY">Tom Scott's video</a> about the various
   * considerations involved in this choice of clock.
   */
  protected static final Clock CLOCK = Clock.systemUTC();
  /**
   * Made available to parse JSON responses.
   */
  protected static final JsonFactory JSON_FACTORY = new JsonFactory();
  private final WebSeedClientConfiguration configuration;
  private final byte[] sourceBuffer;

  /**
   * The value for the HTTP User-Agent header.
   */
  protected final String userAgent;
  protected Instant earliestNextAttempt = Instant.MIN;

  /**
   * @param webSeedClientConfiguration configuration
   */
  protected WebSeedClient(
      final ByteQueue buffer,
      final int sourceReadSize,
      final WebSeedClientConfiguration webSeedClientConfiguration) {
    super(buffer, sourceReadSize);
    configuration = webSeedClientConfiguration;
    userAgent = getClass().getName();
    sourceBuffer = new byte[sourceReadSize];
  }

  /**
   * Creates a {@link BufferedReader} reading the response from the given {@link HttpURLConnection}
   * as UTF-8. The connection must be open and all request properties must be set before this reader
   * is used.
   *
   * @param connection the connection to read the response from
   * @return a BufferedReader reading the response
   * @throws IOException if thrown by {@link HttpURLConnection#getInputStream()}
   */
  protected static BufferedReader getResponseReader(final HttpURLConnection connection)
      throws IOException {
    return new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8));
  }

  /**
   * Parses the response from the given {@link HttpURLConnection} as UTF-8 encoded JSON.
   *
   * @param connection the connection to parse the response from
   * @return the response as a {@link JsonNode}
   * @throws IOException if thrown by {@link HttpURLConnection#getInputStream()}
   */
  protected static JsonNode parseJsonResponse(HttpURLConnection connection) throws IOException {
    try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
         JsonParser jsonParser = JSON_FACTORY.createParser(inputStream)) {
      return jsonParser.readValueAsTree();
    }
  }

  /**
   * Returns the maximum number of bytes that can be obtained with one request to the service.
   * When a seed larger than this is needed, it is obtained using multiple requests.
   *
   * @return the maximum number of bytes per request
   */
  protected abstract int getMaxRequestSize();

  /**
   * Opens an {@link HttpsURLConnection} that will make a GET request to the given URL using this
   * seed generator's current {@link Proxy} and User-Agent string, with the
   * header {@code Content-Type: application/json}.
   *
   * @param url the URL to connect to
   * @return a connection to the URL
   * @throws IOException if thrown by {@link URL#openConnection()} or {@link URL#openConnection(Proxy)}
   */
  protected HttpsURLConnection openConnection(final URL url) throws IOException {
    final HttpsURLConnection connection =
        (HttpsURLConnection) ((getProxy() == null) ? url.openConnection() :
            url.openConnection(getProxy()));
    if (getSocketFactory() != null) {
      connection.setSSLSocketFactory(getSocketFactory());
    }
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("User-Agent", userAgent);
    return connection;
  }

  protected abstract URL getConnectionUrl(int numBytes);

  /**
   * Performs a single request for random bytes.
   *
   * @param connection the connection to download from
   * @param seed the array to save them to
   * @param offset the first index to save them to in the array
   * @param length the number of bytes to download
   * @throws IOException if a connection error occurs
   * @throws SeedException if a malformed response is received
   */
  protected abstract void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      int length) throws IOException, InterruptedException;

  @Override protected byte[] getSeedBytes() throws InterruptedException {
    final int length = sourceReadSize;
    final int batchSize = Math.min(length, getMaxRequestSize());
    final URL batchUrl = getConnectionUrl(batchSize);
    final int batches = divideRoundingUp(length, batchSize);
    final int lastBatchSize = modRange1ToM(length, batchSize);
    final URL lastBatchUrl = getConnectionUrl(lastBatchSize);
    try {
      int batch;
      for (batch = 0; batch < batches - 1; batch++) {
        downloadBatch(sourceBuffer, batch * batchSize, batchSize, batchUrl);
      }
      downloadBatch(sourceBuffer, batch * batchSize, lastBatchSize, lastBatchUrl);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in an applet sandbox).
      throw new SeedException("SecurityManager prevented access to a remote seed source", ex);
    }
    return sourceBuffer;
  }

  protected static int divideRoundingUp(int dividend, int divisor) {
    return (dividend + divisor - 1) / divisor;
  }

  protected static int modRange1ToM(int dividend, int modulus) {
    int result = dividend % modulus;
    if (result == 0) {
      result = modulus;
    }
    return result;
  }

  private void downloadBatch(byte[] dest, int offset, int length, URL batchUrl) throws InterruptedException {
    awaitNextAttemptTime();
    boolean succeeded = false;
    int retries = 0;
    while (!succeeded) {
      try {
        HttpURLConnection connection = openConnection(batchUrl);
        try {
          downloadBytes(connection, dest, offset, length);
          succeeded = true;
        } finally {
          connection.disconnect();
        }
      } catch (IOException e) {
        if (++retries > configuration.maxRetries()) {
          throw new RuntimeException(e);
        } else {
          earliestNextAttempt = CLOCK.instant().plusMillis(getRetryDelayMs());
        }
      }
    }
  }

  protected void awaitNextAttemptTime() throws InterruptedException {
    long timeToSleep = CLOCK.instant().until(earliestNextAttempt, ChronoUnit.MILLIS);
    while (timeToSleep > 0) {
      Thread.sleep(timeToSleep);
      timeToSleep = CLOCK.instant().until(earliestNextAttempt, ChronoUnit.MILLIS);
    }
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebSeedClient that = (WebSeedClient) o;
    return getRetryDelayMs() == that.getRetryDelayMs() && Objects.equals(getProxy(),
        that.getProxy()) &&
        Objects.equals(getSocketFactory(), that.getSocketFactory()) && userAgent.equals(that.userAgent);
  }

  @Override public int hashCode() {
    return Objects.hash(getProxy(), getSocketFactory(), getRetryDelayMs(), userAgent);
  }

  /**
   * The proxy to use with this server, or null to use the JVM default.
   */
  @Nullable protected Proxy getProxy() {
    return configuration.proxy();
  }

  /**
   * The SSLSocketFactory to use with this server.
   */
  @Nullable protected SSLSocketFactory getSocketFactory() {
    return configuration.socketFactory();
  }

  /**
   * Wait this many milliseconds before trying again after an IOException.
   */
  protected long getRetryDelayMs() {
    return configuration.retryDelayMs();
  }
}
