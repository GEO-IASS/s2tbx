/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s2tbx.dataio.s2.l2a;

import https.psd_12_sentinel2_eo_esa_int.psd.s2_pdi_level_2a_tile_metadata.Level2A_Tile;
import https.psd_12_sentinel2_eo_esa_int.psd.user_product_level_2a.Level2A_User_Product;
import org.esa.s2tbx.dataio.Utils;
import org.esa.s2tbx.dataio.metadata.PlainXmlMetadata;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2GranuleDirFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleDirFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleMetadataFilename;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.SystemUtils;
import org.jdom.JDOMException;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Sentinel-2 MSI L1C XML metadata header file.
 * <p>
 * Note: No data interpretation is done in this class, it is intended to serve the pure metadata content only.
 *
 * @author Norman Fomferra
 */
public class L2aMetadata extends S2Metadata {

    //private static final String PSD_STRING = "12";
    private static final int DEFAULT_ANGLES_RESOLUTION = 5000;

    protected Logger logger = SystemUtils.LOG;

    /*public static L2aMetadata parseHeader(File file, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution) throws JDOMException, IOException, JAXBException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return new L2aMetadata(stream, file, file.getParent(), granuleName, config, epsg, productResolution);
        }
    }*/
    public static L2aMetadata parseHeader(File file, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution) throws JDOMException, IOException{
        return new L2aMetadata(file.toPath(), granuleName, config, epsg, productResolution);
    }


    private L2aMetadata(Path path, String granuleName, S2Config s2config, String epsg, S2SpatialResolution productResolution) throws IOException{
        super(s2config);
        resetTileList();
        boolean isGranuleMetadata = S2OrthoGranuleMetadataFilename.isGranuleFilename(path.getFileName().toString());

        if(!isGranuleMetadata) {
            initProduct(path, granuleName, epsg, productResolution);
        } else {
            initTile(path, epsg, productResolution);
        }
        //TODO
    }
   /* private L2aMetadata(InputStream stream, File file, String parent, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution) throws JDOMException, JAXBException, FileNotFoundException {
        super(config, L2aMetadataProc.getJaxbContext(), PSD_STRING);

        try {
            Object userProductOrTile = updateAndUnmarshal(stream);
            resetTileList();

            if (userProductOrTile instanceof Level2A_User_Product) {
                initProduct(file, parent, granuleName, userProductOrTile, epsg, productResolution);
            } else {
                initTile(file, userProductOrTile, epsg, productResolution);
            }
        } catch (JAXBException | JDOMException | IOException e) {
            logger.severe(Utils.getStackTrace(e));
        }
    }*/

    private void initProduct(Path path, String granuleName, String epsg, S2SpatialResolution productResolution) throws IOException {
        IL2aProductMetadata metadataProduct = L2aMetadataFactory.createL2aProductMetadata(path);
        setProductCharacteristics(metadataProduct.getProductOrganization(productResolution));

        Collection<String> tileNames;

        if (granuleName == null) {
            tileNames = metadataProduct.getTiles();
        } else {
            tileNames = Collections.singletonList(granuleName);
        }

        S2DatastripFilename stripName = metadataProduct.getDatastrip();
        S2DatastripDirFilename dirStripName = metadataProduct.getDatastripDir();
        Path datastripPath = path.resolveSibling("DATASTRIP").resolve(dirStripName.name).resolve(stripName.name);
        IL2aDatastripMetadata metadataDatastrip = L2aMetadataFactory.createL2aDatastripMetadata(datastripPath);

        getMetadataElements().add(metadataProduct.getMetadataElement());
        getMetadataElements().add(metadataDatastrip.getMetadataElement());

        //Check if the tiles found in metadata exist and add them to fullTileNamesList
        ArrayList<Path> granuleMetadataPathList = new ArrayList<>();
        for (String tileName : tileNames) {
            S2OrthoGranuleDirFilename aGranuleDir = S2OrthoGranuleDirFilename.create(tileName);

            if (aGranuleDir != null) {
                String theName = aGranuleDir.getMetadataFilename().name;

                Path nestedGranuleMetadata = path.resolveSibling("GRANULE").resolve(tileName).resolve(theName);
                if (Files.exists(nestedGranuleMetadata)) {
                    granuleMetadataPathList.add(nestedGranuleMetadata);
                } else {
                    String errorMessage = "Corrupted product: the file for the granule " + tileName + " is missing";
                    logger.log(Level.WARNING, errorMessage);
                }
            }
        }

        //Init Tiles
        for (Path granuleMetadataPath : granuleMetadataPathList) {
            initTile(granuleMetadataPath, epsg, productResolution);
        }
    }


    /*private void initProduct(File file, String parent, String granuleName, Object casted, String epsg, S2SpatialResolution productResolution) throws IOException, JAXBException, JDOMException {
        Level2A_User_Product product = (Level2A_User_Product) casted;
        setProductCharacteristics(L2aMetadataProc.getProductOrganization(product, productResolution));

        Collection<String> tileNames;

        if (granuleName == null) {
            tileNames = L2aMetadataProc.getTiles(product);
        } else {
            tileNames = Collections.singletonList(granuleName);
        }


        S2DatastripFilename stripName = L2aMetadataProc.getDatastrip(product);
        S2DatastripDirFilename dirStripName = L2aMetadataProc.getDatastripDir(product);

        File dataStripMetadata = new File(parent, "DATASTRIP" + File.separator + dirStripName.name + File.separator + stripName.name);

        Set<String> exclusions = new HashSet<String>() {{
            add("Viewing_Incidence_Angles_Grids");
            add("Sun_Angles_Grid");
        }};
        MetadataElement userProduct = PlainXmlMetadata.parse(file.toPath(), exclusions);
        //MetadataElement userProduct = parseAll(new SAXBuilder().build(file).getRootElement());
        //MetadataElement dataStrip = parseAll(new SAXBuilder().build(dataStripMetadata).getRootElement());
        MetadataElement dataStrip = PlainXmlMetadata.parse(dataStripMetadata.toPath(), null);
        getMetadataElements().add(userProduct);
        getMetadataElements().add(dataStrip);


        //Check if the tiles found in metadata exist and add them to fullTileNamesList
        List<File> fullTileNamesList = new ArrayList<>();
        for (String tileName : tileNames) {
            S2GranuleDirFilename aGranuleDir = S2OrthoGranuleDirFilename.create(tileName);
            if (aGranuleDir != null) {
                String theName = aGranuleDir.getMetadataFilename().name;

                File nestedGranuleMetadata = new File(parent, "GRANULE" + File.separator + tileName + File.separator + theName);
                if (nestedGranuleMetadata.exists()) {
                    fullTileNamesList.add(nestedGranuleMetadata);
                } else {
                    String errorMessage = "Corrupted product: the file for the granule " + tileName + " is missing";
                    logger.log(Level.WARNING, errorMessage);
                }
            }
        }

        //Init Tiles
        for (File aGranuleMetadataFile : fullTileNamesList) {
            FileInputStream granuleStream = new FileInputStream(aGranuleMetadataFile);
            Object granule = updateAndUnmarshal(granuleStream);
            granuleStream.close();
            initTile(aGranuleMetadataFile, granule, epsg, productResolution);
        }
    }*/


    private void initTile(Path path, String epsg, S2SpatialResolution resolution) throws IOException {

        IL2aGranuleMetadata granuleMetadata = L2aMetadataFactory.createL2aGranuleMetadata(path);

        if(getProductCharacteristics() == null) {
            setProductCharacteristics(granuleMetadata.getTileProductOrganization(resolution));
        }

        Map<S2SpatialResolution, TileGeometry> geoms = granuleMetadata.getTileGeometries();

        Tile tile = new Tile(granuleMetadata.getTileID());
        tile.setHorizontalCsCode(granuleMetadata.getHORIZONTAL_CS_CODE());
        tile.setHorizontalCsName(granuleMetadata.getHORIZONTAL_CS_NAME());

        if (epsg != null && !tile.getHorizontalCsCode().equals(epsg)) {
            // skip tiles that are not in the desired UTM zone
            logger.info(String.format("Skipping tile %s because it has crs %s instead of requested %s", path.getFileName().toString(), tile.getHorizontalCsCode(), epsg));
            return;
        }

        tile.setTileGeometries(geoms);

        try {
            tile.setAnglesResolution(granuleMetadata.getAnglesResolution());
        } catch (Exception e) {
            logger.warning("Angles resolution cannot be obtained");
            tile.setAnglesResolution(DEFAULT_ANGLES_RESOLUTION);
        }

        tile.setSunAnglesGrid(granuleMetadata.getSunGrid());
        if(!getProductCharacteristics().getMetaDataLevel().equals("Brief")) {
            tile.setViewingIncidenceAnglesGrids(granuleMetadata.getViewingAnglesGrid());
        }

        //granuleMetadata.getMasks(path);
        tile.setMaskFilenames(granuleMetadata.getMasks(path));

        addTileToList(tile);

        //Search "Granules" metadata element. If it does not exist, it is created
        MetadataElement granulesMetaData = null;
        for(MetadataElement metadataElement : getMetadataElements()) {
            if(metadataElement.getName().equals("Granules")) {
                granulesMetaData = metadataElement;
                break;
            }
        }
        if (granulesMetaData == null) {
            granulesMetaData = new MetadataElement("Granules");
            getMetadataElements().add(granulesMetaData);
        }

        granuleMetadata.updateName(); //for including the tile id
        granulesMetaData.addElement(granuleMetadata.getSimplifiedMetadataElement());
    }


    /*private void initTile(File file, Object casted, String epsg, S2SpatialResolution resolution) throws IOException, JAXBException, JDOMException {

        Level2A_Tile aTile = (Level2A_Tile) casted;
        if(getProductCharacteristics() == null) {
            logger.warning("Warning: the default quantification values will be used because they cannot be found in metadata\n");
            setProductCharacteristics(L2aMetadataProc.getTileProductOrganization(aTile, resolution));
        }

        Map<S2SpatialResolution, TileGeometry> geoms = L2aMetadataProc.getTileGeometries(aTile);

        //Tile tile = new Tile(aTile.getGeneral_Info().getTILE_ID().getValue());
        Tile tile = new Tile(file.getParentFile().getName());
        tile.setHorizontalCsCode(aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_CODE());
        tile.setHorizontalCsName(aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_NAME());

        if (epsg != null && !tile.getHorizontalCsCode().equals(epsg)) {
            // skip tiles that are not in the desired UTM zone
            logger.info(String.format("Skipping tile %s because it has crs %s instead of requested %s", file.getName(), tile.getHorizontalCsCode(), epsg));
            return;
        }

        tile.setTileGeometries(geoms);

        try {
            tile.setAnglesResolution((int) aTile.getGeometric_Info().getTile_Angles().getSun_Angles_Grid().getAzimuth().getCOL_STEP().getValue());
        } catch (Exception e) {
            logger.warning("Angles resolution cannot be obtained");
            tile.setAnglesResolution(DEFAULT_ANGLES_RESOLUTION);
        }

        tile.setSunAnglesGrid(L2aMetadataProc.getSunGrid(aTile));
        tile.setViewingIncidenceAnglesGrids(L2aMetadataProc.getAnglesGrid(aTile));

        L2aMetadataProc.getMasks(aTile, file);
        tile.setMaskFilenames(L2aMetadataProc.getMasks(aTile, file));

        addTileToList(tile);

        //Search "Granules" metadata element. If it does not exist, it is created
        MetadataElement granulesMetaData = null;
        for(MetadataElement metadataElement : getMetadataElements()) {
            if(metadataElement.getName().equals("Granules")) {
                granulesMetaData = metadataElement;
                break;
            }
        }
        if (granulesMetaData == null) {
            granulesMetaData = new MetadataElement("Granules");
            getMetadataElements().add(granulesMetaData);
        }

        //MetadataElement aGranule = parseAll(new SAXBuilder().build(file).getRootElement());
        MetadataElement aGranule = PlainXmlMetadata.parse(file.toPath(), new HashSet<String>() {{
            add("Viewing_Incidence_Angles_Grids");
            add("Sun_Angles_Grid");
        }});

        //write the ID to improve UI
        MetadataElement generalInfo = aGranule.getElement("General_Info");
        if (generalInfo != null) {
            MetadataAttribute tileIdAttr = generalInfo.getAttribute("TILE_ID_2A");
            if (tileIdAttr != null) {
                String newName = tileIdAttr.getData().toString();
                if (newName.length() > 56)
                    aGranule.setName("Level-2A_Tile_" + newName.substring(50, 55));
            }
        }
        granulesMetaData.addElement(aGranule);
    }*/
}
