package org.esa.s2tbx.dataio.s2.filepatterns;

/**
 * Created by obarrile on 19/10/2016.
 */
public enum S2NamingItems {

    MISSION_ID(0,"{{MISSION_ID}}","(S2A|S2B|S2_)"),
    FILE_CLASS(1,"{{FILE_CLASS}}","([A-Z|0-9]{4})"),
    FILE_TYPE_PRODUCT(2,"{{FILE_TYPE_PRODUCT}}","(PRD_MSIL1B|PRD_MSIL1C|PRD_MSITCI)"),
    SITE_CENTRE_PRODUCT(3,"{{SITE_CENTRE_PRODUCT}}","(PDMC)"),
    PRODUCT_DISCRIMINATOR(4,"{{PRODUCT_DISCRIMINATOR}}","([0-9]{8}T[0-9]{6})"),
    RELATIVE_ORBIT(5,"{{RELATIVE_ORBIT}}","([0-9]{3})"),
    START_TIME(6,"{{START_TIME}}","([0-9]{8}T[0-9]{6})"),
    STOP_TIME(7,"{{STOP_TIME}}","([0-9]{8}T[0-9]{6})"),
    FORMAT(8,"{{FORMAT}}","(SAFE|DIMAP)"),
    FILE_TYPE_PRODUCT_XML(9,"{{FILE_TYPE_PRODUCT_XML}}","(MTD_SAFL1B|MTD_SAFL1C|MTD_DMPL1B|MTD_DMPL1C)"),
    FILE_TYPE_DATASTRIP(2,"{{FILE_TYPE_DATASTRIP}}","(MSI_L1B_DS|MSI_L1C_DS|MSI_L2A_DS)"),
    SITE_CENTRE(0,"{{SITE_CENTRE}}","([A-Z|0-9|_]{4})"),
    CREATION_DATE(0,"{{CREATION_DATE}}","([0-9]{8}T[0-9]{6})"),
    PRODUCTION_BASELINE(0,"{{PRODUCTION_BASELINE}}","([0-9]{2}\\.[0-9]{2})"),
    FILE_TYPE_DATASTRIP_XML(2,"{{FILE_TYPE_DATASTRIP_XML}}","(MTD_L1B_DS|MTD_L1C_DS|MTD_L2A_DS)"),
    FILE_TYPE_GRANULE(2,"{{FILE_TYPE_GRANULE}}","(MSI_L1B_GR|MSI_L1C_TL|MSI_L2A_TL)"),
    ABSOLUTE_ORBIT(2,"{{ABSOLUTE_ORBIT}}","([0-9]{6})"),
    TILE_NUMBER(2,"{{TILE_NUMBER}}","([A-Z|0-9]{5})"),
    FILE_TYPE_GRANULE_XML(2,"{{FILE_TYPE_GRANULE_XML}}","(MTD_L1B_GR|MTD_L1C_TL|MTD_L2A_TL)");

    public final int id;
    public final String template;
    public final String REGEX;

    S2NamingItems(int id, String template, String REGEX) {
        this.id = id;
        this.template = template;
        this.REGEX = REGEX;
    }

}
