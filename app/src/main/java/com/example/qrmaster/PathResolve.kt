package com.example.qrmaster

import android.annotation.SuppressLint
import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

object PathResolve {
    //replace this with your authority
    private const val AUTHORITY = "com.ianhanniballake.localstorage.documents"
    private const val DEBUG = false // Set to true to enable logging
    /**
     * @return Whether the URI is a local one.
     */
    private fun isLocal(url: String?): Boolean {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://")
    }

    private fun isLocalStorageDocument(uri: Uri): Boolean {
        return AUTHORITY == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
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
     * @author paulburke
     */
    @SuppressLint("Recycle")
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)

        context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)!!.use { cursor ->
            if (cursor.moveToFirst()) {
                if (DEBUG)
                    DatabaseUtils.dumpCursor(cursor)

                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     * @see .isLocal
     * @see .getFile
     */
    fun getPath(context: Context, uri: Uri): String? {

        // DocumentProvider
        when {
            DocumentsContract.isDocumentUri(context, uri) -> // LocalStorageProvider
                when {
                    isLocalStorageDocument(uri) -> // The path is the id
                        return DocumentsContract.getDocumentId(uri)
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]

                        return if ("primary".equals(type, ignoreCase = true)) {
                            try {
                                Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Environment.getExternalStorageDirectory().toString()
                            }
                        } else {
                            try {
                                "/storage/" + split[0] + "/" + split[1]
                            } catch (e: Exception) {
                                e.printStackTrace()
                                "/storage/" + split[0]
                            }
                        }
                        // TODO handle non-primary volumes
                    }
                    isDownloadsDocument(uri) -> {
                        val id = DocumentsContract.getDocumentId(uri)
                        //                final Uri contentUri = ContentUris.withAppendedId(
                        //                        Uri.parse("content://downloads/public_downloads"), id);
                        val split = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        return getDataColumn(context, Uri.parse(split[1]), null, null)
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val type = split[0]
                        var contentUri: Uri? = null
                        when (type) {
                            "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                }
            "content".equals(uri.scheme!!, ignoreCase = true) -> // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
            "file".equals(uri.scheme!!, ignoreCase = true) -> return uri.path
        }
        return null
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null if the
     * Uri is unsupported or pointed to a remote resource.
     * @author paulburke
     * @see .getPath
     */
    private fun getFile(context: Context, uri: Uri?): File? {
        if (uri != null) {
            val path = getPath(context, uri)
            if (isLocal(path)) {
                return File(path)
            }
        }
        return null
    }
}