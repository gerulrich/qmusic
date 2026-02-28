package quantum.music.providers.tdl.services;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import static java.lang.String.format;

public abstract class TldAbstractService {


    protected static final String COVER_RESOLUTION = "640x640.jpg";
    protected static final String ARTIST_RESOLUTION = "750x750.jpg";

    @ConfigProperty(name = "tdl.image.url")
    protected String imageUrl;

    protected String formatId(Long id) {
        return String.format("tdl:%d", id);
    }

    protected String parsedId(String album) {
        return album.replace("tdl:", "");
    }

    /**
     * Formats a cover URL using the configured pattern.
     *
     * @param image The cover identifier
     * @return The formatted cover URL
     */
    protected String formatImageUrl(String image, String resolution) {
        if (image == null || image.isBlank()) {
            return null;
        }
        return format(imageUrl, image.replaceAll("-", "/"), resolution);
    }
}
