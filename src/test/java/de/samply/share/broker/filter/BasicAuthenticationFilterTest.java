package de.samply.share.broker.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BasicAuthenticationFilterTest {

  @Test
  void decodeCredentials_empty() {
    assertFalse(BasicAuthenticationFilter.decodeCredentials("Basic").isPresent());
  }

  @Test
  void decodeCredentials_onlyUsername() {
    String authHeader = "Basic " + encode("user:");

    Optional<String[]> credentials = BasicAuthenticationFilter.decodeCredentials(authHeader);

    assertFalse(credentials.isPresent());
  }

  @Test
  void decodeCredentials() {
    String authHeader = "Basic " + encode("foo:bar");

    Optional<String[]> credentials = BasicAuthenticationFilter.decodeCredentials(authHeader);

    assertTrue(credentials.isPresent());
    assertEquals("foo", credentials.get()[0]);
    assertEquals("bar", credentials.get()[1]);
  }

  private static String encode(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(UTF_8));
  }
}
