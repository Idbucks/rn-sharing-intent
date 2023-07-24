package com.rnsharingintent;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import android.content.ContentUris;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Objects;

public class RnSharingIntentHelper {

  private Context context;

  public RnSharingIntentHelper(Application context) {
    this.context = context;
  }

  public void sendFileNames(Context context, Intent intent, Promise promise) {
    try {
      String action = intent.getAction();
      String type = intent.getType();
      boolean used = intent.getBooleanExtra("used", false);
      if (type == null || used) {
        return;
      }
      if (!type.startsWith("text")
          && (Objects.equals(action, Intent.ACTION_SEND) || Objects.equals(action, Intent.ACTION_SEND_MULTIPLE))) {
        WritableMap files = getMediaUris(intent, context);
        if (files == null)
          return;
        promise.resolve(files);
      } else if (type.startsWith("text") && Objects.equals(action, Intent.ACTION_SEND)) {
        String text = null;
        try {
          String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
          String url = intent.getStringExtra(Intent.EXTRA_TEXT);
          text = subject + "db74299ff8b603ac8fb1ba76ea177e83" + url;
        } catch (Exception ignored) {
        }
        if (text == null) {
          WritableMap files = getMediaUris(intent, context);
          if (files == null)
            return;
          promise.resolve(files);
        } else {
          WritableMap files = new WritableNativeMap();
          WritableMap file = new WritableNativeMap();
          file.putString("contentUri", null);
          file.putString("filePath", null);
          file.putString("fileName", null);
          file.putString("extension", null);
          file.putString("weblink", null);
          file.putString("text", text);
          files.putMap("0", file);
          promise.resolve(files);
        }

      } else if (Objects.equals(action, Intent.ACTION_VIEW)) {
        String link = intent.getDataString();
        WritableMap files = new WritableNativeMap();
        WritableMap file = new WritableNativeMap();
        file.putString("contentUri", null);
        file.putString("filePath", null);
        file.putString("mimeType", null);
        file.putString("text", null);
        file.putString("weblink", link);
        file.putString("fileName", null);
        file.putString("extension", null);
        files.putMap("0", file);
        promise.resolve(files);
      } else {
        promise.reject("error", "Invalid file type.");
      }
    } catch (Exception e) {
      promise.reject("error", e.toString());
    }
  };

  public WritableMap getMediaUris(Intent intent, Context context) {
    if (intent == null)
      return null;
    WritableMap files = new WritableNativeMap();
    if (Objects.equals(intent.getAction(), Intent.ACTION_SEND)) {
      WritableMap file = new WritableNativeMap();
      Uri contentUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

      if (contentUri == null)
        return null;
      // Based on
      // https://developer.android.com/training/secure-file-sharing/retrieve-info
      ContentResolver contentResolver = context.getContentResolver();
      file.putString("mimeType", contentResolver.getType(contentUri));
      Cursor queryResult = contentResolver.query(contentUri, null, null, null, null);
      String filePath = getPath(context, contentUri);
      if (filePath == null) {
        filePath = FileDirectory.INSTANCE.getAbsolutePath(context, contentUri);
      }

      queryResult.moveToFirst();
      file.putString("fileName", queryResult.getString(queryResult.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
      file.putString("contentUri", contentUri.toString());
      file.putString("filePath", filePath);
      file.putString("text", null);
      file.putString("weblink", null);
      files.putMap("0", file);
    } else if (Objects.equals(intent.getAction(), Intent.ACTION_SEND_MULTIPLE)) {
      ArrayList<Uri> contentUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
      if (contentUris != null) {
        int index = 0;
        for (Uri uri : contentUris) {
          WritableMap file = new WritableNativeMap();
          ContentResolver contentResolver = context.getContentResolver();
          String filePath = getPath(context, uri);
          if (filePath == null) {
            filePath = FileDirectory.INSTANCE.getAbsolutePath(context, uri);
          }
          // Based on
          // https://developer.android.com/training/secure-file-sharing/retrieve-info
          file.putString("mimeType", contentResolver.getType(uri));
          Cursor queryResult = contentResolver.query(uri, null, null, null, null);
          queryResult.moveToFirst();
          file.putString("fileName", queryResult.getString(queryResult.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
          file.putString("filePath", filePath);
          file.putString("contentUri", uri.toString());
          file.putString("text", null);
          file.putString("weblink", null);
          files.putMap(Integer.toString(index), file);
          index++;
        }
      }
    }
    return files;
  }

  public static String getPath(final Context context, final Uri uri) {
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        Log.d("extdoc", uri.toString());
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/"
              + split[1];
        }

        // TODO: handle non-primary volumes
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {
        Log.d("dldoc", uri.toString());
        final String id = DocumentsContract.getDocumentId(uri);

        if (id.startsWith("msf:")) {
          final String[] split = id.split(":");
          final String selection = "_id=?";
          final String[] selectionArgs = new String[] { split[1] };
          return getDataColumn(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs);
        }

        String[] contentUriPrefixesToTry = new String[] {
            "content://downloads/public_downloads",
            "content://downloads/my_downloads",
            "content://downloads/all_downloads"
        };

        for (String contentUriPrefix : contentUriPrefixesToTry) {
          Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
          try {
            String path = getDataColumn(context, contentUri, null, null);
            if (path != null) {
              return path;
            }
          } catch (Exception e) {
          }
        }

      }

      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);

        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] { split[1] };

        return getDataColumn(context, contentUri, selection,
            selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri
        .getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri
        .getAuthority());
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context       The context.
   * @param uri           The Uri to query.
   * @param selection     (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(Context context, Uri uri,
      String selection, String[] selectionArgs) {
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = { column };

    try {
      cursor = context.getContentResolver().query(uri, projection,
          selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor
            .getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri
        .getAuthority());
  }

  private String getMediaType(String url) {
    String mimeType = URLConnection.guessContentTypeFromName(url);
    return mimeType;
  }

  public void clearFileNames(Intent intent) {
    String type = intent.getType();
    intent.putExtra("used", true);
    if (type == null)
      return;
    if (type.startsWith("text")) {
      intent.removeExtra(Intent.EXTRA_SUBJECT);
      intent.removeExtra(Intent.EXTRA_TEXT);
    } else if (type.startsWith("image") || type.startsWith("video") || type.startsWith("application")) {
      intent.removeExtra(Intent.EXTRA_STREAM);
    }
  }

  public String getFileName(String file) {
    return file.substring(file.lastIndexOf('/') + 1);
  }

  public String getExtension(String file) {
    return file.substring(file.lastIndexOf('.') + 1);
  }

}
