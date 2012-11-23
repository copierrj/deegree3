//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
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

 Occam Labs UG (haftungsbeschränkt)
 Godesberger Allee 139, 53175 Bonn
 Germany
 http://www.occamlabs.de/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.tile.persistence.remotewms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.deegree.geometry.Envelope;
import org.deegree.protocol.wms.client.WMSClient;
import org.deegree.protocol.wms.ops.GetMap;
import org.deegree.tile.Tile;
import org.deegree.tile.TileIOException;

/**
 * {@link Tile} implementation used by the {@link RemoteWMSTileStore}.
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: mschneider $
 * 
 * @version $Revision: 31882 $, $Date: 2011-09-15 02:05:04 +0200 (Thu, 15 Sep 2011) $
 */
class RemoteWMSTile implements Tile {

    private final WMSClient client;

    private final GetMap gm;

    private final String outputFormat;

    /**
     * Creates a new {@link RemoteWMSTile} instance.
     * 
     * @param client
     *            client to use for performing the {@link GetMap} request, never <code>null</code>
     * @param gm
     *            request for retrieving the tile image, never <code>null</code>
     * @param outputFormat
     *            if not null, images will be recoded into specified output format (use ImageIO like formats, eg. 'png')
     */
    RemoteWMSTile( WMSClient client, GetMap gm, String outputFormat ) {
        this.client = client;
        this.gm = gm;
        this.outputFormat = outputFormat;
    }

    @Override
    public BufferedImage getAsImage()
                            throws TileIOException {
        InputStream in = null;
        try {
            return ImageIO.read( in = getAsStream() );
        } catch ( IOException e ) {
            throw new TileIOException( "Error decoding image : " + e.getMessage(), e );
        } finally {
            IOUtils.closeQuietly( in );
        }
    }

    @Override
    public InputStream getAsStream()
                            throws TileIOException {
        try {
            if ( outputFormat != null ) {
                BufferedImage img = ImageIO.read( client.getMap( gm ) );
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write( img, outputFormat, out );
                out.close();
                return new ByteArrayInputStream( out.toByteArray() );
            }
            return client.getMap( gm );
        } catch ( IOException e ) {
            throw new TileIOException( "Error performing GetMap request: " + e.getMessage(), e );
        }
    }

    @Override
    public Envelope getEnvelope() {
        return gm.getBoundingBox();
    }
}
