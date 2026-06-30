package com.cfdeployagent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses multipart/form-data request bodies sent by the deployment client.
 */
public final class MultipartParser {

    public static class ParsedMultipart {
        private final Map<String, String> fields;
        private final byte[] fileBytes;
        private final String fileFieldName;

        public ParsedMultipart(Map<String, String> fields, byte[] fileBytes, String fileFieldName) {
            this.fields = fields;
            this.fileBytes = fileBytes;
            this.fileFieldName = fileFieldName;
        }

        public String getField(String name) {
            return fields.get(name);
        }

        public byte[] getFileBytes() {
            return fileBytes;
        }

        public String getFileFieldName() {
            return fileFieldName;
        }
    }

    private MultipartParser() {
    }

    public static ParsedMultipart parse(InputStream inputStream, String contentType) throws IOException {
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            throw new IOException("Missing multipart boundary");
        }

        byte[] body = readAllBytes(inputStream);
        String marker = "--" + boundary;
        byte[] markerBytes = marker.getBytes(StandardCharsets.US_ASCII);

        Map<String, String> fields = new HashMap<>();
        byte[] fileBytes = null;
        String fileFieldName = null;

        int index = indexOf(body, markerBytes, 0);
        while (index >= 0) {
            int partStart = index + markerBytes.length;
            if (partStart + 2 <= body.length && body[partStart] == '-' && body[partStart + 1] == '-') {
                break;
            }

            if (partStart + 2 <= body.length && body[partStart] == '\r' && body[partStart + 1] == '\n') {
                partStart += 2;
            }

            int nextBoundary = indexOf(body, markerBytes, partStart);
            if (nextBoundary < 0) {
                break;
            }

            int partEnd = nextBoundary;
            if (partEnd >= 2 && body[partEnd - 2] == '\r' && body[partEnd - 1] == '\n') {
                partEnd -= 2;
            }

            byte[] partBytes = slice(body, partStart, partEnd);
            Part part = parsePart(partBytes);
            if (part != null) {
                if (part.isFile) {
                    fileBytes = part.content;
                    fileFieldName = part.name;
                } else if (part.name != null) {
                    fields.put(part.name, part.textValue);
                }
            }

            index = nextBoundary;
        }

        return new ParsedMultipart(fields, fileBytes, fileFieldName);
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (String token : contentType.split(";")) {
            String trimmed = token.trim();
            if (trimmed.startsWith("boundary=")) {
                String boundary = trimmed.substring("boundary=".length()).trim();
                if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() >= 2) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                return boundary;
            }
        }
        return null;
    }

    private static Part parsePart(byte[] partBytes) {
        int headerEnd = indexOf(partBytes, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), 0);
        if (headerEnd < 0) {
            return null;
        }

        String headers = new String(partBytes, 0, headerEnd, StandardCharsets.UTF_8);
        byte[] content = slice(partBytes, headerEnd + 4, partBytes.length);

        String disposition = null;
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-disposition:")) {
                disposition = line.substring("content-disposition:".length()).trim();
                break;
            }
        }
        if (disposition == null) {
            return null;
        }

        String name = extractAttribute(disposition, "name");
        String filename = extractAttribute(disposition, "filename");
        boolean isFile = filename != null;

        Part part = new Part();
        part.name = name;
        part.isFile = isFile;
        if (isFile) {
            part.content = content;
        } else {
            part.textValue = new String(content, StandardCharsets.UTF_8);
        }
        return part;
    }

    private static String extractAttribute(String disposition, String attribute) {
        String search = attribute + "=\"";
        int start = disposition.indexOf(search);
        if (start >= 0) {
            start += search.length();
            int end = disposition.indexOf('"', start);
            if (end > start) {
                return disposition.substring(start, end);
            }
        }

        search = attribute + "=";
        start = disposition.indexOf(search);
        if (start >= 0) {
            start += search.length();
            int end = disposition.indexOf(';', start);
            if (end < 0) {
                end = disposition.length();
            }
            return disposition.substring(start, end).trim();
        }
        return null;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        if (target.length == 0 || source.length < target.length) {
            return -1;
        }
        for (int i = Math.max(0, fromIndex); i <= source.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] slice(byte[] source, int start, int end) {
        int length = Math.max(0, end - start);
        byte[] copy = new byte[length];
        System.arraycopy(source, start, copy, 0, length);
        return copy;
    }

    private static final class Part {
        private String name;
        private boolean isFile;
        private String textValue;
        private byte[] content;
    }
}
