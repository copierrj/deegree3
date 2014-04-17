package org.deegree.theme.persistence.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deegree.layer.Layer;
import org.deegree.layer.LayerData;
import org.deegree.layer.LayerQuery;
import org.deegree.layer.LayerRef;
import org.deegree.layer.metadata.LayerMetadata;
import org.deegree.style.StyleRef;
import org.deegree.theme.RootTheme;
import org.deegree.theme.Theme;
import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceMetadata;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public class StandardRootTheme extends StandardTheme implements RootTheme {

    private final ResourceMetadata<RootTheme> resourceMetadata;

    private final Map<String, StandardTheme> themeMap;

    public StandardRootTheme( final LayerMetadata metadata, final List<StandardTheme> themes, final List<Layer> layers,
                              final ResourceMetadata<RootTheme> resourceMetadata ) {
        super( metadata, themes, layers );

        this.resourceMetadata = resourceMetadata;
        this.themeMap = Collections.unmodifiableMap( collectThemes( this, new HashMap<String, StandardTheme>() ) );
    }

    private static Map<String, StandardTheme> collectThemes( final StandardTheme theme,
                                                             final Map<String, StandardTheme> themeMap ) {
        final LayerMetadata layerMetadata = theme.getLayerMetadata();
        if ( layerMetadata != null ) {
            final String name = layerMetadata.getName();
            if ( name != null ) {
                themeMap.put( name, theme );
            }
        }

        for ( final StandardTheme subTheme : theme.getThemes() ) {
            collectThemes( subTheme, themeMap );
        }

        return themeMap;
    }

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public void destroy() {
        // nothing to do
    }    

    @Override
    public List<LayerData> getLayers( final List<? extends LayerQuery> layerQueries ) {
        final Map<LayerQuery, List<Layer>> queryMap = new HashMap<LayerQuery, List<Layer>>();

        for ( final LayerQuery layerQuery : layerQueries ) {
            final ArrayList<Layer> layers = new ArrayList<Layer>();
            queryMap.put( layerQuery, layers );

            final LayerRef layerRef = layerQuery.getLayer();
            if ( layerRef.isResolved() ) {
                layers.add( layerRef.getLayer() );
            } else {
                layers.addAll( themeMap.get( layerRef.getName() ).layers );
            }
        }

        final ArrayList<LayerData> layerData = new ArrayList<LayerData>();
        for ( final Entry<LayerQuery, List<Layer>> queryMapEntry : queryMap.entrySet() ) {
            final LayerQuery layerQuery = queryMapEntry.getKey();
            final StyleRef style = layerQuery.getStyle();

            for ( final Layer layer : queryMapEntry.getValue() ) {
                if ( layer.isStyleApplicable( style ) ) {
                    layerData.add( layer.query( layerQuery ) );
                }
            }
        }

        return layerData;
    }    

    @Override
    public boolean hasTheme( final String themeName ) {
        return themeMap.containsKey( themeName );
    }
    
    @Override
    public Theme getTheme( final String themeName ) {
        return themeMap.get( themeName );
    }

    @Override
    public ResourceMetadata<? extends Resource> getMetadata() {
        return resourceMetadata;
    }
}
