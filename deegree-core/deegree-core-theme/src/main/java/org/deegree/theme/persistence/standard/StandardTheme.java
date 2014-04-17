//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

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

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 Occam Labs Schmitz & Schneider GbR
 Godesberger Allee 139, 53175 Bonn
 Germany
 http://www.occamlabs.de/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.theme.persistence.standard;

import static org.deegree.commons.utils.StringUtils.repeat;

import java.util.ArrayList;
import java.util.List;

import org.deegree.layer.Layer;
import org.deegree.layer.metadata.LayerMetadata;
import org.deegree.theme.Theme;

/**
 * Standard theme implementation.
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 * 
 * @since 3.4
 */
public class StandardTheme implements Theme {

    protected final List<StandardTheme> themes;

    protected final List<Layer> layers;

    protected final LayerMetadata layerMetadata;

    public StandardTheme( LayerMetadata layerMetadata, List<StandardTheme> themes, List<Layer> layers ) {
        this.layerMetadata = layerMetadata;
        this.themes = themes;
        this.layers = layers;
    }

    @Override
    public LayerMetadata getLayerMetadata() {
        LayerMetadata layerMetadata = this.layerMetadata;

        for ( final Layer layer : layers ) {
            if ( layerMetadata == null ) {
                layerMetadata = layer.getMetadata();
            } else {
                layerMetadata.merge( layer.getMetadata() );
            }
        }

        return layerMetadata;
    }

    @Override
    public List<StandardTheme> getThemes() {
        return themes;
    }

    private static <T extends Layer> List<T> collectLayers( final StandardTheme theme, final Class<T> layerType,
                                                            final List<T> layers ) {
        for ( final Layer layer : theme.layers ) {
            if ( layerType.isAssignableFrom( layer.getClass() ) ) {
                layers.add( layerType.cast( layer ) );
            }
        }

        for ( final StandardTheme subTheme : theme.getThemes() ) {
            collectLayers( subTheme, layerType, layers );
        }

        return layers;
    }

    @Override
    public <T extends Layer> List<T> getLayers( Class<T> layerType ) {
        return collectLayers( this, layerType, new ArrayList<T>() );
    }

    public String toString( int indent ) {
        StringBuilder sb = new StringBuilder();
        sb.append( repeat( indent, "  " ) );
        sb.append( " - " );
        sb.append( layerMetadata.getName() );
        sb.append( " " );
        sb.append( layers.size() );
        sb.append( " layers\n" );
        indent += 2;
        for ( StandardTheme theme : themes ) {
            sb.append( theme.toString( indent ) );
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString( 0 );
    }
}
