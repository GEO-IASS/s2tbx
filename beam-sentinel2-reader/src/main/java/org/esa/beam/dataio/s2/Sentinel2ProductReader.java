package org.esa.beam.dataio.s2;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.geometry.Envelope2D;
import org.jdom.JDOMException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.*;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static org.esa.beam.dataio.s2.Config.*;

// todo - register reasonable RGB profile(s)
// todo - add to product the virtual radiance bands from conversion factor found in header
// todo - add to product the virtual reflectance bands from solar flux found in header
// todo - add to product a virtual NDVI band (for demo)
// todo - set a band's validMaskExpr or no-data value
// todo - read product's viewing geometry tie-point grids from header
// todo - set product metadata
// todo - set band's ImageInfo from min,max,histogram found in header

public class Sentinel2ProductReader extends AbstractProductReader {

    private File cacheDir;

    static class BandInfo {
        final Map<String, File> tileIdToFileMap;
        final int bandIndex;
        final S2WavebandInfo wavebandInfo;
        final L1cTileLayout imageLayout;

        BandInfo(Map<String, File> tileIdToFileMap, int bandIndex, S2WavebandInfo wavebandInfo, L1cTileLayout imageLayout) {
            this.tileIdToFileMap = Collections.unmodifiableMap(tileIdToFileMap);
            this.bandIndex = bandIndex;
            this.wavebandInfo = wavebandInfo;
            this.imageLayout = imageLayout;
        }
    }


    Sentinel2ProductReader(Sentinel2ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        // Should never not come here, since we have an OpImage that reads data
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = new File(getInput().toString());
        if (!inputFile.exists()) {
            throw new FileNotFoundException(inputFile.getPath());
        }
        if (S2MtdFilename.isMetadataFilename(inputFile.getName())) {
            return getL1cMosaicProduct(inputFile);
        } else if (S2ImgFilename.isImageFilename(inputFile.getName())) {
            return getL1cTileProduct(inputFile);
        } else {
            throw new IOException("Unhandled file type.");
        }
    }

    private Product getL1cTileProduct(File imageFile) throws IOException {

        S2ImgFilename imgFilename = S2ImgFilename.create(imageFile.getName());
        if (imgFilename == null) {
            throw new IOException();
        }

        File productDir = getProductDir(imageFile);
        initCacheDir(productDir);

        // Try to find metadata header

        Header metadataHeader = null;
        Map<Integer, BandInfo> bandInfoMap = new HashMap<Integer, BandInfo>();
        File[] metadataFiles = productDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return S2MtdFilename.isMetadataFilename(name);
            }
        });
        if (metadataFiles != null && metadataFiles.length > 0) {
            File metadataFile = metadataFiles[0];
            try {
                metadataHeader = Header.parseHeader(metadataFile);
            } catch (JDOMException e) {
                BeamLogManager.getSystemLogger().warning("Failed to parse metadata file: " + metadataFile);
            }
        } else {
            BeamLogManager.getSystemLogger().warning("No metadata file found");
        }

        // Try to find other band images

        File[] files = productDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return S2ImgFilename.isImageFilename(name);
            }
        });
        if (files != null) {
            for (File file : files) {
                int bandIndex = imgFilename.getBand(file.getName());
                if (metadataHeader != null) {
                    Header.SpectralInformation[] bandInformations = metadataHeader.getProductCharacteristics().bandInformations;
                    if (bandIndex >= 0 && bandIndex < bandInformations.length) {
                        BandInfo bandInfo = createBandInfoFromHeaderInfo(bandInformations[bandIndex],
                                                                         metadataHeader.getResampleData(),
                                                                         createFileMap(imgFilename.tileId, file));
                        bandInfoMap.put(bandIndex, bandInfo);
                    } else {
                        // todo - report problem
                    }
                } else {
                    if (bandIndex >= 0 && bandIndex < S2_WAVEBAND_INFOS.length) {
                        S2WavebandInfo wavebandInfo = S2_WAVEBAND_INFOS[bandIndex];
                        BandInfo bandInfo = createBandInfoFromDefaults(bandIndex, wavebandInfo,
                                                                       imgFilename.tileId,
                                                                       file);
                        bandInfoMap.put(bandIndex, bandInfo);
                    } else {
                        // todo - report problem
                    }
                }
            }
        }

        ArrayList<Integer> bandIndexes = new ArrayList<Integer>(bandInfoMap.keySet());
        Collections.sort(bandIndexes);

        if (bandIndexes.isEmpty()) {
            throw new IOException("No valid bands found.");
        }

        String prodType = "S2_MSI_" + imgFilename.procLevel;
        Product product = new Product(String.format("%s_%s_%s", prodType, imgFilename.orbitNo, imgFilename.tileId),
                                      prodType,
                                      L1C_TILE_LAYOUTS[SpatialResolution.R10M.id].width,
                                      L1C_TILE_LAYOUTS[SpatialResolution.R10M.id].height);

        product.setPreferredTileSize(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE);
        product.setNumResolutionsMax(L1C_TILE_LAYOUTS[0].numResolutions);
        setStartStopTime(product, imgFilename.start, imgFilename.stop);

        if (metadataHeader != null) {
            SceneDescription sceneDescription = SceneDescription.create(metadataHeader);
            int tileIndex = sceneDescription.getTileIndex(imgFilename.tileId);
            Envelope2D tileEnvelope = sceneDescription.getTileEnvelope(tileIndex);
            setGeoCoding(product, tileEnvelope);
        }

        for (Integer bandIndex : bandIndexes) {
            BandInfo bandInfo = bandInfoMap.get(bandIndex);
            Band band = addBand(product, bandInfo);
            band.setSourceImage(new DefaultMultiLevelImage(new L1cTileMultiLevelSource(bandInfo)));
        }

        // Test - add TOA reflectance bands
        for (Integer bandIndex : bandIndexes) {
            BandInfo bandInfo = bandInfoMap.get(bandIndex);
            Band band = product.addBand( bandInfo.wavebandInfo. bandName.replace("B", "reflec_"),
                                         bandInfo.wavebandInfo.bandName
                                                 + " / " + bandInfo.wavebandInfo.quantificationValue
                                                 + " / " + bandInfo.wavebandInfo.solarIrradiance);
            band.setSpectralBandIndex(bandIndex);
            band.setSpectralWavelength((float) bandInfo.wavebandInfo.wavelength);
        }
        product.addBand("ndvi", "(reflec_4 - reflec_9) / (reflec_4 + reflec_9)");
        product.setAutoGrouping("reflec");

        return product;
    }

    private Product getL1cMosaicProduct(File metadataFile) throws IOException {
        Header metadataHeader;

        try {
            metadataHeader = Header.parseHeader(metadataFile);
        } catch (JDOMException e) {
            throw new IOException("Failed to parse metadata in " + metadataFile.getName());
        }

        S2MtdFilename mtdFilename = S2MtdFilename.create(metadataFile.getName());
        SceneDescription sceneDescription = SceneDescription.create(metadataHeader);

        File productDir = getProductDir(metadataFile);
        initCacheDir(productDir);

        Header.ProductCharacteristics productCharacteristics = metadataHeader.getProductCharacteristics();
        Header.ResampleData resampleData = metadataHeader.getResampleData();

        String prodType = "S2_MSI_" + productCharacteristics.processingLevel;
        Product product = new Product(FileUtils.getFilenameWithoutExtension(metadataFile).substring("MTD_".length()),
                                      prodType,
                                      sceneDescription.getSceneRectangle().width,
                                      sceneDescription.getSceneRectangle().height);

        product.setPreferredTileSize(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE);
        product.setNumResolutionsMax(L1C_TILE_LAYOUTS[0].numResolutions);
        setStartStopTime(product, mtdFilename.start, mtdFilename.stop);
        setGeoCoding(product, sceneDescription.getSceneEnvelope());

        List<Header.Tile> tileList = metadataHeader.getTileList();
        for (Header.SpectralInformation bandInformation : productCharacteristics.bandInformations) {
            int bandIndex = bandInformation.bandId;
            if (bandIndex >= 0 && bandIndex < productCharacteristics.bandInformations.length) {

                HashMap<String, File> tileFileMap = new HashMap<String, File>();
                for (Header.Tile tile : tileList) {
                    String imgFilename = mtdFilename.getImgFilename(bandIndex, tile.id);
                    File file = new File(productDir, imgFilename);
                    if (file.exists()) {
                        tileFileMap.put(tile.id, file);
                    } else {
                        System.out.printf("Warning: missing file %s\n", file);
                    }
                }

                if (!tileFileMap.isEmpty()) {
                    BandInfo bandInfo = createBandInfoFromHeaderInfo(bandInformation, resampleData, tileFileMap);
                    Band band = addBand(product, bandInfo);
                    band.setSourceImage(new DefaultMultiLevelImage(new L1cMosaicMultiLevelSource(sceneDescription, bandInfo)));
                } else {
                    System.out.printf("Warning: no image files found for band " + bandInformation.physicalBand);
                }
            } else {
                System.out.printf("Warning: illegal band ID detected for band " + bandInformation.physicalBand);
            }
        }

        return product;
    }

    private static Map<String, File> createFileMap(String tileId, File imageFile) {
        Map<String, File> tileIdToFileMap = new HashMap<String, File>();
        tileIdToFileMap.put(tileId, imageFile);
        return tileIdToFileMap;
    }

    private void setStartStopTime(Product product, String start, String stop) {
        try {
            product.setStartTime(ProductData.UTC.parse(start, "yyyyMMddHHmmss"));
        } catch (ParseException e) {
            // todo - report problem
        }

        try {
            product.setEndTime(ProductData.UTC.parse(stop, "yyyyMMddHHmmss"));
        } catch (ParseException e) {
            // todo - report problem
        }
    }

    private BandInfo createBandInfoFromDefaults(int bandIndex, S2WavebandInfo wavebandInfo, String tileId, File imageFile) {
        return new BandInfo(createFileMap(tileId, imageFile),
                            bandIndex,
                            wavebandInfo,
                            L1C_TILE_LAYOUTS[wavebandInfo.resolution.id]);
    }

    private Band addBand(Product product, BandInfo bandInfo) {
        final Band band = product.addBand(bandInfo.wavebandInfo.bandName, SAMPLE_DATA_TYPE);
        band.setSpectralBandIndex(bandInfo.bandIndex);
        band.setSpectralWavelength((float) bandInfo.wavebandInfo.wavelength);
        band.setSpectralBandwidth((float) bandInfo.wavebandInfo.bandwidth);
        band.setSolarFlux((float) bandInfo.wavebandInfo.solarIrradiance);
        //band.setScalingFactor(bandInfo.wavebandInfo.scalingFactor);
        return band;
    }

    private BandInfo createBandInfoFromHeaderInfo(Header.SpectralInformation bandInformation, Header.ResampleData resampleData, Map<String, File> tileFileMap) {
        SpatialResolution spatialResolution = SpatialResolution.valueOfResolution(bandInformation.resolution);
        double solarIrradiance = resampleData.reflectanceConversion.solarIrradiances[bandInformation.bandId];
        return new BandInfo(tileFileMap,
                            bandInformation.bandId,
                            new S2WavebandInfo(bandInformation.bandId,
                                               bandInformation.physicalBand,
                                               spatialResolution, bandInformation.wavelenghtCentral,
                                               bandInformation.wavelenghtMax - bandInformation.wavelenghtMin,
                                               solarIrradiance,
                                               resampleData.quantificationValue),
                            L1C_TILE_LAYOUTS[spatialResolution.id]);
    }

    private void setGeoCoding(Product product, Envelope2D envelope) {
        try {
            product.setGeoCoding(new CrsGeoCoding(envelope.getCoordinateReferenceSystem(),
                                                  product.getSceneRasterWidth(),
                                                  product.getSceneRasterHeight(),
                                                  envelope.getMinX(),
                                                  envelope.getMaxY(),
                                                  SpatialResolution.R10M.resolution,
                                                  SpatialResolution.R10M.resolution,
                                                  0.0, 0.0));
        } catch (FactoryException e) {
            // todo - report problem
        } catch (TransformException e) {
            // todo - report problem
        }
    }

    static File getProductDir(File productFile) throws IOException {
        final File resolvedFile = productFile.getCanonicalFile();
        if (!resolvedFile.exists()) {
            throw new FileNotFoundException("File not found: " + productFile);
        }

        if (productFile.getParentFile() == null) {
            return new File(".").getCanonicalFile();
        }

        return productFile.getParentFile();
    }

    void initCacheDir(File productDir) throws IOException {
        cacheDir = new File(new File(SystemUtils.getApplicationDataDir(), "beam-sentinel2-reader/cache"),
                            productDir.getName());
        cacheDir.mkdirs();
        if (!cacheDir.exists() || !cacheDir.isDirectory() || !cacheDir.canWrite()) {
            throw new IOException("Can't access package cache directory");
        }
    }

    private class L1cTileMultiLevelSource extends AbstractMultiLevelSource {
        final BandInfo bandInfo;

        public L1cTileMultiLevelSource(BandInfo bandInfo) {
            super(new DefaultMultiLevelModel(bandInfo.imageLayout.numResolutions,
                                             new AffineTransform(),
                                             L1C_TILE_LAYOUTS[0].width,
                                             L1C_TILE_LAYOUTS[0].height));
            this.bandInfo = bandInfo;
        }

        @Override
        protected RenderedImage createImage(int level) {
            try {
                return L1cTileOpImage.create(bandInfo.tileIdToFileMap.values().iterator().next(),
                                             cacheDir,
                                             null,
                                             bandInfo.imageLayout,
                                             getModel(),
                                             bandInfo.wavebandInfo.resolution,
                                             level);
            } catch (IOException e) {
                return null;
            }
        }

    }

    private class L1cMosaicMultiLevelSource extends AbstractMultiLevelSource {
        private final SceneDescription sceneDescription;
        private final BandInfo bandInfo;

        public L1cMosaicMultiLevelSource(SceneDescription sceneDescription, BandInfo bandInfo) {
            super(new DefaultMultiLevelModel(bandInfo.imageLayout.numResolutions,
                                             new AffineTransform(),
                                             sceneDescription.getSceneRectangle().width,
                                             sceneDescription.getSceneRectangle().height));
            this.sceneDescription = sceneDescription;
            this.bandInfo = bandInfo;
        }

        @Override
        protected RenderedImage createImage(int level) {
            ArrayList<RenderedImage> tileImages = new ArrayList<RenderedImage>();

            Set<String> tileIds = bandInfo.tileIdToFileMap.keySet();
            for (String tileId : tileIds) {
                File imageFile = bandInfo.tileIdToFileMap.get(tileId);
                int tileIndex = sceneDescription.getTileIndex(tileId);
                Rectangle tileRectangle = sceneDescription.getTileRectangle(tileIndex);
                try {
                    PlanarImage opImage = L1cTileOpImage.create(imageFile,
                                                                cacheDir,
                                                                null, // tileRectangle.getLocation(),
                                                                bandInfo.imageLayout,
                                                                getModel(),
                                                                bandInfo.wavebandInfo.resolution,
                                                                level);

                    System.out.printf("%s %s: minX=%d, minY=%d, width=%d, height%d\n",
                                      bandInfo.wavebandInfo.bandName, tileId,
                                      opImage.getMinX(), opImage.getMinY(),
                                      opImage.getWidth(), opImage.getHeight());

                    /*
                    opImage = new MoveOriginOpImage(opImage,
                                                    tileRectangle.x >> level,
                                                    tileRectangle.y >> level,
                                                    null);
                                                    */

                    opImage = TranslateDescriptor.create(opImage,
                                                         (float) (tileRectangle.x >> level),
                                                         (float) (tileRectangle.y >> level),
                                                         Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);


                    //System.out.printf("opImage added for level %d at (%d,%d)%n", level, opImage.getMinX(), opImage.getMinY());
                    tileImages.add(opImage);
                } catch (IOException e) {
                    // todo - report problem
                }
            }

            if (tileImages.isEmpty()) {
                // todo - report problem
                return null;
            }

            ImageLayout imageLayout = new ImageLayout();
            imageLayout.setMinX(0);
            imageLayout.setMinY(0);
            imageLayout.setTileWidth(DEFAULT_TILE_SIZE);
            imageLayout.setTileHeight(DEFAULT_TILE_SIZE);
            imageLayout.setTileGridXOffset(0);
            imageLayout.setTileGridYOffset(0);
            RenderedOp mosaicOp = MosaicDescriptor.create(tileImages.toArray(new RenderedImage[tileImages.size()]),
                                                          MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                                                          null, null, new double[][]{{1.0}}, new double[]{FILL_CODE_MOSAIC_BG},
                                                          new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));

            //System.out.printf("mosaicOp created for level %d at (%d,%d)%n", level, mosaicOp.getMinX(), mosaicOp.getMinY());
            return mosaicOp;
        }
    }


}
