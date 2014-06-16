
package info.guardianproject.checkey;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class Utils {

    public static String getCertificateFingerprint(File apkFile, String hashAlgorithm)
            throws NoSuchAlgorithmException {
        byte[] rawCertBytes;
        try {
            JarFile apkJar = new JarFile(apkFile);
            JarEntry aSignedEntry = (JarEntry) apkJar.getEntry("AndroidManifest.xml");

            if (aSignedEntry == null) {
                apkJar.close();
                return null;
            }

            InputStream tmpIn = apkJar.getInputStream(aSignedEntry);
            byte[] buff = new byte[2048];
            while (tmpIn.read(buff, 0, buff.length) != -1) {
                /*
                 * NOP - apparently have to READ from the JarEntry before you
                 * can call getCerficates() and have it return != null. Yay
                 * Java.
                 */
            }
            tmpIn.close();

            if (aSignedEntry.getCertificates() == null
                    || aSignedEntry.getCertificates().length == 0) {
                apkJar.close();
                return null;
            }

            Certificate signer = aSignedEntry.getCertificates()[0];
            apkJar.close();
            rawCertBytes = signer.getEncoded();

            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            String hash = toHexString(md.digest(rawCertBytes));
            md.reset();
            Log.i("SigningCertificate", "raw hash: " + hash);

            return hash;
        } catch (CertificateEncodingException e) {
        } catch (IOException e) {
        }
        return "BAD_CERTIFICATE";
    }

    public static String getBinaryHash(File apk, String algo) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            fis = new FileInputStream(apk);
            bis = new BufferedInputStream(fis);

            byte[] dataBytes = new byte[524288];
            int nread = 0;

            while ((nread = bis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);

            byte[] mdbytes = md.digest();
            return toHexString(mdbytes);
        } catch (IOException e) {
            Log.e("FDroid", "Error reading \"" + apk.getAbsolutePath() + "\" to compute SHA1 hash.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("FDroid", "Device does not support " + algo + " MessageDisgest algorithm");
            return null;
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    return null;
                }
        }
    }

    /**
     * Computes the base 16 representation of the byte array argument.
     *
     * @param bytes an array of bytes.
     * @return the bytes represented as a string of hexadecimal digits.
     */
    public static String toHexString(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

}
