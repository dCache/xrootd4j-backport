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
package diskCacheV111.vehicles;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * @author Patrick F.
 * @author Timur Perelmutov. timur@fnal.gov
 * @version 0.0, 28 Jun 2002
 */

public class HttpProtocolInfo implements IpProtocolInfo
{
  private String _name  = "Unkown" ;
  private int    _minor;
  private int    _major;
  private InetSocketAddress _clientSocketAddress;
  private final String [] _hosts;
  private int    _port;
  private long   _transferTime;
  private long   _bytesTransferred;

  /* TODO: Change this to long (but remember backwards compatibility!) */
  private int    _sessionId;

  private boolean _writeAllowed;
  private String httpDoorCellName;
  private String httpDoorDomainName;
  private String path;
  private URI _location;

  private static final long serialVersionUID = 8002182588464502270L;

  public HttpProtocolInfo(String protocol, int major , int minor ,
                          InetSocketAddress clientSocketAddress,
                          String httpDoorCellName ,
                          String httpDoorDomainName,
                          String path) {
      this(protocol, major, minor, clientSocketAddress,
           httpDoorCellName, httpDoorDomainName, path, null);
  }

  public HttpProtocolInfo( String protocol, int major , int minor ,
                           InetSocketAddress clientSocketAddress,
                           String httpDoorCellName ,
                           String httpDoorDomainName,
                           String path,
                           URI location)
  {
    _name  = protocol ;
    _minor = minor ;
    _major = major ;
    _clientSocketAddress = clientSocketAddress;
    _hosts = new String[] { _clientSocketAddress.getAddress().getHostAddress() };
    _port  = _clientSocketAddress.getPort() ;
    this.httpDoorCellName = httpDoorCellName;
    this.httpDoorDomainName = httpDoorDomainName;
    this.path = path;
    _location = location;
  }

  public String getHttpDoorCellName()
  {
    return httpDoorCellName;
  }
  public String getHttpDoorDomainName()
  {
    return httpDoorDomainName;
  }
  public String getPath()
  {
    return path;
  }
  public int getSessionId()
  {
    return _sessionId ;
  }
  public void setSessionId( int sessionId )
  {
    _sessionId = sessionId ;
  }
  //
  //  the ProtocolInfo interface
  //
  @Override
  public String getProtocol(){ return _name ; }
  @Override
  public int    getMinorVersion()
  {
    return _minor ;
  }

  @Override
  public int    getMajorVersion()
  {
    return _major ;
  }

  @Override
  public String getVersionString()
  {
    return _name+"-"+_major+"."+_minor ;
  }

  //
  // and the private stuff
  //
  @Override
  public int    getPort(){ return _port ; }
  @Override
  public String [] getHosts(){ return _hosts ; }
  public void   setBytesTransferred( long bytesTransferred )
  {
    _bytesTransferred = bytesTransferred ;
  }

  public void   setTransferTime( long transferTime )
  {
    _transferTime = transferTime ;
  }

  public long getTransferTime()
  {
    return _transferTime ;
  }

  public long getBytesTransferred()
  {
    return _bytesTransferred ;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder() ;
    sb.append(getVersionString()) ;
    for(int i = 0 ; i < _hosts.length ; i++ )
    {
      sb.append(',').append(_hosts[i]) ;
    }
    sb.append(':').append(_port) ;
    sb.append(':').append(httpDoorCellName);
    sb.append(':').append(httpDoorDomainName);
    sb.append(':').append(path);

    return sb.toString() ;
  }
  //
  // io mode
  //
  public boolean isWriteAllowed()
  {
    return _writeAllowed ;
  }

  public void    setAllowWrite( boolean allow )
  {
    _writeAllowed = allow ;
  }

    @Override
    public InetSocketAddress getSocketAddress() {
        return _clientSocketAddress;
    }

    /**
     * Returns the location of the file. The location is defined
     * as for the HTTP location header for a 201 response, or for
     * the content-location header for other replies. It points
     * to the original URI as seen at the HTTP door.
     */
    public URI getLocation() {
        return _location;
    }
}



