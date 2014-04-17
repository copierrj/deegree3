package org.deegree.theme;

import java.util.List;

import org.deegree.layer.LayerData;
import org.deegree.layer.LayerQuery;
import org.deegree.workspace.Resource;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public interface RootTheme extends Theme, Resource {

    boolean hasTheme( String themeName );
    
    Theme getTheme( String themeName );    

    List<LayerData> getLayers( List<? extends LayerQuery> layerQueries );
}
