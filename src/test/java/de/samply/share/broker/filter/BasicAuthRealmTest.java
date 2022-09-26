package de.samply.share.broker.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.samply.share.broker.utils.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicAuthRealmTest {

  private static final String USERNAME = "username-162152";
  private static final String PASSWORD = "password-162152";

  @Mock
  private Config config;

  @Test
  void isAuthenticated_true() {
    when(config.getProperty("icinga.username")).thenReturn(USERNAME);
    when(config.getProperty("icinga.password")).thenReturn(PASSWORD);

    assertTrue(BasicAuthRealm.ICINGA.isAuthenticated(config, USERNAME, PASSWORD));
  }

  @Test
  void isAuthenticated_false() {
    when(config.getProperty("icinga.username")).thenReturn(USERNAME);
    when(config.getProperty("icinga.password")).thenReturn(PASSWORD);

    assertFalse(BasicAuthRealm.ICINGA.isAuthenticated(config, USERNAME, "foo"));
  }

  @Test
  void isAuthenticated_otherRealm() {
    assertFalse(BasicAuthRealm.STRUCTURED_QUERY.isAuthenticated(config, USERNAME, PASSWORD));
  }

  @Test
  void isAuthenticated_noneRealm() {
    assertFalse(BasicAuthRealm.NONE.isAuthenticated(config, USERNAME, PASSWORD));
  }
}
