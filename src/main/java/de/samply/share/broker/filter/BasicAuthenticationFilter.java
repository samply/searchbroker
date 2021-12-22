package de.samply.share.broker.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import de.samply.share.common.utils.ProjectInfo;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Base64;
import java.util.Optional;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.apache.http.HttpHeaders;

@BasicAuthSecure
@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthenticationFilter implements ContainerRequestFilter {

  private static final String AUTHENTICATION_SCHEME = "Basic";

  @Context
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(AUTHENTICATION_SCHEME)) {
      requestContext.abortWith(Response.status(UNAUTHORIZED)
          .header("WWW-Authenticate", AUTHENTICATION_SCHEME).build());
      return;
    }

    BasicAuthRealm realm = extractRealm(resourceInfo.getResourceMethod());
    if (!isAuthenticated(realm, authHeader)) {
      requestContext.abortWith(Response.status(UNAUTHORIZED).build());
    }
  }

  private static BasicAuthRealm extractRealm(AnnotatedElement annotatedElement) {
    if (annotatedElement == null) {
      return BasicAuthRealm.NONE;
    } else {
      BasicAuthSecure secured = annotatedElement.getAnnotation(BasicAuthSecure.class);
      if (secured == null) {
        return BasicAuthRealm.NONE;
      } else {
        return secured.value();
      }
    }
  }

  private static boolean isAuthenticated(BasicAuthRealm realm, String authHeader) {
    return decodeCredentials(authHeader)
        .map(creds -> realm.isAuthenticated(ProjectInfo.INSTANCE.getConfig(), creds[0], creds[1]))
        .orElse(false);
  }

  static Optional<String[]> decodeCredentials(String authHeader) {
    String base64Credentials = authHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
    String credentials = new String(credDecoded, UTF_8);
    String[] parts = credentials.split(":", 2);
    return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()
        ? Optional.of(parts)
        : Optional.empty();
  }
}
