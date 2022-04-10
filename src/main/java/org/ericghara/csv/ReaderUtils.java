package org.ericghara.csv;

import org.ericghara.exception.FileReadException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

/**
 * A collection of functions to create {@link Reader}s from common text sources.
 */
public class ReaderUtils {

    public static Reader getFileReader(File file) throws FileReadException {
        try {
            return new BufferedReader(new FileReader(file));
        } catch (Exception e) {
            throw new FileReadException("Unable to instantiate reader.", e);
        }
    }

    public static Reader getStringReader(String csv) {
        return new StringReader(csv);
    }

    /**
     * Convenience function to stream a resource file as a {@link Reader}.
     *
     * Usage:
     *
     * <pre>
     *     TestDir.getResourceFile(this, "aFile.csv");
     * </pre>
     *
     * @param aThis an object where the {@link ClassLoader} should be retrieved from.
     * @param csvName name of the resource
     * @return {@link Reader} character stream of the matching resource file
     * @throws FileReadException If resource cannot be found or read
     * @see ClassLoader#getResource(String)
     *
     */
    public static Reader getResourceFileReader(Object aThis, String csvName) throws FileReadException {
        File csv;
        try {
            URI csvPath = aThis.getClass().getResource(csvName).toURI();
            csv = new File(csvPath);
        } catch (Exception e) {
            throw new FileReadException("Couldn't open the csv " + csvName, e);
        }
        return getFileReader(csv);
    }
}
