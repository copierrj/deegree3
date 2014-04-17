package org.deegree.commons.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */
public class StandardContext implements Context {

    private final List<String> headers = new ArrayList<String>();

    @Override
    public void addHeader( String header ) {
        headers.add( header );
    }

    public List<String> getHeaders() {
        return Collections.unmodifiableList( headers );
    }

    @Override
    public void addHeaders( List<String> headers ) {
        headers.addAll( headers );
    }
}
