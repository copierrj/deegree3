package org.deegree.spring;

import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceMetadata;

public interface ResourceFilter {

    boolean include( ResourceMetadata<? extends Resource> resourceMetadata );
}
