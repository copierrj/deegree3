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
package org.deegree.sqldialect.oracle;

import static oracle.sql.ArrayDescriptor.createDescriptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import oracle.jdbc.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;

import org.apache.commons.dbcp.DelegatingConnection;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.GeometryTransformer;
import org.deegree.geometry.utils.GeometryParticleConverter;
import org.deegree.sqldialect.oracle.sdo.SDOGeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author <a href="mailto:reichhelm@grit.de">Stephan Reichhelm</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class OracleGeometryConverter implements GeometryParticleConverter {

    private static Logger LOG = LoggerFactory.getLogger( OracleGeometryConverter.class );

    private final String column;

    private final ICRS crs;

    private final String srid;

    private int isrid;

    /**
     * Creates a new {@link OracleGeometryConverter} instance.
     * 
     * @param column
     *            (unqualified) column that stores the geometry, must not be <code>null</code>
     * @param crs
     *            CRS of the stored geometries, can be <code>null</code>
     * @param srid
     *            Oracle spatial reference identifier, must not be <code>null</code>
     */
    public OracleGeometryConverter( String column, ICRS crs, String srid ) {
        this.column = column;
        this.crs = crs;
        this.srid = srid;
        this.isrid = 0;
        try {
            if ( srid != null )
                this.isrid = Integer.valueOf( srid );
        } catch ( NumberFormatException nfe ) {
            // TODO handle it smoother
        }
    }

    @Override
    public String getSelectSnippet( String tableAlias ) {
        if ( tableAlias != null ) {
            return tableAlias + "." + column;
        }
        return column;
    }

    @Override
    public String getSetSnippet( final Geometry particle ) {
        if ( setAsArrays( particle ) ) {
            return "MDSYS.SDO_GEOMETRY(?,?,NULL,?,?)";
        }
        return "?";
    }

    private boolean setAsArrays( final Geometry particle ) {
        return particle instanceof Envelope;
    }

    @Override
    public Geometry toParticle( ResultSet rs, int colIndex )
                            throws SQLException {
        Object sqlValue = rs.getObject( colIndex );
        if ( sqlValue == null ) {
            return null;
        }
        try {
            return new SDOGeometryConverter().toGeometry( (STRUCT) sqlValue, crs );
        } catch ( Throwable t ) {
            LOG.trace( t.getMessage(), t );
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int setParticle( PreparedStatement stmt, Geometry particle, int paramIndex )
                            throws SQLException {
        int numParams = 1;
        try {
            if ( particle == null ) {
                stmt.setNull( paramIndex, Types.STRUCT, "MDSYS.SDO_GEOMETRY" );
            } else {
                Geometry compatible = getCompatibleGeometry( particle );
                OracleConnection ocon = getOracleConnection( stmt.getConnection() );
                if ( compatible instanceof Envelope ) {
                    numParams = setUsingArrays( stmt, paramIndex, (Envelope) compatible, ocon );
                } else {
                    numParams = setUsingStruct( stmt, paramIndex, compatible, ocon );
                }
            }
        } catch ( Throwable t ) {
            t.printStackTrace();
            throw new IllegalArgumentException();
        }
        return numParams;
    }

    private int setUsingStruct( final PreparedStatement stmt, final int paramIndex, final Geometry compatible,
                                final OracleConnection ocon )
                            throws SQLException {
        Object struct = new SDOGeometryConverter().fromGeometry( ocon, isrid, compatible, true );
        stmt.setObject( paramIndex, struct );
        return 1;
    }

    private int setUsingArrays( final PreparedStatement stmt, final int paramIndex, final Envelope env,
                                final OracleConnection conn )
                            throws SQLException {
        int i = paramIndex;
        stmt.setInt( i++, 2003 );
        stmt.setInt( i++, isrid );
        stmt.setObject( i++, getElemInfoArrayForEnvelope( conn ) );
        stmt.setObject( i, getOrdinateArrayForEnvelope( conn, env ) );
        return 4;
    }

    private ARRAY getElemInfoArrayForEnvelope( final Connection conn )
                            throws SQLException {
        final ArrayDescriptor descriptor = createDescriptor( "MDSYS.SDO_ELEM_INFO_ARRAY", conn );
        final Integer[] elements = new Integer[] { 1, 1003, 3 };
        return new ARRAY( descriptor, conn, elements );
    }

    private ARRAY getOrdinateArrayForEnvelope( final Connection conn, final Envelope env )
                            throws SQLException {
        final ArrayDescriptor descriptor = createDescriptor( "MDSYS.SDO_ORDINATE_ARRAY", conn );
        final Double[] elements = new Double[] { env.getMin().get0(), env.getMin().get1(), env.getMax().get0(),
                                                env.getMax().get1() };
        return new ARRAY( descriptor, conn, elements );
    }

    private OracleConnection getOracleConnection( Connection conn )
                            throws SQLException {
        OracleConnection ocon = null;
        if ( conn instanceof OracleConnection ) {
            ocon = (OracleConnection) conn;
        } else if ( conn instanceof DelegatingConnection ) {
            ocon = (OracleConnection) ( (DelegatingConnection) conn ).getInnermostDelegate();
        } else {
            ocon = conn.unwrap( OracleConnection.class );
        }
        return ocon;
    }

    private Geometry getCompatibleGeometry( Geometry literal )
                            throws SQLException {
        if ( crs == null ) {
            return literal;
        }

        Geometry transformedLiteral = literal;
        if ( literal != null ) {
            ICRS literalCRS = literal.getCoordinateSystem();
            if ( literalCRS != null && !( crs.equals( literalCRS ) ) ) {
                LOG.debug( "Need transformed literal geometry for evaluation: " + literalCRS.getAlias() + " -> "
                           + crs.getAlias() );
                try {
                    GeometryTransformer transformer = new GeometryTransformer( crs );
                    transformedLiteral = transformer.transform( literal );
                } catch ( Exception e ) {
                    throw new SQLException( e.getMessage() );
                }
            }
        }
        return transformedLiteral;
    }

    @Override
    public String getSrid() {
        return srid;
    }

    @Override
    public ICRS getCrs() {
        return crs;
    }
}
