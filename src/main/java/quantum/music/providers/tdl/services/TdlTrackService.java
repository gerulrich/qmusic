package quantum.music.providers.tdl.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Track;
import quantum.music.domain.tdl.MediaInfo;
import quantum.music.domain.providers.TrackDetail;
import quantum.music.service.TokenService;
import quantum.music.providers.tdl.stream.FileStreamer;
import quantum.music.providers.tdl.stream.crypto.DecryptingFileStreamer;
import quantum.music.providers.tdl.stream.http.BasicFileStreamer;
import quantum.music.providers.tdl.stream.http.MultiUrlFileStreamer;

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

@ApplicationScoped
public class TdlTrackService extends TldAbstractService  {

    private static final String MEDIA_TYPE_STREAM = "STREAM";
    public static final String NONE = "NONE";
    public static final String OLD_AES = "OLD_AES";

    @Inject
    @RestClient
    private ApiClient apiClient;

    @Inject
    TokenService tokenService;

    @Inject
    private Vertx vertx;

    private HttpClient httpClient;
    @ConfigProperty(name = "tdl.master.key")
    private String masterKey;


    @PostConstruct
    void init() {
        httpClient = vertx.createHttpClient();
    }

    public Uni<TrackDetail> getTrackById(String trackId) {
        LOG.debugf("Retrieving track details for: %s", trackId);
        return tokenService.withToken(() -> apiClient.track(parsedId(trackId))
                .onItem().transform(json -> {
                    JsonObject albumNode = json.getJsonObject("album");
                    JsonObject artistNode = json.getJsonObject("artist");

                    return new TrackDetail(
                            Album.builder()
                                .id(formatId(albumNode.getLong("id")))
                                .title(albumNode.getString("title"))
                                .artist(
                                    Artist.builder()
                                            .id(formatId(artistNode.getLong("id")))
                                            .name(artistNode.getString("name"))
                                        .build()
                                )
                                .cover(formatCoverUrl(albumNode.getString("cover")))
                                .build()
                            ,
                            new Track(
                        formatId(json.getLong("id")),
                        json.getString("title"),
                        json.getInteger("duration"),
                        json.getInteger("trackNumber"),
                        json.getInteger("volumeNumber"),
                        json.getString("audioQuality"),
                        json.getString("audioCodec"),
                        json.getJsonObject("mediaMetadata").getJsonArray("tags").stream().map(Object::toString).toList(),
                        json.getString("version"),
                        json.getString("copyright"),
                        formatResourceUrl("tracks", formatId(json.getLong("id")), "stream")
                        )
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting track: %s", trackId)));
    }

    public Uni<MediaInfo> content(String track, String codec, String quality, String presentation) {
        LOG.debugf("Retrieving media content for track: %s with codec: %s and quality: %s", track, codec, quality);
        String q = quality.replaceAll("HIRES", "HI_RES");
        return tokenService.withToken(() -> apiClient.media(parsedId(track), q, MEDIA_TYPE_STREAM, presentation)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving content for track: %s", parsedId(track));
                    String manifestMimeType = json.getString("manifestMimeType");
                    if (manifestMimeType.equals("application/vnd.tidal.bts")) {
                        String encodedManifest = json.getString("manifest");
                        JsonObject manifest = new JsonObject(new String(Base64.getDecoder().decode(encodedManifest)));

                        return new MediaInfo(
                                manifest.getJsonArray("urls").getString(0),
                                json.getString("audioQuality"),
                                manifest.getString("codecs"),
                                manifest.getString("encryptionType"),
                                manifest.getString("keyId"),
                                null
                        );
                    } else if (manifestMimeType.equals("application/dash+xml")) {
                        DashManifest dashManifest = parseDashManifest(json.getString("manifest"));
                        return new MediaInfo(
                                null,
                                json.getString("audioQuality"),
                                dashManifest.codecs,
                                dashManifest.encryptionType,
                                json.getString("keyId"),
                                dashManifest.urls
                        );
                    }
                    throw new WebApplicationException("Unsupported manifest type: " + manifestMimeType, 400);


                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting content for track: %s", track)));
    }

    class DashManifest {
        String[] urls;
        String codecs;
        String encryptionType;
        String mimeType;
    }

    private DashManifest parseDashManifest(String manifestB64) {
        String manifestXml = new String(Base64.getDecoder().decode(manifestB64), StandardCharsets.UTF_8);
        LOG.debugf("Parsed DASH manifest XML: %s", manifestXml);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8)));

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

            String codecs = representation.getAttribute("codecs");
            if (codecs == null || codecs.isBlank()) {
                codecs = "flac";
            }

            Node segmentTemplateNode = (Node) xPath.evaluate(".//mpd:SegmentTemplate", representation, XPathConstants.NODE);
            if (segmentTemplateNode == null || segmentTemplateNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new IllegalStateException("No SegmentTemplate found in DASH manifest");
            }
            Element segmentTemplate = (Element) segmentTemplateNode;

            String mediaTemplate = segmentTemplate.getAttribute("media");
            if (mediaTemplate == null || mediaTemplate.isBlank()) {
                throw new IllegalStateException("No media template found in DASH manifest");
            }

            String initializationTemplate = segmentTemplate.getAttribute("initialization");

            String startNumberRaw = segmentTemplate.getAttribute("startNumber");
            int startNumber = 1;
            if (startNumberRaw != null && !startNumberRaw.isBlank()) {
                startNumber = Integer.parseInt(startNumberRaw);
            }

            NodeList timelineNodes = segmentTemplate.getElementsByTagNameNS("urn:mpeg:dash:schema:mpd:2011", "SegmentTimeline");
            if (timelineNodes == null || timelineNodes.getLength() == 0) {
                throw new IllegalStateException("No SegmentTimeline found in DASH manifest");
            }

            NodeList segmentNodes = ((Element) timelineNodes.item(0))
                    .getElementsByTagNameNS("urn:mpeg:dash:schema:mpd:2011", "S");
            if (segmentNodes == null || segmentNodes.getLength() == 0) {
                throw new IllegalStateException("No S elements found in DASH manifest SegmentTimeline");
            }

            List<String> segmentUrls = new ArrayList<>();
            if (initializationTemplate != null && !initializationTemplate.isBlank()) {
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
                if (repeatRaw != null && !repeatRaw.isBlank()) {
                    repeat = Integer.parseInt(repeatRaw);
                }

                int numSegments = repeat >= 0 ? repeat + 1 : 1;
                for (int j = 0; j < numSegments; j++) {
                    String url = expandTemplate(mediaTemplate, representation, segmentNumber);
                    segmentUrls.add(url);
                    segmentNumber += 1;
                }
            }

            DashManifest dashManifest = new DashManifest();
            dashManifest.urls = segmentUrls.toArray(new String[0]);
            dashManifest.codecs = codecs;
            dashManifest.encryptionType = NONE;
            dashManifest.mimeType = "audio/" + codecs;
            return dashManifest;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DASH XML manifest", e);
        }
    }

    private String expandTemplate(String template, Element representation, Integer number) {
        String expanded = template;
        String representationId = representation.getAttribute("id");
        String bandwidth = representation.getAttribute("bandwidth");
        if (representationId != null && !representationId.isBlank()) {
            expanded = expanded.replace("$RepresentationID$", representationId);
        }
        if (bandwidth != null && !bandwidth.isBlank()) {
            expanded = expanded.replace("$Bandwidth$", bandwidth);
        }
        if (number != null) {
            expanded = expanded.replace("$Number$", Integer.toString(number));
        }
        return expanded;
    }

    /**
     * Proxies a file from a given URL.
     *
     * @param mediaInfo Media information containing the URL and encryption type
     * @return A Multi emitting the file's content as Buffer
     */
    public Multi<Buffer> streamFile(MediaInfo mediaInfo) {
        String url = mediaInfo.url();
        String encryption = mediaInfo.encryption();
        FileStreamer base;
        if (mediaInfo.urls() != null && mediaInfo.urls().length > 0) {
            base = new MultiUrlFileStreamer(httpClient, List.of(mediaInfo.urls()));
        } else {
            base = new BasicFileStreamer(httpClient, new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url));
        }
        FileStreamer streamer = switch (encryption) {
            case NONE -> base;
            case OLD_AES -> new DecryptingFileStreamer(base, mediaInfo.keyId(), masterKey);
            default -> throw new IllegalStateException("Unexpected value: " + encryption);
        };
        return streamer.stream();
    }


}
