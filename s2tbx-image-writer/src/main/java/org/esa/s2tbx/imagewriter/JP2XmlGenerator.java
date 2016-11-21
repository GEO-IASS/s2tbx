package org.esa.s2tbx.imagewriter;

//import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.xml.stream.*;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Class that prints the JP2 XML header
 * Created by rdumitrascu on 11/15/2016.
 */
public class JP2XmlGenerator {
    private  JP2XmlGenerator(){
    }
    /**
     *
     * @param fileOutputStream
     * @param img
     * @param metadata
     * @throws XMLStreamException
     */
    public static void xmlJP2WriteStream(FileOutputStream fileOutputStream, RenderedImage img, IIOMetadata metadata)throws XMLStreamException {
        int depth=0;
        StringWriter sw = new StringWriter();
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        XMLStreamWriter tmpWriter = outputFactory.createXMLStreamWriter(fileOutputStream);
        // IndentingXMLStreamWriter xmlStreamWriter = new IndentingXMLStreamWriter(tmpWriter);
        tmpWriter.writeStartDocument("UTF-8", "1.0");
        tmpWriter.writeCharacters("\n");
        tmpWriter.writeStartElement("gml:FeatureCollection");
        tmpWriter.writeNamespace("gml","http://www.opengis.net/gml");
        tmpWriter.writeNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");
        tmpWriter.writeAttribute("http://www.w3.org/2001/XMLSchema-instance","schemaLocation","http://www.opengeospatial.net/gml http://schemas.opengis.net/gml/3.1.1/profiles/gmlJP2Profile/1.0.0/gmlJP2Profile.xsd");
        tmpWriter.writeCharacters("\n");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:boundedBy");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:Null");
        tmpWriter.writeCharacters("withheld");//To DO
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        tmpWriter.writeCharacters("\n");
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:featureMember");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:FeatureCollection");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:featureMember");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:RectifiedGridCoverage");
        tmpWriter.writeAttribute("dimension","2"); //TO DO
        tmpWriter.writeAttribute("gml:id","RGC0001"); // TO DO
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:rectifiedGridDomain");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:RectifiedGrid");
        tmpWriter.writeAttribute("dimension","2"); //TO DO
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:limits");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:GridEnvelope");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:low");
        tmpWriter.writeCharacters("1 1");
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:high");
        tmpWriter.writeCharacters(img.getWidth()+" "+img.getHeight());
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:axisName");
        tmpWriter.writeCharacters("x");
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:axisName");
        tmpWriter.writeCharacters("y");
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:origin");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:Point");
        tmpWriter.writeAttribute("gml:id","P0001"); //TO DO
        tmpWriter.writeAttribute("srsName","urn:ogc:def:crs:EPSG::32629"); //TO DO
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:pos");
        tmpWriter.writeCharacters("500010 3800010"); //TO DO
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:offsetVector");
        tmpWriter.writeAttribute("srsName","urn:ogc:def:crs:EPSG::32629"); //TO DO
        tmpWriter.writeCharacters("60 0"); //TO DO
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:offsetVector");
        tmpWriter.writeAttribute("srsName","urn:ogc:def:crs:EPSG::32629"); //TO DO
        tmpWriter.writeCharacters("0 -60"); //TO DO
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:rangeSet");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:File");
        depth++;indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:fileName");
        tmpWriter.writeCharacters("gmljp2://codestream/0"); //TO DO
        tmpWriter.writeEndElement();
        indent(depth,tmpWriter );
        tmpWriter.writeStartElement("gml:fileStructure");
        tmpWriter.writeCharacters("Record Interleaved"); //TO DO
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        depth--;indent(depth,tmpWriter );
        tmpWriter.writeEndElement();
        tmpWriter.writeCharacters("\n\n");
        tmpWriter.writeEndDocument();
        tmpWriter.writeCharacters("\n");
        tmpWriter.flush();
        tmpWriter.close();
    }
    private static void indent(int depth,XMLStreamWriter tmpXMLWriter) throws XMLStreamException {
        tmpXMLWriter.writeCharacters("\n");
        for(int index=0; index<depth;index++){
            tmpXMLWriter.writeCharacters("  ");
        }
    }
}
