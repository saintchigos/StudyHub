package com.saintchigos.studyhub.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Free, dependency-free ways for a student to get data in and out of the app.
 *
 * Sharing uses the system share sheet (WhatsApp, email, Bluetooth) and file picking
 * uses the system picker, so nothing here needs an account, a server or a paid service.
 */
object Sharing {

    const val SUPPORT_NUMBER = "+26662848760"
    private const val WHATSAPP = "https://wa.me/26662848760"

    /**
     * Writes the catalogue to a real file and shares it, so a beta tester can send the
     * whole thing to Chigos Media without pasting walls of text into a chat.
     */
    fun shareCatalogue(context: Context, json: String): String {
        return runCatching {
            val file = File(context.cacheDir, "studyhub-catalogue.json")
            file.writeText(json, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "StudyHub programme catalogue")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is the programme data I typed into StudyHub. Please add it so " +
                        "other students can use it. - Beta tester"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Send catalogue to Chigos Media"))
            "Catalogue ready to send."
        }.getOrElse { "Could not prepare the catalogue file." }
    }

    fun openWhatsApp(context: Context, text: String) {
        val encoded = Uri.encode(text)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("$WHATSAPP?text=$encoded"))
            )
        }.onFailure {
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_NUMBER"))) }
        }
    }

    fun callSupport(context: Context) {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_NUMBER"))) }
    }

    fun openStoreListing(context: Context) {
        val uri = Uri.parse("market://details?id=${context.packageName}")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    )
                )
            }
        }
    }
}

const val FEEDBACK_MESSAGE =
    "Hi Chigos Media, I found a problem in StudyHub.\n\nWhat I was doing: \nWhat happened: \n" +
        "My phone and app version: "

const val SUGGESTION_MESSAGE =
    "Hi Chigos Media, a programme is missing from StudyHub. It is:\n\nProgramme name: \n" +
        "Year: \nSemester: \n\nI can type the courses and class times in if you add it."