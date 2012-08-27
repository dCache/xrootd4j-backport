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
package org.dcache.vehicles;

import java.util.Collection;

import diskCacheV111.movers.NetIFContainer;
import diskCacheV111.vehicles.Message;

public class XrootdDoorAdressInfoMessage extends Message {

	private static final long serialVersionUID = -5306759219838126273L;

	private int xrootdFileHandle;
	private Collection networkInterfaces;
	private int serverPort;
	private final boolean uuidEnabledPool;

	public XrootdDoorAdressInfoMessage(int xrootdFileHandle, int serverPort, Collection networkInterfaces) {

		this(xrootdFileHandle, serverPort, networkInterfaces, false);
	}

	public XrootdDoorAdressInfoMessage(int xrootdFileHandle,
	                                   int serverPort,
	                                   Collection<NetIFContainer> networkInterfaces,
	                                   boolean uuidEnabled) {
		this.xrootdFileHandle = xrootdFileHandle;
		this.serverPort = serverPort;

		this.networkInterfaces = networkInterfaces;
		this.uuidEnabledPool = uuidEnabled;
	}

	public Collection getNetworkInterfaces() {
		return networkInterfaces;
	}

	public int getXrootdFileHandle() {
		return xrootdFileHandle;
	}

	public int getServerPort() {
		return serverPort;
	}

	public boolean isUUIDEnabledPool() {
		return uuidEnabledPool;
	}

}
