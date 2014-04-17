package org.deegree.layer;

import java.util.List;
import java.util.Map;

import org.deegree.filter.OperatorFilter;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.GeometryFactory;
import org.deegree.rendering.r2d.context.MapOptionsMaps;
import org.deegree.style.StyleRef;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public class LayerInfoQuery extends LayerQuery {

    private final int x, y, featureCount, layerRadius;

    public LayerInfoQuery( Envelope envelope, int width, int height, int x, int y, int featureCount,
                           OperatorFilter filter, LayerRef layer, StyleRef style, Map<String, String> parameters,
                           Map<String, List<?>> dimensions, double pixelSize, MapOptionsMaps options,
                           Envelope queryBox, int layerRadius ) {
        super( envelope, width, height, layer, style, filter, parameters, dimensions, pixelSize, options );

        this.x = x;
        this.y = y;
        this.featureCount = featureCount;
        this.layerRadius = layerRadius;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public int getLayerRadius() {
        return layerRadius;
    }
    
    public Envelope getClickBox( ) {        
        GeometryFactory fac = new GeometryFactory();
        double dw = envelope.getSpan0() / width;
        double dh = envelope.getSpan1() / height;
        int r2 = layerRadius / 2;
        r2 = r2 <= 0 ? 1 : r2;
        return fac.createEnvelope( new double[] { envelope.getMin().get0() + ( x - r2 ) * dw,
                                                 envelope.getMax().get1() - ( y + r2 ) * dh },
                                   new double[] { envelope.getMin().get0() + ( x + r2 ) * dw,
                                                 envelope.getMax().get1() - ( y - r2 ) * dh },
                                   envelope.getCoordinateSystem() );
    }
}
