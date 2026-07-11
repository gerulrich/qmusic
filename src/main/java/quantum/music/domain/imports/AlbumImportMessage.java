package quantum.music.domain.imports;

import quantum.music.domain.providers.TrackList;

/**
 * Payload sent to the local importer when a provider requests an album import.
 *
 * @param sourceProviderId provider that initiated the import
 * @param trackList album metadata and track list
 */
public record AlbumImportMessage(String sourceProviderId, TrackList trackList) {
}

