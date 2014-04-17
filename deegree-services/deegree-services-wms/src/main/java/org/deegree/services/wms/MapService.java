//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

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

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.services.wms;

import static org.deegree.commons.utils.MapUtils.DEFAULT_PIXEL_SIZE;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.deegree.theme.persistence.ThemeProvider;
import org.deegree.commons.annotations.LoggingNotes;
import org.deegree.commons.ows.exception.OWSException;
import org.deegree.commons.utils.Pair;
import org.deegree.feature.types.FeatureType;
import org.deegree.featureinfo.context.InfoContext;
import org.deegree.filter.OperatorFilter;
import org.deegree.layer.LayerData;
import org.deegree.layer.LayerInfoQuery;
import org.deegree.layer.LayerQuery;
import org.deegree.layer.LayerRef;
import org.deegree.protocol.wms.filter.ScaleFunction;
import org.deegree.protocol.wms.ops.GetFeatureInfo;
import org.deegree.protocol.wms.ops.GetFeatureInfoSchema;
import org.deegree.protocol.wms.ops.GetLegendGraphic;
import org.deegree.protocol.wms.ops.GetMap;
import org.deegree.rendering.r2d.context.MapOptions;
import org.deegree.rendering.r2d.context.MapOptionsMaps;
import org.deegree.rendering.r2d.context.RenderContext;
import org.deegree.services.jaxb.wms.ServiceConfigurationType;
import org.deegree.style.StyleRef;
import org.deegree.style.se.unevaluated.Style;
import org.deegree.style.utils.ImageUtils;
import org.deegree.theme.RootTheme;
import org.deegree.workspace.Workspace;
import org.slf4j.Logger;

/**
 * <code>MapService</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
@LoggingNotes(error = "logs errors when querying feature stores/evaluating filter encoding expressions", trace = "logs stack traces", warn = "logs problems when loading layers, also invalid values for vendor specific parameters such as ANTIALIAS, QUALITY etc.", debug = "logs if layers are skipped because of scale constraints, and info about feature store queries")
public class MapService {

    private static final Logger LOG = getLogger( MapService.class );

    /**
     * 
     */
    public StyleRegistry registry;

    MapOptionsMaps layerOptions = new MapOptionsMaps();

    MapOptions defaultLayerOptions;

    /**
     * The current update sequence.
     */
    public int updateSequence = 0; // TODO how to restore this after restart?

    private final RootTheme rootTheme;

    private final GetLegendHandler getLegendHandler;

    /**
     * @param conf
     * @param adapter
     * @throws MalformedURLException
     */
    public MapService( ServiceConfigurationType conf, Workspace workspace ) throws MalformedURLException {
        this.registry = new StyleRegistry();

        MapServiceBuilder builder = new MapServiceBuilder( conf );

        defaultLayerOptions = builder.buildMapOptions();

        getLegendHandler = new GetLegendHandler( this );
        
        final String themeId = conf.getThemeId();
        rootTheme = workspace.getResource( ThemeProvider.class, themeId );
    }

    /**
     * @return the list of themes if configuration is based on themes, else null
     */
    public RootTheme getRootTheme() {
        return rootTheme;
    }

    /**
     * @param req
     *            should be a GetMap or GetLegendGraphic
     * @return an empty image conforming to the request parameters
     */
    public static BufferedImage prepareImage( Object req ) {
        String format = null;
        int width = 0, height = 0;
        Color bgcolor = null;
        boolean transparent = false;
        if ( req instanceof GetLegendGraphic ) {
            GetLegendGraphic glg = (GetLegendGraphic) req;
            format = glg.getFormat();
            width = glg.getWidth();
            height = glg.getHeight();
            transparent = true;
        } else {
            return null;
        }
        return ImageUtils.prepareImage( format, width, height, transparent, bgcolor );
    }

    public boolean hasTheme( final String themeName ) {
        return rootTheme.hasTheme( themeName );
    }

    public void getMap( GetMap getMap, RenderContext context )
                            throws OWSException {
        Iterator<StyleRef> styleItr = getMap.getStyles().iterator();        
        MapOptionsMaps options = getMap.getRenderingOptions();
        
        double scale = getMap.getScale();

        List<LayerQuery> queries = new ArrayList<LayerQuery>();

        Iterator<LayerRef> layerItr = getMap.getLayers().iterator();
        List<OperatorFilter> filters = getMap.getFilters();
        Iterator<OperatorFilter> filterItr = filters == null ? null : filters.iterator();
        while ( layerItr.hasNext() ) {
            LayerRef layerRef = layerItr.next();
            StyleRef styleRef = styleItr.next();
            OperatorFilter filter = filterItr == null ? null : filterItr.next();

            LayerQuery query = buildQuery( styleRef, layerRef, options, filter, getMap );
            queries.add( query );
        }

        ScaleFunction.getCurrentScaleValue().set( scale );

        List<LayerData> layerDataList = rootTheme.getLayers( queries );        
        for ( LayerData layerData : layerDataList ) {            
            layerData.render( context );
        }
        context.optimizeAndDrawLabels();

        ScaleFunction.getCurrentScaleValue().remove();
    }

    private LayerQuery buildQuery( StyleRef styleRef, LayerRef layerRef, MapOptionsMaps options,
                                   OperatorFilter filter, GetMap gm ) {

        LayerQuery query = new LayerQuery( gm.getBoundingBox(), gm.getWidth(), gm.getHeight(), layerRef, styleRef,
                                           filter, gm.getParameterMap(), gm.getDimensions(), gm.getPixelSize(), options );
        return query;
    }

    public void getFeatures( GetFeatureInfo getFeatureInfo, InfoContext infoContext )
                            throws OWSException {
        List<LayerInfoQuery> queries = prepareGetFeatures( getFeatureInfo );
        List<LayerData> layers = rootTheme.getLayers( queries );
        
        for ( LayerData layerData : layers ) {
            layerData.info( infoContext );
        }        
    }

    private List<LayerInfoQuery> prepareGetFeatures( GetFeatureInfo gfi ) {
        List<LayerInfoQuery> queries = new ArrayList<LayerInfoQuery>();

        Iterator<LayerRef> layerItr = gfi.getQueryLayers().iterator();
        Iterator<StyleRef> styleItr = gfi.getStyles().iterator();
        List<OperatorFilter> filters = gfi.getFilters();
        Iterator<OperatorFilter> filterItr = filters == null ? null : filters.iterator();
        while ( layerItr.hasNext() ) {
            LayerRef layerRef = layerItr.next();
            StyleRef styleRef = styleItr.next();
            OperatorFilter filter = filterItr == null ? null : filterItr.next();

            LayerInfoQuery query = new LayerInfoQuery( gfi.getEnvelope(), gfi.getWidth(), gfi.getHeight(), gfi.getX(),
                                                       gfi.getY(), gfi.getFeatureCount(), filter, layerRef, styleRef,
                                                       gfi.getParameterMap(), gfi.getDimensions(), DEFAULT_PIXEL_SIZE,
                                                       new MapOptionsMaps(), gfi.getEnvelope(),
                                                       defaultLayerOptions.getFeatureInfoRadius() );
            queries.add( query );
        }
        return queries;
    }

    /*
     * private void getFeatureTypes( Collection<FeatureType> types, String name ) { for ( org.deegree.layer.Layer l :
     * Themes.getAllLayers( themeMap.get( name ) ) ) { types.addAll( l.getMetadata().getFeatureTypes() ); } }
     */

    /**
     * @param fis
     * @return an application schema object
     */
    public List<FeatureType> getSchema( GetFeatureInfoSchema fis ) {
        List<FeatureType> list = new LinkedList<FeatureType>();
        for ( String l : fis.getLayers() ) {
            // getFeatureTypes( list, l );
        }
        return list;
    }

    /**
     * @return the style registry
     */
    public StyleRegistry getStyles() {
        return registry;
    }

    /**
     * @param style
     * @return the optimal legend size
     */
    public Pair<Integer, Integer> getLegendSize( Style style ) {
        return getLegendHandler.getLegendSize( style );
    }

    public BufferedImage getLegend( GetLegendGraphic req ) {
        return getLegendHandler.getLegend( req );
    }

    /**
     * @return the extensions object with default extension parameter settings
     */
    public MapOptionsMaps getExtensions() {
        return layerOptions;
    }

    /**
     * @return the default feature info radius
     */
    public int getGlobalFeatureInfoRadius() {
        return defaultLayerOptions.getFeatureInfoRadius();
    }

    /**
     * @return the global max features setting
     */
    public int getGlobalMaxFeatures() {
        return defaultLayerOptions.getMaxFeatures();
    }

}
