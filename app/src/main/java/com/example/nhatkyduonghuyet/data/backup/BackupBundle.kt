package com.example.nhatkyduonghuyet.data.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A whole backup as one file.
 *
 * Exporting used to be three separate save dialogs, which meant a user who did
 * two of them and got distracted ended up with an incomplete backup and no way
 * to tell. A bundle is all-or-nothing: one action, one file, everything in it.
 *
 * ZIP was chosen over "one big CSV with section markers" because the individual
 * entries stay ordinary CSV files - the user can still open them in Excel after
 * unzipping, which is the main reason CSV was picked in the first place.
 */
object BackupBundle {

    const val MIME_TYPE = "application/zip"
    const val FILE_PREFIX = "sao_luu_nhat_ky"

    /** ZIP local file header, used to recognise a bundle by content. */
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    /** A short note so someone opening the zip knows what they are looking at. */
    private const val README_ENTRY = "DOC_TRUOC_KHI_MO.txt"

    private val readmeText = """
        Bản sao lưu ứng dụng Nhật Ký Đường Huyết
        =========================================

        Tệp nén này chứa toàn bộ dữ liệu của bạn, gồm 3 tệp CSV:

          - ${BackupPart.DIARY.fileName}: ${BackupPart.DIARY.label}
          - ${BackupPart.PRESCRIPTION.fileName}: ${BackupPart.PRESCRIPTION.label}
          - ${BackupPart.MEDICATION_HISTORY.fileName}: ${BackupPart.MEDICATION_HISTORY.label}

        CÁCH KHÔI PHỤC
        Mở app, vào màn hình "Sao lưu & Khôi phục", chọn "Khôi phục" rồi chọn
        chính tệp .zip này. Không cần giải nén trước.

        Bạn cũng có thể giải nén và mở từng tệp CSV bằng Excel để xem.
        Hãy giữ tệp này ở nơi an toàn (Google Drive, email cho chính mình...).
        Nếu gỡ app mà không có tệp này, dữ liệu sẽ mất vĩnh viễn.
    """.trimIndent()

    fun isBundle(fileName: String?, bytes: ByteArray): Boolean {
        if (bytes.size >= 4 && bytes.copyOfRange(0, 4).contentEquals(ZIP_MAGIC)) return true
        return fileName?.endsWith(".zip", ignoreCase = true) == true
    }

    /** Packs every dataset into one zip. */
    fun pack(snapshot: BackupSnapshot): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            BackupPart.entries.forEach { part ->
                zip.putNextEntry(ZipEntry(part.fileName))
                zip.write(BackupCsv.encode(part, snapshot).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry(README_ENTRY))
            zip.write(readmeText.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    /**
     * Reads a bundle back. Entries are matched the same way single files are,
     * so a zip whose entries were renamed still restores as long as the CSV
     * headers are intact. Unknown entries (such as the readme) are ignored.
     */
    fun unpack(bytes: ByteArray): BackupSnapshot {
        var result = BackupSnapshot()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry: ZipEntry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                val content = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()

                val part = BackupCsv.detectPart(entry.name, content) ?: continue
                val piece = runCatching { BackupCsv.decode(part, content) }.getOrNull() ?: continue
                result = BackupSnapshot(
                    logEntries = result.logEntries + piece.logEntries,
                    medications = result.medications + piece.medications,
                    medicationLogs = result.medicationLogs + piece.medicationLogs
                )
            }
        }
        return result
    }
}
