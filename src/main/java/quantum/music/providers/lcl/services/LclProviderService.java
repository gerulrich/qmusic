package quantum.music.providers.lcl.services;

import quantum.music.domain.local.QSource;
import quantum.music.providers.AbstractProviderService;

import java.util.List;

/**
 * Base service for local provider implementations.
 *
 * <p>Defines the provider prefix used when formatting local ids.</p>
 */
public abstract class LclProviderService extends AbstractProviderService {

    @Override
    public String getProviderPrefix() {
        return "lcl";
    }

    /**
     * Builds tag values from source format and quality.
     *
     * @param source local source data
     * @return list of non-null tags in format, quality order
     */
    protected List<String> sourceTags(QSource source) {
        if (source == null) {
            return List.of();
        }
        if (source.format == null && source.quality == null) {
            return List.of();
        }
        if (source.format == null) {
            return List.of(source.quality);
        }
        if (source.quality == null) {
            return List.of(source.format);
        }
        return List.of(source.format, source.quality);
    }

}
