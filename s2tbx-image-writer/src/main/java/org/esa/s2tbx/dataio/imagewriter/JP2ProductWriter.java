package org.esa.s2tbx.dataio.imagewriter;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.io.FileUtils;
import javax.imageio.IIOImage;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rdumitrascu on 12/9/2016.
 */
public class JP2ProductWriter extends AbstractProductWriter {

    private File outputFile;
    private JP2ImageWriter imageWriter;
    private boolean isWriten;

    /**
     * Constructs a <code>ProductWriter</code>. Since no output destination is set, the <code>setOutput</code>
     * method must be called before data can be written.
     *
     * @param writerPlugIn the plug-in which created this writer, must not be <code>null</code>
     * @throws IllegalArgumentException
     * @see #writeProductNodes
     */
    public JP2ProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        this.outputFile = null;

        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }
        this.outputFile = FileUtils.ensureExtension(file, JP2FormatConstants._fileExtentions[0]);
        deleteOutput();

        ensureNamingConvention();

        setJP2ImageWriter(this.outputFile);
    }

    /**
     * Creates an JP2ImageWriter and sets its output
     *
     * @param outputFile the output file for the product
     */
    private void setJP2ImageWriter(File outputFile) {
        this.imageWriter = new JP2ImageWriter();
        this.imageWriter.setOutput(outputFile);
    }

    private void ensureNamingConvention() {
        if (this.outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(outputFile)+JP2FormatConstants._fileExtentions[0]);
        }
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        if(this.isWriten){
            return;
        }
        final Product sourceProduct = sourceBand.getProduct();
        final int nodeCount = sourceProduct.getNumBands();
        final ArrayList<Band> bandsToWrite = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            final Band band = sourceProduct.getBandAt(i);
            if (shouldWrite(band)) {
                bandsToWrite.add(band);
            }
        }

        int maxSourceDataType = getMaxElemSizeBandDataType(sourceProduct.getBands());
        final int targetDataType =  ImageManager.getDataBufferType(maxSourceDataType);
        RenderedImage writeImage;
        if (bandsToWrite.size() > 1) {
            final ParameterBlock parameterBlock = new ParameterBlock();
            for (int i = 0; i < bandsToWrite.size(); i++) {
                final Band subsetBand = bandsToWrite.get(i);
                final RenderedImage sourceImage =  getImageWithTargetDataType(targetDataType, subsetBand);
                parameterBlock.setSource(sourceImage, i);
            }
            writeImage = JAI.create("bandmerge", parameterBlock, null);
        } else {
            writeImage =  getImageWithTargetDataType(targetDataType, bandsToWrite.get(0));
        }
        IIOImage outputImage = new IIOImage(writeImage, null, null);
        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();
        if(geoCoding!=null) {
            JP2Metadata metadata = new JP2Metadata(null, this.imageWriter);
            metadata.createJP2Metadata(geoCoding,width, height);
            this.imageWriter.write(metadata, outputImage, null);
        }else{
            this.imageWriter.write(null, outputImage, null);
        }

        this.isWriten = true;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void deleteOutput() throws IOException {
        if (this.outputFile != null && this.outputFile.isFile()) {
            this.outputFile.delete();
        }
    }

    static int getMaxElemSizeBandDataType(final Band[] bands) {
        int maxSignedIntType = -1;
        int maxUnsignedIntType = -1;
        int maxFloatType = -1;
        for (Band band : bands) {
            int dt = band.getDataType();
            if (ProductData.isIntType(dt)) {
                if (ProductData.isUIntType(dt)) {
                    maxUnsignedIntType = Math.max(maxUnsignedIntType, dt);
                } else {
                    maxSignedIntType = Math.max(maxSignedIntType, dt);
                }
            }
            if (ProductData.isFloatingPointType(dt)) {
                maxFloatType = Math.max(maxFloatType, dt);
            }
        }

        if (maxFloatType != -1) {
            if (maxUnsignedIntType == ProductData.TYPE_UINT32 || maxSignedIntType == ProductData.TYPE_INT32) {
                return ProductData.TYPE_FLOAT64;
            } else {
                return maxFloatType;
            }
        }

        if (maxUnsignedIntType != -1) {
            if (maxSignedIntType == -1) {
                return maxUnsignedIntType;
            }
            if (ProductData.getElemSize(maxUnsignedIntType) >= ProductData.getElemSize(maxSignedIntType)) {
                int returnType = maxUnsignedIntType - 10 + 1;
                if (returnType > 12) {
                    return ProductData.TYPE_FLOAT64;
                } else {
                    return returnType;
                }
            }
        }

        if (maxSignedIntType != -1) {
            return maxSignedIntType;
        }

        return DataBuffer.TYPE_UNDEFINED;
    }

    private RenderedImage getImageWithTargetDataType(int targetDataType, Band subsetBand) {
        RenderedImage sourceImage = subsetBand.getSourceImage();
        final int actualTargetBandDataType = sourceImage.getSampleModel().getDataType();
        if (actualTargetBandDataType != targetDataType) {
            sourceImage = FormatDescriptor.create(sourceImage, targetDataType, null);
        }
        return sourceImage;
    }
}
