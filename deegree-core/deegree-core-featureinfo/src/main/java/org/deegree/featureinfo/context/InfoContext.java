package org.deegree.featureinfo.context;

import org.deegree.commons.context.Context;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.stream.FeatureInputStream;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public interface InfoContext extends Context {
    
    void addFeatures( Feature feature );

    void addFeatures( FeatureCollection featureCollection );

    void addFeatures( FeatureInputStream featureInputStream );    
}
