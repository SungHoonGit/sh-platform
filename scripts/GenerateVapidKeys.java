import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class GenerateVapidKeys {
    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = gen.generateKeyPair();

        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();

        byte[] pubBytes = pub.getW().getAffineX().toByteArray();
        byte[] yBytes = pub.getW().getAffineY().toByteArray();
        byte[] pubUncompressed = new byte[65];
        pubUncompressed[0] = 0x04;
        System.arraycopy(pubBytes, Math.max(0, pubBytes.length - 32), pubUncompressed, 1, 32);
        System.arraycopy(yBytes, Math.max(0, yBytes.length - 32), pubUncompressed, 33, 32);

        byte[] privBytes = priv.getS().toByteArray();
        byte[] privPadded = new byte[32];
        System.arraycopy(privBytes, Math.max(0, privBytes.length - 32), privPadded, 0, 32);

        String pubBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(pubUncompressed);
        String privBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(privPadded);

        System.out.println("WEBPUSH_VAPID_PUBLIC_KEY=" + pubBase64);
        System.out.println("WEBPUSH_VAPID_PRIVATE_KEY=" + privBase64);
    }
}
