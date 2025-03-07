/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.filter;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

/**
 * Class IPFilterTest
 * <p>
 * IP Filter tests
 *
 * @author sbinda
 */
public class JWTAuthenticationFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilterTest.class);

    private final JWTService jwtService = new JWTService();

    @Before
    public void init() {
        jwtService.setSecret("!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!");
    }

    /**
     * Check security filter with no Jwt access token
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token")
    @Test
    public void jwtFilterAccessDeniedWithoutToken() {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager,
                                                                           Mockito.mock(IRuntimeTenantResolver.class),
                                                                           Collections.emptySet());

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("Authentication should fail.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check security filter with no Jwt access token
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token for public access (scope as query parameter)")
    @Test
    public void jwtFilterPublicAccess() throws JwtException {
        // the public filter should generate this token:
        JWTAuthentication token = jwtService.parseToken(new JWTAuthentication(jwtService.generateToken("project-test",
                                                                                                       "public",
                                                                                                       "public@regards.com",
                                                                                                       DefaultRole.PUBLIC.name())));

        final MockHttpServletRequest mockedRequest = new MockHttpServletRequest();
        mockedRequest.addParameter(HttpConstants.SCOPE, "project-test");

        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        PublicAuthenticationFilter publicFilter = new PublicAuthenticationFilter(jwtService, Collections.emptySet());
        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);
        Mockito.when(mockedManager.authenticate(any(JWTAuthentication.class))).thenReturn(token);
        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager,
                                                                           Mockito.mock(IRuntimeTenantResolver.class),
                                                                           Collections.emptySet());

        DispatcherServlet servlet = Mockito.mock(DispatcherServlet.class);
        MockFilterChain mockedFilterChain = new MockFilterChain(servlet, publicFilter, filter);

        try {
            mockedFilterChain.doFilter(mockedRequest, mockedResponse);
            // filter.doFilter(mockedFilterChain.getRequest(), mockedFilterChain.getResponse(), mockedFilterChain);
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check security filter with no Jwt access token
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with no Jwt access token for public access (scope in header)")
    @Test
    public void jwtFilterPublicAccessWithHeader() throws JwtException {

        // the public filter should generate this token:
        String tenant = "project-test";
        String jwt = jwtService.generateToken(tenant, "public", "public@regards.com", DefaultRole.PUBLIC.name());
        JWTAuthentication token = jwtService.parseToken(new JWTAuthentication(jwt));

        final MockHttpServletRequest mockedRequest = new MockHttpServletRequest();
        mockedRequest.addHeader(HttpConstants.SCOPE, tenant);

        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        PublicAuthenticationFilter publicFilter = new PublicAuthenticationFilter(jwtService, Collections.emptySet());
        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);
        // As generateToken seems to have some random added into computation, we cannot specify what is expected
        Mockito.when(mockedManager.authenticate(any())).thenReturn(token);
        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager,
                                                                           Mockito.mock(IRuntimeTenantResolver.class),
                                                                           Collections.emptySet());

        DispatcherServlet servlet = Mockito.mock(DispatcherServlet.class);
        MockFilterChain mockedFilterChain = new MockFilterChain(servlet, publicFilter, filter);

        try {
            mockedFilterChain.doFilter(mockedRequest, mockedResponse);
            // filter.doFilter(mockedFilterChain.getRequest(), mockedFilterChain.getResponse(), mockedFilterChain);
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * "Check security filter with Jwt access token
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with invalid request authorization header")
    @Test
    public void jwtFilterAccessDeniedTest() {

        final JWTAuthentication token = new JWTAuthentication(jwtService.generateToken("PROJECT",
                                                                                       "test",
                                                                                       "test@test.test",
                                                                                       "USER"));

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);

        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager,
                                                                           Mockito.mock(IRuntimeTenantResolver.class),
                                                                           Collections.emptySet());

        // Header whithout Bearer: prefix.
        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(token.getJwt());

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("Authentication should fail.");
            }
        } catch (final InsufficientAuthenticationException e) {
            // Nothing to do
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check security filter with valid Jwt access token
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Check security filter with valid Jwt access token")
    @Test
    public void jwtFilterAccessGrantedTest() throws JwtException {

        JWTAuthentication token = new JWTAuthentication(jwtService.generateToken("PROJECT",
                                                                                 "test",
                                                                                 "test@test.test",
                                                                                 "USER"));
        token = jwtService.parseToken(token);

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final AuthenticationManager mockedManager = Mockito.mock(AuthenticationManager.class);
        Mockito.when(mockedManager.authenticate(token)).thenReturn(token);
        final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(mockedManager,
                                                                           Mockito.mock(IRuntimeTenantResolver.class),
                                                                           Collections.emptySet());

        Mockito.when(mockedRequest.getHeader(HttpConstants.AUTHORIZATION))
               .thenReturn(String.format("%s %s", HttpConstants.BEARER, token.getJwt()));

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Authentication should be granted");
            }
        } catch (IOException | ServletException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void jwtFilterShouldNotFilter() {
        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getRequestURI()).thenReturn("/some/route");

        final JWTAuthenticationFilter filterWithEmptyNoSecurityRoutes = new JWTAuthenticationFilter(null,
                                                                                                    null,
                                                                                                    Collections.emptySet());
        Assert.assertFalse(filterWithEmptyNoSecurityRoutes.shouldNotFilter(mockedRequest));

        final JWTAuthenticationFilter filterWithANoSecurityRoutes = new JWTAuthenticationFilter(null,
                                                                                                null,
                                                                                                Sets.newHashSet(
                                                                                                    "/some/route"));
        Assert.assertTrue(filterWithANoSecurityRoutes.shouldNotFilter(mockedRequest));

    }
}
