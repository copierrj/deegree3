//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
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
package org.deegree.feature.persistence.sql.insert;

import static org.deegree.gml.GMLVersion.GML_32;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.deegree.commons.jdbc.SQLIdentifier;
import org.deegree.commons.jdbc.TableName;
import org.deegree.commons.tom.TypedObjectNode;
import org.deegree.commons.tom.genericxml.GenericXMLElement;
import org.deegree.commons.tom.gml.property.Property;
import org.deegree.commons.tom.primitive.PrimitiveValue;
import org.deegree.commons.tom.sql.ParticleConverter;
import org.deegree.commons.tom.sql.SQLValueMangler;
import org.deegree.feature.Feature;
import org.deegree.feature.persistence.FeatureStoreException;
import org.deegree.feature.persistence.sql.FeatureTypeMapping;
import org.deegree.feature.persistence.sql.MappedAppSchema;
import org.deegree.feature.persistence.sql.SQLFeatureStore;
import org.deegree.feature.persistence.sql.SQLFeatureStoreTransaction;
import org.deegree.feature.persistence.sql.expressions.TableJoin;
import org.deegree.feature.persistence.sql.id.KeyPropagation;
import org.deegree.feature.persistence.sql.id.TableDependencies;
import org.deegree.feature.persistence.sql.rules.CompoundMapping;
import org.deegree.feature.persistence.sql.rules.FeatureMapping;
import org.deegree.feature.persistence.sql.rules.GeometryMapping;
import org.deegree.feature.persistence.sql.rules.Mapping;
import org.deegree.feature.persistence.sql.rules.PrimitiveMapping;
import org.deegree.feature.xpath.FeatureXPathEvaluator;
import org.deegree.filter.FilterEvaluationException;
import org.deegree.geometry.Geometry;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.feature.FeatureReference;
import org.deegree.protocol.wfs.transaction.IDGenMode;
import org.deegree.sqldialect.SQLDialect;
import org.deegree.sqldialect.filter.DBField;
import org.deegree.sqldialect.filter.MappingExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs inserts in courtesy of the {@link SQLFeatureStoreTransaction}.
 * <p>
 * Important aspects of the implementation:
 * <ul>
 * <li>Streaming/low memory footprint</li>
 * <li>Usability for complex structures/mappings</li>
 * <li>Coping with unresolved feature references (forward/backward xlinks)</li>
 * <li>Auto-generated feature ids/key columns</li>
 * </ul>
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class InsertRowManager {

    private static Logger LOG = LoggerFactory.getLogger( InsertRowManager.class );

    private final SQLFeatureStore fs;

    private final SQLDialect dialect;

    private final Connection conn;

    private final GMLVersion gmlVersion;

    private final IDGenMode idGenMode;

    private final TableDependencies tableDeps;

    // key: original feature id (from Feature or FeatureReference), value: feature row
    private final Map<String, FeatureRow> origFidToFeatureRow = new HashMap<String, FeatureRow>();

    // key: insert row, value: dependent rows (never null)
    private final Map<InsertRow, List<InsertRow>> rowToChildRows = new HashMap<InsertRow, List<InsertRow>>();

    // values: rows that have not been inserted yet
    private final Set<InsertRow> delayedRows = new HashSet<InsertRow>();

    // values: rows that have not been inserted yet, but can be inserted (no parents)
    private final Set<InsertRow> rootRows = new HashSet<InsertRow>();

    /**
     * Creates a new {@link InsertRowManager} instance.
     * 
     * @param fs
     *            feature store, must not be <code>null</code>
     * @param conn
     *            connection, must not be <code>null</code>
     * @param idGenMode
     *            feature id generation mode, must not be <code>null</code>
     */
    public InsertRowManager( SQLFeatureStore fs, Connection conn, IDGenMode idGenMode ) {
        this.fs = fs;
        this.dialect = fs.getDialect();
        this.conn = conn;
        this.gmlVersion = fs.getSchema().getGMLSchema() == null ? GML_32 : fs.getSchema().getGMLSchema().getVersion();
        this.idGenMode = idGenMode;
        this.tableDeps = fs.getSchema().getKeyDependencies();
    }

    /**
     * Inserts the specified feature.
     * <p>
     * Note that some or all of the corresponding table rows may actually not be inserted when this method returns. They
     * may be delayed until their dependencies are inserted. This method also takes care of checking for rows that have
     * been delayed and can be inserted now.
     * </p>
     * 
     * @param feature
     *            feature instance to be inserted, must not be <code>null</code>
     * @param ftMapping
     *            mapping of the corresponding feature type, must not be <code>null</code>
     * @return id of the stored feature, never <code>null</code>
     */
    public FeatureRow insertFeature( Feature feature, FeatureTypeMapping ftMapping )
                            throws SQLException, FeatureStoreException, FilterEvaluationException {

        FeatureRow featureRow = null;
        try {
            featureRow = lookupFeatureRow( feature );

            // tracks all rows of this feature instance
            List<InsertRow> allRows = new ArrayList<InsertRow>();
            allRows.add( featureRow );

            for ( Mapping particleMapping : ftMapping.getMappings() ) {
                buildInsertRows( feature, particleMapping, featureRow, allRows );
            }

            LOG.debug( "Built rows for feature '" + feature.getId() + "': " + allRows.size() );

            for ( InsertRow insertRow : allRows ) {
                if ( !insertRow.hasParents() ) {
                    rootRows.add( insertRow );
                }
            }

            LOG.debug( "Before heap run: uninserted rows: " + delayedRows.size() + ", root rows: " + rootRows.size() );
            processHeap();
            LOG.debug( "After heap run: uninserted rows: " + delayedRows.size() + ", root rows: " + rootRows.size() );

        } catch ( Throwable t ) {
            LOG.debug( t.getMessage(), t );
            throw new FeatureStoreException( t.getMessage(), t );
        }
        return featureRow;
    }

    SQLDialect getDialect() {
        return dialect;
    }

    Connection getConnection() {
        return conn;
    }

    IDGenMode getIdGenMode() {
        return idGenMode;
    }

    MappedAppSchema getSchema() {
        return fs.getSchema();
    }

    Set<SQLIdentifier> getGenColumns( TableName table ) {
        return tableDeps.getGenColumns( table );
    }

    private FeatureRow lookupFeatureRow( String fid )
                            throws FeatureStoreException {
        FeatureRow featureRow = origFidToFeatureRow.get( fid );
        if ( featureRow == null ) {
            featureRow = new FeatureRow( this, fid );
            origFidToFeatureRow.put( fid, featureRow );
            delayedRows.add( featureRow );
        }
        return featureRow;
    }

    private FeatureRow lookupFeatureRow( Feature feature )
                            throws FeatureStoreException {
        FeatureRow featureRow = origFidToFeatureRow.get( feature.getId() );
        if ( featureRow == null ) {
            featureRow = new FeatureRow( this, feature.getId() );
            delayedRows.add( featureRow );
            if ( feature.getId() != null ) {
                origFidToFeatureRow.put( feature.getId(), featureRow );
            }
        }

        if ( !featureRow.isAssigned() ) {
            featureRow.assign( feature );
        }

        return featureRow;
    }

    private void buildInsertRows( final TypedObjectNode particle, final Mapping mapping, final InsertRow row,
                                  List<InsertRow> additionalRows )
                            throws FilterEvaluationException, FeatureStoreException {

        List<TableJoin> jc = mapping.getJoinedTable();
        if ( jc != null ) {
            if ( jc.size() != 1 ) {
                throw new FeatureStoreException( "Handling of joins with " + jc.size() + " steps is not implemented." );
            }
        }

        FeatureXPathEvaluator evaluator = new FeatureXPathEvaluator( gmlVersion );
        TypedObjectNode[] values = evaluator.eval( particle, mapping.getPath() );
        int childIdx = 1;
        for ( TypedObjectNode value : values ) {
            InsertRow currentRow = row;
            if ( jc != null && !( mapping instanceof FeatureMapping ) ) {
                TableJoin join = jc.get( 0 );
                currentRow = buildJoinRow( currentRow, join );
                additionalRows.add( currentRow );
            }
            if ( mapping instanceof PrimitiveMapping ) {
                MappingExpression me = ( (PrimitiveMapping) mapping ).getMapping();
                if ( !( me instanceof DBField ) ) {
                    LOG.debug( "Skipping primitive mapping. Not mapped to database column." );
                } else {
                    @SuppressWarnings("unchecked")
                    ParticleConverter<PrimitiveValue> converter = (ParticleConverter<PrimitiveValue>) fs.getConverter( mapping );
                    PrimitiveValue primitiveValue = getPrimitiveValue( value );
                    String column = ( (DBField) me ).getColumn();
                    currentRow.addPreparedArgument( column, primitiveValue, converter );
                }
            } else if ( mapping instanceof GeometryMapping ) {
                MappingExpression me = ( (GeometryMapping) mapping ).getMapping();
                if ( !( me instanceof DBField ) ) {
                    LOG.debug( "Skipping geometry mapping. Not mapped to database column." );
                } else {
                    Geometry geom = (Geometry) getPropValue( value );
                    @SuppressWarnings("unchecked")
                    ParticleConverter<Geometry> converter = (ParticleConverter<Geometry>) fs.getConverter( mapping );
                    String column = ( (DBField) me ).getColumn();
                    currentRow.addPreparedArgument( column, geom, converter );
                }
            } else if ( mapping instanceof FeatureMapping ) {
                FeatureRow subFeatureRow = null;
                String href = null;
                Feature feature = (Feature) getPropValue( value );
                if ( feature instanceof FeatureReference ) {
                    if ( ( (FeatureReference) feature ).isLocal() ) {
                        subFeatureRow = lookupFeatureRow( feature.getId() );
                    } else {
                        href = ( (FeatureReference) feature ).getURI();
                        MappingExpression me = ( (FeatureMapping) mapping ).getHrefMapping();
                        if ( !( me instanceof DBField ) ) {
                            LOG.debug( "Skipping feature mapping (href). Not mapped to database column." );
                        } else {
                            String column = ( (DBField) me ).getColumn();
                            Object sqlValue = SQLValueMangler.internalToSQL( href );
                            row.addPreparedArgument( column, sqlValue );
                        }
                    }
                } else if ( feature != null ) {
                    subFeatureRow = lookupFeatureRow( feature );
                }

                if ( subFeatureRow != null ) {

                    // TODO: pure href propagation (no fk)

                    if ( jc.isEmpty() ) {
                        LOG.debug( "Skipping feature mapping (fk). Not mapped to database column." );
                    } else {
                        TableJoin join = jc.get( 0 );
                        KeyPropagation keyPropagation = getKeyPropagation( (FeatureMapping) mapping, join );
                        // standard: pk in subfeature table (usually feature id)
                        ParentRowReference ref = new ParentRowReference( subFeatureRow, keyPropagation );
                        currentRow.addParent( ref );
                        List<InsertRow> children = rowToChildRows.get( subFeatureRow );
                        if ( children == null ) {
                            children = new ArrayList<InsertRow>();
                            rowToChildRows.put( subFeatureRow, children );
                        }
                        children.add( currentRow );

                        SQLIdentifier hrefCol = null;
                        if ( ( (FeatureMapping) mapping ).getHrefMapping() != null ) {
                            hrefCol = new SQLIdentifier( ( (FeatureMapping) mapping ).getHrefMapping().toString() );
                        }
                        ref.addHrefingRow( currentRow, hrefCol );

                        if ( !delayedRows.contains( subFeatureRow ) ) {
                            // sub feature already inserted, propagate key values right away
                            currentRow.removeParent( subFeatureRow );
                        }
                    }
                }
            } else if ( mapping instanceof CompoundMapping ) {
                for ( Mapping child : ( (CompoundMapping) mapping ).getParticles() ) {
                    buildInsertRows( value, child, currentRow, additionalRows );
                }
            } else {
                LOG.warn( "Unhandled mapping type '" + mapping.getClass() + "'." );
            }

            // add index column value if the join uses a numbered order column
            if ( jc != null && jc.size() == 1 ) {
                TableJoin join = jc.get( 0 );
                if ( join.isNumberedOrder() ) {
                    for ( SQLIdentifier col : join.getOrderColumns() ) {
                        if ( currentRow.get( col ) == null ) {
                            // TODO do this properly
                            currentRow.addLiteralValue( col, "" + childIdx++ );
                        }
                    }
                }
            }
        }
    }

    // special handling for joins to feature type tables
    private KeyPropagation getKeyPropagation( FeatureMapping mapping, TableJoin join )
                            throws FeatureStoreException {

        SQLIdentifier fromColumn = join.getFromColumns().get( 0 );
        SQLIdentifier toColumn = join.getToColumns().get( 0 );

        // a bit dirty: if no feature type is specified, use any
        QName ftName = getSchema().getFtMappings().keySet().iterator().next();
        if ( mapping.getValueFtName() != null ) {
            ftName = mapping.getValueFtName();
            // may be abstract, so take any concrete substitution feature type
            ftName = getSchema().getConcreteSubtypes( getSchema().getFeatureType( ftName ) )[0].getName();
        }
        FeatureTypeMapping ftMapping = getSchema().getFtMapping( ftName );
        TableName ftTable = ftMapping.getFtTable();
        Set<SQLIdentifier> ftTableGenColumns = tableDeps.getGenColumns( ftTable );
        if ( ftTableGenColumns != null && ftTableGenColumns.contains( toColumn ) ) {
            return new KeyPropagation( ftTable, toColumn, join.getFromTable(), fromColumn );
        }

        // must be the other way round
        throw new UnsupportedOperationException(
                                                 "Propagating feature property fks from join tables into target feature tables is not supported yet. " );
        // Set<SQLIdentifier> joinTableGenColumns = tableDeps.getGenColumns( join.getFromTable() );
        // if ( !joinTableGenColumns.contains( fromColumn ) ) {
        // String msg = "Invalid feature property join configuration.";
        // throw new FeatureStoreException( msg );
        // }
        //
        // return new KeyPropagation( join.getFromTable(), fromColumn, ftTable, toColumn );
    }

    private JoinRow buildJoinRow( InsertRow row, TableJoin join )
                            throws FeatureStoreException {

        JoinRow newRow = new JoinRow( this, join );
        delayedRows.add( newRow );

        KeyPropagation keyPropagation = tableDeps.getKeyPropagation( join.getFromTable(), join.getFromColumns(),
                                                                     join.getToTable(), join.getToColumns() );
        if ( keyPropagation == null ) {
            String msg = "Internal error: table dependencies don't contain join " + join;
            throw new FeatureStoreException( msg );
        }

        if ( keyPropagation.getPKTable().equals( join.getFromTable() ) ) {
            ParentRowReference ref = new ParentRowReference( row, keyPropagation );
            newRow.addParent( ref );
            List<InsertRow> children = rowToChildRows.get( row );
            if ( children == null ) {
                children = new ArrayList<InsertRow>();
                rowToChildRows.put( row, children );
            }
            children.add( newRow );
        } else {
            ParentRowReference ref = new ParentRowReference( newRow, keyPropagation );
            row.addParent( ref );
            List<InsertRow> children = new ArrayList<InsertRow>();
            rowToChildRows.put( newRow, children );
            children.add( row );
        }

        return newRow;
    }

    private PrimitiveValue getPrimitiveValue( TypedObjectNode value ) {
        if ( value instanceof Property ) {
            value = ( (Property) value ).getValue();
        }
        if ( value instanceof GenericXMLElement ) {
            value = ( (GenericXMLElement) value ).getValue();
        }
        return (PrimitiveValue) value;
    }

    private TypedObjectNode getPropValue( TypedObjectNode prop ) {
        if ( prop instanceof Property ) {
            return ( (Property) prop ).getValue();
        }
        return prop;
    }

    private void processHeap()
                            throws SQLException, FeatureStoreException {

        while ( !rootRows.isEmpty() ) {
            List<InsertRow> rootRemoves = new ArrayList<InsertRow>();
            List<InsertRow> rootAdds = new ArrayList<InsertRow>();
            for ( InsertRow row : rootRows ) {
                LOG.debug( "Inserting row " + row );
                row.performInsert( conn, rowToChildRows.get( row ) != null );
                delayedRows.remove( row );
                rootRemoves.add( row );

                // update child rows
                List<InsertRow> childRows = rowToChildRows.get( row );
                if ( childRows != null ) {
                    for ( InsertRow childRow : childRows ) {
                        LOG.debug( "Child row: " + childRow );
                        childRow.removeParent( row );
                        if ( !childRow.hasParents() ) {
                            rootAdds.add( childRow );
                        }
                    }
                    rowToChildRows.remove( row );
                }
            }
            rootRows.removeAll( rootRemoves );
            rootRows.addAll( rootAdds );
        }
    }

    /**
     * Returns the number of currently delayed rows (rows that depend on some other row to be inserted first).
     * 
     * @return number of currently delayed rows
     */
    public int getDelayedRows() {
        return delayedRows.size();
    }
}