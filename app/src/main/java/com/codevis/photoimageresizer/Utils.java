package com.codevis.photoimageresizer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.codevis.photoimageresizer.R;
import com.fxn.stash.Stash;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class Utils {

    public static final String OLD_FILE = "OLD_FILE";
    public static final String PDF_FILE = "PDF_FILE";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String NEW_FILE = "NEW_FILE";

    public static final String TYPE_PDF = "application/pdf";
    public static final String TYPE_IMG = "image/*";
    public static final String PARAMS = "PARAMS";
    public static final String SHOW_AD = "SHOW_AD";
    public static final String COMPRESS = "COMPRESS";

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        if (Stash.getBoolean(result, false)) {
            // OLD FILE NAME FOUND
            result = result + "_1";

        }
        Stash.put(result, true);

        return result;
    }

    public static boolean isEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    public static void toast(Context context, String Message) {
        Toast.makeText(context, Message, Toast.LENGTH_SHORT).show();
    }


    static public boolean resetExternalStorageMedia(Context context) {
        if (Environment.isExternalStorageEmulated())
            return (false);
        Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory());
        Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, uri);

        context.sendBroadcast(intent);
        return (true);
    }

    static public void notifyMediaScannerService(Context context, String path) {
        MediaScannerConnection.scanFile(context,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public static String getFileSize(long v) {

        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));

    }

    public static File commonDocumentDirPath(String FolderName, Context context) {
        File dir = null;
        File i = new File("path");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dir = new File(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    + File.separator
                    + context.getResources().getString(R.string.app_name)
                    + File.separator
                    + FolderName);
        } else {
           /* dir = new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + "Pictures"
                    + File.separator
                    + context.getResources().getString(R.string.app_name)
                    + File.separator
                    + FolderName);*/

            dir = new File(Environment.getExternalStorageDirectory() + "/" + context.getResources().getString(R.string.app_name) + "/" + FolderName);
//            dir = new File(Environment.getExternalStorageDirectory() + "/" + R.string.app_name + "/" + FolderName);
        }

        // Make sure the path directory exists.
        if (!dir.exists()) {
            // Make it, if it doesn't exit
            boolean success = dir.mkdirs();
            if (!success) {
                dir = null;
            }
        }
        return dir;
    }

    private String getFilePathString() {
        String path_save_vid = "";

        if (Build.VERSION.SDK_INT >= 30) {//Build.VERSION_CODES.R
            path_save_vid =
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS) +
                            File.separator +
                            R.string.app_name +
                            File.separator + "Text";
        } else {
            path_save_vid =
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                            File.separator +
                            R.string.app_name +
                            File.separator + "Text";

        }


        return path_save_vid;

    }

    public static void share(Context context, String path, String type) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        Uri screenshotUri = Uri.parse(path);
        sharingIntent.setType(type);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
        context.startActivity(Intent.createChooser(sharingIntent, "Share..."));
    }

}
