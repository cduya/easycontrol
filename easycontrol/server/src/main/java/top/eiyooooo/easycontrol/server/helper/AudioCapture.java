package top.eiyooooo.easycontrol.server.helper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.media.*;
import android.os.Build;
import android.os.Looper;
import android.os.Parcel;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AudioCapture {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int CHANNELS = 2;
    public static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_LEFT | AudioFormat.CHANNEL_IN_RIGHT;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BYTES_PER_SAMPLE = 2;

    public static AudioRecord init() {
        AudioRecord recorder;
        try {
            recorder = createAudioRecord();
        } catch (NullPointerException e) {
            L.w("Cannot create AudioRecord, try workaround");
            recorder = createAudioRecordWorkaround();
        }
        recorder.startRecording();
        return recorder;
    }

    public static int millisToBytes(int millis) {
        return (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE / 1000) * millis;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecord() {
        AudioRecord.Builder audioRecordBuilder = new AudioRecord.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioRecordBuilder.setContext(FakeContext.get());

        audioRecordBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
        AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
        audioFormatBuilder.setEncoding(ENCODING);
        audioFormatBuilder.setSampleRate(SAMPLE_RATE);
        audioFormatBuilder.setChannelMask(CHANNEL_CONFIG);
        audioRecordBuilder.setAudioFormat(audioFormatBuilder.build());
        audioRecordBuilder.setBufferSizeInBytes(16 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING));
        return audioRecordBuilder.build();
    }

    @TargetApi(Build.VERSION_CODES.R)
    @SuppressLint("WrongConstant,MissingPermission,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
    private static AudioRecord createAudioRecordWorkaround() {
        try {
            // AudioRecord audioRecord = new AudioRecord(0L);
            Constructor<AudioRecord> audioRecordConstructor = AudioRecord.class.getDeclaredConstructor(long.class);
            audioRecordConstructor.setAccessible(true);
            AudioRecord audioRecord = audioRecordConstructor.newInstance(0L);

            // audioRecord.mRecordingState = RECORDSTATE_STOPPED;
            Field mRecordingStateField = AudioRecord.class.getDeclaredField("mRecordingState");
            mRecordingStateField.setAccessible(true);
            mRecordingStateField.set(audioRecord, AudioRecord.RECORDSTATE_STOPPED);

            Looper looper = Looper.myLooper();
            if (looper == null) {
                looper = Looper.getMainLooper();
            }

            // audioRecord.mInitializationLooper = looper;
            Field mInitializationLooperField = AudioRecord.class.getDeclaredField("mInitializationLooper");
            mInitializationLooperField.setAccessible(true);
            mInitializationLooperField.set(audioRecord, looper);

            // Create `AudioAttributes` with fixed capture preset
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
            Method setInternalCapturePresetMethod = AudioAttributes.Builder.class.getMethod("setInternalCapturePreset", int.class);
            setInternalCapturePresetMethod.invoke(audioAttributesBuilder, MediaRecorder.AudioSource.REMOTE_SUBMIX);
            AudioAttributes attributes = audioAttributesBuilder.build();

            // audioRecord.mAudioAttributes = attributes;
            Field mAudioAttributesField = AudioRecord.class.getDeclaredField("mAudioAttributes");
            mAudioAttributesField.setAccessible(true);
            mAudioAttributesField.set(audioRecord, attributes);

            // audioRecord.audioParamCheck(capturePreset, sampleRate, encoding);
            Method audioParamCheckMethod = AudioRecord.class.getDeclaredMethod("audioParamCheck", int.class, int.class, int.class);
            audioParamCheckMethod.setAccessible(true);
            audioParamCheckMethod.invoke(audioRecord, MediaRecorder.AudioSource.REMOTE_SUBMIX, SAMPLE_RATE, ENCODING);

            // audioRecord.mChannelCount = channels
            Field mChannelCountField = AudioRecord.class.getDeclaredField("mChannelCount");
            mChannelCountField.setAccessible(true);
            mChannelCountField.set(audioRecord, CHANNELS);

            // audioRecord.mChannelMask = channelMask
            Field mChannelMaskField = AudioRecord.class.getDeclaredField("mChannelMask");
            mChannelMaskField.setAccessible(true);
            mChannelMaskField.set(audioRecord, CHANNEL_MASK);

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
            int bufferSizeInBytes = minBufferSize * 8;

            // audioRecord.audioBuffSizeCheck(bufferSizeInBytes)
            Method audioBuffSizeCheckMethod = AudioRecord.class.getDeclaredMethod("audioBuffSizeCheck", int.class);
            audioBuffSizeCheckMethod.setAccessible(true);
            audioBuffSizeCheckMethod.invoke(audioRecord, bufferSizeInBytes);

            final int channelIndexMask = 0;

            int[] sampleRateArray = new int[]{SAMPLE_RATE};
            int[] session = new int[]{AudioManager.AUDIO_SESSION_ID_GENERATE};

            int initResult;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // private native final int native_setup(Object audiorecord_this,
                // Object /*AudioAttributes*/ attributes,
                // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
                // int buffSizeInBytes, int[] sessionId, String opPackageName,
                // long nativeRecordInJavaObj);
                Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class, int.class,
                        int.class, int.class, int.class, int[].class, String.class, long.class);
                nativeSetupMethod.setAccessible(true);
                initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes, sampleRateArray,
                        CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session, FakeContext.get().getOpPackageName(),
                        0L);
            } else {
                // Assume `context` is never `null`
                AttributionSource attributionSource = FakeContext.get().getAttributionSource();

                // Assume `attributionSource.getPackageName()` is never null

                // ScopedParcelState attributionSourceState = attributionSource.asScopedParcelState()
                Method asScopedParcelStateMethod = AttributionSource.class.getDeclaredMethod("asScopedParcelState");
                asScopedParcelStateMethod.setAccessible(true);

                try (AutoCloseable attributionSourceState = (AutoCloseable) asScopedParcelStateMethod.invoke(attributionSource)) {
                    Method getParcelMethod = attributionSourceState.getClass().getDeclaredMethod("getParcel");
                    Parcel attributionSourceParcel = (Parcel) getParcelMethod.invoke(attributionSourceState);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        // private native int native_setup(Object audiorecordThis,
                        // Object /*AudioAttributes*/ attributes,
                        // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
                        // int buffSizeInBytes, int[] sessionId, @NonNull Parcel attributionSource,
                        // long nativeRecordInJavaObj, int maxSharedAudioHistoryMs);
                        Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class,
                                int.class, int.class, int.class, int.class, int[].class, Parcel.class, long.class, int.class);
                        nativeSetupMethod.setAccessible(true);
                        initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes,
                                sampleRateArray, CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session,
                                attributionSourceParcel, 0L, 0);
                    } else {
                        // Android 14 added a new int parameter "halInputFlags"
                        // <https://github.com/aosp-mirror/platform_frameworks_base/commit/f6135d75db79b1d48fad3a3b3080d37be20a2313>
                        Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class,
                                int.class, int.class, int.class, int.class, int[].class, Parcel.class, long.class, int.class, int.class);
                        nativeSetupMethod.setAccessible(true);
                        initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes,
                                sampleRateArray, CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session,
                                attributionSourceParcel, 0L, 0, 0);
                    }
                }
            }

            if (initResult != AudioRecord.SUCCESS) {
                L.w("AudioRecord initialization failed with code " + initResult);
                throw new RuntimeException("Cannot create AudioRecord");
            }

            // mSampleRate = sampleRate[0]
            Field mSampleRateField = AudioRecord.class.getDeclaredField("mSampleRate");
            mSampleRateField.setAccessible(true);
            mSampleRateField.set(audioRecord, sampleRateArray[0]);

            // audioRecord.mSessionId = session[0]
            Field mSessionIdField = AudioRecord.class.getDeclaredField("mSessionId");
            mSessionIdField.setAccessible(true);
            mSessionIdField.set(audioRecord, session[0]);

            // audioRecord.mState = AudioRecord.STATE_INITIALIZED
            Field mStateField = AudioRecord.class.getDeclaredField("mState");
            mStateField.setAccessible(true);
            mStateField.set(audioRecord, AudioRecord.STATE_INITIALIZED);

            return audioRecord;
        } catch (Exception e) {
            L.e("Cannot create AudioRecord", e);
            throw new RuntimeException("Cannot create AudioRecord");
        }
    }
}
