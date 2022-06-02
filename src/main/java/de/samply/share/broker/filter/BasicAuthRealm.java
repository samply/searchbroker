package de.samply.share.broker.filter;

import de.samply.share.common.utils.AbstractConfig;

/**
 * A protection space of resources secured by credentials.
 */
public enum BasicAuthRealm {

  ICINGA("icinga.username", "icinga.password"),
  STRUCTURED_QUERY("structured.query.username", "structured.query.password"),
  NONE(null, null);

  public final String usernameProperty;
  public final String passwordProperty;

  BasicAuthRealm(String username, String password) {
    this.usernameProperty = username;
    this.passwordProperty = password;
  }

  /**
   * Returns {@code true} if the user with the given {@code username} and {@code password} is
   * authenticated according to this realm using the given {@code config}.
   *
   * @param config   the config to use for obtaining the credentials
   * @param username the username of the user being authenticated
   * @param password the password  of the user being authenticated
   * @return {@code true} if the user could be authenticated, {@code false} if not
   */
  public boolean isAuthenticated(AbstractConfig config, String username, String password) {
    return usernameProperty != null
        && username.equals(config.getProperty(usernameProperty))
        && password.equals(config.getProperty(passwordProperty));
  }
}
