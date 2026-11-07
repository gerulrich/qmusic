package quantum.music.providers.tdl.stream.crypto;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;
import quantum.music.providers.tdl.stream.FileStreamer;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static io.quarkus.arc.ComponentsProvider.LOG;

/// Decrypts the byte stream produced by another {@link FileStreamer}.
public class DecryptingFileStreamer implements FileStreamer {

    private final FileStreamer delegate;
    private final String keyId;
    private final String masterKey;

    /// Creates a decrypting streamer for the supplied encrypted file stream.
    ///
    /// @param delegate the streamer that supplies encrypted data
    /// @param keyId the Base64-encoded security token containing the stream key and nonce
    /// @param masterKey the Base64-encoded key used to decrypt the security token
    public DecryptingFileStreamer(FileStreamer delegate, String keyId, String masterKey) {
        this.delegate = delegate;
        this.keyId = keyId;
        this.masterKey = masterKey;
    }

    /// Returns the decrypted stream.
    ///
    /// The security token is decrypted before the delegate stream is subscribed to. The Mutiny
    /// operators preserve downstream demand and propagate cancellation to the delegate.
    ///
    /// @return a stream of decrypted buffers
    /// @throws RuntimeException if the security token cannot be decrypted
    @Override
    public Multi<Buffer> stream() {
        DecryptionKeys dk = decryptSecurityToken(keyId, masterKey);
        if (dk.nonce().length != 8) {
            // TODO cambiar mensaje, ya que el user de la api no puede hacer nada respecto a esto
            return Multi.createFrom().failure(new IllegalArgumentException("Nonce must be 8 bytes long."));
        }

        return Multi.createFrom().deferred(() -> {
            final Cipher cipher;
            try {
                cipher = initCipher(dk.key(), dk.nonce());
            } catch (RuntimeException e) {
                return Multi.createFrom().failure(e);
            }

            return delegate.stream()
                .onItem().transform(buffer -> decryptChunk(buffer, cipher))
                .select().where(Optional::isPresent)
                .onItem().transform(Optional::get)
                .onCompletion().continueWith(() -> complete(cipher));
        });
    }

    /// Decrypts one encrypted stream chunk.
    ///
    /// @param buffer the encrypted chunk
    /// @param cipher the initialized stream cipher
    /// @return the decrypted buffer, or an empty optional when the cipher produces no bytes
    private static Optional<Buffer> decryptChunk(Buffer buffer, Cipher cipher) {
        try {
            byte[] decryptedBytes = cipher.update(buffer.getBytes());
            if (decryptedBytes == null || decryptedBytes.length == 0) {
                return Optional.empty();
            }
            return Optional.of(Buffer.buffer(decryptedBytes));
        } catch (Exception e) {
            LOG.errorf(e, "Error decrypting stream chunk");
            throw new RuntimeException("Error decrypting stream chunk", e);
        }
    }

    /// Finalizes decryption after the delegate completes.
    ///
    /// @param cipher the cipher used to decrypt the stream
    /// @return any remaining decrypted bytes
    /// @throws RuntimeException if decryption cannot be finalized
    private static Iterable<Buffer> complete(Cipher cipher) {
        try {
            byte[] finalBytes = cipher.doFinal();
            return finalBytes != null && finalBytes.length > 0
                ? List.of(Buffer.buffer(finalBytes))
                : List.of();
        } catch (Exception e) {
            LOG.errorf(e, "Error finalizing decryption");
            throw new RuntimeException("Error finalizing decryption", e);
        }
    }

    /// Creates and initializes the AES-CTR cipher for the stream.
    ///
    /// The 8-byte nonce is extended to the 16-byte IV expected by AES.
    ///
    /// @param key the AES stream key
    /// @param nonce the 8-byte stream nonce
    /// @return an initialized cipher in decryption mode
    /// @throws RuntimeException if the cipher cannot be initialized
    private Cipher initCipher(byte[] key, byte[] nonce) {
        try {
            byte[] extendedNonce = new byte[16];
            System.arraycopy(nonce, 0, extendedNonce, 0, 8);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(extendedNonce);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            LOG.debugf("Decrypting stream with AES-CTR");
            return cipher;
        } catch (Exception e) {
            LOG.errorf(e, "Error initializing AES-CTR cipher");
            throw new RuntimeException("Error initializing AES-CTR cipher", e);
        }
    }

    /// Decrypts the security token with the master key and extracts its stream key and nonce.
    ///
    /// The token contains a 16-byte IV followed by AES-CBC ciphertext. The decrypted payload
    /// starts with a 16-byte stream key followed by an 8-byte nonce.
    ///
    /// @param securityToken the Base64-encoded security token
    /// @param masterKey the Base64-encoded master key
    /// @return the stream decryption key and nonce
    /// @throws RuntimeException if the token is malformed or cannot be decrypted
    private DecryptionKeys decryptSecurityToken(String securityToken, String masterKey) {
        try {
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
            byte[] securityTokenBytes = Base64.getDecoder().decode(securityToken);

            byte[] iv = new byte[16];
            System.arraycopy(securityTokenBytes, 0, iv, 0, 16);

            byte[] encryptedSt = new byte[securityTokenBytes.length - 16];
            System.arraycopy(securityTokenBytes, 16, encryptedSt, 0, encryptedSt.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedSt = cipher.doFinal(encryptedSt);

            byte[] key = new byte[16];
            byte[] nonce = new byte[8];
            System.arraycopy(decryptedSt, 0, key, 0, 16);
            System.arraycopy(decryptedSt, 16, nonce, 0, 8);

            return new DecryptionKeys(key, nonce);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to decrypt security token");
            throw new RuntimeException("Failed to decrypt security token", e);
        }
    }
}
