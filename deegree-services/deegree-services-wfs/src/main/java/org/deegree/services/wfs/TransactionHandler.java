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

package org.deegree.services.wfs;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.deegree.commons.xml.CommonNamespaces.OGCNS;
import static org.deegree.commons.xml.CommonNamespaces.XLNNS;
import static org.deegree.commons.xml.XMLAdapter.writeElement;
import static org.deegree.gml.GMLInputFactory.createGMLStreamReader;
import static org.deegree.protocol.ows.exception.OWSException.INVALID_PARAMETER_VALUE;
import static org.deegree.protocol.ows.exception.OWSException.NO_APPLICABLE_CODE;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_100;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_110;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_200;
import static org.deegree.protocol.wfs.WFSConstants.WFS_100_TRANSACTION_URL;
import static org.deegree.protocol.wfs.WFSConstants.WFS_110_SCHEMA_URL;
import static org.deegree.protocol.wfs.WFSConstants.WFS_200_NS;
import static org.deegree.protocol.wfs.WFSConstants.WFS_200_SCHEMA_URL;
import static org.deegree.protocol.wfs.WFSConstants.WFS_NS;
import static org.deegree.services.wfs.WebFeatureService.getXMLResponseWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.commons.tom.ReferenceResolvingException;
import org.deegree.commons.tom.TypedObjectNode;
import org.deegree.commons.tom.genericxml.GenericXMLElement;
import org.deegree.commons.tom.gml.property.Property;
import org.deegree.commons.tom.gml.property.PropertyType;
import org.deegree.commons.tom.ows.Version;
import org.deegree.commons.utils.kvp.InvalidParameterValueException;
import org.deegree.commons.utils.kvp.MissingParameterException;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.commons.xml.stax.XMLStreamReaderWrapper;
import org.deegree.commons.xml.stax.XMLStreamUtils;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.GenericFeatureCollection;
import org.deegree.feature.persistence.FeatureStore;
import org.deegree.feature.persistence.FeatureStoreException;
import org.deegree.feature.persistence.FeatureStoreTransaction;
import org.deegree.feature.persistence.lock.Lock;
import org.deegree.feature.persistence.lock.LockManager;
import org.deegree.feature.property.GenericProperty;
import org.deegree.feature.types.AppSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.property.CustomPropertyType;
import org.deegree.filter.Filter;
import org.deegree.filter.Filters;
import org.deegree.filter.IdFilter;
import org.deegree.filter.OperatorFilter;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.validation.CoordinateValidityInspector;
import org.deegree.gml.GMLInputFactory;
import org.deegree.gml.GMLStreamReader;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.feature.FeatureReference;
import org.deegree.gml.feature.GMLFeatureReader;
import org.deegree.protocol.ows.exception.OWSException;
import org.deegree.protocol.wfs.transaction.ReleaseAction;
import org.deegree.protocol.wfs.transaction.Transaction;
import org.deegree.protocol.wfs.transaction.TransactionOperation;
import org.deegree.protocol.wfs.transaction.action.Delete;
import org.deegree.protocol.wfs.transaction.action.IDGenMode;
import org.deegree.protocol.wfs.transaction.action.Insert;
import org.deegree.protocol.wfs.transaction.action.Native;
import org.deegree.protocol.wfs.transaction.action.PropertyReplacement;
import org.deegree.protocol.wfs.transaction.action.Update;
import org.deegree.services.controller.utils.HttpResponseBuffer;
import org.deegree.services.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a single {@link Transaction} request for the {@link WebFeatureService}.
 * 
 * @see WebFeatureService
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
class TransactionHandler {

    private static final Logger LOG = LoggerFactory.getLogger( TransactionHandler.class );

    private final WebFeatureService master;

    private final WFSFeatureStoreManager service;

    private final Transaction request;

    private final Map<FeatureStore, FeatureStoreTransaction> acquiredTransactions = new HashMap<FeatureStore, FeatureStoreTransaction>();

    // keys: handle of the insert operation, value: list of newly introduced feature ids
    private final Map<String, List<String>> insertHandleToFids = new LinkedHashMap<String, List<String>>();

    private final List<String> insertedFidswithoutHandle = new LinkedList<String>();

    private int inserted, deleted, updated;

    /**
     * Creates a new {@link TransactionHandler} instance that uses the given service to lookup requested
     * {@link FeatureType}s.
     * 
     * @param master
     * 
     * @param service
     *            WFS instance used to lookup the feature types
     * @param request
     *            request to be handled
     */
    TransactionHandler( WebFeatureService master, WFSFeatureStoreManager service, Transaction request ) {
        this.master = master;
        this.service = service;
        this.request = request;
    }

    /**
     * Performs the given {@link Transaction} request.
     * 
     * @param response
     *            response that is used to write the result
     * @throws OWSException
     *             if a WFS specific exception occurs, e.g. a feature type is not served
     * @throws IOException
     * @throws XMLStreamException
     */
    void doTransaction( HttpResponseBuffer response )
                            throws OWSException, XMLStreamException, IOException {

        LOG.debug( "doTransaction: " + request );

        try {
            Lock lock = null;
            String lockId = request.getLockId();
            LockManager manager = null;
            try {
                // TODO: determine correct feature store
                manager = service.getStores()[0].getLockManager();
            } catch ( FeatureStoreException e ) {
                throw new OWSException( "Cannot acquire lock manager: " + e.getMessage(),
                                        OWSException.NO_APPLICABLE_CODE );
            }
            if ( lockId != null && manager != null ) {
                lock = manager.getLock( lockId );
            }

            for ( TransactionOperation operation : request.getOperations() ) {
                switch ( operation.getType() ) {
                case DELETE: {
                    doDelete( (Delete) operation, lock );
                    break;
                }
                case INSERT: {
                    doInsert( (Insert) operation, lock );
                    break;
                }
                case NATIVE: {
                    doNative( (Native) operation, lock );
                    break;
                }
                case UPDATE: {
                    doUpdate( (Update) operation, lock );
                    break;
                }
                }
            }

            // if a lockId has been specified and releaseAction="ALL", release lock
            ReleaseAction releaseAction = request.getReleaseAction();
            if ( lock != null
                 && ( releaseAction == null || releaseAction == ReleaseAction.ALL || lock.getNumLocked() == 0 ) ) {
                lock.release();
            } else {
                // TODO renew expiry timeout according to WFS spec
            }

            for ( FeatureStoreTransaction ta : acquiredTransactions.values() ) {
                LOG.debug( "Committing feature store transaction:" + ta );
                ta.commit();
            }
        } catch ( MissingParameterException e ) {
            // needed for CITE compliance (wfs:wfs-1.1.0-Transaction-tc12.1)
            LOG.debug( "Error occured during transaction, performing rollback." );
            for ( FeatureStoreTransaction ta : acquiredTransactions.values() ) {
                try {
                    LOG.debug( "Rolling back feature store transaction:" + ta );
                    ta.rollback();
                } catch ( FeatureStoreException e1 ) {
                    LOG.debug( "Error occured during rollback: " + e.getMessage(), e );
                }
            }
            if ( request.getVersion().equals( VERSION_100 ) ) {
                sendResponse100( request, response, true );
                return;
            }
            throw new OWSException( "Error occured during transaction: " + e.getMessage(),
                                    OWSException.MISSING_PARAMETER_VALUE, e.getName() );
        } catch ( InvalidParameterValueException e ) {
            // needed for CITE compliance (wfs:wfs-1.1.0-LockFeature-tc2.1)
            LOG.debug( "Error occured during transaction, performing rollback." );
            for ( FeatureStoreTransaction ta : acquiredTransactions.values() ) {
                try {
                    LOG.debug( "Rolling back feature store transaction:" + ta );
                    ta.rollback();
                } catch ( FeatureStoreException e1 ) {
                    LOG.debug( "Error occured during rollback: " + e.getMessage(), e );
                }
            }
            if ( request.getVersion().equals( VERSION_100 ) ) {
                sendResponse100( request, response, true );
                return;
            }
            throw new OWSException( "Error occured during transaction: " + e.getMessage(),
                                    OWSException.INVALID_PARAMETER_VALUE, e.getName() );
        } catch ( OWSException e ) {
            LOG.debug( "Error occured during transaction, performing rollback." );
            for ( FeatureStoreTransaction ta : acquiredTransactions.values() ) {
                try {
                    LOG.debug( "Rolling back feature store transaction:" + ta );
                    ta.rollback();
                } catch ( FeatureStoreException e1 ) {
                    LOG.debug( "Error occured during rollback: " + e.getMessage(), e );
                }
            }
            throw e;
        } catch ( Exception e ) {
            LOG.debug( "Error occured during transaction, performing rollback." );
            for ( FeatureStoreTransaction ta : acquiredTransactions.values() ) {
                try {
                    LOG.debug( "Rolling back feature store transaction:" + ta );
                    ta.rollback();
                } catch ( FeatureStoreException e1 ) {
                    LOG.debug( "Error occured during rollback: " + e.getMessage(), e );
                }
            }
            throw new OWSException( "Error occured during transaction: " + e.getMessage(), NO_APPLICABLE_CODE );
        }

        if ( VERSION_100.equals( request.getVersion() ) ) {
            sendResponse100( request, response, false );
        } else {
            sendResponse110and200( request, response );
        }
    }

    private void doDelete( Delete delete, Lock lock )
                            throws OWSException {

        LOG.debug( "doDelete: " + delete );
        QName ftName = delete.getTypeName();
        FeatureStore fs = service.getStore( ftName );
        if ( fs == null ) {
            throw new OWSException( Messages.get( "WFS_FEATURE_TYPE_NOT_SERVED", ftName ), INVALID_PARAMETER_VALUE );
        }

        FeatureStoreTransaction ta = acquireTransaction( fs );

        Filter filter = delete.getFilter();
        // superimpose default query CRS
        Filters.setDefaultCRS( filter, master.getDefaultQueryCrs() );

        try {
            switch ( filter.getType() ) {
            case ID_FILTER: {
                deleted += ta.performDelete( (IdFilter) filter, lock );
                break;
            }
            case OPERATOR_FILTER: {
                deleted += ta.performDelete( ftName, (OperatorFilter) filter, lock );
                break;
            }
            }
        } catch ( FeatureStoreException e ) {
            throw new OWSException( Messages.get( "WFS_ERROR_PERFORMING_DELETE", e.getMessage() ), NO_APPLICABLE_CODE );
        }
    }

    private void doInsert( Insert insert, Lock lock )
                            throws OWSException {

        LOG.debug( "doInsert: " + insert );

        if ( service.getStores().length == 0 ) {
            throw new OWSException( "Cannot perform insert. No feature store defined.", NO_APPLICABLE_CODE );
        }

        // TODO deal with this problem
        if ( service.getStores().length > 1 ) {
            String msg = "Cannot perform insert. More than one feature store is active -- "
                         + "this is currently not supported. Please deactivate all feature stores, "
                         + "but one in order to make Insert transactions work.";
            throw new OWSException( msg, NO_APPLICABLE_CODE );
        }

        ICRS defaultCRS = null;
        if ( insert.getSrsName() != null ) {
            try {
                defaultCRS = CRSManager.lookup( insert.getSrsName() );
            } catch ( UnknownCRSException e ) {
                String msg = "Cannot perform insert. Specified srsName '" + insert.getSrsName()
                             + "' is not supported by this WFS.";
                throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "srsName" );
            }
        }

        GMLVersion inputFormat = determineFormat( request.getVersion(), insert.getInputFormat() );

        // TODO streaming
        FeatureStoreTransaction ta = null;
        try {
            FeatureCollection fc = parseFeaturesOrCollection( insert.getFeatures(), inputFormat, defaultCRS );
            FeatureStore fs = service.getStores()[0];
            ta = acquireTransaction( fs );
            IDGenMode mode = insert.getIdGen();
            if ( mode == null ) {
                mode = IDGenMode.GENERATE_NEW;
            }
            List<String> newFids = ta.performInsert( fc, mode );
            inserted += newFids.size();
            if ( insert.getHandle() != null ) {
                if ( insertHandleToFids.containsKey( insert.getHandle() ) ) {
                    insertHandleToFids.get( insert.getHandle() ).addAll( newFids );
                } else {
                    insertHandleToFids.put( insert.getHandle(), newFids );
                }
            } else {
                insertedFidswithoutHandle.addAll( newFids );
            }
        } catch ( Exception e ) {
            LOG.debug( e.getMessage(), e );
            String msg = "Cannot perform insert operation: " + e.getMessage();
            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE );
        }
    }

    private FeatureCollection parseFeaturesOrCollection( XMLStreamReader xmlStream, GMLVersion inputFormat,
                                                         ICRS defaultCRS )
                            throws XMLStreamException, XMLParsingException, UnknownCRSException,
                            ReferenceResolvingException {

        FeatureCollection fc = null;

        // TODO determine correct schema
        AppSchema schema = service.getStores()[0].getSchema();
        GMLStreamReader gmlStream = GMLInputFactory.createGMLStreamReader( inputFormat, xmlStream );
        gmlStream.setApplicationSchema( schema );
        gmlStream.setDefaultCRS( defaultCRS );

        if ( new QName( WFS_NS, "FeatureCollection" ).equals( xmlStream.getName() ) ) {
            LOG.debug( "Features embedded in wfs:FeatureCollection" );
            fc = parseWFSFeatureCollection( xmlStream, gmlStream );
            // skip to wfs:Insert END_ELEMENT
            xmlStream.nextTag();
        } else {
            // must contain one or more features or a feature collection from the application schema
            Feature feature = gmlStream.readFeature();
            if ( feature instanceof FeatureCollection ) {
                LOG.debug( "Features embedded in application FeatureCollection" );
                fc = (FeatureCollection) feature;
                // skip to wfs:Insert END_ELEMENT
                xmlStream.nextTag();
            } else {
                LOG.debug( "Unenclosed features to be inserted" );
                List<Feature> features = new LinkedList<Feature>();
                features.add( feature );
                while ( xmlStream.nextTag() == START_ELEMENT ) {
                    // more features
                    feature = gmlStream.readFeature();
                    features.add( feature );
                }
                fc = new GenericFeatureCollection( null, features );
            }
        }

        // resolve local xlink references
        gmlStream.getIdContext().resolveLocalRefs();

        // skip to next START_ELEMENT / END_ELEMENT
        xmlStream.nextTag();
        return fc;
    }

    private FeatureCollection parseWFSFeatureCollection( XMLStreamReader xmlStream, GMLStreamReader gmlStream )
                            throws XMLStreamException, XMLParsingException, UnknownCRSException {

        // TODO handle crs + move this method somewhere else
        xmlStream.require( START_ELEMENT, WFS_NS, "FeatureCollection" );
        List<Feature> memberFeatures = new ArrayList<Feature>();

        while ( xmlStream.nextTag() == START_ELEMENT ) {
            QName elName = xmlStream.getName();
            if ( CommonNamespaces.GMLNS.equals( elName.getNamespaceURI() ) ) {
                if ( "featureMember".equals( elName.getLocalPart() ) ) {
                    // xlink?
                    String href = xmlStream.getAttributeValue( XLNNS, "href" );
                    if ( href != null ) {
                        FeatureReference refFeature = new FeatureReference( gmlStream.getIdContext(), href, null );
                        memberFeatures.add( refFeature );
                        gmlStream.getIdContext().addReference( refFeature );
                    } else {
                        xmlStream.nextTag();
                        memberFeatures.add( gmlStream.readFeature() );
                    }
                    xmlStream.nextTag();
                } else if ( "featureMembers".equals( elName.getLocalPart() ) ) {
                    while ( xmlStream.nextTag() == START_ELEMENT ) {
                        memberFeatures.add( gmlStream.readFeature() );
                    }
                } else {
                    LOG.debug( "Ignoring element '" + elName + "'" );
                    XMLStreamUtils.skipElement( xmlStream );
                }
            } else {
                LOG.debug( "Ignoring element '" + elName + "'" );
                XMLStreamUtils.skipElement( xmlStream );
            }
        }

        // idContext.resolveXLinks( decoder.getApplicationSchema() );
        xmlStream.require( END_ELEMENT, WFS_NS, "FeatureCollection" );
        return new GenericFeatureCollection( null, memberFeatures );
    }

    private void doNative( Native nativeOp, Lock lock )
                            throws OWSException {
        LOG.debug( "doNative: " + nativeOp );
        if ( nativeOp.isSafeToIgnore() == false ) {
            throw new OWSException( "Native operations are not supported by this WFS.",
                                    OWSException.INVALID_PARAMETER_VALUE, "Native" );
        }

        XMLStreamReader xmlStream = nativeOp.getVendorSpecificData();
        try {
            XMLStreamUtils.skipElement( xmlStream );
        } catch ( XMLStreamException e ) {
            String msg = "Error in native operation: " + e.getMessage();
            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE );
        }
    }

    private void doUpdate( Update update, Lock lock )
                            throws OWSException {

        LOG.debug( "doUpdate: " + update );
        QName ftName = update.getTypeName();
        FeatureType ft = service.lookupFeatureType( ftName );
        FeatureStore fs = service.getStore( ftName );
        if ( fs == null ) {
            throw new OWSException( Messages.get( "WFS_FEATURE_TYPE_NOT_SERVED", ftName ),
                                    OWSException.INVALID_PARAMETER_VALUE );
        }

        GMLVersion inputFormat = determineFormat( request.getVersion(), update.getInputFormat() );

        FeatureStoreTransaction ta = acquireTransaction( fs );
        List<Property> replacementProps = getReplacementProps( update, ft, inputFormat );
        Filter filter = null;
        try {
            filter = update.getFilter();
            // superimpose default query CRS
            Filters.setDefaultCRS( filter, master.getDefaultQueryCrs() );
        } catch ( Exception e ) {
            throw new OWSException( e.getMessage(), OWSException.INVALID_PARAMETER_VALUE );
        }

        try {
            updated += ta.performUpdate( ftName, replacementProps, filter, lock );
        } catch ( FeatureStoreException e ) {
            throw new OWSException( "Error performing update: " + e.getMessage(), e, NO_APPLICABLE_CODE );
        }
    }

    private List<Property> getReplacementProps( Update update, FeatureType ft, GMLVersion inputFormat )
                            throws OWSException {

        List<Property> newProperties = new ArrayList<Property>();
        Iterator<PropertyReplacement> replacementIter = update.getReplacementProps();
        while ( replacementIter.hasNext() ) {
            PropertyReplacement replacement = replacementIter.next();
            QName propName = replacement.getPropertyName();
            PropertyType pt = ft.getPropertyDeclaration( propName );
            if ( pt == null ) {
                throw new OWSException( "Cannot update property '" + propName + "' of feature type '" + ft.getName()
                                        + "'. The feature type does not define this property.",
                                        OWSException.OPERATION_NOT_SUPPORTED );
            }
            XMLStreamReader xmlStream = replacement.getReplacementValue();
            if ( xmlStream != null ) {
                try {
                    xmlStream.require( START_ELEMENT, WFS_NS, "Value" );
                    GMLStreamReader gmlReader = createGMLStreamReader( inputFormat, xmlStream );
                    gmlReader.setApplicationSchema( ft.getSchema() );
                    GeometryFactory geomFac = new GeometryFactory();
                    geomFac.addInspector( new CoordinateValidityInspector() );
                    gmlReader.setGeometryFactory( geomFac );
                    GMLFeatureReader featureReader = gmlReader.getFeatureReader();

                    ICRS crs = master.getDefaultQueryCrs();
                    Property prop = featureReader.parseProperty( new XMLStreamReaderWrapper( xmlStream, null ), pt, crs );

                    // TODO make this hack unnecessary
                    TypedObjectNode propValue = prop.getValue();
                    if ( pt instanceof CustomPropertyType && propValue instanceof GenericXMLElement ) {
                        prop.setValue( ( (GenericXMLElement) propValue ).getValue() );
                    }

                    newProperties.add( prop );

                    // contract: skip to "wfs:Property" END_ELEMENT
                    xmlStream.nextTag();
                    xmlStream.require( END_ELEMENT, WFS_NS, "Property" );
                    // contract: skip to next ELEMENT_EVENT
                    xmlStream.nextTag();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    LOG.debug( e.getMessage(), e );
                    throw new OWSException( e.getMessage(), NO_APPLICABLE_CODE );
                }

            } else {
                // if the wfs:Value element is omitted, the property shall be removed (CITE 1.1.0 test,
                // wfs:wfs-1.1.0-Transaction-tc11.1)
                newProperties.add( new GenericProperty( pt, null ) );
            }
        }
        return newProperties;
    }

    private FeatureStoreTransaction acquireTransaction( FeatureStore fs )
                            throws OWSException {

        FeatureStoreTransaction ta = acquiredTransactions.get( fs );
        if ( ta == null ) {
            try {
                LOG.debug( "Acquiring transaction for feature store " + fs );
                ta = fs.acquireTransaction();
                acquiredTransactions.put( fs, ta );
            } catch ( FeatureStoreException e ) {
                throw new OWSException( Messages.get( "WFS_CANNOT_ACQUIRE_TA", e.getMessage() ), NO_APPLICABLE_CODE );
            }
        }
        return ta;
    }

    private void sendResponse100( Transaction request, HttpResponseBuffer response, boolean failed )
                            throws XMLStreamException, IOException {

        String schemaLocation = WFS_NS + " " + WFS_100_TRANSACTION_URL;
        XMLStreamWriter xmlWriter = getXMLResponseWriter( response, "text/xml", schemaLocation );
        xmlWriter.setPrefix( "wfs", WFS_NS );
        xmlWriter.writeStartElement( WFS_NS, "WFS_TransactionResponse" );
        xmlWriter.writeNamespace( "wfs", WFS_NS );
        xmlWriter.writeNamespace( "ogc", OGCNS );
        xmlWriter.writeAttribute( "version", VERSION_100.toString() );

        if ( inserted > 0 ) {
            for ( String handle : insertHandleToFids.keySet() ) {
                xmlWriter.writeStartElement( "wfs", "InsertResult", WFS_NS );
                writeHandle( xmlWriter, handle );
                Collection<String> fids = insertHandleToFids.get( handle );
                for ( String fid : fids ) {
                    LOG.debug( "Inserted fid: " + fid );
                    xmlWriter.writeStartElement( "ogc", "FeatureId", OGCNS );
                    xmlWriter.writeAttribute( "fid", fid );
                    xmlWriter.writeEndElement();
                }
                xmlWriter.writeEndElement();
            }
            if ( insertedFidswithoutHandle.size() > 0 ) {
                xmlWriter.writeStartElement( "wfs", "InsertResult", WFS_NS );
                for ( String fid : insertedFidswithoutHandle ) {
                    LOG.debug( "Inserted fid: " + fid );
                    xmlWriter.writeStartElement( "ogc", "FeatureId", OGCNS );
                    xmlWriter.writeAttribute( "fid", fid );
                    xmlWriter.writeEndElement();
                }
                xmlWriter.writeEndElement();
            }
        }

        xmlWriter.writeStartElement( "wfs", "TransactionResult", WFS_NS );
        writeHandle( xmlWriter, request.getHandle() );
        xmlWriter.writeStartElement( "wfs", "Status", WFS_NS );
        if ( failed ) {
            xmlWriter.writeEmptyElement( "wfs", "FAILED", WFS_NS );
        } else {
            xmlWriter.writeEmptyElement( "wfs", "SUCCESS", WFS_NS );
        }

        xmlWriter.writeEndElement(); // wfs:Status
        xmlWriter.writeEndElement(); // wfs:TransactionResult
        xmlWriter.writeEndElement(); // wfs:WFS_TransactionResult
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
    }

    private void writeHandle( XMLStreamWriter xmlWriter, String handle )
                            throws XMLStreamException {
        if ( handle != null ) {
            xmlWriter.writeAttribute( "handle", handle );
        }
    }

    private void sendResponse110and200( Transaction request, HttpResponseBuffer response )
                            throws XMLStreamException, IOException {

        String schemaLocation = null;
        String ns = null;
        if ( VERSION_110.equals( request.getVersion() ) ) {
            ns = WFS_NS;
            schemaLocation = ns + " " + WFS_110_SCHEMA_URL;
        } else if ( VERSION_200.equals( request.getVersion() ) ) {
            ns = WFS_200_NS;
            schemaLocation = ns + " " + WFS_200_SCHEMA_URL;
        } else {
            throw new RuntimeException();
        }

        XMLStreamWriter xmlWriter = getXMLResponseWriter( response, "text/xml", schemaLocation );
        if ( VERSION_110.equals( request.getVersion() ) ) {
            xmlWriter.setPrefix( "wfs", WFS_NS );
            xmlWriter.writeStartElement( WFS_NS, "TransactionResponse" );
            xmlWriter.writeNamespace( "wfs", WFS_NS );
            xmlWriter.writeNamespace( "ogc", OGCNS );
        }

        if ( VERSION_110.equals( request.getVersion() ) ) {
            xmlWriter.writeAttribute( "version", VERSION_110.toString() );
        }

        if ( VERSION_110.equals( request.getVersion() ) ) {

            xmlWriter.writeStartElement( WFS_NS, "TransactionSummary" );
            writeElement( xmlWriter, WFS_NS, "totalInserted", "" + inserted );
            writeElement( xmlWriter, WFS_NS, "totalUpdated", "" + updated );
            writeElement( xmlWriter, WFS_NS, "totalDeleted", "" + deleted );
            xmlWriter.writeEndElement();
            if ( inserted > 0 ) {
                xmlWriter.writeStartElement( WFS_NS, "InsertResults" );
                for ( String handle : insertHandleToFids.keySet() ) {
                    Collection<String> fids = insertHandleToFids.get( handle );
                    for ( String fid : fids ) {
                        LOG.debug( "Inserted fid: " + fid );
                        xmlWriter.writeStartElement( WFS_NS, "Feature" );
                        xmlWriter.writeAttribute( "handle", handle );
                        xmlWriter.writeStartElement( OGCNS, "FeatureId" );
                        xmlWriter.writeAttribute( "fid", fid );
                        xmlWriter.writeEndElement();
                        xmlWriter.writeEndElement();
                    }
                }
                for ( String fid : insertedFidswithoutHandle ) {
                    LOG.debug( "Inserted fid: " + fid );
                    xmlWriter.writeStartElement( WFS_NS, "Feature" );
                    xmlWriter.writeStartElement( OGCNS, "FeatureId" );
                    xmlWriter.writeAttribute( "fid", fid );
                    xmlWriter.writeEndElement();
                    xmlWriter.writeEndElement();
                }
                xmlWriter.writeEndElement();
            }
        }
        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
    }

    private GMLVersion determineFormat( Version requestVersion, String format ) {

        GMLVersion gmlVersion = null;

        if ( format == null ) {
            // default values for the different WFS version
            if ( VERSION_100.equals( requestVersion ) ) {
                gmlVersion = GMLVersion.GML_2;
            } else if ( VERSION_110.equals( requestVersion ) ) {
                gmlVersion = GMLVersion.GML_31;
            } else if ( VERSION_200.equals( requestVersion ) ) {
                gmlVersion = GMLVersion.GML_32;
            } else {
                throw new RuntimeException( "Internal error: Unhandled WFS version: " + requestVersion );
            }
        } else {
            if ( "text/xml; subtype=gml/2.1.2".equals( format ) || "GML2".equals( format ) ) {
                gmlVersion = GMLVersion.GML_2;
            } else if ( "text/xml; subtype=gml/3.0.1".equals( format ) ) {
                gmlVersion = GMLVersion.GML_30;
            } else if ( "text/xml; subtype=gml/3.1.1".equals( format ) || "GML3".equals( format ) ) {
                gmlVersion = GMLVersion.GML_31;
            } else if ( "text/xml; subtype=gml/3.2.1".equals( format ) ) {
                gmlVersion = GMLVersion.GML_32;
            }
        }
        return gmlVersion;
    }
}