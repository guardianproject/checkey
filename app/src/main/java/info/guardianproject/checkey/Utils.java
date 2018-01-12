
package info.guardianproject.checkey;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class Utils {

    private static PackageManager pm;
    private static CertificateFactory certificateFactory;

    public static String getCertificateFingerprint(X509Certificate cert, String hashAlgorithm) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            byte[] rawCert = cert.getEncoded();
            hash = toHexString(md.digest(rawCert));
            md.reset();
        } catch (CertificateEncodingException e) {
            hash = "CertificateEncodingException";
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            hash = "NoSuchAlgorithm";
            e.printStackTrace();
        }
        return hash;
    }

    public static String getCertificateFingerprint(File apkFile, String hashAlgorithm)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        String hash = null;
        byte [] cert = getCertificate(apkFile);
        if (cert != null)
            hash = toHexString(md.digest(cert));
        md.reset();
        return hash;
    }

    public static X509Certificate[] getX509Certificates(Context context, String packageName) {
        X509Certificate[] certs = null;
        if (pm == null)
            pm = context.getApplicationContext().getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (certificateFactory == null)
                certificateFactory = CertificateFactory.getInstance("X509");
            certs = new X509Certificate[pkgInfo.signatures.length];
            for (int i = 0; i < certs.length; i++) {
                byte[] cert = pkgInfo.signatures[i].toByteArray();
                InputStream inStream = new ByteArrayInputStream(cert);
                certs[i] = (X509Certificate) certificateFactory.generateCertificate(inStream);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return certs;
    }

    public static byte[] getCertificate(File apkFile)
            throws NoSuchAlgorithmException {
        byte[] rawCertBytes = null;
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
        } catch (CertificateEncodingException e) {
        } catch (IOException e) {
        }
        return rawCertBytes;
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
