package com.orderanalyzer.utils;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Utility methods for file handling with Android's Storage Access Framework (SAF).
 */
public class FileUtils {

    /**
     * Extracts the file extension from a SAF Uri.
     * First tries the MIME type; falls back to parsing the path string.
     *
     * @param context application context
     * @param uri     the Uri returned by ACTION_OPEN_DOCUMENT
     * @return lowercase extension string, e.g. "xlsx", "csv", or "" if unknown
     */
    public static String getFileExtension(Context context, Uri uri) {
        // Try MIME type first
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null && !ext.isEmpty()) return ext.toLowerCase();
        }

        // Fallback: parse the uri path
        String path = uri.getLastPathSegment();
        if (path != null && path.contains(".")) {
            return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Copies the content of a SAF Uri to a temporary file in the app cache directory.
     *
     * <p>Apache POI requires a real {@link File} — it cannot work directly with a stream
     * that wraps a content:// Uri. This method bridges that gap.</p>
     *
     * @param context   application context
     * @param uri       the Uri to read from
     * @param extension file extension used to name the temp file
     * @return a {@link File} in {@code getCacheDir()} containing the copied data
     * @throws Exception if the stream cannot be opened or the copy fails
     */
    public static File copyUriToTempFile(Context context, Uri uri, String extension)
            throws Exception {
        File tempFile = File.createTempFile("order_temp_", "." + extension, context.getCacheDir());
        try (InputStream in  = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            if (in == null) throw new Exception("Cannot open file stream for the selected file.");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}
