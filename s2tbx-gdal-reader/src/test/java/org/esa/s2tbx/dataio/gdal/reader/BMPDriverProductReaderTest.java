package org.esa.s2tbx.dataio.gdal.reader;

import org.esa.s2tbx.dataio.gdal.GdalInstallInfo;
import org.esa.s2tbx.dataio.gdal.reader.plugins.BMPDriverProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Jean Coravu
 */
public class BMPDriverProductReaderTest extends AbstractTestDriverProductReader {

    public BMPDriverProductReaderTest() {
    }

    @Test
    public void testBMPReadProductNodes() throws IOException {
        if (GdalInstallInfo.INSTANCE.isPresent()) {
            File file = this.gdalTestsFolderPath.resolve("BMP-driver.bmp").toFile();

            BMPDriverProductReaderPlugIn readerPlugin = new BMPDriverProductReaderPlugIn();
            GDALProductReader reader = (GDALProductReader)readerPlugin.createReaderInstance();
            Product finalProduct = reader.readProductNodes(file, null);
            assertNull(finalProduct.getSceneGeoCoding());
            assertEquals(1, finalProduct.getBands().length);
            assertEquals("GDAL", finalProduct.getProductType());
            assertEquals(20, finalProduct.getSceneRasterWidth());
            assertEquals(30, finalProduct.getSceneRasterHeight());

            Band band = finalProduct.getBandAt(0);
            assertEquals(20, band.getDataType());
            assertEquals(600, band.getNumDataElems());

            float bandValue = band.getSampleFloat(10, 10);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(13, 24);
            assertEquals(238.0f, bandValue, 0);

            bandValue = band.getSampleFloat(6, 23);
            assertEquals(211.0f, bandValue, 0);

            bandValue = band.getSampleFloat(17, 29);
            assertEquals(86.0f, bandValue, 0);

            bandValue = band.getSampleFloat(15, 21);
            assertEquals(180.0f, bandValue, 0);
        }
    }
}
