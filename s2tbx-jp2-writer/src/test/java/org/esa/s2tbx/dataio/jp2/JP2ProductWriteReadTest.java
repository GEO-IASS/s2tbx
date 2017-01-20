package org.esa.s2tbx.dataio.jp2;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s2tbx.dataio.jp2.internal.JP2Constants;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.utils.TestUtil;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

/**
 * Created by Razvan Dumitrascu on 1/6/2017.
 */
public class JP2ProductWriteReadTest {

    private String productsFolder = "_temp" + File.separator;
    private final String FILE_NAME = "test_product.jp2";
    private Product outProduct;
    private File location;
    @Before
    public void setup() {

        final int width = 14;
        final int height = 14;
        outProduct = new Product("JP2Product", "JPEG-2000", width, height);
        final Band bandInt16 = outProduct.addBand("band_1", ProductData.TYPE_INT16);
        bandInt16.setDataElems(createShortData(getProductSize(), 23));
        ImageManager.getInstance().getSourceImage(bandInt16, 0);
    }

    @After
    public void tearDown() {
        if (!FileUtils.deleteTree(new File(TestUtil.getTestDirectory(productsFolder), FILE_NAME))) {
            fail("unable to delete test directory");
        }
    }
    @Test
    public void testWriteReadBeamMetadata() throws IOException {
        final Band expectedBand = outProduct.getBand("band_1");
        expectedBand.setLog10Scaled(false);
        expectedBand.setNoDataValueUsed(false);

        final Product inProduct = writeReadProduct();
        try {
            assertEquals(outProduct.getName(), inProduct.getName());
            assertEquals(outProduct.getProductType(), inProduct.getProductType());
            assertEquals(outProduct.getNumBands(), inProduct.getNumBands());

            final Band actualBand = inProduct.getBandAt(0);
            assertEquals(expectedBand.getName(), actualBand.getName());
            assertEquals(expectedBand.getDataType(), actualBand.getDataType());
            assertEquals(expectedBand.isLog10Scaled(), actualBand.isLog10Scaled());
            assertEquals(expectedBand.isNoDataValueUsed(), actualBand.isNoDataValueUsed());
        } finally {
            inProduct.dispose();
        }
    }

    @Test
    public void testWriteReadVirtualBandIsNotExcludedInProduct() throws IOException {
        final VirtualBand virtualBand = new VirtualBand("band_2", ProductData.TYPE_UINT8,
                outProduct.getSceneRasterWidth(),
                outProduct.getSceneRasterHeight(), "X * Y");
        outProduct.addBand(virtualBand);
        final Product inProduct = writeReadProduct();
        try {
            assertEquals(2, inProduct.getNumBands());
            assertNotNull(inProduct.getBand("band_2"));
        } finally {
            inProduct.dispose();
        }
    }

    @Test
    public void testWriteReadTiePointGeoCoding() throws IOException {
        setTiePointGeoCoding(outProduct);
        final Band bandUInt8 = outProduct.addBand("band_2", ProductData.TYPE_UINT8);
        bandUInt8.setDataElems(createByteData(getProductSize(), 23));
        final Product inProduct = writeReadProduct();
        try {
            assertEquals(outProduct.getName(), inProduct.getName());
            assertEquals(outProduct.getProductType(), inProduct.getProductType());
            assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
            assertEquals(outProduct.getBandAt(0).getName(), inProduct.getBandAt(0).getName());
            assertEquals(outProduct.getBandAt(0).getDataType(), inProduct.getBandAt(0).getDataType());
            assertEquals(outProduct.getBandAt(0).getScalingFactor(), inProduct.getBandAt(0).getScalingFactor(), 1.0e-6);
            assertEquals(outProduct.getBandAt(0).getScalingOffset(), inProduct.getBandAt(0).getScalingOffset(), 1.0e-6);
            assertEquals(location, inProduct.getFileLocation());
            assertNotNull(inProduct.getSceneGeoCoding());
            assertNotNull(outProduct.getSceneGeoCoding());
            assertEquals(inProduct.getSceneGeoCoding().canGetGeoPos(), outProduct.getSceneGeoCoding().canGetGeoPos());
            assertEquals(inProduct.getSceneGeoCoding().isCrossingMeridianAt180(), outProduct.getSceneGeoCoding().isCrossingMeridianAt180());

            if (inProduct.getSceneGeoCoding() instanceof CrsGeoCoding) {
                assertEquals(CrsGeoCoding.class, outProduct.getSceneGeoCoding().getClass());
                CRS.equalsIgnoreMetadata(inProduct.getSceneGeoCoding(), outProduct.getSceneGeoCoding());
            } else if (inProduct.getSceneGeoCoding() instanceof TiePointGeoCoding) {
                assertEquals(TiePointGeoCoding.class, outProduct.getSceneGeoCoding().getClass());
            }
            final int width = outProduct.getSceneRasterWidth();
            final int height = outProduct.getSceneRasterHeight();
            GeoPos geoPos1 = null;
            GeoPos geoPos2 = null;
            final String msgPattern = "%s at [%d,%d] is not equal:";
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    final PixelPos pixelPos = new PixelPos(i, j);
                    geoPos1 = inProduct.getSceneGeoCoding().getGeoPos(pixelPos, geoPos1);
                    geoPos2 = outProduct.getSceneGeoCoding().getGeoPos(pixelPos, geoPos2);
                    assertEquals(String.format(msgPattern, "Latitude", i, j), geoPos1.lat, geoPos2.lat, 1e-6f);
                    assertEquals(String.format(msgPattern, "Longitude", i, j), geoPos1.lon, geoPos2.lon, 1e-6f);
                }
            }
        } finally {
            inProduct.dispose();
        }
    }

    private static void setTiePointGeoCoding(final Product product) {
        final TiePointGrid latGrid = new TiePointGrid("latitude", 3, 3, 0, 0, 14,14, new float[]{
                85, 84, 83,
                75, 74, 73,
                65, 64, 63
        },TiePointGrid.DISCONT_NONE);

        final TiePointGrid lonGrid = new TiePointGrid("longitude", 3, 3, 0, 0, 14, 14, new float[]{
                -15, -5, 5,
                -16, -6, 4,
                -17, -7, 3
        },TiePointGrid.DISCONT_AT_180);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    private static short[] createShortData(final int size, final int offset) {
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (i + offset);
        }
        return shorts;
    }

    private static byte[] createByteData(final int size, final int offset) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i + offset);
        }
        return bytes;
    }

    private int getProductSize() {
        final int w = outProduct.getSceneRasterWidth();
        final int h = outProduct.getSceneRasterHeight();
        return w * h;
    }

    private Product writeReadProduct() throws IOException {
        location = new File(TestUtil.getTestDirectory(productsFolder), FILE_NAME);

        final String JP2FormatName = JP2Constants.FORMAT_NAMES[0];
        ProductIO.writeProduct(outProduct, location.getAbsolutePath(), JP2FormatName);

        return ProductIO.readProduct(location, JP2FormatName);
    }

}
