package org.example.learn.java.lang.spec.string.util;

public class Bin {

    public static String toBinaryString(byte[] data) {
        StringBuilder binary = new StringBuilder();
        for (byte b : data) {
            // Mask to avoid sign-extension, convert to 8-bit binary, and pad with zeros
            binary.append(Integer.toBinaryString(b & 0xFF));
        }

        return binary.toString();
    }

    public static String toBinaryStringWithSeparator(byte[] data) {
        StringBuilder binary = new StringBuilder();
        for (byte b : data) {
            binary.append(" ");
            // Mask to avoid sign-extension, convert to 8-bit binary, and pad with zeros
            binary.append(Integer.toBinaryString(b & 0xFF));
        }

        return binary.toString().replaceFirst(" ", "");
    }
}
