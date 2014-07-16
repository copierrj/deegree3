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
package org.deegree.geometry.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.deegree.geometry.Geometry;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.LineString;
import org.deegree.geometry.primitive.Point;

/**
 * Writes {@link Geometry} objects encoded as Well-Known Binary (WKB).
 * 
 * 
 * @author <a href="mailto:reijer.copier@idgis.nl">Reijer Copier</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class WKBWriter {

    static enum Types {

        GEOMETRY, POINT, LINE_STRING, POLYGON, MULTI_POINT, MULT_LINE_STRING, MULTI_POLYGON, GEOMETRY_COLLECTION, CIRCULAR_STRING, COMPOUND_CURVE, CURVE_POLYGON, MULTI_CURVE, MULTI_SURFACE, CURVE, SURFACE, POLYHEDRAL_SURFACE, TIN, TRIANGLE;

        int get2D() {
            return ordinal();
        }

        int getZ() {
            return ordinal() + 1000;
        }

        int getM() {
            return ordinal() + 2000;
        }

        int getZM() {
            return ordinal() + 3000;
        }
    }

    public static byte[] write( Geometry geom )
                            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        write( geom, baos );

        baos.close();
        return baos.toByteArray();
    }

    public static void write( Geometry geom, OutputStream os )
                            throws IOException {

        os.write( 0 ); // Big-endian;

        DataOutputStream dos = new DataOutputStream( os );

        final int dims = geom.getCoordinateDimension();
        if ( dims != 2 && dims != 3 ) {
            throw new IllegalArgumentException( "Unsupported dimensions: " + dims );
        }

        if ( geom instanceof Point ) {
            Point point = (Point) geom;
            if ( dims == 3 ) {
                dos.writeInt( Types.POINT.getZ() );
                dos.writeDouble( point.get0() );
                dos.writeDouble( point.get1() );
                dos.writeDouble( point.get2() );
            } else if ( dims == 2 ) {
                dos.writeInt( Types.POINT.get2D() );
                dos.writeDouble( point.get0() );
                dos.writeDouble( point.get1() );
            }
        } else if ( geom instanceof LineString ) {
            LineString lineString = (LineString) geom;

            if ( dims == 3 ) {
                dos.writeInt( Types.LINE_STRING.getZ() );
            } else if ( dims == 2 ) {
                dos.writeInt( Types.LINE_STRING.get2D() );
            }

            Points points = lineString.getControlPoints();
            dos.write( points.size() );

            for ( Point p : points ) {
                dos.writeDouble( p.get0() );
                dos.writeDouble( p.get1() );
                if ( dims == 3 ) {
                    dos.writeDouble( p.get2() );
                }
            }
            
            Point start = points.getStartPoint();
            
            dos.writeDouble( start.get0() );
            dos.writeDouble( start.get1() );
            if ( dims == 3 ) {
                dos.writeDouble( start.get2() );
            }
        } else {
            throw new IllegalArgumentException( "Unsupported geometry type: " + geom.getClass().getCanonicalName() );
        }

        dos.flush();
    }
}