/**
 * Copyright (C) 2012 dCache.org <support@dcache.org>
 *
 * This file is part of xrootd4j-backport.
 *
 * xrootd4j-backport is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * xrootd4j-backport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with xrootd4j-backport.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.dcache.xrootd.door;

import javax.security.auth.Subject;

import diskCacheV111.util.PermissionDeniedCacheException;
import diskCacheV111.util.CacheException;

import org.dcache.auth.LoginReply;
import org.dcache.auth.LoginStrategy;

import org.dcache.xrootd.core.XrootdAuthenticationHandler;
import org.dcache.xrootd.core.XrootdException;
import org.dcache.xrootd.plugins.AuthenticationFactory;
import static org.dcache.xrootd.protocol.XrootdProtocol.*;

import org.jboss.netty.channel.ChannelHandlerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An XrootdAuthenticationHandler which after successful
 * authentication delegates login to a LoginStrategy.
 *
 * Generates a LoginEvent after successful login.
 */
public class LoginAuthenticationHandler
    extends XrootdAuthenticationHandler
{
    private final static Logger _log =
        LoggerFactory.getLogger(LoginAuthenticationHandler.class);

    private LoginStrategy _loginStrategy;

    public LoginAuthenticationHandler(AuthenticationFactory authenticationFactory, LoginStrategy loginStrategy)
    {
        super(authenticationFactory);
        _loginStrategy = loginStrategy;
    }

    @Override
    protected Subject login(ChannelHandlerContext context, Subject subject)
        throws XrootdException
    {
        try {
            LoginReply reply = _loginStrategy.login(subject);
            context.sendUpstream(new LoginEvent(context.getChannel(), reply));
            return reply.getSubject();
        } catch (PermissionDeniedCacheException e) {
            _log.warn("Authorization denied for {}: {}",
                      subject, e.getMessage());
            throw new XrootdException(kXR_NotAuthorized, e.getMessage());
        } catch (CacheException e) {
            _log.error("Authorization failed for {}: {}",
                       subject, e.getMessage());
            throw new XrootdException(kXR_ServerError, e.getMessage());
        }
    }
}