package org.deegree.featureinfo.context;

import java.util.ArrayList;
import java.util.List;

import org.deegree.commons.context.StandardContext;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.Features;
import org.deegree.feature.GenericFeatureCollection;
import org.deegree.feature.stream.FeatureInputStream;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public class StandardInfoContext extends StandardContext implements InfoContext {

    private final List<Feature> features = new ArrayList<Feature>();

    @Override
    public void addFeatures( final FeatureCollection featureCollection ) {
        features.addAll( featureCollection );
    }

    public FeatureCollection getFeatures() {
        final GenericFeatureCollection featureCollection = new GenericFeatureCollection();
        featureCollection.addAll( Features.clearDuplicates( features ) );
        return featureCollection;
    }

    @Override
    public void addFeatures( final FeatureInputStream featureInputStream ) {
        for ( final Feature feature : featureInputStream ) {
            features.add( feature );
        }

        featureInputStream.close();
    }

    @Override
    public void addFeatures( final Feature feature ) {
        features.add( feature );
    }
}
