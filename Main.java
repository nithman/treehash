package org.nithman.data.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    private static final int ONE_MB = 1024 * 1024;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing required filename argument");
            System.exit(-1);
        }
        for (String arg : args) {
            File inputFile = new File(arg);
            try {
                long nowMillis = System.currentTimeMillis();
                byte[] treeHash = computeSHA256TreeHash(inputFile);
                System.out.printf("%s *%s\n", toHex(treeHash), arg);
                System.err.println(arg + " " + Long.toString(System.currentTimeMillis() - nowMillis));
            } catch (IOException ioe) {
                System.err.format("Exception when reading from file %s: %s", inputFile,
                        ioe.getMessage());
                System.exit(-1);

            } catch (NoSuchAlgorithmException nsae) {
                System.err.format("Cannot locate MessageDigest algorithm for SHA-256: %s",
                        nsae.getMessage());
                System.exit(-1);
            }
        }
    }

    private static Object toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);

        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i] & 0xFF);

            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }

    private static byte[] computeSHA256TreeHash(File inputFile)
            throws IOException, NoSuchAlgorithmException {
        byte[][] chunkSHA256Hashes = getChunkSHA256Hashes(inputFile);
        return computeSHA256TreeHash(chunkSHA256Hashes);
    }

    private static byte[] computeSHA256TreeHash(byte[][] chunkSHA256Hashes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[][] prevLvlHashes = chunkSHA256Hashes;

        while (prevLvlHashes.length > 1) {

            int len = prevLvlHashes.length / 2;
            if (prevLvlHashes.length % 2 != 0) {
                len++;
            }

            byte[][] currLvlHashes = new byte[len][];

            int j = 0;
            for (int i = 0; i < prevLvlHashes.length; i = i + 2, j++) {

                // If there are at least two elements remaining
                if (prevLvlHashes.length - i > 1) {

                    // Calculate a digest of the concatenated nodes
                    md.reset();
                    md.update(prevLvlHashes[i]);
                    md.update(prevLvlHashes[i + 1]);
                    currLvlHashes[j] = md.digest();

                } else { // Take care of remaining odd chunk
                    currLvlHashes[j] = prevLvlHashes[i];
                }
            }
            prevLvlHashes = currLvlHashes;
        }
        return prevLvlHashes[0];
    }

    private static byte[][] getChunkSHA256Hashes(File file) throws IOException,
    NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        long numChunks = file.length() / ONE_MB;
        if (file.length() % ONE_MB > 0) {
            numChunks++;
        }

        if (numChunks == 0) {
            return new byte[][] { md.digest() };
        }

        byte[][] chunkSHA256Hashes = new byte[(int) numChunks][];
        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(file);
            byte[] buff = new byte[ONE_MB];

            int bytesRead;
            int idx = 0;

            while ((bytesRead = fileStream.read(buff, 0, ONE_MB)) > 0) {
                md.reset();
                md.update(buff, 0, bytesRead);
                chunkSHA256Hashes[idx++] = md.digest();
            }

            return chunkSHA256Hashes;

        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException ioe) {
                    System.err.printf("Exception while closing %s.\n %s", file.getName(),
                            ioe.getMessage());
                }
            }
        }
    }
}
