package org.deegree.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.ResourceInitException;
import org.deegree.workspace.Workspace;

public class ResourceInvocationHandler implements InvocationHandler {

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
