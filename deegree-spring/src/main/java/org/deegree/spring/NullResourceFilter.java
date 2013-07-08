package org.deegree.spring;

import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceMetadata;

public class NullResourceFilter implements ResourceFilter {

    @Override
    public boolean include( ResourceMetadata<? extends Resource> resourceMetadata ) {
        return true;
    }
}
