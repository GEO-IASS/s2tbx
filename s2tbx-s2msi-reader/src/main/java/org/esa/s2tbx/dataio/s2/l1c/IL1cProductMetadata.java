package org.esa.s2tbx.dataio.s2.l1c;

import https.psd_13_sentinel2_eo_esa_int.psd.user_product_level_1c.Level1C_User_Product;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.snap.core.datamodel.MetadataElement;

import java.util.Collection;

/**
 * Created by obarrile on 29/09/2016.
 */
public interface IL1cProductMetadata {
    S2Metadata.ProductCharacteristics getProductOrganization();
    Collection<String> getTiles();
    S2DatastripFilename getDatastrip();
    S2DatastripDirFilename getDatastripDir();
    MetadataElement getMetadataElement();

}
