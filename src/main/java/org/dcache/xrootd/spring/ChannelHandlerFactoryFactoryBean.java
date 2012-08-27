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
package org.dcache.xrootd.spring;

import com.google.common.collect.Lists;
import dmg.cells.nucleus.EnvironmentAware;
import dmg.util.Formats;
import dmg.util.Replaceable;
import org.dcache.auth.LoginStrategy;
import org.dcache.xrootd.door.LoginAuthenticationHandlerFactory;
import org.dcache.xrootd.plugins.AuthenticationFactory;
import org.dcache.xrootd.plugins.AuthenticationProvider;
import org.dcache.xrootd.plugins.ChannelHandlerFactory;
import org.dcache.xrootd.plugins.ChannelHandlerProvider;
import org.dcache.xrootd.plugins.authn.none.NoAuthenticationFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.collect.Iterables.*;
import static java.util.Arrays.asList;

/**
 * A Spring FactoryBean that creates ChannelHandlerFactory instances.
 *
 * A ChannelHandlerFactory is created by an ChannelHandlerProvider.
 * The FactoryBean uses the Java 6 ServiceLoader system to obtain
 * ChannelHandlerProvider instances.
 */
public class ChannelHandlerFactoryFactoryBean
        implements FactoryBean, EnvironmentAware
{
    private final static ServiceLoader<ChannelHandlerProvider> _channelHandlerProviders =
            ServiceLoader.load(ChannelHandlerProvider.class);
    private final static ServiceLoader<AuthenticationProvider> _authenticationProviders =
            ServiceLoader.load(AuthenticationProvider.class);

    private final static String GPLAZMA_PREFIX = "gplazma:";

    private List<String> _plugins;
    private Properties _properties = new Properties();
    private LoginStrategy _loginStrategy;
    private LoginStrategy _anonymousLoginStrategy;

    @Required
    public void setPlugins(String plugins)
    {
        _plugins = asList(plugins.split(","));

        if (any(_plugins, containsPattern("^authn:"))) {
            throw new IllegalArgumentException("The authn: prefix is not allowed in the xrootd door");
        }

        if (size(filter(_plugins, containsPattern("^gplazma:"))) != 1) {
            throw new IllegalArgumentException("Exactly one authentication plugin is required");
        }

        int authn = indexOf(_plugins, containsPattern("^gplazma:"));
        int authz = indexOf(_plugins, containsPattern("^authz:"));
        if (authz > -1 && authz < authn) {
            throw new IllegalArgumentException("Authorization plugins must be placed after authentication plugins");
        }
    }

    @Override
    public void setEnvironment(Map<String,Object> environment)
    {
        _properties = toProperties(environment);
    }

    public void setLoginStrategy(LoginStrategy loginStrategy)
    {
        _loginStrategy = loginStrategy;
    }

    public void setAnonymousLoginStrategy(
            LoginStrategy anonymousLoginStrategy)
    {
        _anonymousLoginStrategy = anonymousLoginStrategy;
    }

    @Override
    public Object getObject()
        throws Exception
    {
        List<ChannelHandlerFactory> factories = Lists.newArrayList();
        for (String plugin: _plugins) {
            /* We need special logic for the authentication handler as we
             * cannot use a generic provider: The provider would not have
             * access to the login strategies. REVISIT: Is there some way
             * we could get Spring to inject them anyway?
             */
            if (plugin.startsWith(GPLAZMA_PREFIX)) {
                String name = plugin.substring(GPLAZMA_PREFIX.length());
                factories.add(createAuthenticationHandlerFactory(name));
            } else {
                factories.add(createChannelHandlerFactory(plugin));
            }
        }
        return factories;
    }

    @Override
    public Class getObjectType()
    {
        return List.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    private ChannelHandlerFactory createChannelHandlerFactory(String plugin)
            throws Exception
    {
        for (ChannelHandlerProvider provider: _channelHandlerProviders) {
            ChannelHandlerFactory factory =
                    provider.createFactory(plugin, _properties);
            if (factory != null) {
                return factory;
            }
        }
        throw new IllegalArgumentException("Channel handler plugin not found: " + plugin);
    }

    private ChannelHandlerFactory createAuthenticationHandlerFactory(
            String name) throws Exception
    {
        if (name.equals("none")) {
            return new LoginAuthenticationHandlerFactory(
                    GPLAZMA_PREFIX + "none", new NoAuthenticationFactory(), _anonymousLoginStrategy);
        }

        for (AuthenticationProvider provider: _authenticationProviders) {
            AuthenticationFactory factory = provider.createFactory(name, _properties);
            if (factory != null) {
                return new LoginAuthenticationHandlerFactory(
                        GPLAZMA_PREFIX + name, factory, _loginStrategy);
            }
        }
        throw new IllegalArgumentException("Authentication plugin not found: " + name);
    }

    private static Properties toProperties(final Map<String,Object> env)
    {
        Replaceable replaceable = new Replaceable() {
            @Override
            public String getReplacement(String name)
            {
                Object value =  env.get(name);
                return (value == null) ? null : value.toString().trim();
            }
        };

        Properties properties = new Properties();
        for (Map.Entry<String,Object> e: env.entrySet()) {
            String key = e.getKey();
            String value = String.valueOf(e.getValue());
            properties.put(key, Formats.replaceKeywords(value, replaceable));
        }

        return properties;
    }
}