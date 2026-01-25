package quantum.music.providers.tdl.stream.crypto;

/**
 * Represents the decryption keys extracted from a security token used for decrypting
 * encrypted audio streams.
 * <p>
 * This immutable record encapsulates the cryptographic keys required to decrypt media content.
 * The keys are obtained by decrypting a security token using AES-256-CBC encryption with a
 * master key. The decrypted security token contains both the stream decryption key and
 * the nonce value needed for the decryption process.
 * </p>
 *
 * <h2>Domain Context:</h2>
 * <ul>
 *   <li>These keys are essential for enabling playback of encrypted tracks and ensuring secure content delivery.</li>
 *   <li>They are used by service and resource layers to unlock audio streams for authorized users.</li>
 *   <li>Proper handling of these keys is critical for maintaining the security model of the Quantum Music application.</li>
 * </ul>
 *
 * <h2>Typical Usage:</h2>
 * <pre>{@code
 * // Obtain decryption keys from a security token
 * DecryptionKeys keys = trackService.decryptSecurityToken(securityToken);
 *
 * // Use the keys to decrypt audio stream
 * byte[] decryptedAudio = decryptStream(audioData, keys.key(), keys.nonce());
 * }</pre>
 *
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li>Keys should reside in memory only as long as needed for decryption.</li>
 *   <li>Never log or persist these keys, as they are sensitive cryptographic material.</li>
 *   <li>Byte arrays are mutable; callers must avoid accidental modification or leakage.</li>
 * </ul>
 *
 * @param key   The 16-byte (128-bit) decryption key for the audio stream, extracted from bytes 0-15 of the decrypted token.
 * @param nonce The 8-byte (64-bit) nonce for the decryption algorithm, extracted from bytes 16-23 of the decrypted token.
 */
public record DecryptionKeys(byte[] key, byte[] nonce) {
}
