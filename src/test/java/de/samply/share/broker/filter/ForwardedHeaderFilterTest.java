package de.samply.share.broker.filter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForwardedHeaderFilterTest {

  private static final URI LOCAL_BASE_URI = URI.create("http://localhost:8080");

  @Mock
  private ContainerRequestContext context;

  @Mock
  private UriInfo uriInfo;

  private ForwardedHeaderFilter filter;

  @BeforeEach
  void setUp() {
    filter = new ForwardedHeaderFilter();

    when(context.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(LOCAL_BASE_URI));
    when(uriInfo.getRequestUriBuilder()).thenReturn(
        UriBuilder.fromUri(LOCAL_BASE_URI.resolve("/resource/1")));
  }

  @Test
  void filter_WithBothHeadersPresent() throws IOException {
    when(context.getHeaderString("X-Forwarded-Proto")).thenReturn("https");
    when(context.getHeaderString("X-Forwarded-Host")).thenReturn("extern");

    filter.filter(context);

    verify(context).setRequestUri(URI.create("https://extern"),
        URI.create("https://extern/resource/1"));
  }

  @Test
  void filter_WithHostHeaderPresent() throws IOException {
    when(uriInfo.getBaseUri()).thenReturn(LOCAL_BASE_URI);
    when(context.getHeaderString("X-Forwarded-Proto")).thenReturn(null);
    when(context.getHeaderString("X-Forwarded-Host")).thenReturn("extern");

    filter.filter(context);

    verify(context).setRequestUri(URI.create("http://extern"),
        URI.create("http://extern/resource/1"));
  }

  @Test
  void filter_WithHostHeaderAndPortPresent() throws IOException {
    when(uriInfo.getBaseUri()).thenReturn(LOCAL_BASE_URI);
    when(context.getHeaderString("X-Forwarded-Proto")).thenReturn(null);
    when(context.getHeaderString("X-Forwarded-Host")).thenReturn("extern:9090");

    filter.filter(context);

    verify(context).setRequestUri(URI.create("http://extern:9090"),
        URI.create("http://extern:9090/resource/1"));
  }

  @Test
  void filter_WithCustomPort() throws IOException {
    when(context.getHeaderString("X-Forwarded-Proto")).thenReturn("https");
    when(context.getHeaderString("X-Forwarded-Host")).thenReturn("extern:8443");

    filter.filter(context);

    verify(context).setRequestUri(URI.create("https://extern:8443"),
        URI.create("https://extern:8443/resource/1"));
  }

  @Test
  void filter_WithInvalidProtoHeader() throws IOException {
    when(uriInfo.getBaseUri()).thenReturn(LOCAL_BASE_URI);
    when(context.getHeaderString("X-Forwarded-Proto")).thenReturn(":");
    when(context.getHeaderString("X-Forwarded-Host")).thenReturn(null);

    filter.filter(context);

    verify(context).setRequestUri(LOCAL_BASE_URI, LOCAL_BASE_URI.resolve("/resource/1"));
  }
}
