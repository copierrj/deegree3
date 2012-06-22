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

 Occam Labs UG (haftungsbeschränkt)
 Godesberger Allee 139, 53175 Bonn
 Germany
 http://www.occamlabs.de/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.layer.persistence.tile;

import static org.deegree.commons.xml.jaxb.JAXBUtils.unmarshall;
import static org.deegree.geometry.metadata.SpatialMetadataConverter.fromJaxb;
import static org.deegree.protocol.ows.metadata.DescriptionConverter.fromJaxb;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.deegree.commons.config.DeegreeWorkspace;
import org.deegree.commons.config.ResourceInitException;
import org.deegree.commons.config.ResourceManager;
import org.deegree.commons.utils.DoublePair;
import org.deegree.geometry.metadata.SpatialMetadata;
import org.deegree.layer.Layer;
import org.deegree.layer.config.ConfigUtils;
import org.deegree.layer.metadata.LayerMetadata;
import org.deegree.layer.persistence.LayerStoreProvider;
import org.deegree.layer.persistence.MultipleLayerStore;
import org.deegree.layer.persistence.base.jaxb.ScaleDenominatorsType;
import org.deegree.layer.persistence.tile.jaxb.TileLayerType;
import org.deegree.layer.persistence.tile.jaxb.TileLayerType.TileStoreId;
import org.deegree.layer.persistence.tile.jaxb.TileLayers;
import org.deegree.protocol.ows.metadata.Description;
import org.deegree.tile.persistence.TileStore;
import org.deegree.tile.persistence.TileStoreManager;

/**
 * <code>TileLayerProvider</code>
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: mschneider $
 * 
 * @version $Revision: 31882 $, $Date: 2011-09-15 02:05:04 +0200 (Thu, 15 Sep 2011) $
 */

public class TileLayerProvider implements LayerStoreProvider {

    private static final URL SCHEMA = TileLayerProvider.class.getResource( "/META-INF/schemas/layers/tile/3.2.0/tile.xsd" );

    private DeegreeWorkspace workspace;

    @Override
    public void init( DeegreeWorkspace workspace ) {
        this.workspace = workspace;
    }

    private TileLayer createLayer( TileLayerType cfg )
                            throws ResourceInitException {
        TileStoreManager mgr = workspace.getSubsystemManager( TileStoreManager.class );
        TileStoreId id = cfg.getTileStoreId();
        TileStore store = mgr.get( id.getValue() );
        if ( store == null ) {
            throw new ResourceInitException( "Tile store with id " + id.getValue() + " was not available." );
        }
        if ( store.getTileDataSet( id.getTileMatrixSet() ) == null ) {
            throw new ResourceInitException( "Tile store with id " + id.getValue()
                                             + " does not provide a tile matrix set with id " + id.getTileMatrixSet()
                                             + "." );
        }
        SpatialMetadata smd = fromJaxb( cfg.getEnvelope(), cfg.getCRS() );
        if ( smd.getEnvelope() == null ) {
            smd.setEnvelope( store.getMetadata( id.getTileMatrixSet() ).getEnvelope() );
        }
        if ( smd.getCoordinateSystems().isEmpty() ) {
            smd.getCoordinateSystems().addAll( store.getMetadata( id.getTileMatrixSet() ).getCoordinateSystems() );
        }
        Description desc = fromJaxb( cfg.getTitle(), cfg.getAbstract(), cfg.getKeywords() );
        LayerMetadata md = new LayerMetadata( cfg.getName(), desc, smd );
        md.setMapOptions( ConfigUtils.parseLayerOptions( cfg.getLayerOptions() ) );
        ScaleDenominatorsType sd = cfg.getScaleDenominators();
        if ( sd != null ) {
            DoublePair p = new DoublePair( sd.getMin(), sd.getMax() );
            md.setScaleDenominators( p );
        }
        md.setMetadataId( cfg.getMetadataSetId() );
        return new TileLayer( md, store, id.getTileMatrixSet() );
    }

    @Override
    public MultipleLayerStore create( URL configUrl )
                            throws ResourceInitException {
        try {
            TileLayers cfg = (TileLayers) unmarshall( "org.deegree.layer.persistence.tile.jaxb", SCHEMA, configUrl,
                                                      workspace );
            Map<String, Layer> map = new HashMap<String, Layer>();
            for ( TileLayerType lay : cfg.getTileLayer() ) {
                TileLayer l = createLayer( lay );
                map.put( l.getMetadata().getName(), l );
            }
            return new MultipleLayerStore( map );
        } catch ( Throwable e ) {
            throw new ResourceInitException( "Unable to create tile layer store.", e );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ResourceManager>[] getDependencies() {
        return new Class[] { TileStoreManager.class };
    }

    @Override
    public String getConfigNamespace() {
        return "http://www.deegree.org/layers/tile";
    }

    @Override
    public URL getConfigSchema() {
        return SCHEMA;
    }

}
