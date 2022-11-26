package com.github.bitstuffing.campdf;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;

public class Utils {

    public static boolean checkPermissions(Context context){
        return ContextCompat.checkSelfPermission(context,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean requestPermissions(Activity activity){
        if (!checkPermissions(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[] {
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },100);
        }
        return true;
    }

    public static File copyToTempFile(Uri uri, File tempFile, Activity activity) throws IOException {
        // Obtain an input stream from the uri
        InputStream inputStream = activity.getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new IOException("Unable to obtain input stream from URI");
        }
        // Copy the stream to the temp file
        FileUtils.copyInputStreamToFile(inputStream, tempFile);

        return tempFile;
    }

    public static void shareUri(String fileName, Activity activity){
        Uri fileUri = getFileUri(fileName, activity);
        ArrayList<Uri> fileUris = new ArrayList<Uri>();
        fileUris.add(fileUri);
        if (fileUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.setType("application/pdf");
            activity.startActivity(Intent.createChooser(shareIntent, null));

        }
    }

    private static Uri getFileUri(String fileName, Activity activity) {
        Uri uri = null;

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //uri = MediaStore.getDocumentUri(activity, uri);
            for(File file : activity.getFilesDir().listFiles()){
                if(file.getName().equals(fileName)){
                    uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID+".provider", file); //Uri.fromFile(file);
                }
            };
        }else{
            uri = Uri.parse(fileName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                uri = MediaStore.getDocumentUri(activity, uri);
            }
        }
        return uri;
    }

    public static void openPDFFile(String fileName, Activity activity)  throws IOException {
        Uri uri = getFileUri(fileName, activity);

        // Surrounded with if statement for Android R to get access of complete file.
        Intent pdfOpenIntent = new Intent(Intent.ACTION_VIEW);
        //pdfOpenIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pdfOpenIntent.setClipData(ClipData.newRawUri("", uri));
        pdfOpenIntent.setDataAndType(uri, "application/pdf");
        pdfOpenIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |  Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        //pdfOpenIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(pdfOpenIntent);
        } catch (ActivityNotFoundException e) {
            // Instruct the user to install a PDF reader here, or something
            e.printStackTrace();
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     */
    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     */
    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
