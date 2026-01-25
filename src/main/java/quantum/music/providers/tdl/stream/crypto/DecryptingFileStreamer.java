package quantum.music.providers.tdl.stream.crypto;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;
import quantum.music.providers.tdl.stream.FileStreamer;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static io.quarkus.arc.ComponentsProvider.LOG;

public class DecryptingFileStreamer implements FileStreamer {

    private final FileStreamer delegate;
    private final String keyId;
    private final String masterKey;

    public DecryptingFileStreamer(FileStreamer delegate, String keyId, String masterKey) {
        this.delegate = delegate;
        this.keyId = keyId;
        this.masterKey = masterKey;
    }

    @Override
    public Multi<Buffer> stream() {
        DecryptionKeys dk = decryptSecurityToken(keyId, masterKey);
        if (dk.nonce().length != 8) {
            return Multi.createFrom().failure(new IllegalArgumentException("Nonce must be 8 bytes long."));
        }

        return Multi.createFrom().emitter(emitter -> {
            final Cipher cipher;
            try {
                cipher = initCipher(dk.key(), dk.nonce());
            } catch (RuntimeException e) {
                emitter.fail(e);
                return;
            }

            delegate.stream().subscribe().with(
                buffer -> {
                    if (emitter.isCancelled()) {
                        return;
                    }
                    try {
                        byte[] decryptedBytes = cipher.update(buffer.getBytes());
                        if (decryptedBytes != null && decryptedBytes.length > 0) {
                            emitter.emit(Buffer.buffer(decryptedBytes));
                        }
                    } catch (Exception e) {
                        if (!emitter.isCancelled()) {
                            LOG.errorf(e, "Error decrypting stream chunk");
                            emitter.fail(e);
                        }
                    }
                },
                failure -> {
                    if (!emitter.isCancelled()) {
                        emitter.fail(failure);
                    }
                },
                () -> {
                    if (emitter.isCancelled()) {
                        return;
                    }
                    try {
                        byte[] finalBytes = cipher.doFinal();
                        if (finalBytes != null && finalBytes.length > 0) {
                            emitter.emit(Buffer.buffer(finalBytes));
                        }
                        emitter.complete();
                    } catch (Exception e) {
                        if (!emitter.isCancelled()) {
                            LOG.errorf(e, "Error finalizing decryption");
                            emitter.fail(e);
                        }
                    }
                }
            );
        });
    }

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

    /**
     * Decrypts the security token using the master key.
     * This method extracts the decryption key and nonce from an encrypted security token.
     *
     * @param securityToken The base64-encoded security token to decrypt
     * @param masterKey The base64-encoded master key
     * @return The decryption keys containing the key and nonce
     */
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

