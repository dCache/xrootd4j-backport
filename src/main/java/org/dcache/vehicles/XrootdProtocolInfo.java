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

import java.net.InetSocketAddress;
import java.util.UUID;

import diskCacheV111.util.PnfsId;
import diskCacheV111.vehicles.IpProtocolInfo;
import dmg.cells.nucleus.CellPath;

public class XrootdProtocolInfo implements IpProtocolInfo {

	private static final long serialVersionUID = -7070947404762513894L;

	private String _name;

	private int _minor;

	private int _major;

        private final String[] _hosts;

        private int _port;

        private InetSocketAddress _clientSocketAddress;

	private CellPath _pathToDoor;

	private PnfsId _pnfsId;

	private int _xrootdFileHandle;

	private String _path;

	private UUID _uuid;

	private InetSocketAddress _doorAddress;

	public XrootdProtocolInfo(String protocol,  int major,int minor,
		InetSocketAddress clientAddress, CellPath pathToDoor, PnfsId pnfsID,
			int xrootdFileHandle, UUID uuid,
			InetSocketAddress doorAddress) {

		_name = protocol;
		_minor = minor;
		_major = major;
                _clientSocketAddress = clientAddress;
                _hosts = new String[] {_clientSocketAddress.getAddress().getHostAddress() };
                _port = _clientSocketAddress.getPort();
		_pathToDoor = pathToDoor;
		_pnfsId = pnfsID;
		_xrootdFileHandle = xrootdFileHandle;
		_uuid = uuid;
		_doorAddress = doorAddress;
	}

        @Override
        @Deprecated
	public String[] getHosts() {
		return _hosts;
	}

        @Override
        @Deprecated
	public int getPort() {
		return _port;
	}

	@Override
        public String getProtocol() {
		return _name;
	}

	@Override
        public int getMinorVersion() {
		return _minor;
	}

	@Override
        public int getMajorVersion() {
		return _major;
	}

	@Override
        public String getVersionString() {
		return _name + "-" + _major + "." + _minor;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getVersionString());
		for (int i = 0; i < _hosts.length; i++) {
                    sb.append(",").append(_hosts[i]);
                }
		sb.append(":").append(_port);

		return sb.toString();
	}

	public CellPath getXrootdDoorCellPath() {
		return _pathToDoor;
	}

	public void setXrootdDoorCellPath(CellPath toDoor) {
		_pathToDoor = toDoor;
	}

	public PnfsId getPnfsId() {
		return _pnfsId;
	}

	public int getXrootdFileHandle() {
		return _xrootdFileHandle;
	}

	public boolean isFileCheckRequired() {
//		we do it the fast way. The PoolMgr will not check whether a file is really on the pool where
//		it is supposed to be. This saves one message.
		return false;
	}

	public UUID getUUID() {
		return _uuid;
	}

	public InetSocketAddress getDoorAddress() {
		return _doorAddress;
	}

	public void setPath(String path)
	{
		_path = path;
	}

	public String getPath()
	{
		return _path;
	}

        @Override
        public InetSocketAddress getSocketAddress()
        {
            return _clientSocketAddress;
        }
}
