//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
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
package org.deegree.protocol.wps.execute.datatypes;

import java.net.URL;

/**
 * The <code></code> class TODO add class documentation here.
 * 
 * @author <a href="mailto:ionita@lat-lon.de">Andrei Ionita</a>
 * 
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 * 
 */
public class LiteralDataType implements DataType {

    private String value;

    // optional
    private String dataType;

    // optional
    private String uom;

    public LiteralDataType( String value, String dataType, String uom ) {
        this.value = value;
        this.dataType = dataType;
        this.uom = uom;
    }

    /**
     * Get value of literal instance
     * 
     * @return the value as String
     */
    public String getValue() {
        return value;
    }

    /**
     * Get data type of literal instance
     * 
     * @return datatype as String
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Get unit-of-measure of literal instance
     * 
     * @return uom as String
     */
    public String getUom() {
        return uom;
    }

    @Override
    public URL getWebAccessibleURL() {
        return null;
    }
}
