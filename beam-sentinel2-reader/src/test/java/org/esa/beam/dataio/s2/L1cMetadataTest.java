package org.esa.beam.dataio.s2;

import org.geotools.geometry.Envelope2D;
import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class L1cMetadataTest {

    private L1cMetadata header;

    @Before
    public void before() throws JDOMException, IOException {
        InputStream stream = getClass().getResourceAsStream("l1c/MTD_GPPL1C_054_20091210235100_20091210235130_0001.xml");
        header = L1cMetadata.parseHeader(new InputStreamReader(stream));
    }

    @Test
    public void testProductCharacteristics() {
        L1cMetadata.ProductCharacteristics pc = header.getProductCharacteristics();
        assertEquals("SENTINEL-2A", pc.spacecraft);
        assertEquals("11-09-21-17:47:18", pc.datasetProductionDate);
        assertEquals("L1C", pc.processingLevel);
        assertEquals(13, pc.bandInformations.length);
        testSpectralInformation(pc.bandInformations[0], 0, "B1", 60, 443.0);
        testSpectralInformation(pc.bandInformations[1], 1, "B2", 10, 490.0);
        testSpectralInformation(pc.bandInformations[2], 2, "B3", 10, 560.0);
        testSpectralInformation(pc.bandInformations[3], 3, "B4", 10, 665.0);
        testSpectralInformation(pc.bandInformations[4], 4, "B5", 20, 705.0);
        testSpectralInformation(pc.bandInformations[5], 5, "B6", 20, 740.0);
        testSpectralInformation(pc.bandInformations[6], 6, "B7", 20, 783.0);
        testSpectralInformation(pc.bandInformations[7], 7, "B8", 10, 842.0);
        testSpectralInformation(pc.bandInformations[8], 8, "B8A", 20, 865.0);
        testSpectralInformation(pc.bandInformations[9], 9, "B9", 60, 945.0);
        testSpectralInformation(pc.bandInformations[10], 10, "B10", 60, 1375.0);
        testSpectralInformation(pc.bandInformations[11], 11, "B11", 20, 1610.0);
        testSpectralInformation(pc.bandInformations[12], 12, "B12", 20, 2190.0);
    }

    @Test
    public void testResampleData() {
        L1cMetadata.ResampleData rd = header.getResampleData();
        assertEquals(3413, rd.quantificationValue);
        assertEquals(1.030577302, rd.reflectanceConversion.u, 1e-10);
        assertEquals(13, rd.reflectanceConversion.solarIrradiances.length);
        assertEquals(1895.27, rd.reflectanceConversion.solarIrradiances[0], 1e-10);
        assertEquals(1165.87, rd.reflectanceConversion.solarIrradiances[6], 1e-10);
        assertEquals(86.98, rd.reflectanceConversion.solarIrradiances[12], 1e-10);
    }

    @Test
    public void testQuicklookDescriptor() {
        L1cMetadata.QuicklookDescriptor qld = header.getQuicklookDescriptor();
        assertEquals(905, qld.imageNCols);
        assertEquals(793, qld.imageNRows);
        assertEquals(6, qld.histogramList.length);

        assertEquals(9, qld.histogramList[0].bandId);
        assertEquals(256, qld.histogramList[0].step, 1e-5);
        assertEquals(0, qld.histogramList[0].min, 1e-5);
        assertEquals(267, qld.histogramList[0].max, 1e-5);
        assertEquals(111.155, qld.histogramList[0].mean, 1e-5);
        assertEquals(55.6774, qld.histogramList[0].stdDev, 1e-5);
        final int[] values9 = qld.histogramList[0].values;
        assertEquals(256, values9.length, 1e-5);
        int sum9 = 0;
        for (int v : values9) {
            sum9 += v;
        }
        assertEquals(717660, sum9);
        assertEquals(717665, qld.imageNCols * qld.imageNRows);

        assertEquals(1, qld.histogramList[1].bandId);
        assertEquals(256, qld.histogramList[1].step, 1e-5);
        assertEquals(0, qld.histogramList[1].min, 1e-5);
        assertEquals(1485, qld.histogramList[1].max, 1e-5);
        assertEquals(565.748, qld.histogramList[1].mean, 1e-5);
        assertEquals(276.146, qld.histogramList[1].stdDev, 1e-5);
        final int[] values1 = qld.histogramList[1].values;
        assertEquals(256, values1.length, 1e-5);
        int sum1 = 0;
        for (int v : values1) {
            sum1 += v;
        }
        assertEquals(717664, sum1);
        assertEquals(717665, qld.imageNCols * qld.imageNRows);
    }

    @Test
    public void testTileList() {

        List<L1cMetadata.Tile> tileList = header.getTileList();

        assertEquals(11, tileList.size());

        L1cMetadata.Tile tile0 = tileList.get(0);
        L1cMetadata.Tile tile1 = tileList.get(1);
        L1cMetadata.Tile tile2 = tileList.get(2);
        L1cMetadata.Tile tile3 = tileList.get(3);
        L1cMetadata.Tile tile4 = tileList.get(4);
        L1cMetadata.Tile tile5 = tileList.get(5);
        L1cMetadata.Tile tile6 = tileList.get(6);
        L1cMetadata.Tile tile7 = tileList.get(7);
        L1cMetadata.Tile tile8 = tileList.get(8);
        L1cMetadata.Tile tile9 = tileList.get(9);
        L1cMetadata.Tile tile10 = tileList.get(10);

        assertEquals("15SUC", tile0.id);
        assertEquals("15SUD", tile1.id);
        assertEquals("15SVC", tile2.id);
        assertEquals("15SVD", tile3.id);
        assertEquals("15SWC", tile4.id);
        assertEquals("15SWD", tile5.id);
        assertEquals("15SXC", tile6.id);
        assertEquals("15SXD", tile7.id);
        assertEquals("15TUE", tile8.id);
        assertEquals("15TVE", tile9.id);
        assertEquals("15TWE", tile10.id);


        assertEquals("WGS84 / UTM zone 15N", tile0.horizontalCsName);
        assertEquals("EPSG:32615", tile0.horizontalCsCode);

        assertEquals(10960, tile0.tileGeometry10M.numRows);
        assertEquals(10960, tile0.tileGeometry10M.numCols);

        assertEquals(299940.0, tile0.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(299940.0, tile1.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(399960.0, tile2.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(399960.0, tile3.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(499980.0, tile4.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(499980.0, tile5.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(599940.0, tile6.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(599940.0, tile7.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(299940.0, tile8.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(399960.0, tile9.tileGeometry10M.upperLeftX, 1e-10);
        assertEquals(499980.0, tile10.tileGeometry10M.upperLeftX, 1e-10);

        assertEquals(4300060.0, tile0.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4400060.0, tile1.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4300060.0, tile2.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4400060.0, tile3.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4300060.0, tile4.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4400060.0, tile5.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4300060.0, tile6.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4400060.0, tile7.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4500060.0, tile8.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4500060.0, tile9.tileGeometry10M.upperLeftY, 1e-10);
        assertEquals(4500060.0, tile10.tileGeometry10M.upperLeftY, 1e-10);

        assertEquals(10.0, tile0.tileGeometry10M.xDim, 1e-10);
        assertEquals(-10.0, tile0.tileGeometry10M.yDim, 1e-10);

        assertEquals(5480, tile0.tileGeometry20M.numRows);
        assertEquals(5480, tile0.tileGeometry20M.numCols);
        assertEquals(299940.0, tile0.tileGeometry20M.upperLeftX, 1e-10);
        assertEquals(4300060.0, tile0.tileGeometry20M.upperLeftY, 1e-10);
        assertEquals(20.0, tile0.tileGeometry20M.xDim, 1e-10);
        assertEquals(-20.0, tile0.tileGeometry20M.yDim, 1e-10);

        assertEquals(1826, tile0.tileGeometry60M.numRows);
        assertEquals(1826, tile0.tileGeometry60M.numCols);
        assertEquals(299940.0, tile0.tileGeometry60M.upperLeftX, 1e-10);
        assertEquals(4300060.0, tile0.tileGeometry60M.upperLeftY, 1e-10);
        assertEquals(60.0, tile0.tileGeometry60M.xDim, 1e-10);
        assertEquals(-60.0, tile0.tileGeometry60M.yDim, 1e-10);

        dumpNans(tileList);
    }

    @Test
    public void testTileGrid() throws IOException {
        L1cSceneDescription sceneDescription = L1cSceneDescription.create(header);
        ImageIO.write(sceneDescription.createTilePicture(2048), "PNG", new File("tile-grid.png"));

        Envelope2D sceneEnvelope = sceneDescription.getSceneEnvelope();

        assertEquals(299940.0, sceneEnvelope.getX(), 1e-10);
        assertEquals(4190460.0, sceneEnvelope.getY(), 1e-10);
        assertEquals(409600.0, sceneEnvelope.getWidth(), 1e-10);
        assertEquals(309600.0, sceneEnvelope.getHeight(), 1e-10);

        assertEquals(11, sceneDescription.getTileCount());
        Envelope2D tileEnvelope0 = sceneDescription.getTileEnvelope(0);
        assertEquals(299940.0, tileEnvelope0.getX(), 1e-10);
        assertEquals(4190460.0, tileEnvelope0.getY(), 1e-10);
        assertEquals(109600.0, tileEnvelope0.getWidth(), 1e-10);
        assertEquals(109600.0, tileEnvelope0.getHeight(), 1e-10);

        assertEquals(new Rectangle(0, 0, 40960, 30960), sceneDescription.getSceneRectangle());

        assertEquals(new Rectangle(0, 20000, 10960, 10960), sceneDescription.getTileRectangle(0));
        assertEquals(new Rectangle(0, 10000, 10960, 10960), sceneDescription.getTileRectangle(1));
        assertEquals(new Rectangle(10002, 20000, 10960, 10960), sceneDescription.getTileRectangle(2));
        assertEquals(new Rectangle(10002, 10000, 10960, 10960), sceneDescription.getTileRectangle(3));
        assertEquals(new Rectangle(20004, 20000, 10960, 10960), sceneDescription.getTileRectangle(4));
        assertEquals(new Rectangle(20004, 10000, 10960, 10960), sceneDescription.getTileRectangle(5));
        assertEquals(new Rectangle(30000, 20000, 10960, 10960), sceneDescription.getTileRectangle(6));
        assertEquals(new Rectangle(30000, 10000, 10960, 10960), sceneDescription.getTileRectangle(7));
        assertEquals(new Rectangle(0, 0, 10960, 10960), sceneDescription.getTileRectangle(8));
        assertEquals(new Rectangle(10002, 0, 10960, 10960), sceneDescription.getTileRectangle(9));
        assertEquals(new Rectangle(20004, 0, 10960, 10960), sceneDescription.getTileRectangle(10));

        assertEquals(4, sceneDescription.getTileGridWidth());
        assertEquals(3, sceneDescription.getTileGridHeight());
    }

    private void testSpectralInformation(L1cMetadata.SpectralInformation bi, int bandId, String bandName, int res, double wl) {
        assertEquals(bandId, bi.bandId);
        assertEquals(bandName, bi.physicalBand);
        assertEquals(res, bi.resolution);
        assertEquals(wl, bi.wavelenghtCentral, 1e-10);
    }

    private void dumpNans(List<L1cMetadata.Tile> tileList) {
        for (L1cMetadata.Tile tile : tileList) {
            String horizontalCsCode = tile.horizontalCsCode;
            System.out.println("horizontalCsCode = " + horizontalCsCode);

            for (int y = 0; y < 23; y++) {
                for (int x = 0; x < 23; x++) {
                    L1cMetadata.AnglesGrid[] grids = tile.viewingIncidenceAnglesGrids;
                    int numAziNans = 0;
                    int numZenNans = 0;
                    for (L1cMetadata.AnglesGrid grid : grids) {
                        if (Float.isNaN(grid.azimuth[y][x])) {
                            numAziNans++;
                        }
                        if (Float.isNaN(grid.zenith[y][x])) {
                            numZenNans++;
                        }
                    }
                    System.out.printf("x=%d,y=%d: #azi=%d (%d), #zen=%d (%d)\n",
                                      x, y,
                                      grids.length - numAziNans, numAziNans,
                                      grids.length - numZenNans, numZenNans);
                }

            }
        }
    }
}
