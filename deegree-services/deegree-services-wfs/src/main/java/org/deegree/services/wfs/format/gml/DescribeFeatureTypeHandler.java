//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
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

package org.deegree.services.wfs.format.gml;

import static org.deegree.commons.xml.CommonNamespaces.GML3_2_NS;
import static org.deegree.commons.xml.CommonNamespaces.GMLNS;
import static org.deegree.commons.xml.CommonNamespaces.GML_PREFIX;
import static org.deegree.commons.xml.CommonNamespaces.XSNS;
import static org.deegree.commons.xml.CommonNamespaces.XS_PREFIX;
import static org.deegree.gml.GMLVersion.GML_32;
import static org.deegree.gml.schema.GMLAppSchemaWriter.GML_2_DEFAULT_INCLUDE;
import static org.deegree.gml.schema.GMLAppSchemaWriter.GML_30_DEFAULT_INCLUDE;
import static org.deegree.gml.schema.GMLAppSchemaWriter.GML_31_DEFAULT_INCLUDE;
import static org.deegree.gml.schema.GMLAppSchemaWriter.GML_32_DEFAULT_INCLUDE;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_100;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_110;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_200;
import static org.deegree.protocol.wfs.WFSConstants.WFS_200_NS;
import static org.deegree.protocol.wfs.WFSConstants.WFS_NS;
import static org.deegree.protocol.wfs.WFSConstants.WFS_PREFIX;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.commons.tom.ows.Version;
import org.deegree.commons.utils.URITranslator;
import org.deegree.feature.persistence.FeatureStore;
import org.deegree.feature.types.AppSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.schema.GMLAppSchemaWriter;
import org.deegree.gml.schema.GMLSchemaInfoSet;
import org.deegree.protocol.ows.exception.OWSException;
import org.deegree.protocol.wfs.WFSConstants;
import org.deegree.protocol.wfs.describefeaturetype.DescribeFeatureType;
import org.deegree.protocol.wfs.describefeaturetype.DescribeFeatureTypeKVPAdapter;
import org.deegree.services.controller.OGCFrontController;
import org.deegree.services.controller.utils.HttpResponseBuffer;
import org.deegree.services.i18n.Messages;
import org.deegree.services.resources.ResourcesServlet;
import org.deegree.services.wfs.WFSFeatureStoreManager;
import org.deegree.services.wfs.WebFeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles {@link DescribeFeatureType} requests for the {@link GMLFormat}.
 * 
 * @see GMLFormat
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author: schneider $
 * 
 * @version $Revision: $, $Date: $
 */
class DescribeFeatureTypeHandler {

    private static final Logger LOG = LoggerFactory.getLogger( DescribeFeatureTypeHandler.class );

    private static final String APPSCHEMAS = "appschemas";

    private WFSFeatureStoreManager service;

    // URL of workspace appschema directory
    private String wsAppSchemaBaseURL;

    // external URL for accessing appschema directory
    private String appSchemaBaseURL;

    private String ogcSchemaJarBaseURL;

    private final boolean exportOriginalSchema;

    /**
     * Creates a new {@link DescribeFeatureTypeHandler} instance that uses the given service to lookup requested
     * {@link FeatureType}s.
     * 
     * @param service
     *            WFS instance used to lookup the feature types, must not be <code>null</code>
     * @param exportOriginalSchema
     *            true, if the original GML application schema files shall be used, false otherwise
     * @param appSchemaBaseURL
     *            base URL to use for referring to static appschema fragments, can be <code>null</code> (use resources
     *            servlet)
     */
    DescribeFeatureTypeHandler( WFSFeatureStoreManager service, boolean exportOriginalSchema, String appSchemaBaseURL ) {
        this.service = service;
        try {
            File wsBaseDir = OGCFrontController.getServiceWorkspace().getLocation();
            File appSchemaBaseDir = new File( wsBaseDir, APPSCHEMAS );
            this.wsAppSchemaBaseURL = appSchemaBaseDir.toURI().toURL().toString();
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        URL baseUrl = DescribeFeatureType.class.getResource( "/META-INF/SCHEMAS_OPENGIS_NET" );
        if ( baseUrl != null ) {
            this.ogcSchemaJarBaseURL = baseUrl.toString();
        }
        this.exportOriginalSchema = exportOriginalSchema;
        this.appSchemaBaseURL = appSchemaBaseURL;
    }

    /**
     * Performs the given {@link DescribeFeatureType} request.
     * <p>
     * If the request targets feature types in multiple namespaces, a WFS 2.0.0-style wrapper document is generated. The
     * response document embeds all element and type declarations from one of the namespaces and imports the
     * declarations from the other namespaces using a KVP-<code>DescribeFeatureType</code> request that refers back to
     * the service.
     * </p>
     * 
     * @param request
     *            request to be handled
     * @param response
     *            response that is used to write the result
     * @param format
     * @throws OWSException
     *             if a WFS specific exception occurs, e.g. a requested feature type is not served
     * @throws XMLStreamException
     *             if writing the XML response fails
     * @throws IOException
     *             if an IO-error occurs
     */
    void doDescribeFeatureType( DescribeFeatureType request, HttpResponseBuffer response, GMLFormat format )
                            throws OWSException, XMLStreamException, IOException {

        LOG.debug( "doDescribeFeatureType: " + request );

        String mimeType = format.getMimeType();
        GMLVersion version = format.gmlVersion;

        LOG.debug( "contentType:" + response.getContentType() );
        LOG.debug( "characterEncoding:" + response.getCharacterEncoding() );

        XMLStreamWriter writer = WebFeatureService.getXMLResponseWriter( response, mimeType, null );

        // check for deegree-specific DescribeFeatureType request that asks for the WFS schema in a GML
        // version that does not match the WFS schema (e.g. WFS 1.1.0, GML 2)
        if ( request.getTypeNames() != null && request.getTypeNames().length == 1
             && "FeatureCollection".equals( request.getTypeNames()[0].getLocalPart() )
             && "wfs".equals( request.getTypeNames()[0].getPrefix() ) ) {
            writeWFSSchema( writer, request.getVersion(), version );
        } else {
            Collection<String> namespaces = determineRequiredNamespaces( request );
            String targetNs = namespaces.iterator().next();
            if ( exportOriginalSchema && service.getStores().length == 1
                 && service.getStores()[0].getSchema().getGMLSchema() != null
                 && service.getStores()[0].getSchema().getGMLSchema().getVersion() == version ) {
                exportOriginalInfoSet( writer, service.getStores()[0].getSchema().getGMLSchema(), targetNs );
            } else {
                reencodeSchema( request, writer, targetNs, namespaces, version );
            }
        }
        writer.flush();
    }

    private void reencodeSchema( DescribeFeatureType request, XMLStreamWriter writer, String targetNs,
                                 Collection<String> importNs, GMLVersion version )
                            throws XMLStreamException {

        Map<String, String> importMap = buildImportMap( request, importNs );
        Map<String, String> prefixToNs = service.getPrefixToNs();
        GMLAppSchemaWriter exporter = new GMLAppSchemaWriter( version, targetNs, importMap, prefixToNs );

        List<FeatureType> fts = new ArrayList<FeatureType>();
        for ( FeatureStore fs : service.getStores() ) {
            for ( FeatureType ft : fs.getSchema().getFeatureTypes( targetNs, true, true ) ) {
                fts.add( ft );
            }
        }

        exporter.export( writer, fts );
    }

    /**
     * Writes a response based on the original {@link GMLSchemaInfoSet}.
     * 
     * @param writer
     * @param infoSet
     * @param targetNs
     * @throws XMLStreamException
     */
    private void exportOriginalInfoSet( XMLStreamWriter writer, GMLSchemaInfoSet infoSet, String targetNs )
                            throws XMLStreamException {
        LOG.debug( "Exporting wrapper schema for original infoset." );
        GMLAppSchemaWriter.export( writer, infoSet, targetNs, new URITranslator() {
            @SuppressWarnings("synthetic-access")
            @Override
            public String translate( String uri ) {
                if ( uri.startsWith( wsAppSchemaBaseURL ) ) {
                    String relativePath = uri.substring( wsAppSchemaBaseURL.length() );
                    if ( appSchemaBaseURL == null ) {
                        return ResourcesServlet.getHttpGetURL( APPSCHEMAS + "/" + relativePath );
                    }
                    return appSchemaBaseURL + "/" + relativePath;
                }
                if ( uri.startsWith( ogcSchemaJarBaseURL ) ) {
                    return "http://schemas.opengis.net" + uri.substring( ogcSchemaJarBaseURL.length() );
                }
                return uri;
            }
        } );
    }

    private void writeWFSSchema( XMLStreamWriter writer, Version version, GMLVersion gmlVersion )
                            throws XMLStreamException {

        writer.setPrefix( XS_PREFIX, XSNS );
        writer.writeStartElement( XSNS, "schema" );
        writer.writeNamespace( XS_PREFIX, XSNS );
        writer.writeAttribute( "attributeFormDefault", "unqualified" );
        writer.writeAttribute( "elementFormDefault", "qualified" );

        if ( VERSION_100.equals( version ) || VERSION_110.equals( version ) ) {
            writer.writeAttribute( "targetNamespace", WFS_NS );
            writer.writeNamespace( WFS_PREFIX, WFSConstants.WFS_NS );
        } else if ( VERSION_200.equals( version ) ) {
            writer.writeAttribute( "targetNamespace", WFS_200_NS );
            writer.writeNamespace( WFS_PREFIX, WFSConstants.WFS_200_NS );
        }

        writer.writeNamespace( GML_PREFIX, gmlVersion.getNamespace() );

        // import GML core schema
        String parentElement = null;
        String parentType = null;
        writer.writeEmptyElement( XSNS, "import" );
        writer.writeAttribute( "namespace", gmlVersion.getNamespace() );
        switch ( gmlVersion ) {
        case GML_2:
            parentElement = GML_PREFIX + ":_FeatureCollection";
            parentType = GML_PREFIX + ":AbstractFeatureCollectionType";
            writer.writeAttribute( "schemaLocation", GML_2_DEFAULT_INCLUDE );
            break;
        case GML_30:
            parentElement = GML_PREFIX + ":_FeatureCollection";
            parentType = GML_PREFIX + ":AbstractFeatureCollectionType";
            writer.writeAttribute( "schemaLocation", GML_30_DEFAULT_INCLUDE );
            break;
        case GML_31:
            parentElement = GML_PREFIX + ":_FeatureCollection";
            parentType = GML_PREFIX + ":AbstractFeatureCollectionType";
            writer.writeAttribute( "schemaLocation", GML_31_DEFAULT_INCLUDE );
            break;
        case GML_32:
            // there is no abstract FeatureCollection element in GML 3.2 anymore
            parentElement = GML_PREFIX + ":AbstractFeature";
            parentType = GML_PREFIX + ":AbstractFeatureType";
            writer.writeAttribute( "schemaLocation", GML_32_DEFAULT_INCLUDE );
            break;
        }

        // write wfs:FeatureCollection element declaration
        writer.writeStartElement( XSNS, "element" );
        writer.writeAttribute( "name", "FeatureCollection" );
        writer.writeAttribute( "type", WFS_PREFIX + ":FeatureCollectionType" );
        writer.writeAttribute( "substitutionGroup", parentElement );
        writer.writeEndElement();

        // write wfs:FeatureCollectionType declaration
        writer.writeStartElement( XSNS, "complexType" );
        writer.writeAttribute( "name", "FeatureCollectionType" );
        writer.writeStartElement( XSNS, "complexContent" );
        writer.writeStartElement( XSNS, "extension" );
        writer.writeAttribute( "base", parentType );

        if ( GML_32 == gmlVersion ) {
            // in GML 3.2, FeatureCollections are Features with properties that derive
            // gml:AbstractFeatureMemberType
            writer.writeStartElement( XSNS, "sequence" );
            writer.writeEmptyElement( XSNS, "element" );
            writer.writeAttribute( "name", "member" );
            writer.writeAttribute( "type", WFS_PREFIX + ":FeaturePropertyType" );
            writer.writeAttribute( "minOccurs", "0" );
            writer.writeAttribute( "maxOccurs", "unbounded" );
            writer.writeEndElement();
        }

        writer.writeEmptyElement( XSNS, "attribute" );
        writer.writeAttribute( "name", "lockId" );
        writer.writeAttribute( "type", "xs:string" );
        writer.writeAttribute( "use", "optional" );

        if ( VERSION_110.equals( version ) ) {
            writer.writeEmptyElement( XSNS, "attribute" );
            writer.writeAttribute( "name", "timeStamp" );
            writer.writeAttribute( "type", "xs:dateTime" );
            writer.writeAttribute( "use", "optional" );

            writer.writeEmptyElement( XSNS, "attribute" );
            writer.writeAttribute( "name", "numberOfFeatures" );
            writer.writeAttribute( "type", "xs:nonNegativeInteger" );
            writer.writeAttribute( "use", "optional" );
        }

        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();

        if ( GML_32 == gmlVersion ) {
            // write wfs:FeaturePropertyType declaration
            writer.writeStartElement( XSNS, "complexType" );
            writer.writeAttribute( "name", "FeaturePropertyType" );
            writer.writeStartElement( XSNS, "complexContent" );
            writer.writeStartElement( XSNS, "extension" );
            writer.writeAttribute( "base", GML_PREFIX + ":AbstractFeatureMemberType" );
            writer.writeStartElement( XSNS, "sequence" );
            writer.writeEmptyElement( XSNS, "element" );
            writer.writeAttribute( "ref", GML_PREFIX + ":AbstractFeature" );
            writer.writeAttribute( "minOccurs", "0" );
            writer.writeEndElement();
            writer.writeEmptyElement( XSNS, "attributeGroup" );
            writer.writeAttribute( "ref", GML_PREFIX + ":AssociationAttributeGroup" );
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }

        // end 'xs:schema'
        writer.writeEndElement();
    }

    private Map<String, String> buildImportMap( DescribeFeatureType request, Collection<String> namespaces ) {
        Map<String, String> nsToDescribeFtRequest = new HashMap<String, String>();
        Iterator<String> namespaceIter = namespaces.iterator();
        // skip first namespace (will be included directly in output document)
        namespaceIter.next();
        while ( namespaceIter.hasNext() ) {
            String ns = namespaceIter.next();
            String requestURL = buildDescribeFeatureTypeRequest( request, ns );
            nsToDescribeFtRequest.put( ns, requestURL );
        }
        return nsToDescribeFtRequest;
    }

    private String buildDescribeFeatureTypeRequest( DescribeFeatureType request, String namespace ) {

        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put( "", namespace );
        DescribeFeatureType subRequest = new DescribeFeatureType( request.getVersion(), null,
                                                                  request.getOutputFormat(), null, nsBindings );

        String baseURL = OGCFrontController.getHttpGetURL();
        String paramPart = DescribeFeatureTypeKVPAdapter.export( subRequest, request.getVersion() );
        return baseURL + paramPart;
    }

    /**
     * Determines the application namespaces that have to be included in the generated schema response.
     * 
     * @param request
     * @return key: namespace, value: list of feature types in the namespace
     * @throws OWSException
     */
    private List<String> determineRequiredNamespaces( DescribeFeatureType request )
                            throws OWSException {

        Set<String> set = new LinkedHashSet<String>();
        if ( request.getTypeNames() == null || request.getTypeNames().length == 0 ) {
            if ( request.getNsBindings() == null ) {
                LOG.debug( "Adding all namespaces." );
                for ( FeatureStore fs : service.getStores() ) {
                    set.addAll( fs.getSchema().getAppNamespaces() );
                }
            } else {
                LOG.debug( "Adding requested namespaces." );
                for ( String ns : request.getNsBindings().values() ) {
                    for ( FeatureStore fs : service.getStores() ) {
                        AppSchema schema = fs.getSchema();
                        if ( schema.getNamespaceBindings().values().contains( ns ) ) {
                            set.add( ns );
                            break;
                        }
                    }
                }
            }
        } else {
            LOG.debug( "Adding namespaces of requested feature types." );
            for ( QName ftName : request.getTypeNames() ) {
                FeatureType ft = service.lookupFeatureType( ftName );
                if ( ft == null ) {
                    throw new OWSException( Messages.get( "WFS_FEATURE_TYPE_NOT_SERVED", ftName ),
                                            OWSException.INVALID_PARAMETER_VALUE, "typenames" );
                }
                set.add( ft.getName().getNamespaceURI() );
            }
        }

        // add dependent namespaces
        Set<String> add = findUnhandledNs( set );
        while ( !add.isEmpty() ) {
            set.addAll( add );
            add = findUnhandledNs( set );
        }

        set.remove( GMLNS );
        set.remove( GML3_2_NS );

        return new ArrayList<String>( set );
    }

    private Set<String> findUnhandledNs( Set<String> set ) {
        Set<String> dependentNamespaces = new HashSet<String>();
        for ( String ns : set ) {
            for ( FeatureStore fs : service.getStores() ) {
                AppSchema schema = fs.getSchema();
                List<String> depNs = schema.getNamespacesDependencies( ns );
                for ( String n : depNs ) {
                    if ( !set.contains( n ) ) {
                        dependentNamespaces.add( n );
                    }
                }
            }
        }
        return dependentNamespaces;
    }
}
