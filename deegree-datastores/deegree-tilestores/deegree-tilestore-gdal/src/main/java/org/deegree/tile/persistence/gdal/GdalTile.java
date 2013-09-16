/*----------------------------------------------------------------------------
 This file is part of deegree
 Copyright (C) 2001-2013 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -
 and
 - Occam Labs UG (haftungsbeschränkt) -
 and others

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

 e-mail: info@deegree.org
 website: http://www.deegree.org/
----------------------------------------------------------------------------*/
package org.deegree.tile.persistence.gdal;

import static java.awt.image.DataBuffer.TYPE_BYTE;
import static org.gdal.gdalconst.gdalconstConstants.GDT_Byte;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.deegree.feature.FeatureCollection;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.standard.DefaultEnvelope;
import org.deegree.geometry.standard.primitive.DefaultPoint;
import org.deegree.tile.Tile;
import org.deegree.tile.TileIOException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;

/**
 * {@link Tile} backed by a GDAL <code>Dataset</code>.
 * 
 * @author <a href="mailto:schneider@occamlabs.de">Markus Schneider</a>
 * 
 * @since 3.5
 */
class GdalTile implements Tile {

    // private static final Logger LOG = getLogger( GeoTIFFTile.class );

    private final Envelope tileEnvelope;

    private final Envelope datasetEnvelope;

    private final Envelope readWindow;

    private final int pixelsX, pixelsY;

    private final GenericObjectPool<Dataset> gdalDatasetPool;

    private final int datasetPixelsY;

    private final int datasetPixelsX;

    private final int datasetMinY;

    private final int datasetMinX;

    private long y;

    private long x;

    private double unitsPerPixel;

    private double unitsPerPixelX;

    private double unitsPerPixelY;

    private Envelope tileEnvelope2;

    /**
     * Creates a new {@link GdalTile} instance.
     * 
     * @param tileEnvelope
     *            bounding box of the tile, must not be <code>null</code>
     * @param datasetEnvelope
     *            bounding box of the dataset, must not be <code>null</code>
     * @param pixelsX
     *            width of the tile in pixels
     * @param pixelsY
     *            height of the tile in pixels
     * @param gdalDatasetPool
     *            pool of GDAL Datasets, never <code>null</code>
     * @param tileEnvelope2 
     */
    GdalTile( Envelope tileEnvelope, Envelope datasetEnvelope, int pixelsX, int pixelsY,
              GenericObjectPool<Dataset> gdalDatasetPool, int datasetMinX, int datasetMinY, int datasetPixelsX,
              int datasetPixelsY, long x, long y, double unitsPerPixel, double unitsPerPixelX, double unitsPerPixelY, Envelope tileEnvelope2 ) {
        this.tileEnvelope = tileEnvelope;
        this.tileEnvelope2 = tileEnvelope2;
        this.datasetEnvelope = datasetEnvelope;
        this.pixelsX = pixelsX;
        this.pixelsY = pixelsY;
        this.gdalDatasetPool = gdalDatasetPool;
        this.datasetMinX = datasetMinX;
        this.datasetMinY = datasetMinY;
        this.datasetPixelsX = datasetPixelsX;
        this.datasetPixelsY = datasetPixelsY;
        this.x = x;
        this.y = y;
        this.unitsPerPixel = unitsPerPixel;
        this.unitsPerPixelX = unitsPerPixelX;
        this.unitsPerPixelY = unitsPerPixelY;
        readWindow = determineReadWindow();
    }

    private Envelope determineReadWindow() {
        double minX = tileEnvelope.getMin().get0();
        double minY = tileEnvelope.getMin().get1();
        double maxX = tileEnvelope.getMax().get0();
        double maxY = tileEnvelope.getMax().get1();
        if ( datasetEnvelope.getMin().get0() > minX ) {
            minX = datasetEnvelope.getMin().get0();
        }
        if ( datasetEnvelope.getMin().get1() > minY ) {
            minY = datasetEnvelope.getMin().get1();
        }
        if ( datasetEnvelope.getMax().get0() < maxX ) {
            maxX = datasetEnvelope.getMax().get0();
        }
        if ( datasetEnvelope.getMax().get1() < maxY ) {
            maxY = datasetEnvelope.getMax().get1();
        }
        Point min = new DefaultPoint( null, null, null, new double[] { minX, minY } );
        Point max = new DefaultPoint( null, null, null, new double[] { maxX, maxY } );
        return new DefaultEnvelope( min, max );
    }

    @Override
    public BufferedImage getAsImage()
                            throws TileIOException {
        Dataset dataset = null;
        try {
            dataset = gdalDatasetPool.borrowObject();
            BufferedImage img = extractTile( dataset );
            return img;
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new TileIOException( "Error retrieving image: " + e.getMessage(), e );
        } finally {
            try {
                gdalDatasetPool.returnObject( dataset );
            } catch ( Exception e ) {
                // ignore closing error
            }
        }
    }

    private BufferedImage extractTile( Dataset dataset )
                            throws IOException {

        int numBands = dataset.getRasterCount();

        BufferedImage img = null;
        boolean isTileCompletelyInsideDataset = isTileCompletelyInsideDataset( dataset );
        if ( isTileCompletelyInsideDataset ) {
            img = readTile( dataset, numBands );
        } else if ( readWindow.getSpan0() <= 0 || readWindow.getSpan1() <= 0 ) {
            byte[][] bands = new byte[numBands][pixelsX * pixelsY];
            img = toImage( bands, pixelsX, pixelsY );
        } else {
            img = readTileWindowAndBlitIntoTile( dataset, numBands );
        }

        return img;
    }

    private BufferedImage readTileWindowAndBlitIntoTile( Dataset dataset, int numBands )
                            throws IOException {
        double worldMinX = readWindow.getMin().get0() - datasetEnvelope.getMin().get0();
        double worldMaxY = readWindow.getMax().get1() - datasetEnvelope.getMin().get1();
        worldMaxY = datasetEnvelope.getSpan1() - worldMaxY;
        int offsetX = (int) Math.round( worldMinX / unitsPerPixelX );
        int offsetY = (int) Math.round( worldMaxY / unitsPerPixelY );
        int xSize = (int) Math.round( readWindow.getSpan0() / unitsPerPixelX );
        int ySize = (int) Math.round( readWindow.getSpan1() / unitsPerPixelY );
        int targetOffsetX = (int) Math.round( (readWindow.getMin().get0() - tileEnvelope.getMin().get0()) / unitsPerPixel );
        int targetOffsetY = (int) Math.round( (tileEnvelope.getMax().get1() - readWindow.getMax().get1()) / unitsPerPixel );
        int targetSizeX = (int) Math.round( readWindow.getSpan0() / unitsPerPixel );
        int targetSizeY = (int) Math.round( readWindow.getSpan1() / unitsPerPixel );
        byte[][] windowData = readTileWindow( dataset, numBands, offsetX, offsetY, xSize, ySize, targetSizeX,
                                              targetSizeY );
        byte[][] tileData = createTileFromWindow( windowData, targetSizeX, targetSizeY, targetOffsetX, targetOffsetY );
        BufferedImage img = toImage( tileData, pixelsX, pixelsY );
        // TODO Auto-generated method stub
        return img;
    }

    private byte[][] createTileFromWindow( byte[][] windowData, int windowSizeX, int windowSizeY, int tileOffsetX,
                                           int tileOffsetY ) {
        byte[][] bands = new byte[windowData.length][pixelsX * pixelsY];
        for ( int i = 0; i < bands.length; i++ ) {
            byte[] src = windowData[i];
            byte[] dst = bands[i];
            for ( int y = 0; y < windowSizeY; y++ ) {
                for ( int x = 0; x < windowSizeX; x++ ) {
                    int targetX = tileOffsetX + x;
                    int targetY = tileOffsetY + y;
                    try {
                        dst[targetX + targetY * pixelsX] = src[x + y * windowSizeX];
                    } catch ( Exception e ) {
                        System.out.println(targetX + ", " + targetY);
                    }
                }
            }
        }
        return bands;
    }

    private byte[][] readTileWindow( Dataset dataset, int numBands, int offsetX, int offsetY, int xSize, int ySize,
                                     int targetSizeX, int targetSizeY ) {
        byte[][] bands = new byte[numBands][targetSizeX * targetSizeY];
        for ( int i = 0; i < numBands; i++ ) {
            Band band = dataset.GetRasterBand( i + 1 );
            byte[] bandBytes = bands[i];
            synchronized ( this.getClass() ) {
                band.ReadRaster( offsetX, offsetY, xSize, ySize, targetSizeX, targetSizeY, GDT_Byte, bandBytes, 0, 0 );
            }
        }
        return bands;
    }

    private BufferedImage readTile( Dataset dataset, int numBands )
                            throws IOException {
        BufferedImage img;
        byte[][] bands = new byte[numBands][pixelsX * pixelsY];
        for ( int i = 0; i < numBands; i++ ) {
            Band band = dataset.GetRasterBand( i + 1 );
            byte[] bandBytes = bands[i];
            synchronized ( this.getClass() ) {
                band.ReadRaster( datasetMinX, datasetMinY, datasetPixelsX, datasetPixelsY, pixelsX, pixelsY, GDT_Byte,
                                 bandBytes, 0, 0 );
            }
        }
        img = toImage( bands, pixelsX, pixelsY );
        return img;
    }

    private boolean isTileCompletelyInsideDataset( Dataset dataset ) {
        return datasetMinX >= 0 && datasetMinY >= 0 && datasetMinX + datasetPixelsX < dataset.getRasterXSize()
               && datasetMinY + datasetPixelsY < dataset.getRasterYSize();
    }

    private BufferedImage toImage( byte[][] bands, int xSize, int ySize )
                            throws IOException {
        int numBytes = xSize * ySize * bands.length;
        DataBuffer imgBuffer = new DataBufferByte( bands, numBytes );
        SampleModel sampleModel = new BandedSampleModel( TYPE_BYTE, xSize, ySize, bands.length );
        WritableRaster raster = Raster.createWritableRaster( sampleModel, imgBuffer, null );
        ColorSpace cs = ColorSpace.getInstance( ColorSpace.CS_sRGB );        
        
        ColorModel cm;        
        if( bands.length == 3 ) {
            cm = new ComponentColorModel( cs, false, false, ColorModel.OPAQUE, TYPE_BYTE );
        } else if( bands.length == 4 ) {
            cm = new ComponentColorModel( cs, true, false, ColorModel.TRANSLUCENT, TYPE_BYTE );
        } else {
            throw new IllegalArgumentException( "Unsupported number of bands: " + bands.length );
        }
        
        return new BufferedImage( cm, raster, false, null );
    }

    @Override
    public InputStream getAsStream()
                            throws TileIOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write( getAsImage(), "png", bos );
        } catch ( IOException e ) {
            throw new TileIOException( "Error retrieving image: " + e.getMessage(), e );
        }
        return new ByteArrayInputStream( bos.toByteArray() );
    }

    @Override
    public Envelope getEnvelope() {
        return tileEnvelope2;
    }

    @Override
    public FeatureCollection getFeatures( int i, int j, int limit )
                            throws UnsupportedOperationException {
        throw new UnsupportedOperationException( "Feature retrieval is not supported by the GDALTileStore." );
    }

}
