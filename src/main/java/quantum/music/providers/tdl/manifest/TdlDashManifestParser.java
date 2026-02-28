package quantum.music.providers.tdl.manifest;

import quantum.music.domain.tdl.MediaInfo;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.quarkus.arc.ComponentsProvider.LOG;

public class TdlDashManifestParser {

    private static final String ENCRYPTION_NONE = "NONE";

    public MediaInfo parse(String manifestB64) {
        String manifestXml = new String(Base64.getDecoder().decode(manifestB64), StandardCharsets.UTF_8);
        LOG.debugf("Parsed DASH manifest XML: %s", manifestXml);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8)));

            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return "mpd".equals(prefix) ? "urn:mpeg:dash:schema:mpd:2011" : XMLConstants.NULL_NS_URI;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                    return java.util.Collections.emptyIterator();
                }
            });

            Node representationNode = (Node) xPath.evaluate("//mpd:Representation", document, XPathConstants.NODE);
            if (representationNode == null || representationNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new IllegalStateException("No Representation found in DASH manifest");
            }
            Element representation = (Element) representationNode;

            Node segmentTemplateNode = (Node) xPath.evaluate(".//mpd:SegmentTemplate", representation, XPathConstants.NODE);
            if (segmentTemplateNode == null || segmentTemplateNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new IllegalStateException("No SegmentTemplate found in DASH manifest");
            }
            Element segmentTemplate = (Element) segmentTemplateNode;

            String mediaTemplate = segmentTemplate.getAttribute("media");
            if (mediaTemplate.isBlank()) {
                throw new IllegalStateException("No media template found in DASH manifest");
            }

            String initializationTemplate = segmentTemplate.getAttribute("initialization");

            String startNumberRaw = segmentTemplate.getAttribute("startNumber");
            int startNumber = 1;
            if (!startNumberRaw.isBlank()) {
                startNumber = Integer.parseInt(startNumberRaw);
            }

            NodeList timelineNodes = segmentTemplate.getElementsByTagNameNS("urn:mpeg:dash:schema:mpd:2011", "SegmentTimeline");
            if (timelineNodes.getLength() == 0) {
                throw new IllegalStateException("No SegmentTimeline found in DASH manifest");
            }

            NodeList segmentNodes = ((Element) timelineNodes.item(0)).getElementsByTagNameNS("urn:mpeg:dash:schema:mpd:2011", "S");
            if (segmentNodes.getLength() == 0) {
                throw new IllegalStateException("No S elements found in DASH manifest SegmentTimeline");
            }

            List<String> segmentUrls = new ArrayList<>();
            if (!initializationTemplate.isBlank()) {
                segmentUrls.add(expandTemplate(initializationTemplate, representation, null));
            }
            int segmentNumber = startNumber;

            for (int i = 0; i < segmentNodes.getLength(); i++) {
                Node segmentNode = segmentNodes.item(i);
                if (segmentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element segment = (Element) segmentNode;
                String repeatRaw = segment.getAttribute("r");
                int repeat = 0;
                if (!repeatRaw.isBlank()) {
                    repeat = Integer.parseInt(repeatRaw);
                }

                int numSegments = repeat >= 0 ? repeat + 1 : 1;
                for (int j = 0; j < numSegments; j++) {
                    String url = expandTemplate(mediaTemplate, representation, segmentNumber);
                    segmentUrls.add(url);
                    segmentNumber += 1;
                }
            }
            return new MediaInfo(segmentUrls.toArray(new String[0]), ENCRYPTION_NONE, null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DASH XML manifest", e);
        }
    }

    private String expandTemplate(String template, Element representation, Integer number) {
        String expanded = template;
        String representationId = representation.getAttribute("id");
        String bandwidth = representation.getAttribute("bandwidth");
        if (!representationId.isBlank()) {
            expanded = expanded.replace("$RepresentationID$", representationId);
        }
        if (!bandwidth.isBlank()) {
            expanded = expanded.replace("$Bandwidth$", bandwidth);
        }
        if (number != null) {
            expanded = expanded.replace("$Number$", Integer.toString(number));
        }
        return expanded;
    }
}
