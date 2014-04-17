package org.deegree.commons.context;

import java.util.List;

/**
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 *
 */  
public interface Context {

    void addHeader( String header );
    
    void addHeaders( List<String> headers );
}
