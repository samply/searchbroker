package de.samply.share.broker.filter;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.omnifaces.filter.HttpFilter;
import org.omnifaces.util.Servlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access Control Filter.
 * If a user is trying to go to an area he is not supposed to, redirect him to his start page.
 */
@WebFilter("/ccp_office/*")
public class AccessControlFilterCcpOffice extends HttpFilter {

  private static final Logger logger = LoggerFactory.getLogger(AccessControlFilterCcpOffice.class);

  /**
   * The Constant SESSION_ROLE.
   */
  private static final String SESSION_ROLES = "roles";

  /**
   * The Constant CCP OFFICE.
   */
  private static final String ROLE_CCP_OFFICE = "ccp_office";

  private static final String SESSION_USERNAME = "username";

  @Override
  public void doFilter(HttpServletRequest request, HttpServletResponse response,
      HttpSession session, FilterChain chain) throws ServletException, IOException {
    String loginUrl = request.getContextPath() + "/loginRedirect.xhtml";
    String dashboardUrl = request.getContextPath() + "/dashboard.xhtml";
    boolean loggedIn = (session != null) && (session.getAttribute(SESSION_USERNAME) != null);
    boolean loginRequest = request.getRequestURI().equals(loginUrl);
    boolean resourceRequest = Servlets.isFacesResourceRequest(request);

    String roleConcat = (String) request.getSession().getAttribute(SESSION_ROLES);
    List<String> roles = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(roleConcat);

    if (loggedIn || loginRequest || resourceRequest) {
      if (!resourceRequest) { // Prevent browser from caching restricted resources. See also http://stackoverflow.com/q/4194207/157882
        Servlets.setNoCacheHeaders(response);
      }
      if (roles != null && roles.contains(ROLE_CCP_OFFICE)) {
        chain.doFilter(request, response); // So, just continue request.
      } else {
        String username = (String) request.getSession().getAttribute(SESSION_USERNAME);
        logger.warn("User " + username + " tried to access ccp office area. Access was denied.");
        Servlets.facesRedirect(request, response, dashboardUrl);
      }

    } else {
      Servlets.facesRedirect(request, response, loginUrl);
    }
  }


}
