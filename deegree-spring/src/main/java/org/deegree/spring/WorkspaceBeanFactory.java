package org.deegree.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.deegree.spring.annotation.InjectResource;
import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.ResourceInitException;
import org.deegree.workspace.ResourceManager;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.ResourceProvider;
import org.deegree.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class WorkspaceBeanFactory extends DefaultListableBeanFactory {

    private static final Logger LOG = LoggerFactory.getLogger( WorkspaceBeanFactory.class );

    private final Workspace workspace;

    public WorkspaceBeanFactory( final Workspace workspace ) {
        this.workspace = workspace;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Class<? extends Resource> getResourceClass( final Class<? extends ResourceManager> managerClass ) {
        return (Class<? extends Resource>) doGetResourceClass( managerClass );
    }

    private Type locateResourceType( Type t ) {
        if ( t instanceof ParameterizedType ) {
            final ParameterizedType pt = (ParameterizedType) t;
            for ( final Type typeArgument : pt.getActualTypeArguments() ) {
                if ( typeArgument instanceof Class ) {
                    if ( Resource.class.isAssignableFrom( (Class<?>) typeArgument ) ) {
                        return typeArgument;
                    }
                }
            }
        }

        return null;
    }

    private Type doGetResourceClass( final Class<?> managerClass ) {
        if ( managerClass == null ) {
            return null;
        }

        for ( final Type genericInterface : managerClass.getGenericInterfaces() ) {
            final Type t = locateResourceType( genericInterface );
            if ( t != null ) {
                return t;
            }
        }

        final Type t = locateResourceType( managerClass.getGenericSuperclass() );
        if ( t != null ) {
            return t;
        }

        return doGetResourceClass( managerClass.getSuperclass() );
    }

    protected Object getProxy( Class<?> type, ResourceIdentifier<?> resourceIdentifier ) {
        return Proxy.newProxyInstance( workspace.getModuleClassLoader(), new Class<?>[] { type },
                                       new ResourceInvocationHandler( workspace, resourceIdentifier ) );
    }

    protected ResourceFilter getResourceFilter( String beanName, Class<?> requiredType, DependencyDescriptor descriptor )
                            throws InstantiationException, IllegalAccessException {
        for ( final Annotation annotation : descriptor.getAnnotations() ) {
            if ( annotation instanceof InjectResource ) {
                final InjectResource injectResource = (InjectResource) annotation;

                final Class<? extends ResourceFilter> filter = injectResource.filter();
                final String[] value = injectResource.value();

                if ( filter.equals( NullResourceFilter.class ) ) {
                    if ( value.length > 0 ) {
                        return new IdResourceFilter( Arrays.asList( value ) );
                    }
                } else {
                    if ( value.length > 0 ) {
                        LOG.warn( "@InjectResource value is ignored because a resource filter is configured" );
                    }

                    return filter.newInstance();
                }
            }
        }

        return null;
    }

    @Override
    protected Map<String, Object> findAutowireCandidates( String beanName, Class<?> requiredType,
                                                          DependencyDescriptor descriptor ) {

        final ResourceFilter filter;
        try {
            filter = getResourceFilter( beanName, requiredType, descriptor );
        } catch ( Exception e ) {
            throw new ResourceInitException( "Could not initialize resource filter", e );
        }

        final Map<String, Object> candidates = new HashMap<String, Object>();

        if ( requiredType.isInterface() ) {
            for ( ResourceManager<? extends Resource> manager : workspace.getResourceManagers() ) {
                if ( requiredType.equals( getResourceClass( manager.getClass() ) ) ) {
                    final String workspacePath = manager.getMetadata().getWorkspacePath();
                    for ( ResourceMetadata<?> resourceMetadata : manager.getResourceMetadata() ) {
                        final ResourceIdentifier<?> resourceIdentifier = resourceMetadata.getIdentifier();
                        ResourceProvider<?> provider = resourceMetadata.getProvider();
                        if ( !provider.getNamespace().startsWith( "http://www.deegree.org/spring" ) ) {
                            final String resourceBeanName = workspacePath + "/" + resourceIdentifier.getId();

                            if ( filter == null || filter.include( resourceMetadata ) ) {
                                LOG.debug( "Providing deegree workspace resource '{}' as autowire candidate",
                                           resourceBeanName );
                                candidates.put( resourceBeanName, getProxy( requiredType, resourceIdentifier ) );
                            } else {
                                LOG.debug( "deegree workspace resource '{}' ignored for autowiring", resourceBeanName );
                            }
                        }
                    }
                }
            }
        }

        if ( filter == null ) {
            candidates.putAll( super.findAutowireCandidates( beanName, requiredType, descriptor ) );
        }

        return candidates;
    }
}
