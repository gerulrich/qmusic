package quantum.music.providers.tdl.stream.crypto;

/// Represents the decryption keys extracted from a security token used for decrypting
/// encrypted audio streams.
///
/// This immutable record encapsulates the cryptographic keys required to decrypt media content.
/// The keys are obtained by decrypting a security token using AES-256-CBC encryption with a
/// master key. The decrypted security token contains both the stream decryption key and
/// the nonce value needed for the decryption process.
///
///
/// ## Domain context
///
/// - These keys are essential for enabling playback of encrypted tracks and ensuring secure content delivery.
/// - They are used by service and resource layers to unlock audio streams for authorized users.
/// - Proper handling of these keys is critical for maintaining the security model of the Quantum Music application.
///
/// ## Typical usage
///
/// ```java
/// // Obtain decryption keys from a security token
/// DecryptionKeys keys = trackService.decryptSecurityToken(securityToken);
///
/// // Use the keys to decrypt audio stream
/// byte[] decryptedAudio = decryptStream(audioData, keys.key(), keys.nonce());
/// ```
///
/// ## Security considerations
///
/// - Keys should reside in memory only as long as needed for decryption.
/// - Never log or persist these keys, as they are sensitive cryptographic material.
/// - Byte arrays are mutable; callers must avoid accidental modification or leakage.
/// @param key   The 16-byte (128-bit) decryption key for the audio stream, extracted from bytes 0-15 of the decrypted token.
/// @param nonce The 8-byte (64-bit) nonce for the decryption algorithm, extracted from bytes 16-23 of the decrypted token.
public record DecryptionKeys(byte[] key, byte[] nonce) {
}
