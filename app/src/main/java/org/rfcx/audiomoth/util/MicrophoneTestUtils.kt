package org.rfcx.audiomoth.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Base64

class MicrophoneTestUtils {
    private val sampleRate = 24000
    private val channelConfiguration = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var audioTrack: AudioTrack? = null
    private var minBufSize: Int? = null
    private val DEF_MINBUFSIZE = 5760

    var buffer = ByteArray(0)

    fun init(minBufSize: Int) {
        stop()
        release()

        this.minBufSize = minBufSize

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioEncoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfiguration)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfiguration,
                audioEncoding,
                minBufSize,
                AudioTrack.MODE_STREAM
            )
        }
    }

    fun setTrack() {
        audioTrack?.write(buffer, 0, minBufSize ?: DEF_MINBUFSIZE)
    }

    fun play() {
        audioTrack?.play()
    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.flush()
    }

    fun release() {
        audioTrack?.release()
        audioTrack = null
    }

    /*
     * Utils functions for Audio Byte Array
     */
    fun decodeEncodedAudio(encodedAudio: String): ByteArray {
        return Base64.decode(encodedAudio, Base64.NO_WRAP)
    }

    fun getEncodedAudioBufferSize(encodedAudio: String): Int {
        return decodeEncodedAudio(encodedAudio).size
    }
}
