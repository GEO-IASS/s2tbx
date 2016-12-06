/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.radiometry;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.awt.*;
import java.util.Map;

@OperatorMetadata(
        alias = "Bi2Op",
        version="1.0",
        category = "Optical/Thematic Land Processing/Soil Radiometric Indices",
        description = "The Brightness index represents the average of the brightness of a satellite image.\n" +
                "This index is sensitive to the brightness of soils which is highly correlated with the humidity and the presence of salts in surface",
        authors = "Dragos Mihailescu",
        copyright = "Copyright (C) 2016 by CS ROMANIA")
public class Bi2Op extends BaseIndexOp{

    // constants
    public static final String BAND_NAME = "bi2";

    @Parameter(label = "Red factor", defaultValue = "1.0F", description = "The value of the RED source band is multiplied by this value.")
    private float redFactor;

    @Parameter(label = "Green factor", defaultValue = "1.0F", description = "The value of the GREEN source band is multiplied by this value.")
    private float greenFactor;

    @Parameter(label = "NIR factor", defaultValue = "1.0F", description = "The value of the NIR source band is multiplied by this value.")
    private float nirFactor;

    @Parameter(label = "Red source band",
            description = "The red band for the BI2 computation. If not provided, the " +
                    "operator will try to find the best fitting band.",
            rasterDataNodeType = Band.class)
    private String redSourceBand;

    @Parameter(label = "Green source band",
            description = "The green band for the BI2 computation. If not provided, the " +
                    "operator will try to find the best fitting band.",
            rasterDataNodeType = Band.class)
    private String greenSourceBand;

    @Parameter(label = "NIR source band",
            description = "The near-infrared band for the BI2 computation. If not provided," +
                    " the operator will try to find the best fitting band.",
            rasterDataNodeType = Band.class)
    private String nirSourceBand;

    @Override
    public String getBandName() {
        return BAND_NAME;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing BI2", rectangle.height);
        try {

            Tile redTile = getSourceTile(getSourceProduct().getBand(redSourceBand), rectangle);
            Tile greenTile = getSourceTile(getSourceProduct().getBand(greenSourceBand), rectangle);
            Tile nirTile = getSourceTile(getSourceProduct().getBand(nirSourceBand), rectangle);

            Tile bi2 = targetTiles.get(targetProduct.getBand(BAND_NAME));
            Tile bi2Flags = targetTiles.get(targetProduct.getBand(FLAGS_BAND_NAME));

            float bi2Value;
            int bi2FlagsValue;

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {

                    final float red = redFactor * redTile.getSampleFloat(x, y);
                    final float green = greenFactor * greenTile.getSampleFloat(x, y);
                    final float nir = nirFactor * nirTile.getSampleFloat(x, y);

                    bi2Value = (float) Math.sqrt( ( (red * red) + (green * green) + (nir * nir) ) / 3.0f );

                    bi2FlagsValue = 0;
                    if (Float.isNaN(bi2Value) || Float.isInfinite(bi2Value)) {
                        bi2FlagsValue |= ARITHMETIC_FLAG_VALUE;
                        bi2Value = 0.0f;
                    }
                    if (bi2Value < 0.0f) {
                        bi2FlagsValue |= LOW_FLAG_VALUE;
                    }
                    if (bi2Value > 1.0f) {
                        bi2FlagsValue |= HIGH_FLAG_VALUE;
                    }
                    bi2.setSample(x, y, bi2Value);
                    bi2Flags.setSample(x, y, bi2FlagsValue);
                }
                checkForCancellation();
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    protected void loadSourceBands(Product product) throws OperatorException {
        if (redSourceBand == null) {
            redSourceBand = findBand(600, 650, product); /* Band Centre = 550.7 nm, Band width 88.6 nm*/
            getLogger().info("Using band '" + redSourceBand + "' as RED input band.");
        }
        if (greenSourceBand == null) {
            greenSourceBand = findBand(495, 570, product); /* Band Centre = 664.8 nm, Band width 65.8 nm*/
            getLogger().info("Using band '" + greenSourceBand + "' as GREEN input band.");
        }
        if (nirSourceBand == null) {
            nirSourceBand = findBand(800, 900, product);
            getLogger().info("Using band '" + nirSourceBand + "' as NIR input band.");
        }

        if (redSourceBand == null) {
            throw new OperatorException("Unable to find band that could be used as red input band. Please specify band.");
        }
        if (greenSourceBand == null) {
            throw new OperatorException("Unable to find band that could be used as green input band. Please specify band.");
        }
        if (nirSourceBand == null) {
            throw new OperatorException("Unable to find band that could be used as nir input band. Please specify band.");
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Bi2Op.class);
        }

    }

}
