/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.guest;

import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that allows guest user access via a guest user account. The guest/guest account must be
 * present in the user store for this handler to work correctly.
 */
public class GuestHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GuestHandler.class.getName());

  /** Guest type to use when configuring context policy. */
  public static final String AUTH_TYPE = "GUEST";

  public static final String INVALID_MESSAGE = "Username/Password is invalid.";

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  /**
   * This method takes a guest request and attaches a username token to the HTTP request to allow
   * access. The method also allows the user to sign-in and authenticate.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return HandlerResult
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationException {
    HandlerResult result = new HandlerResult();

    // For guest - always return guest credentials
    BaseAuthenticationToken authToken = new GuestAuthenticationToken(request.getRemoteAddr());

    result.setSource("GuestHandler");
    result.setStatus(HandlerResult.Status.COMPLETED);
    result.setToken(authToken);
    return result;
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    try {
      httpResponse.getWriter().write(INVALID_MESSAGE);
      httpResponse.flushBuffer();
    } catch (IOException e) {
      LOGGER.debug("Failed to send auth response: {}", e);
    }

    HandlerResult result = new HandlerResult();
    result.setSource("GuestHandler");
    LOGGER.debug("In error handler for guest - returning action completed.");
    result.setStatus(HandlerResult.Status.REDIRECTED);
    return result;
  }
}
