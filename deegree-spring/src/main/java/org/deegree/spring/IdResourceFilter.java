package org.deegree.spring;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceMetadata;

public class IdResourceFilter implements ResourceFilter {

    private final Set<String> resourceIds;

    public IdResourceFilter( final List<String> resourceIds ) {
        this.resourceIds = Collections.unmodifiableSet( new HashSet<String>( resourceIds ) );
    }

    @Override
    public boolean include(ResourceMetadata<? extends Resource> resourceMetadata) {
        return resourceIds.contains( resourceMetadata.getIdentifier().getId() );
    }
}
