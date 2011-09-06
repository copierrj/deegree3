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

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static org.deegree.gml.GMLVersion.GML_31;
import static org.deegree.protocol.ows.exception.OWSException.INVALID_PARAMETER_VALUE;
import static org.deegree.protocol.ows.exception.OWSException.MISSING_PARAMETER_VALUE;
import static org.deegree.services.wfs.StoredQueryHandler.GET_FEATURE_BY_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.deegree.commons.utils.QNameUtils;
import org.deegree.commons.xml.NamespaceBindings;
import org.deegree.cs.CRSUtils;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.exceptions.TransformationException;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.deegree.feature.persistence.FeatureStore;
import org.deegree.feature.persistence.query.Query;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.filter.Filter;
import org.deegree.filter.Filters;
import org.deegree.filter.IdFilter;
import org.deegree.filter.OperatorFilter;
import org.deegree.filter.expression.ValueReference;
import org.deegree.filter.sort.SortProperty;
import org.deegree.filter.spatial.BBOX;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.GeometryTransformer;
import org.deegree.gml.GMLVersion;
import org.deegree.protocol.ows.exception.OWSException;
import org.deegree.protocol.wfs.getfeature.GetFeature;
import org.deegree.protocol.wfs.getfeature.TypeName;
import org.deegree.protocol.wfs.query.AdHocQuery;
import org.deegree.protocol.wfs.query.BBoxQuery;
import org.deegree.protocol.wfs.query.FeatureIdQuery;
import org.deegree.protocol.wfs.query.FilterQuery;
import org.deegree.protocol.wfs.query.ProjectionClause;
import org.deegree.protocol.wfs.query.StoredQuery;
import org.jaxen.NamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for validating the queries contained in {@link GetFeature} requests and generating a corresponding
 * sequence of feature store queries.
 * <p>
 * Also performs some normalizing on the values of {@link ValueReference}s. TODO describe strategy
 * </p>
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class QueryAnalyzer {

    private final GeometryFactory geomFac = new GeometryFactory();

    private static final Logger LOG = LoggerFactory.getLogger( QueryAnalyzer.class );

    private final WebFeatureService controller;

    private final WFSFeatureStoreManager service;

    private final GMLVersion outputFormat;

    private final Set<FeatureType> requestedFts = new HashSet<FeatureType>();

    private final Map<Query, org.deegree.protocol.wfs.query.Query> queryToWFSQuery = new HashMap<Query, org.deegree.protocol.wfs.query.Query>();

    private final Map<FeatureStore, List<Query>> fsToQueries = new LinkedHashMap<FeatureStore, List<Query>>();

    private ProjectionClause[] projections = null;

    private ICRS requestedCrs;

    private boolean allFtsPossible;

    private final boolean checkAreaOfUse;

    private String requestedId;

    /**
     * Creates a new {@link QueryAnalyzer}.
     * 
     * @param wfsQueries
     *            queries be performed, must not be <code>null</code>
     * @param service
     *            {@link WFSFeatureStoreManager} to be used, must not be <code>null</code>
     * @param outputFormat
     *            output format, must not be <code>null</code>
     * @param checkInputDomain
     *            true, if geometries in query constraints should be checked against validity domain of the SRS (needed
     *            for CITE 1.1.0 compliance)
     * @throws OWSException
     *             if the request cannot be performed, e.g. because it queries feature types that are not served
     */
    public QueryAnalyzer( List<org.deegree.protocol.wfs.query.Query> wfsQueries, WebFeatureService controller,
                          WFSFeatureStoreManager service, GMLVersion outputFormat, boolean checkInputDomain )
                            throws OWSException {

        this.controller = controller;
        this.service = service;
        this.outputFormat = outputFormat;
        this.checkAreaOfUse = checkInputDomain;

        // generate validated feature store queries
        if ( wfsQueries.isEmpty() ) {
            // TODO perform the check here?
            String msg = "Either the typeName parameter must be present or the query must provide feature ids.";
            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "typeName" );
        }

        List<AdHocQuery> adHocQueries = convertStoredQueries( wfsQueries );

        Query[] queries = new Query[adHocQueries.size()];
        for ( int i = 0; i < adHocQueries.size(); i++ ) {
            AdHocQuery wfsQuery = adHocQueries.get( i );
            Query query = validateQuery( wfsQuery );
            queries[i] = query;
            queryToWFSQuery.put( query, wfsQuery );

            // TODO what about queries with different SRS?
            if ( wfsQuery.getSrsName() != null ) {
                requestedCrs = ( (AdHocQuery) wfsQuery ).getSrsName();
            } else {
                requestedCrs = controller.getDefaultQueryCrs();
            }
        }

        // associate queries with feature stores
        for ( Query query : queries ) {
            if ( query.getTypeNames().length == 0 ) {
                for ( FeatureStore fs : service.getStores() ) {
                    List<Query> fsQueries = fsToQueries.get( fs );
                    if ( fsQueries == null ) {
                        fsQueries = new ArrayList<Query>();
                        fsToQueries.put( fs, fsQueries );
                    }
                    fsQueries.add( query );
                }
            } else {
                FeatureStore fs = service.getStore( query.getTypeNames()[0].getFeatureTypeName() );
                List<Query> fsQueries = fsToQueries.get( fs );
                if ( fsQueries == null ) {
                    fsQueries = new ArrayList<Query>();
                    fsToQueries.put( fs, fsQueries );
                }
                fsQueries.add( query );
            }
        }

        // TODO cope with more queries than one
        if ( adHocQueries.size() == 1 ) {
            if ( adHocQueries.get( 0 ) instanceof FilterQuery ) {
                FilterQuery featureQuery = ( (FilterQuery) adHocQueries.get( 0 ) );
                if ( featureQuery.getProjectionClauses() != null ) {
                    this.projections = featureQuery.getProjectionClauses();
                }
            } else if ( adHocQueries.get( 0 ) instanceof BBoxQuery ) {
                BBoxQuery bboxQuery = ( (BBoxQuery) adHocQueries.get( 0 ) );
                if ( bboxQuery.getProjectionClauses() != null && bboxQuery.getProjectionClauses().length > 0 ) {
                    this.projections = bboxQuery.getProjectionClauses();
                }
            } else if ( adHocQueries.get( 0 ) instanceof FeatureIdQuery ) {
                FeatureIdQuery idQuery = ( (FeatureIdQuery) adHocQueries.get( 0 ) );
                if ( idQuery.getProjectionClauses() != null && idQuery.getProjectionClauses().length > 0 ) {
                    // TODO cope with arrays with more than one entry
                    this.projections = idQuery.getProjectionClauses()[0];
                }
            }
        }
    }

    private List<AdHocQuery> convertStoredQueries( List<org.deegree.protocol.wfs.query.Query> wfsQueries )
                            throws OWSException {
        List<AdHocQuery> adHocQueries = new ArrayList<AdHocQuery>();
        for ( org.deegree.protocol.wfs.query.Query wfsQuery : wfsQueries ) {
            if ( wfsQuery instanceof AdHocQuery ) {
                adHocQueries.add( (AdHocQuery) wfsQuery );
            } else {
                StoredQuery storedQuery = (StoredQuery) wfsQuery;
                if ( storedQuery.getId().equals( GET_FEATURE_BY_ID ) ) {
                    OMElement literalEl = storedQuery.getParams().get( "ID" );
                    if ( literalEl == null ) {
                        String msg = "Stored query '" + storedQuery.getId() + "' requires parameter 'ID'.";
                        throw new OWSException( msg, MISSING_PARAMETER_VALUE, "ID" );
                    }
                    LOG.debug( "GetFeatureById query" );
                    requestedId = literalEl.getText();
                } else {
                    String msg = "Stored query with id '" + storedQuery.getId() + "' is not known.";
                    throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "storedQueryId" );
                }
            }
        }
        return adHocQueries;
    }

    /**
     * In case of a WFS 2.0 <code>GetFeatureById</code> request, this returns the requested id.
     * 
     * @return id of the requested feature or <code>null</code> if the request is not a <code>GetFeatureById</code>
     *         request
     */
    public String getRequestedFeatureId() {
        return requestedId;
    }

    /**
     * Returns all {@link FeatureType}s that may be returned in the response to the request.
     * 
     * @return list of requested feature types, or <code>null</code> if any of the feature types served by the WFS could
     *         be returned (happens only for KVP-request with feature ids and without typenames)
     */
    public Collection<FeatureType> getFeatureTypes() {
        return allFtsPossible ? null : requestedFts;
    }

    /**
     * Returns the feature store queries that have to performed for this request.
     * 
     * @return the feature store queries that have to performed, never <code>null</code>
     */
    public Map<FeatureStore, List<Query>> getQueries() {
        return fsToQueries;
    }

    /**
     * Returns the crs that the returned geometries should have.
     * 
     * TODO what about multiple queries with different CRS
     * 
     * @return the crs, or <code>null</code> (use native crs)
     */
    public ICRS getRequestedCRS() {
        return requestedCrs;
    }

    /**
     * Returns the specific XLink-behaviour for features properties.
     * 
     * TODO what about multiple queries that specify different sets of properties
     * 
     * @return specific XLink-behaviour or <code>null</code> (no specific behaviour)
     */
    public ProjectionClause[] getProjection() {
        return projections;
    }

    /**
     * Builds a feature store {@link Query} from the given WFS query and checks if the feature type / property name
     * references in the given {@link Query} are resolvable against the served application schema.
     * <p>
     * Incorrectly or un-qualified feature type or property names are repaired. These often stem from WFS 1.0.0
     * KVP-requests (which doesn't have a namespace parameter) or broken clients.
     * </p>
     * 
     * @param wfsQuery
     *            query to be validated, must not be <code>null</code>
     * @return the feature store query, using only correctly fully qualified feature / property names
     * @throws OWSException
     *             if an unresolvable feature type / property name is used
     */
    private Query validateQuery( org.deegree.protocol.wfs.query.Query wfsQuery )
                            throws OWSException {

        // requalify query typenames and keep track of them
        TypeName[] wfsTypeNames = ( (AdHocQuery) wfsQuery ).getTypeNames();
        TypeName[] typeNames = new TypeName[wfsTypeNames.length];
        FeatureStore commonFs = null;
        for ( int i = 0; i < wfsTypeNames.length; i++ ) {
            String alias = wfsTypeNames[i].getAlias();
            FeatureType ft = service.lookupFeatureType( wfsTypeNames[i].getFeatureTypeName() );
            if ( ft == null ) {
                String msg = "Feature type with name '" + wfsTypeNames[i].getFeatureTypeName()
                             + "' is not served by this WFS.";
                throw new OWSException( msg, INVALID_PARAMETER_VALUE, "typeName" );
            }
            FeatureStore fs = service.getStore( ft.getName() );
            if ( commonFs != null ) {
                if ( fs != commonFs ) {
                    String msg = "Requested join of feature types from different feature stores. This is not supported.";
                    throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "typeName" );
                }
            } else {
                commonFs = fs;
            }
            requestedFts.add( ft );
            QName ftName = ft.getName();
            typeNames[i] = new TypeName( ftName, alias );
        }
        if ( wfsTypeNames.length == 0 ) {
            allFtsPossible = true;
        }

        // check requested / filter property names and geometries
        Filter filter = null;
        if ( wfsQuery instanceof FilterQuery ) {
            FilterQuery fQuery = ( (FilterQuery) wfsQuery );
            if ( fQuery.getProjectionClauses() != null ) {
                for ( ProjectionClause projection : fQuery.getProjectionClauses() ) {
                    validatePropertyName( projection.getPropertyName(), typeNames );
                }
            }
            if ( fQuery.getFilter() != null ) {
                for ( ValueReference pt : Filters.getPropertyNames( fQuery.getFilter() ) ) {
                    validatePropertyName( pt, typeNames );
                }
                if ( checkAreaOfUse ) {
                    for ( Geometry geom : Filters.getGeometries( fQuery.getFilter() ) ) {
                        validateGeometryConstraint( geom, ( (AdHocQuery) wfsQuery ).getSrsName() );
                    }
                }
            }
            filter = fQuery.getFilter();
        } else if ( wfsQuery instanceof BBoxQuery ) {
            BBoxQuery bboxQuery = (BBoxQuery) wfsQuery;
            ProjectionClause[] propNames = bboxQuery.getProjectionClauses();
            if ( propNames != null ) {
                for ( ProjectionClause propertyName : propNames ) {
                    validatePropertyName( propertyName.getPropertyName(), typeNames );
                }
            }
            if ( checkAreaOfUse ) {
                validateGeometryConstraint( ( (BBoxQuery) wfsQuery ).getBBox(), ( (AdHocQuery) wfsQuery ).getSrsName() );
            }

            Envelope bbox = bboxQuery.getBBox();
            BBOX bboxOperator = new BBOX( bbox );
            filter = new OperatorFilter( bboxOperator );
        } else if ( wfsQuery instanceof FeatureIdQuery ) {
            FeatureIdQuery fidQuery = (FeatureIdQuery) wfsQuery;
            ProjectionClause[][] propNames = fidQuery.getProjectionClauses();
            if ( propNames != null ) {
                for ( ProjectionClause[] propertyNames : propNames ) {
                    for ( ProjectionClause propertyName : propertyNames ) {
                        validatePropertyName( propertyName.getPropertyName(), typeNames );
                    }
                }
            }
            filter = new IdFilter( fidQuery.getFeatureIds() );
        }

        if ( wfsTypeNames.length == 0 && ( filter == null || !( filter instanceof IdFilter ) ) ) {
            String msg = "Either the typeName parameter must be present or the query must provide feature ids.";
            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "typeName" );
        }

        SortProperty[] sortProps = ( (AdHocQuery) wfsQuery ).getSortBy();
        if ( sortProps != null ) {
            for ( SortProperty sortProperty : sortProps ) {
                validatePropertyName( sortProperty.getSortProperty(), typeNames );
            }
        }

        // superimpose default query CRS
        if ( filter != null ) {
            Filters.setDefaultCRS( filter, controller.getDefaultQueryCrs() );
        }

        return new Query( typeNames, filter, ( (AdHocQuery) wfsQuery ).getFeatureVersion(),
                          ( (AdHocQuery) wfsQuery ).getSrsName(), sortProps );
    }

    private void validatePropertyName( ValueReference propName, TypeName[] typeNames )
                            throws OWSException {

        // no check possible if feature type is unknown
        if ( typeNames.length > 0 ) {
            if ( propName.getAsQName() != null ) {
                if ( !isPrefixedAndBound( propName ) ) {
                    repairSimpleUnqualified( propName, typeNames[0] );
                }

                // check that the propName is indeed valid as belonging to serviced features
                QName name = getPropertyNameAsQName( propName );
                if ( name != null ) {
                    if ( typeNames.length == 1 ) {
                        FeatureType ft = service.lookupFeatureType( typeNames[0].getFeatureTypeName() );
                        if ( ft.getPropertyDeclaration( name, outputFormat ) == null ) {
                            String msg = "Specified PropertyName '" + propName.getAsText() + "' (='" + name
                                         + "') does not exist for feature type '" + ft.getName() + "'.";
                            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "PropertyName" );
                        }
                    }
                    // TODO really skip this check for join queries?
                }
            } else {
                // TODO property name may be an XPath and use aliases...
            }
        }
    }

    /**
     * Returns whether the propName has to be considered for re-qualification.
     * 
     * @param propName
     * @return
     */
    private boolean isPrefixedAndBound( ValueReference propName ) {
        QName name = propName.getAsQName();
        return !name.getPrefix().equals( DEFAULT_NS_PREFIX )
               && !name.getNamespaceURI().equals( XMLConstants.NULL_NS_URI );
    }

    /**
     * Repairs a {@link ValueReference} that contains the local name of a {@link FeatureType}'s property or a prefixed
     * name, but without a correct namespace binding.
     * <p>
     * This types of propertynames especially occurs in WFS 1.0.0 requests.
     * </p>
     * 
     * @param propName
     *            property name to be repaired, must be "simple", i.e. contain only of a QName
     * @param typeName
     *            feature type specification from the query, must not be <code>null</code>
     * @throws OWSException
     *             if no match could be found
     */
    private void repairSimpleUnqualified( ValueReference propName, TypeName typeName )
                            throws OWSException {

        FeatureType ft = service.lookupFeatureType( typeName.getFeatureTypeName() );

        List<QName> propNames = new ArrayList<QName>();
        // TODO which GML version
        for ( PropertyType pt : ft.getPropertyDeclarations( GML_31 ) ) {
            propNames.add( pt.getName() );
        }

        QName match = QNameUtils.findBestMatch( propName.getAsQName(), propNames );
        if ( match == null ) {
            String msg = "Specified PropertyName '" + propName.getAsText() + "' does not exist for feature type '"
                         + ft.getName() + "'.";
            throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "PropertyName" );
        }
        if ( !match.equals( propName.getAsQName() ) ) {
            LOG.warn( "Repairing unqualified PropertyName: " + QNameUtils.toString( propName.getAsQName() ) + " -> "
                      + QNameUtils.toString( match ) );
            // vague match
            String text = match.getLocalPart();
            if ( !match.getPrefix().equals( DEFAULT_NS_PREFIX ) ) {
                text = match.getPrefix() + ":" + match.getLocalPart();
            }
            NamespaceBindings nsContext = new NamespaceBindings();
            nsContext.addNamespace( match.getPrefix(), match.getNamespaceURI() );
            propName.set( text, nsContext );
        }
    }

    // TODO do this properly
    private QName getPropertyNameAsQName( ValueReference propName ) {
        QName name = null;
        NamespaceContext nsContext = propName.getNsContext();
        String s = propName.getAsText();
        int colonIdx = s.indexOf( ':' );
        if ( !s.contains( "/" ) && colonIdx != -1 ) {
            if ( Character.isLetterOrDigit( s.charAt( 0 ) ) && Character.isLetterOrDigit( s.charAt( s.length() - 1 ) ) ) {
                String prefix = s.substring( 0, colonIdx );
                String localName = s.substring( colonIdx + 1, s.length() );
                String nsUri = null;

                if ( nsContext != null ) {
                    nsUri = nsContext.translateNamespacePrefixToUri( prefix );
                } else {

                    nsUri = service.getPrefixToNs().get( prefix );
                    if ( nsUri == null ) {
                        nsUri = XMLConstants.NULL_NS_URI;
                    }
                }
                name = new QName( nsUri, localName, prefix );
            }
        } else {
            if ( !s.contains( "/" ) && !s.isEmpty() && Character.isLetterOrDigit( s.charAt( 0 ) )
                 && Character.isLetterOrDigit( s.charAt( s.length() - 1 ) ) ) {
                name = new QName( s );
            }
        }
        return name;
    }

    private void validateGeometryConstraint( Geometry geom, ICRS queriedCrs )
                            throws OWSException {

        // check if geometry's bbox is inside the domain of its CRS
        Envelope bbox = geom.getEnvelope();
        if ( bbox.getCoordinateSystem() != null ) {
            // check if geometry's bbox is valid with respect to the CRS domain
            try {
                double[] b = bbox.getCoordinateSystem().getAreaOfUseBBox();
                Envelope domainOfValidity = geomFac.createEnvelope( b[0], b[1], b[2], b[3], CRSUtils.EPSG_4326 );
                domainOfValidity = transform( domainOfValidity, bbox.getCoordinateSystem() );
                if ( !bbox.isWithin( domainOfValidity ) ) {
                    String msg = "Invalid geometry constraint in filter. The envelope of the geometry is not within the domain of validity ('"
                                 + domainOfValidity
                                 + "') of its CRS ('"
                                 + bbox.getCoordinateSystem().getAlias()
                                 + "').";
                    throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "filter" );
                }
            } catch ( UnknownCRSException e ) {
                // could not validate constraint, but let's assume it's met
            } catch ( IllegalArgumentException e ) {
                // could not validate constraint, but let's assume it's met
            } catch ( TransformationException e ) {
                // could not validate constraint, but let's assume it's met
            }
        }

        // check if geometry's bbox is inside the validity domain of the queried CRS
        if ( queriedCrs != null ) {
            try {
                double[] b = queriedCrs.getAreaOfUseBBox();
                Envelope domainOfValidity = geomFac.createEnvelope( b[0], b[1], b[2], b[3], CRSUtils.EPSG_4326 );
                domainOfValidity = transform( domainOfValidity, queriedCrs );
                Envelope bboxTransformed = transform( bbox, queriedCrs );
                if ( !bboxTransformed.isWithin( domainOfValidity ) ) {
                    String msg = "Invalid geometry constraint in filter. The envelope of the geometry is not within the domain of validity ('"
                                 + domainOfValidity + "') of the queried CRS ('" + queriedCrs.getAlias() + "').";
                    throw new OWSException( msg, OWSException.INVALID_PARAMETER_VALUE, "filter" );
                }
            } catch ( UnknownCRSException e ) {
                // could not validate constraint, but let's assume it's met
            } catch ( IllegalArgumentException e ) {
                // could not validate constraint, but let's assume it's met
            } catch ( TransformationException e ) {
                // could not validate constraint, but let's assume it's met
            }
        }
    }

    private Envelope transform( Envelope bbox, ICRS targetCrs )
                            throws IllegalArgumentException, TransformationException, UnknownCRSException {
        if ( bbox.getEnvelope().equals( targetCrs ) ) {
            return bbox;
        }
        GeometryTransformer transformer = new GeometryTransformer( targetCrs );
        return transformer.transform( bbox );
    }
}