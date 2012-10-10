//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.services.controller;

import javax.servlet.http.HttpServletRequest;

/**
 * Encapsulates security and other information that are associated with the currently processed request.
 * 
 * @see OGCFrontController#getServletContext()
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author: schneider $
 * 
 * @version $Revision: $, $Date: $
 */
public class RequestContext {

    private final String requestedEndpointUrl;

    private final Credentials credentials;

    private final String webappBaseUrl;

    /**
     * @param request
     *            request for which the context will be created
     * @param credentials
     *            credentials associated with the request
     */
    RequestContext( HttpServletRequest request, Credentials credentials ) {
        requestedEndpointUrl = request.getRequestURL().toString();
        this.credentials = credentials;
        webappBaseUrl = deriveWebappBaseUrl( requestedEndpointUrl, request );
    }

    private String deriveWebappBaseUrl( String requestedEndpointUrl, HttpServletRequest request ) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        int webappBaseUrlLength = requestedEndpointUrl.length() - servletPath.length();
        if ( pathInfo != null ) {
            webappBaseUrlLength -= pathInfo.length();
        }
        return requestedEndpointUrl.substring( 0, webappBaseUrlLength );
    }

    /**
     * Returns the endpoint URL that was used to contact the {@link OGCFrontController} and initiated the request.
     * 
     * @return the endpoint URL, never <code>null</code>
     */
    public String getRequestedEndpointUrl() {
        return requestedEndpointUrl;
    }

    /**
     * Returns the base webapp URL that was used to contact the {@link OGCFrontController} and initiated the request.
     * 
     * @return the base webapp URL (without trailing slash or questionmark), never <code>null</code>
     */
    public String getRequestedWebappBaseUrl() {
        return webappBaseUrl;
    }

    /**
     * @return the credentials, can be <code>null</code>
     */
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return "{credentials=" + credentials + ",requestURL=" + requestedEndpointUrl + "}";
    }
}
