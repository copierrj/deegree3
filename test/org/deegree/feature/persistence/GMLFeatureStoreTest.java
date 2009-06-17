//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
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

package org.deegree.feature.persistence;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Assert;

import org.deegree.commons.filter.Filter;
import org.deegree.commons.filter.IdFilter;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.commons.xml.stax.XMLStreamWriterWrapper;
import org.deegree.crs.exceptions.UnknownCRSException;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.gml.GML311GeometryExporter;
import org.deegree.feature.gml.GMLFeatureParserTest;
import org.deegree.feature.types.ApplicationSchema;
import org.deegree.feature.types.JAXBAdapter;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.primitive.Ring;
import org.deegree.protocol.wfs.getfeature.FilterQuery;
import org.junit.Before;
import org.junit.Test;

/**
 * The <code></code> class TODO add class documentation here.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author: schneider $
 * 
 * @version $Revision: $, $Date: $
 */
public class GMLFeatureStoreTest {

    private static final String BASE_DIR = "../gml/testdata/features/";

    private GMLFeatureStore store;

    @Before
    public void setUp()
                            throws XMLParsingException, XMLStreamException, UnknownCRSException,
                            FactoryConfigurationError, IOException, JAXBException {
        URL url = new URL( "file:/home/schneider/workspace/d3_commons/resources/schema/feature/example.xml" );
        JAXBAdapter adapter = new JAXBAdapter( url );
        ApplicationSchema schema = adapter.getApplicationSchema();

        URL docURL = GMLFeatureParserTest.class.getResource( BASE_DIR + "Philosopher_FeatureCollection.xml" );
        store = new GMLFeatureStore( docURL, schema );
    }

    @Test
    public void testQueryAllPhilosophers() {
        QName ftName = new QName( "http://www.deegree.org/app", "Philosopher" );
        FilterQuery query = new FilterQuery( ftName, null, null, null );
        FeatureCollection fc = store.performQuery( query );
        Assert.assertEquals( ftName, fc.iterator().next().getName() );
        Assert.assertEquals( 7, fc.size() );
    }

    @Test
    public void testQueryAllPlaces() {
        QName ftName = new QName( "http://www.deegree.org/app", "Place" );
        FilterQuery query = new FilterQuery( ftName, null, null, null );
        FeatureCollection fc = store.performQuery( query );
        Assert.assertEquals( ftName, fc.iterator().next().getName() );
        Assert.assertEquals( 7, fc.size() );
    }

    @Test
    public void testQueryAllCountries() {
        QName ftName = new QName( "http://www.deegree.org/app", "Country" );
        FilterQuery query = new FilterQuery( ftName, null, null, null );
        FeatureCollection fc = store.performQuery( query );
        Assert.assertEquals( ftName, fc.iterator().next().getName() );
        Assert.assertEquals( 4, fc.size() );
    }

    @Test
    public void testQueryAllBooks() {
        QName ftName = new QName( "http://www.deegree.org/app", "Book" );
        FilterQuery query = new FilterQuery( ftName, null, null, null );
        FeatureCollection fc = store.performQuery( query );
        Assert.assertEquals( ftName, fc.iterator().next().getName() );
        Assert.assertEquals( 1, fc.size() );
    }

    @Test
    public void testQueryPhilosopherById() {
        QName ftName = new QName( "http://www.deegree.org/app", "Philosopher" );
        Filter filter = new IdFilter( "PHILOSOPHER_1", "PHILOSOPHER_2" );
        FilterQuery query = new FilterQuery( ftName, null, null, filter );
        FeatureCollection fc = store.performQuery( query );
        Assert.assertEquals( ftName, fc.iterator().next().getName() );
        Assert.assertEquals( 2, fc.size() );
    }

    @Test
    public void testGetObjectByIdFeature() {
        Object o = store.getObjectById( "PHILOSOPHER_7" );
        Assert.assertTrue( o instanceof Feature );
    }

    @Test
    public void testGetObjectByIdGeometry1() {
        Object o = store.getObjectById( "MULTIPOLYGON_1" );
        Assert.assertTrue( o instanceof Geometry );
    }

    @Test
    public void testGetObjectByIdGeometry2() throws FileNotFoundException, XMLStreamException {
        Object o = store.getObjectById( "RING_1" );
        Assert.assertTrue( o instanceof Ring );

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        outputFactory.setProperty( "javax.xml.stream.isRepairingNamespaces", new Boolean( true ) );
        OutputStream out = new FileOutputStream( "/tmp/exported_ring.gml");
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter( out );
        writer.setDefaultNamespace( "http://www.opengis.net/gml" );
        GML311GeometryExporter exporter = new GML311GeometryExporter(writer );
        exporter.export( (Ring) o);
    }
}
