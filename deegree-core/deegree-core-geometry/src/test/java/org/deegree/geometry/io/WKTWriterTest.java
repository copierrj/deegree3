package org.deegree.geometry.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.LineString;
import org.junit.Test;

import com.vividsolutions.jts.io.ParseException;

public class WKTWriterTest {

    @Test
    public void testPoint()
                            throws IOException {
        GeometryFactory factory = new GeometryFactory();
        Geometry geom = factory.createPoint( "point0", 2.0, 4.0, null );

        byte[] wkb = WKBWriter.write( geom );
        assertEquals( "000000000140000000000000004010000000000000", Hex.encodeHexString( wkb ) );
    }

    @Test
    public void testLineString()
                            throws IOException, ParseException {
        GeometryFactory factory = new GeometryFactory();
        Points points = factory.createPoints( Arrays.asList( factory.createPoint( "point0", 2.0, 4.0, null ),
                                                             factory.createPoint( "point1", 4.0, 2.0, null ) ) );
        Geometry geom = factory.createLineString( "lineString0", null, points );

        byte[] wkb = WKBWriter.write( geom );
        LineString lineString = (LineString) WKBReader.read( wkb, null );
        
    }
}
