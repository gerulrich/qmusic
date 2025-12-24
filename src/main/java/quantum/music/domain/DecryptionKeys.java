package quantum.music.domain;

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
 *   <li>The keys should be kept in memory only for the duration needed to decrypt the stream</li>
 *   <li>Avoid logging or persisting these keys as they contain sensitive cryptographic material</li>
 *   <li>The byte arrays are mutable; callers should handle them carefully to prevent modification</li>
 * </ul>
 *
 * @param key   The 16-byte (128-bit) decryption key used to decrypt the audio stream.
 *              This is extracted from bytes 0-15 of the decrypted security token.
 * @param nonce The 8-byte (64-bit) nonce (number used once) for the decryption algorithm.
 *              This is extracted from bytes 16-23 of the decrypted security token.
 *              The nonce ensures that the same plaintext encrypted multiple times
 *              produces different ciphertexts.
 */
public record DecryptionKeys(byte[] key, byte[] nonce) {
}
