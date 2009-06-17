//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth  
 lat/lon GmbH 
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/

package org.deegree.feature.types.property;

import javax.xml.namespace.QName;

import org.deegree.feature.Feature;
import org.deegree.feature.types.FeatureType;

/**
 * A {@link PropertyType} that defines a property with a {@link Feature} value.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class FeaturePropertyType extends AbstractPropertyType {

    private QName valueFtName;

    private FeatureType valueFt;

    public FeaturePropertyType( QName name, int minOccurs, int maxOccurs, QName valueFtName ) {
        super( name, minOccurs, maxOccurs );
        this.valueFtName = valueFtName;
    }

    @Override
    public QName getName() {
        return name;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    /**
     * Returns the name of the contained feature type.
     * 
     * @return the name of the contained feature type, or null if unrestricted (any feature is allowed)
     */
    public QName getFTName() {
        return valueFtName;
    }

    public FeatureType getValueFt() {
        // if ( valueFt == null ) {
        // String msg = "Internal error. Reference to feature type '" + valueFtName + "' has not been resolved.";
        // throw new RuntimeException (msg);
        // }
        return valueFt;
    }

    public void resolve( FeatureType valueFt ) {
        if ( valueFt == null ) {
            String msg = "Internal error. Reference to feature type '" + valueFtName + "' cannot be null.";
            throw new IllegalArgumentException( msg );
        }
        if ( this.valueFt != null ) {
            String msg = "Internal error. Reference to feature type '" + valueFtName + "' has already been resolved.";
            throw new IllegalArgumentException( msg );
        }
        this.valueFt = valueFt;
    }

    @Override
    public String toString() {
        String s = "- feature property type: '" + name + "', minOccurs=" + minOccurs + ", maxOccurs=" + maxOccurs
                   + ", value feature type: " + valueFtName;
        return s;
    }
}
