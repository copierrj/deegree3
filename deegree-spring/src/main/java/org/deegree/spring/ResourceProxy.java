package org.deegree.spring;

import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.Workspace;

public interface ResourceProxy {

    ResourceIdentifier<? extends Resource> getIdentifier();
    Workspace getWorkspace();
}
