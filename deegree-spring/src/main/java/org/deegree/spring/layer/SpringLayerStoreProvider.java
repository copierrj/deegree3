package org.deegree.spring.layer;

import java.net.URL;

import org.deegree.layer.persistence.LayerStore;
import org.deegree.layer.persistence.LayerStoreProvider;
import org.deegree.spring.GenericSpringResourceMetadata;
import org.deegree.workspace.ResourceLocation;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.Workspace;

public class SpringLayerStoreProvider extends LayerStoreProvider {

    private static final String CONFIG_NS = "http://www.deegree.org/spring/layer";

    private static final URL CONFIG_SCHEMA = SpringLayerStoreProvider.class.getResource( "/META-INF/schemas/spring/3.4.0/layer.xsd" );

    private static final String CONFIG_JAXB_PACKAGE = "org.deegree.spring.layer.jaxb";

    @Override
    public String getNamespace() {
        return CONFIG_NS;
    }

    @Override
    public ResourceMetadata<LayerStore> createFromLocation( Workspace workspace, ResourceLocation<LayerStore> location ) {
        return new GenericSpringResourceMetadata<LayerStore>( workspace, location, this, CONFIG_JAXB_PACKAGE,
                                                              LayerStore.class );
    }

    @Override
    public URL getSchema() {
        return CONFIG_SCHEMA;
    }

}
