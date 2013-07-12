//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2013 by:

 IDgis bv

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

 IDgis bv
 Boomkamp 16
 7461 AX Rijssen
 The Netherlands
 http://idgis.nl/ 

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
package org.deegree.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.ResourceInitException;
import org.deegree.workspace.Workspace;

/**
 * The {@link java.lang.reflect.InvocationHandler} used by 
 * {@link org.deegree.spring.WorkspaceBeanFactory} to generate
 * {@link org.deegree.workspace.Resource} proxies.
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
class ResourceInvocationHandler implements InvocationHandler {

    private final Workspace workspace;

    private final ResourceIdentifier<?> resourceIdentifier;

    private final Set<Method> forResourceIdentifier;

    private final Method toString, getIdentifier, getWorkspace;

    private Object resource = null;

    ResourceInvocationHandler( final Workspace workspace, final ResourceIdentifier<?> resourceIdentifier ) {
        this.workspace = workspace;
        this.resourceIdentifier = resourceIdentifier;

        try {
            forResourceIdentifier = new HashSet<Method>();
            forResourceIdentifier.add( Object.class.getMethod( "equals", Object.class ) );
            forResourceIdentifier.add( Object.class.getMethod( "hashCode" ) );

            toString = Object.class.getMethod( "toString" );

            getIdentifier = ResourceProxy.class.getMethod( "getIdentifier" );
            getWorkspace = ResourceProxy.class.getMethod( "getWorkspace" );
        } catch ( Exception e ) {
            throw new ResourceInitException( "Couldn't construct proxy invocation handler for " + resourceIdentifier, e );
        }
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args )
                            throws Throwable {

        if ( method.equals( toString ) ) {
            return "Proxy for deegree workspace resource: " + resourceIdentifier;
        }

        if ( method.equals( getIdentifier ) ) {
            return resourceIdentifier;
        }
        
        if ( method.equals( getWorkspace ) ) {
            return workspace;
        }

        if ( forResourceIdentifier.contains( method ) ) {
            return method.invoke( resourceIdentifier, args );
        }

        if ( resource == null ) {
            resource = workspace.getResource( resourceIdentifier.getProvider(), resourceIdentifier.getId() );

            if ( resource == null ) {
                throw new IllegalStateException( "Workspace resource " + resourceIdentifier
                                                 + " is not available. Dependency missing?" );
            }
        }

        return method.invoke( resource, args );
    }
}
