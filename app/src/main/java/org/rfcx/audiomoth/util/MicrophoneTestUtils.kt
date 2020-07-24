package org.rfcx.audiomoth.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log

class MicrophoneTestUtils {
    private val sampleRate = 24000
    private val channelConfiguration = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBufSize = AudioTrack.getMinBufferSize(
        sampleRate,
        channelConfiguration,
        audioEncoding
    )
    private var audioTrack: AudioTrack? = null

    var buffer = ByteArray(minBufSize)
    var readSize = 0

    fun init() {
        Log.d("MICROPHONE", "init")
        stop()
        release()

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
        Log.d("MICROPHONE", "set")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    fun play() {
        Log.d("MICROPHONE", "play")
        audioTrack?.play()
    }

    fun stop() {
        Log.d("MICROPHONE", "stop")
        audioTrack?.stop()
        audioTrack?.flush()
    }

    fun release() {
        Log.d("MICROPHONE", "release")
        audioTrack?.release()
        audioTrack = null
    }
}
