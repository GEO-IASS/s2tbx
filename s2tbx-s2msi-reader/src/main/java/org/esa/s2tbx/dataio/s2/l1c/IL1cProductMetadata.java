package org.esa.s2tbx.dataio.s2.l1c;

import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2NamingItems;
import org.esa.snap.core.datamodel.MetadataElement;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by obarrile on 29/09/2016.
 */
public interface IL1cProductMetadata {
    S2Metadata.ProductCharacteristics getProductOrganization();
    HashMap<S2NamingItems,String> getNamingItems();
    Collection<String> getTiles();
    S2DatastripFilename getDatastrip();
    S2DatastripDirFilename getDatastripDir();
    MetadataElement getMetadataElement();
}
