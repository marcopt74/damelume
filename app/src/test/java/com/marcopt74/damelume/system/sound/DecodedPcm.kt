package com.marcopt74.damelume.system.sound

import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import java.io.ByteArrayInputStream

internal data class DecodedPcm(
    val sampleRate: Int,
    val samples: DoubleArray,
)

internal fun decodeVorbis(ogg: ByteArray): DecodedPcm = VorbisDecoder(ogg).decode()

private class VorbisDecoder(
    ogg: ByteArray,
) {
    private val input = ByteArrayInputStream(ogg)
    private val sync = SyncState()
    private val stream = StreamState()
    private val page = Page()
    private val packet = Packet()
    private val info = Info()
    private val comment = Comment()
    private val dsp = DspState()
    private val block = Block(dsp)

    fun decode(): DecodedPcm {
        readHeaders()
        dsp.synthesis_init(info)
        block.init(dsp)
        val mono = ArrayList<Double>()
        readAudio(mono)
        return DecodedPcm(info.rate, mono.toDoubleArray())
    }

    private fun readHeaders() {
        fill(required = true)
        if (sync.pageout(page) != 1) error("Chime is not an Ogg stream")
        stream.init(page.serialno())
        info.init()
        comment.init()
        if (stream.pagein(page) < 0) error("Ogg page rejected")
        if (stream.packetout(packet) != 1) error("Vorbis header missing")
        if (info.synthesis_headerin(comment, packet) < 0) error("Not a Vorbis stream")
        var headers = 0
        while (headers < 2) {
            headers += consumeHeaderPage()
        }
    }

    private fun consumeHeaderPage(): Int {
        val pageResult = awaitPage()
        if (pageResult < 0) error("Corrupt Ogg page")
        stream.pagein(page)
        return consumeHeaderPackets()
    }

    private fun awaitPage(): Int {
        var pageResult = sync.pageout(page)
        while (pageResult == 0) {
            fill(required = true)
            pageResult = sync.pageout(page)
        }
        return pageResult
    }

    private fun consumeHeaderPackets(): Int {
        var accepted = 0
        var packetResult = stream.packetout(packet)
        while (packetResult != 0) {
            if (packetResult < 0) error("Corrupt Vorbis header")
            info.synthesis_headerin(comment, packet)
            accepted += 1
            packetResult = stream.packetout(packet)
        }
        return accepted
    }

    private fun readAudio(mono: ArrayList<Double>) {
        var open = true
        while (open) {
            open =
                when (pullPage()) {
                    PageStep.NEED_MORE -> true
                    PageStep.END -> false
                    PageStep.HOLE -> true
                    PageStep.DATA -> {
                        stream.pagein(page)
                        appendPackets(mono)
                        page.eos() == 0
                    }
                }
        }
    }

    private fun pullPage(): PageStep {
        val result = sync.pageout(page)
        if (result > 0) return PageStep.DATA
        if (result < 0) return PageStep.HOLE
        return if (fill(required = false) == 0) PageStep.END else PageStep.NEED_MORE
    }

    private fun appendPackets(mono: ArrayList<Double>) {
        val pcm = arrayOfNulls<Array<FloatArray>>(1)
        val index = IntArray(info.channels)
        var packetResult = stream.packetout(packet)
        while (packetResult != 0) {
            if (packetResult > 0 && block.synthesis(packet) == 0) {
                dsp.synthesis_blockin(block)
                appendPcm(pcm, index, mono)
            }
            packetResult = stream.packetout(packet)
        }
    }

    private fun appendPcm(
        pcm: Array<Array<FloatArray>?>,
        index: IntArray,
        mono: ArrayList<Double>,
    ) {
        var samples = dsp.synthesis_pcmout(pcm, index)
        while (samples > 0) {
            val channel = pcm[0]!![0]
            val start = index[0]
            for (sample in 0 until samples) {
                mono += channel[start + sample].toDouble()
            }
            dsp.synthesis_read(samples)
            samples = dsp.synthesis_pcmout(pcm, index)
        }
    }

    private fun fill(required: Boolean): Int {
        val offset = sync.buffer(CHUNK)
        val read = input.read(sync.data, offset, CHUNK)
        val count = if (read < 0) 0 else read
        sync.wrote(count)
        if (required && count == 0) error("Unexpected end of chime")
        return count
    }

    private enum class PageStep {
        DATA,
        HOLE,
        NEED_MORE,
        END,
    }

    private companion object {
        const val CHUNK = 8192
    }
}
