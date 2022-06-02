package de.samply.share.broker.filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class ForwardedHeaderFilter implements ContainerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(ForwardedHeaderFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    UriInfo uriInfo = requestContext.getUriInfo();

    URI baseUri = baseUri(requestContext);

    requestContext.setRequestUri(
        uriInfo.getBaseUriBuilder()
            .scheme(baseUri.getScheme())
            .host(baseUri.getHost())
            .port(baseUri.getPort())
            .build(),
        uriInfo.getRequestUriBuilder()
            .scheme(baseUri.getScheme())
            .host(baseUri.getHost())
            .port(baseUri.getPort())
            .build()
    );
  }

  private URI baseUri(ContainerRequestContext requestContext) {
    String scheme = scheme(requestContext);
    String host = host(requestContext);

    try {
      return new URI(scheme + "://" + host);
    } catch (URISyntaxException e) {
      logger.warn(
          "Ignoring invalid X-Forwarded-Proto and/or X-Forwarded-Host headers: `{}` and `{}`",
          requestContext.getHeaderString("X-Forwarded-Proto"),
          requestContext.getHeaderString("X-Forwarded-Host"));
      return requestContext.getUriInfo().getBaseUri();
    }
  }

  private String scheme(ContainerRequestContext requestContext) {
    String scheme = requestContext.getHeaderString("X-Forwarded-Proto");
    return scheme == null ? requestContext.getUriInfo().getBaseUri().getScheme() : scheme;
  }

  private String host(ContainerRequestContext requestContext) {
    String host = requestContext.getHeaderString("X-Forwarded-Host");
    return host == null ? requestContext.getUriInfo().getBaseUri().getHost() : host;
  }
}
