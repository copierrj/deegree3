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
package org.deegree.commons.filter.comparison;

import org.deegree.commons.filter.Expression;
import org.deegree.commons.filter.FilterEvaluationException;
import org.deegree.commons.filter.MatchableObject;

/**
 * TODO add documentation here
 *
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 *
 * @version $Revision:$, $Date:$
 */
public class PropertyIsEqualTo extends BinaryComparisonOperator {

    public PropertyIsEqualTo( Expression param1, Expression param2, boolean matchCase ) {
        super( param1, param2, matchCase );
    }

    public SubType getSubType() {
        return SubType.PROPERTY_IS_EQUAL_TO;
    }

    public boolean evaluate( MatchableObject object )
                            throws FilterEvaluationException {
        Comparable<Object> parameter1Value = checkComparableOrNull( param1.evaluate( object ) );
        Comparable<Object> parameter2Value = checkComparableOrNull( param2.evaluate( object ) );
        if ( parameter1Value == null && parameter2Value == null ) {
            return true;
        }
        if ( ( parameter1Value != null && parameter2Value == null )
             || ( parameter1Value == null && parameter2Value != null ) ) {
            return false;
        }
        return parameter1Value.equals( parameter2Value );
    }

    public String toString( String indent ) {
        String s = indent + "-PropertyIsEqualTo\n";
        s += param1.toString( indent + "  " );
        s += param2.toString( indent + "  " );
        return s;
    }
}
