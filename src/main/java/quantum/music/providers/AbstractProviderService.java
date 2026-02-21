package quantum.music.providers;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.function.Function;

/**
 * Base service with shared utilities for music provider integrations.
 * Helps standardize provider identifiers, pagination, and list mapping.
 */
public abstract class AbstractProviderService {

    /**
     * Returns the provider prefix used to namespace identifiers.
     */
    public abstract String getProviderPrefix();

    /**
     * Builds a provider-scoped identifier in the form "prefix:objectId".
     */
    protected String formatId(ObjectId id) {
        return STR."\{getProviderPrefix()}:\{id}";
    }

    /**
     * Removes the provider prefix from a provider-scoped identifier.
     */
    protected String parsedId(String album) {
        return album.replace(STR."\{getProviderPrefix()}:", "");
    }

    /**
     * Computes a zero-based page index given offset and page size.
     */
    protected int pageIndex(int offset, int limit) {
        return limit <= 0 ? 0 : offset / limit;
    }

    /**
     * Maps a list of items using the provided mapper, returning an empty list when null.
     */
    protected <O,T> List<T> mapList(List<O> items, Function<O, T> mapper) {
        return items == null ? List.of() : items.stream().map(mapper).toList();
    }

}
