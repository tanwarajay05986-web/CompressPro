package com.akay.compresspro

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// "Special" mode: every file is made at most this big. 200 KB = 200,000 bytes, which is
// what the phone's file manager also counts (1 KB = 1000 bytes).
const val SPECIAL_BYTES = 200_000

// settings chosen on the Compression Settings screen
class Opts {
    var quality = 60
    var scale = 100
    var exact = false
    var exactW = 0
    var exactH = 0
    var fmt = 0
    var pdf = false
    var special = false

    fun copy(): Opts {
        val n = Opts()
        n.quality = quality
        n.scale = scale
        n.exact = exact
        n.exactW = exactW
        n.exactH = exactH
        n.fmt = fmt
        n.pdf = pdf
        n.special = special
        return n
    }
}

// one picked image or PDF
class Item(val uri: Uri, val name: String, val size: Long, val pdf: Boolean) {
    var w = 0
    var h = 0
    var pages = 0
    var mime = "image/jpeg"
    var path = ""
    var thumb: Bitmap? = null
    var sample: Bitmap? = null
    var outFile: File? = null
}

object Engine {

    // ---------- loading ----------

    fun rotationOf(ctx: Context, uri: Uri): Int {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Throwable) {
            0
        }
    }

    private fun rotate(b: Bitmap, rot: Int): Bitmap {
        if (rot == 0) return b
        val m = Matrix()
        m.postRotate(rot.toFloat())
        val r = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        if (r !== b) b.recycle()
        return r
    }

    private fun decodeSmall(ctx: Context, uri: Uri, maxSide: Int, rot: Int): Bitmap? {
        val bo = BitmapFactory.Options()
        bo.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bo) }
        if (bo.outWidth <= 0 || bo.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bo.outWidth, bo.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sample
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
            ?: return null
        return rotate(bmp, rot)
    }

    // reads name, size, dimensions and a small preview. Call it on a background thread.
    fun load(ctx: Context, uri: Uri, pdf: Boolean): Item? {
        return try {
            var name = "file"
            var size = 0L
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
            if (size <= 0L) {
                size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
                if (size < 0L) size = 0L
            }
            if (pdf) loadPdf(ctx, uri, name, size) else loadImage(ctx, uri, name, size)
        } catch (e: Throwable) {
            null
        }
    }

    private fun loadImage(ctx: Context, uri: Uri, name: String, size: Long): Item? {
        val bo = BitmapFactory.Options()
        bo.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bo) }
        if (bo.outWidth <= 0 || bo.outHeight <= 0) return null
        val rot = rotationOf(ctx, uri)
        val item = Item(uri, name, size, false)
        item.mime = bo.outMimeType ?: "image/jpeg"
        if (rot == 90 || rot == 270) {
            item.w = bo.outHeight
            item.h = bo.outWidth
        } else {
            item.w = bo.outWidth
            item.h = bo.outHeight
        }
        val s = decodeSmall(ctx, uri, 400, rot) ?: return null
        item.sample = s
        item.thumb = s
        return item
    }

    private fun loadPdf(ctx: Context, uri: Uri, name: String, size: Long): Item? {
        val f = File(ctx.cacheDir, "in_" + System.nanoTime() + ".pdf")
        ctx.contentResolver.openInputStream(uri)?.use { i ->
            FileOutputStream(f).use { o -> i.copyTo(o) }
        } ?: return null
        val r = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
        try {
            if (r.pageCount <= 0) return null
            val item = Item(uri, name, if (size > 0L) size else f.length(), true)
            item.path = f.absolutePath
            item.pages = r.pageCount
            val pg = r.openPage(0)
            val pw = pg.width.toFloat()
            val ph = pg.height.toFloat()
            item.w = Math.round(pw * 150f / 72f)
            item.h = Math.round(ph * 150f / 72f)
            val bw = 360
            val bh = maxOf(1, Math.round(bw * ph / pw))
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pg.close()
            item.sample = bmp
            item.thumb = bmp
            return item
        } finally {
            r.close()
        }
    }

    // ---------- previews of the finished files ----------

    private fun exifRot(f: File): Int {
        return try {
            when (ExifInterface(f.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Throwable) {
            0
        }
    }

    // width and height of a finished image
    fun dimsOf(f: File): IntArray {
        val bo = BitmapFactory.Options()
        bo.inJustDecodeBounds = true
        BitmapFactory.decodeFile(f.absolutePath, bo)
        val r = exifRot(f)
        return if (r == 90 || r == 270) intArrayOf(bo.outHeight, bo.outWidth) else intArrayOf(bo.outWidth, bo.outHeight)
    }

    fun pageCountOf(f: File): Int {
        return try {
            val r = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
            val n = r.pageCount
            r.close()
            n
        } catch (e: Throwable) {
            0
        }
    }

    // small picture of a finished image (or of the first page of a finished PDF)
    fun previewOf(f: File, pdf: Boolean): Bitmap? {
        return try {
            if (pdf) {
                val r = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
                try {
                    val pg = r.openPage(0)
                    val bw = 420
                    val bh = maxOf(1, Math.round(bw * pg.height.toFloat() / pg.width))
                    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pg.close()
                    bmp
                } finally {
                    r.close()
                }
            } else {
                val bo = BitmapFactory.Options()
                bo.inJustDecodeBounds = true
                BitmapFactory.decodeFile(f.absolutePath, bo)
                if (bo.outWidth <= 0 || bo.outHeight <= 0) return null
                var sample = 1
                while (maxOf(bo.outWidth, bo.outHeight) / (sample * 2) >= 500) sample *= 2
                val o2 = BitmapFactory.Options()
                o2.inSampleSize = sample
                val bmp = BitmapFactory.decodeFile(f.absolutePath, o2) ?: return null
                rotate(bmp, exifRot(f))
            }
        } catch (e: Throwable) {
            null
        }
    }

    // ---------- sizes and formats ----------

    // size of the result for one file (for PDF: the page size at 150 dpi)
    fun outDims(item: Item, o: Opts): IntArray {
        if (!item.pdf && o.exact && o.exactW > 0 && o.exactH > 0) return intArrayOf(o.exactW, o.exactH)
        val sc = o.scale / 100.0
        return intArrayOf(
            maxOf(1, Math.round(item.w * sc).toInt()),
            maxOf(1, Math.round(item.h * sc).toInt())
        )
    }

    // 0 JPG, 1 PNG, 2 WEBP (Original = same type as the picture)
    fun fmtOf(fmt: Int, mime: String): Int {
        if (fmt != 3) return fmt
        if (mime.contains("png")) return 1
        if (mime.contains("webp")) return 2
        return 0
    }

    @Suppress("DEPRECATION")
    fun cf(fmt: Int, mime: String): Bitmap.CompressFormat = when (fmtOf(fmt, mime)) {
        1 -> Bitmap.CompressFormat.PNG
        2 -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
    }

    fun ext(fmt: Int, mime: String): String = when (fmtOf(fmt, mime)) {
        1 -> "png"
        2 -> "webp"
        else -> "jpg"
    }

    private fun origExt(item: Item): String {
        val e = item.name.substringAfterLast('.', "").lowercase(Locale.US)
        if (e.isNotEmpty() && e.length <= 5) return e
        if (item.pdf) return "pdf"
        if (item.mime.contains("png")) return "png"
        if (item.mime.contains("webp")) return "webp"
        return "jpg"
    }

    fun mimeOf(f: File): String = when (f.extension.lowercase(Locale.US)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> "image/jpeg"
    }

    // sizes are counted like the phone's file manager does: 1 KB = 1000 bytes
    fun fmtSize(b: Long): Pair<String, String> {
        val kb = b / 1000.0
        val mb = kb / 1000.0
        return when {
            mb >= 1.0 -> Pair(String.format(Locale.US, "%.1f", mb), "MB")
            kb >= 1.0 -> Pair(String.format(Locale.US, "%.0f", kb), "KB")
            else -> Pair(b.toString(), "B")
        }
    }

    // ---------- estimate ----------

    private fun encodedSize(b: Bitmap, f: Bitmap.CompressFormat, q: Int): Int {
        val bos = ByteArrayOutputStream()
        b.compress(f, q, bos)
        return bos.size()
    }

    private fun pdfBytes(item: Item, s: Bitmap, scale: Int, q: Int): Long {
        val tw = item.w * scale / 100.0
        val th = item.h * scale / 100.0
        val per = encodedSize(s, Bitmap.CompressFormat.JPEG, q) * (tw * th) /
            (s.width.toDouble() * s.height.toDouble()) * 0.8
        return maxOf(2000L, (per * item.pages + 1500.0).toLong())
    }

    private fun estimateWith(item: Item, o: Opts, quality: Int, fmt: Int): Long {
        val s = item.sample ?: return item.size
        if (item.pdf) return pdfBytes(item, s, o.scale, quality)
        val d = outDims(item, o)
        val target = d[0].toDouble() * d[1].toDouble()
        val samplePx = s.width.toDouble() * s.height.toDouble()
        val f = cf(fmt, item.mime)
        var bytes = encodedSize(s, f, quality) * target / samplePx
        if (f != Bitmap.CompressFormat.PNG) bytes *= 0.8
        return maxOf(1500L, bytes.toLong())
    }

    // quick guess of the result size. It never says more than the original size.
    fun estimate(item: Item, o: Opts): Long {
        var r = estimateWith(item, o, if (o.special) 90 else o.quality, if (o.special) 0 else o.fmt)
        if (o.special) r = minOf(r, SPECIAL_BYTES.toLong())
        if (item.size > 0) r = minOf(r, item.size)
        return maxOf(1000L, r)
    }

    // ---------- compress an image ----------

    private fun jpegBytes(b: Bitmap, q: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, q, bos)
        return bos.toByteArray()
    }

    // finds the best JPEG quality that keeps the file under `target` bytes. If even the lowest
    // quality is too big, the picture is made a little smaller and tried again.
    private fun fitJpeg(b0: Bitmap, target: Int): ByteArray {
        var cur = b0
        var owned: Bitmap? = null
        var result: ByteArray? = null
        var tries = 0
        while (tries < 8) {
            tries++
            val low = jpegBytes(cur, 8)
            if (low.size > target) {
                val k = Math.sqrt(target.toDouble() / low.size) * 0.92
                val nw = maxOf(8, Math.round(cur.width * k).toInt())
                val nh = maxOf(8, Math.round(cur.height * k).toInt())
                val s = Bitmap.createScaledBitmap(cur, nw, nh, true)
                val old = owned
                if (old != null) old.recycle()
                owned = s
                cur = s
                result = low
                continue
            }
            var lo = 8
            var hi = 95
            var best = low
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                val bt = jpegBytes(cur, mid)
                if (bt.size <= target) {
                    lo = mid
                    best = bt
                } else {
                    hi = mid - 1
                }
            }
            result = best
            break
        }
        val last = owned
        if (last != null) last.recycle()
        return result ?: jpegBytes(b0, 8)
    }

    fun compressImage(
        ctx: Context, item: Item, o: Opts, dir: File, idx: Int,
        cancelled: () -> Boolean, prog: (Float) -> Unit
    ): File? {
        prog(0.05f)
        val d = outDims(item, o)
        val rot = rotationOf(ctx, item.uri)
        val bo = BitmapFactory.Options()
        bo.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, bo) }
        if (bo.outWidth <= 0 || bo.outHeight <= 0) return null
        val needW = if (rot == 90 || rot == 270) d[1] else d[0]
        val needH = if (rot == 90 || rot == 270) d[0] else d[1]
        var sample = 1
        while (bo.outWidth / (sample * 2) >= needW && bo.outHeight / (sample * 2) >= needH) sample *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sample
        var bmp = ctx.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
            ?: return null
        if (cancelled()) {
            bmp.recycle()
            return null
        }
        prog(0.3f)
        bmp = rotate(bmp, rot)
        if (bmp.width != d[0] || bmp.height != d[1]) {
            val sc = Bitmap.createScaledBitmap(bmp, d[0], d[1], true)
            if (sc !== bmp) bmp.recycle()
            bmp = sc
        }
        prog(0.6f)
        val useJpeg = o.special || cf(o.fmt, item.mime) == Bitmap.CompressFormat.JPEG
        if (useJpeg && bmp.hasAlpha()) {
            val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val cv = Canvas(flat)
            cv.drawColor(Color.WHITE)
            cv.drawBitmap(bmp, 0f, 0f, null)
            bmp.recycle()
            bmp = flat
        }
        if (cancelled()) {
            bmp.recycle()
            return null
        }
        val base = item.name.substringBeforeLast('.', item.name)
        val e = if (o.special) "jpg" else ext(o.fmt, item.mime)
        var out = File(dir, base + "_compressed." + e)
        if (out.exists()) out = File(dir, base + "_compressed_" + idx + "." + e)
        if (o.special) {
            out.writeBytes(fitJpeg(bmp, SPECIAL_BYTES))
        } else {
            val f = cf(o.fmt, item.mime)
            BufferedOutputStream(FileOutputStream(out)).use { bmp.compress(f, o.quality, it) }
        }
        bmp.recycle()
        prog(0.95f)

        // the result must never be bigger than the original: then the original file is given back
        if (item.size > 0 && out.length() >= item.size) {
            val oe = origExt(item)
            var keep = File(dir, base + "_compressed." + oe)
            if (keep.exists() && keep.absolutePath != out.absolutePath) {
                keep = File(dir, base + "_compressed_" + idx + "." + oe)
            }
            ctx.contentResolver.openInputStream(item.uri)?.use { i ->
                FileOutputStream(keep).use { w -> i.copyTo(w) }
            }
            if (keep.absolutePath != out.absolutePath) out.delete()
            if (keep.exists() && keep.length() > 0) {
                prog(1f)
                return keep
            }
        }
        prog(1f)
        return out
    }

    // ---------- compress a PDF (every page is drawn again as a JPEG) ----------

    // writes one PDF. Returns false when it was cancelled.
    private fun writePdf(
        item: Item, scale: Int, quality: Int, out: File,
        cancelled: () -> Boolean, prog: (Float) -> Unit
    ): Boolean {
        val r = PdfRenderer(ParcelFileDescriptor.open(File(item.path), ParcelFileDescriptor.MODE_READ_ONLY))
        val os = BufferedOutputStream(FileOutputStream(out))
        try {
            val n = r.pageCount
            val offsets = ArrayList<Long>()
            var pos = 0L
            fun w(s: String) {
                val b = s.toByteArray(Charsets.ISO_8859_1)
                os.write(b)
                pos += b.size
            }
            fun wb(b: ByteArray) {
                os.write(b)
                pos += b.size
            }
            w("%PDF-1.4\n")
            offsets.add(pos)
            w("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            offsets.add(pos)
            val kids = (0 until n).joinToString(" ") { "${3 + 3 * it} 0 R" }
            w("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $n >>\nendobj\n")
            for (i in 0 until n) {
                if (cancelled()) return false
                val pg = r.openPage(i)
                val pw = pg.width.toFloat()
                val ph = pg.height.toFloat()
                var bw = maxOf(8, Math.round(pw * 150f / 72f * scale / 100f))
                var bh = maxOf(8, Math.round(ph * 150f / 72f * scale / 100f))
                val big = maxOf(bw, bh)
                if (big > 3200) {
                    val k = 3200f / big
                    bw = maxOf(8, Math.round(bw * k))
                    bh = maxOf(8, Math.round(bh * k))
                }
                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                pg.close()
                val jpg = jpegBytes(bmp, quality)
                bmp.recycle()
                val po = 3 + 3 * i
                offsets.add(pos)
                w("$po 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pw $ph] /Resources << /XObject << /Im0 ${po + 2} 0 R >> >> /Contents ${po + 1} 0 R >>\nendobj\n")
                val content = "q $pw 0 0 $ph 0 0 cm /Im0 Do Q"
                offsets.add(pos)
                w("${po + 1} 0 obj\n<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n")
                offsets.add(pos)
                w("${po + 2} 0 obj\n<< /Type /XObject /Subtype /Image /Width $bw /Height $bh /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpg.size} >>\nstream\n")
                wb(jpg)
                w("\nendstream\nendobj\n")
                prog((i + 1f) / n)
            }
            val xrefPos = pos
            w("xref\n0 ${offsets.size + 1}\n")
            w("0000000000 65535 f \n")
            for (off in offsets) w(String.format(Locale.US, "%010d 00000 n \n", off))
            w("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
            os.flush()
            return true
        } finally {
            try {
                os.close()
            } catch (e: Throwable) {
            }
            r.close()
        }
    }

    // Special mode: picks a page size and quality that should give about 200 KB
    private fun pickPdf(item: Item, startScale: Int): IntArray {
        val s = item.sample ?: return intArrayOf(startScale, 30)
        val qs = intArrayOf(80, 65, 50, 38, 28, 20, 14)
        var sc = startScale
        var guard = 0
        while (guard < 14) {
            guard++
            for (q in qs) {
                if (pdfBytes(item, s, sc, q) <= SPECIAL_BYTES * 0.9) return intArrayOf(sc, q)
            }
            sc = maxOf(8, (sc * 0.82).toInt())
            if (sc <= 8) break
        }
        return intArrayOf(sc, 14)
    }

    fun compressPdf(
        ctx: Context, item: Item, o: Opts, dir: File, idx: Int,
        cancelled: () -> Boolean, prog: (Float) -> Unit
    ): File? {
        val base = item.name.substringBeforeLast('.', item.name)
        var out = File(dir, base + "_compressed.pdf")
        if (out.exists()) out = File(dir, base + "_compressed_" + idx + ".pdf")
        var scale = o.scale
        var q = o.quality
        if (o.special) {
            val pick = pickPdf(item, o.scale)
            scale = pick[0]
            q = pick[1]
        }
        if (!writePdf(item, scale, q, out, cancelled, prog)) {
            out.delete()
            return null
        }
        if (o.special) {
            var tries = 0
            while (out.length() > SPECIAL_BYTES && tries < 6 && !cancelled()) {
                tries++
                val k = Math.sqrt(SPECIAL_BYTES.toDouble() / out.length()) * 0.93
                scale = maxOf(8, minOf(scale - 1, (scale * k).toInt()))
                q = maxOf(15, q - 4)
                if (!writePdf(item, scale, q, out, cancelled, prog)) {
                    out.delete()
                    return null
                }
            }
        }
        // the result must never be bigger than the original: then the original file is given back
        if (item.size > 0 && out.length() >= item.size) {
            File(item.path).copyTo(out, true)
        }
        return out
    }

    // ---------- save and share ----------

    // copies a result into Pictures or Downloads. Returns the folder name, or null on failure.
    fun saveToDevice(ctx: Context, f: File, storage: Int): String? {
        return try {
            val mime = mimeOf(f)
            val isPdf = mime == "application/pdf"
            val useDownloads = isPdf || storage == 1
            val rel = (if (useDownloads) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES) + "/CompressPro"
            val cv = ContentValues()
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            val base = if (useDownloads) MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = ctx.contentResolver.insert(base, cv) ?: return null
            val ok = ctx.contentResolver.openOutputStream(uri)?.use { o ->
                f.inputStream().use { it.copyTo(o) }
                true
            } ?: false
            if (ok) rel else null
        } catch (e: Throwable) {
            null
        }
    }

    fun share(act: Activity, files: List<File>) {
        val uris = ArrayList<Uri>()
        for (f in files) uris.add(FileProvider.getUriForFile(act, act.packageName + ".fileprovider", f))
        val i = if (uris.size == 1) {
            val one = Intent(Intent.ACTION_SEND)
            one.putExtra(Intent.EXTRA_STREAM, uris[0])
            one
        } else {
            val many = Intent(Intent.ACTION_SEND_MULTIPLE)
            many.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            many
        }
        i.type = if (files[0].extension.equals("pdf", true)) "application/pdf" else "image/*"
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        act.startActivity(Intent.createChooser(i, "Share"))
    }
}
