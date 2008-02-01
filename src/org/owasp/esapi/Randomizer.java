/**
 * OWASP Enterprise Security API (ESAPI)
 * 
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * http://www.owasp.org/esapi.
 *
 * Copyright (c) 2007 - The OWASP Foundation
 * 
 * The ESAPI is published by OWASP under the LGPL. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
 * 
 * @author Jeff Williams <a href="http://www.aspectsecurity.com">Aspect Security</a>
 * @created 2007
 */
package org.owasp.esapi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.owasp.esapi.errors.EncryptionException;

/**
 * Reference implemenation of the IRandomizer interface. This implementation builds on the JCE provider to provide a
 * cryptographically strong source of entropy. The specific algorithm used is configurable in ESAPI.properties.
 * 
 * @author Jeff Williams
 * @author Jeff Williams (jeff.williams .at. aspectsecurity.com) <a href="http://www.aspectsecurity.com">Aspect Security</a>
 * @since June 1, 2007
 * @see org.owasp.esapi.interfaces.IRandomizer
 */
public class Randomizer implements org.owasp.esapi.interfaces.IRandomizer {

    /** The instance. */
    private static Randomizer instance = new Randomizer();

    /** The sr. */
    private SecureRandom secureRandom = null;

    /** The logger. */
    private static final Logger logger = Logger.getLogger("ESAPI", "Randomizer");

    /**
     * Hide the constructor for the Singleton pattern.
     */
    private Randomizer() {
        String algorithm = SecurityConfiguration.getInstance().getRandomAlgorithm();
        try {
            secureRandom = SecureRandom.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            // Can't throw an exception from the constructor, but this will get
            // it logged and tracked
            new EncryptionException("Error creating randomizer", "Can't find random algorithm " + algorithm, e);
        }
    }

    /**
     * Gets the single instance of Randomizer.
     * 
     * @return single instance of Randomizer
     */
    public static Randomizer getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.owasp.esapi.interfaces.IRandomizer#getRandomString(int, char[])
     */
    public String getRandomString(int length, char[] characterSet) {
        StringBuffer sb = new StringBuffer();
        for (int loop = 0; loop < length; loop++) {
            int index = secureRandom.nextInt(characterSet.length);
            sb.append(characterSet[index]);
        }
        String nonce = sb.toString();
        return nonce;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.owasp.esapi.interfaces.IRandomizer#getRandomBoolean()
     */
    public boolean getRandomBoolean() {
        return secureRandom.nextBoolean();
    }
    
    
    /**
     * FIXME: ENHANCE document whether this is inclusive or not
     * (non-Javadoc)
     * 
     * @see org.owasp.esapi.interfaces.IRandomizer#getRandomInteger(int, int)
     */
    public int getRandomInteger(int min, int max) {
        return secureRandom.nextInt(max - min) + min;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.owasp.esapi.interfaces.IRandomizer#getRandomReal(float, float)
     */
    public float getRandomReal(float min, float max) {
        float factor = max - min;
        return secureRandom.nextFloat() * factor + min;
    }

    /**
     * Returns an unguessable random filename with the specified extension.
     */
    public String getRandomFilename(String extension) {
        return this.getRandomString(12, Encoder.CHAR_ALPHANUMERICS) + "." + extension;
    }

    public String getRandomGUID() {
        // create random string to seed the GUID
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(InetAddress.getLocalHost().toString());
        } catch (UnknownHostException e) {
            sb.append("0.0.0.0");
        }
        sb.append(":");
        sb.append(Long.toString(System.currentTimeMillis()));
        sb.append(":");
        sb.append(this.getRandomString(20, Encoder.CHAR_ALPHANUMERICS));

        // hash the random string to get some random bytes
        String hash = Encryptor.getInstance().hash(sb.toString(), "salt");
        byte[] array = null;
        try {
            array = Encoder.getInstance().decodeFromBase64(hash);
        } catch (IOException e) {
            logger.logCritical(Logger.SECURITY, "Problem decoding hash while creating GUID: " + hash);
        }
        
        // convert to printable hexadecimal characters 
        StringBuffer hex = new StringBuffer();
        for (int j = 0; j < array.length; ++j) {
            int b = array[j] & 0xFF;
            if (b < 0x10)
                hex.append('0');
            hex.append(Integer.toHexString(b));
        }
        String raw = hex.toString().toUpperCase();

        // convert to standard GUID format
        StringBuffer result = new StringBuffer();
        result.append(raw.substring(0, 8));
        result.append("-");
        result.append(raw.substring(8, 12));
        result.append("-");
        result.append(raw.substring(12, 16));
        result.append("-");
        result.append(raw.substring(16, 20));
        result.append("-");
        result.append(raw.substring(20));
        return result.toString();
    }

    /**
     * Union two character arrays.
     * 
     * @param c1 the c1
     * @param c2 the c2
     * @return the char[]
     */
    public static char[] union(char[] c1, char[] c2) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c1.length; i++) {
            if (!contains(sb, c1[i]))
                sb.append(c1[i]);
        }
        for (int i = 0; i < c2.length; i++) {
            if (!contains(sb, c2[i]))
                sb.append(c2[i]);
        }
        char[] c3 = new char[sb.length()];
        sb.getChars(0, sb.length(), c3, 0);
        Arrays.sort(c3);
        return c3;
    }

    /**
     * Contains.
     * 
     * @param sb the sb
     * @param c the c
     * @return true, if successful
     */
    public static boolean contains(StringBuffer sb, char c) {
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == c)
                return true;
        }
        return false;
    }
}