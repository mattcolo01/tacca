package it.matteocolombo.tacca.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/** Opens user-picked documents; abstracted so ViewModels can be tested without a ContentResolver. */
interface DocumentStreams {
    fun openInput(uri: Uri): InputStream?
    fun openOutput(uri: Uri): OutputStream?
}

class ContentResolverDocumentStreams(private val resolver: ContentResolver) : DocumentStreams {
    override fun openInput(uri: Uri): InputStream? = resolver.openInputStream(uri)
    override fun openOutput(uri: Uri): OutputStream? = resolver.openOutputStream(uri, "wt")
}
