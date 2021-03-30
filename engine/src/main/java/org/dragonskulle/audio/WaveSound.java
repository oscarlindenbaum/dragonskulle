/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import com.sun.media.sound.WaveFileReader;

import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.lwjgl.openal.AL11;

/**
 * This class is used to load .wav files and create OpenAL buffers with them
 *
 * @author Harry Stoltz
 *     <p>Wave files are loaded by calling the static method loadWave and passing in a File that is
 *     to be parsed. If the file is of the correct format and can be read, a new WaveSound object is
 *     created and all of the fields will be filled in. The audio bytes will be read, processed and
 *     then buffered using alBufferData.
 */
@Log
public class WaveSound implements Serializable {

    public int buffer;
    public int sampleRate;
    public int format;
    public float length;
    public int bits;
    public int channels;

    public void setALFormat() {
        switch (bits) {
            case 16:
                if (channels > 1) {
                    format = AL11.AL_FORMAT_STEREO16;
                } else {
                    format = AL11.AL_FORMAT_MONO16;
                }
                break;
            case 8:
                if (channels > 1) {
                    format = AL11.AL_FORMAT_STEREO8;
                } else {
                    format = AL11.AL_FORMAT_MONO8;
                }
        }
    }

    /**
     * Fix up the raw audio bytes and get them into a format that OpenAL can play.
     *
     * @param rawBytes Raw audio bytes to process
     * @param eightBitAudio Whether the sample size is 8 bits
     * @param order The endianness of the audio bytes
     * @return ByteBuffer containing the fixed bytes
     */
    private static ByteBuffer processRawBytes(
            byte[] rawBytes, boolean eightBitAudio, ByteOrder order) {
        ByteBuffer dst = ByteBuffer.allocateDirect(rawBytes.length);
        dst.order(ByteOrder.nativeOrder());
        ByteBuffer src = ByteBuffer.wrap(rawBytes);
        src.order(order);

        if (eightBitAudio) {
            while (src.hasRemaining()) {
                dst.put(src.get());
            }
        } else {
            ShortBuffer srcBuffer = src.asShortBuffer();
            ShortBuffer dstBuffer = dst.asShortBuffer();

            while (srcBuffer.hasRemaining()) {
                dstBuffer.put(srcBuffer.get());
            }
        }
        dst.rewind();
        return dst;
    }

    /**
     * Parses a .wav file from a FileInputStream. This is really slow so ideally all sounds should
     * be loaded straight away instead of during gameplay
     *
     * @param file .wav File to parse
     * @return A WaveSound object if file could be parsed, null otherwise
     */
    public static WaveSound loadWave(File file) {
        try {
            AudioInputStream audioInputStream = new WaveFileReader().getAudioInputStream(file);

            WaveSound sound = new WaveSound();
            AudioFormat format = audioInputStream.getFormat();

            sound.sampleRate = (int) format.getSampleRate();

            sound.bits = format.getSampleSizeInBits();
            sound.channels = format.getChannels();
            sound.setALFormat();

            int audioLength = (int) audioInputStream.getFrameLength() * format.getFrameSize();

            byte[] audioBytes = new byte[audioLength];

            // TODO: Probably isn't the best way to do this
            int bytesRead = audioInputStream.read(audioBytes);
            if (audioLength != bytesRead) {
                log.warning("Failed to read in expected number of audio bytes");
                return null;
            }

            sound.length = (float) bytesRead / format.getSampleRate();

            if (sound.bits == 16) {
                sound.length /= 2;
            }

            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer buffer = processRawBytes(audioBytes, sound.bits == 8, order);

            sound.buffer = AL11.alGenBuffers();
            AL11.alBufferData(sound.buffer, sound.format, buffer, sound.sampleRate);

            return sound;
        } catch (UnsupportedAudioFileException e) {
            log.warning("Attempted to load unsupported audio file");
        } catch (IOException e) {
            log.warning("Attempted to load file that doesn't exist");
        }
        return null;
    }
}
