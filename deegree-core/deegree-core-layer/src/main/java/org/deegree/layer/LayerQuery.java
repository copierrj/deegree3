//$HeadURL: svn+ssh://aschmitz@wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
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

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.layer;

import java.util.List;
import java.util.Map;

import org.deegree.filter.OperatorFilter;
import org.deegree.geometry.Envelope;
import org.deegree.rendering.r2d.RenderHelper;
import org.deegree.rendering.r2d.context.MapOptionsMaps;
import org.deegree.style.StyleRef;

/**
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: $, $Date: $
 */
public class LayerQuery {

    protected final Envelope envelope;

    protected final int width, height;

    protected final Map<String, String> parameters;    
    
    protected final LayerRef layer;

    protected final StyleRef style;

    protected final OperatorFilter filter;

    protected final double scale;

    protected final Map<String, List<?>> dimensions;

    protected final double resolution;

    protected final MapOptionsMaps options;

    /**
     * @param envelope
     * @param width
     * @param height
     * @param style
     * @param filters
     * @param parameters
     * @param dimensions
     * @param pixelSize
     *            must be in meter, not mm
     * @param options
     */
    public LayerQuery( Envelope envelope, int width, int height, LayerRef layer, StyleRef style, OperatorFilter filter,
                       Map<String, String> parameters, Map<String, List<?>> dimensions, double pixelSize,
                       MapOptionsMaps options ) {
        this.envelope = envelope;
        this.width = width;
        this.height = height;
        this.layer = layer;
        this.style = style;
        this.filter = filter;
        this.parameters = parameters;
        this.dimensions = dimensions;
        this.options = options;
        this.scale = RenderHelper.calcScaleWMS130( width, height, envelope, envelope.getCoordinateSystem(), pixelSize );
        this.resolution = Utils.calcResolution( envelope, width, height );
    }    

    public Envelope getEnvelope() {
        return envelope;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }    

    public OperatorFilter getFilter() {
        return filter;
    }

    public StyleRef getStyle() {
        return style;
    }
    
    public LayerRef getLayer() {
        return layer;
    }

    public Map<String, List<?>> getDimensions() {
        return dimensions;
    }    

    public double getScale() {
        return scale;
    }

    public double getResolution() {
        return resolution;
    }   

    public MapOptionsMaps getRenderingOptions() {
        return options;
    }      
}
