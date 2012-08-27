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

import org.dcache.auth.LoginStrategy;
import org.dcache.xrootd.plugins.AuthenticationFactory;
import org.dcache.xrootd.plugins.ChannelHandlerFactory;
import org.jboss.netty.channel.ChannelHandler;

public class LoginAuthenticationHandlerFactory implements ChannelHandlerFactory
{
    private final String _name;
    private final LoginStrategy _loginStrategy;
    private final AuthenticationFactory _authenticationFactory;

    public LoginAuthenticationHandlerFactory(String name,
                                             AuthenticationFactory authenticationFactory,
                                             LoginStrategy loginStrategy)
    {
        _name = name;
        _authenticationFactory = authenticationFactory;
        _loginStrategy = loginStrategy;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public String getDescription()
    {
        return "Authentication handler";
    }

    @Override
    public ChannelHandler createHandler()
    {
        return new LoginAuthenticationHandler(_authenticationFactory, _loginStrategy);
    }
}
