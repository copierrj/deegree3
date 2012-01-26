//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
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
package org.deegree.feature.xpath;

import java.util.ArrayList;
import java.util.List;

import org.deegree.commons.tom.gml.GMLObject;
import org.deegree.commons.tom.gml.property.Property;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;

/**
 * Provides the <code>wfs:valueOf</code> function needed by WFS 2.0.0.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class ValueOf implements Function {

    @SuppressWarnings("rawtypes")
    @Override
    public Object call( Context context, List args )
                            throws FunctionCallException {
        List<Object> values = new ArrayList<Object>();
        for ( Object arg : args ) {
            if ( arg instanceof List ) {
                for ( Object o : ( (List) arg ) ) {
                    if ( o instanceof PropertyNode ) {
                        Property prop = ( (PropertyNode) o ).getValue();
                        GMLObject gmlObject = (GMLObject) prop.getValue();
                        GMLObjectNode elNode = new GMLObjectNode( ( (PropertyNode) o ), gmlObject, null );
                        values.add( elNode );
                    } else {
                        throw new FunctionCallException(
                                                         "Arguments of valueOf() must be feature properties, but found: "
                                                                                 + o.getClass() );
                    }
                }
            } else if ( arg instanceof PropertyNode ) {
                Property prop = ( (PropertyNode) arg ).getValue();
                GMLObject gmlObject = (GMLObject) prop.getValue();
                GMLObjectNode elNode = new GMLObjectNode( ( (PropertyNode) arg ), gmlObject, null );
                values.add( elNode );
            } else {
                throw new FunctionCallException( "Arguments of valueOf() must be feature properties, but found: "
                                                 + arg.getClass() );
            }
        }
        return values;
    }
}