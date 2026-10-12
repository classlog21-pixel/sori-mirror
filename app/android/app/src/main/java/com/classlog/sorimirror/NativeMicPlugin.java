package com.classlog.sorimirror;

import android.Manifest;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * 폰 내장 마이크를 일반 녹음(미디어)으로 열어 PCM을 웹 화면에 보냄.
 * WebView의 getUserMedia는 통화용 오디오 모드(MODE_IN_COMMUNICATION, 블루투스 SCO)를 켜서
 * 차량 오디오나 블루투스 기기가 앱을 전화 통화로 잡기 때문에 이 경로를 씀.
 */
@CapacitorPlugin(name = "NativeMic", permissions = @Permission(strings = { Manifest.permission.RECORD_AUDIO }, alias = "mic"))
public class NativeMicPlugin extends Plugin {

    private AudioRecord rec;
    private Thread reader;
    private volatile boolean running;

    @PluginMethod
    public void start(PluginCall call) {
        if (getPermissionState("mic") != PermissionState.GRANTED) {
            requestPermissionForAlias("mic", call, "micPermission");
            return;
        }
        open(call);
    }

    @PermissionCallback
    private void micPermission(PluginCall call) {
        if (getPermissionState("mic") == PermissionState.GRANTED) open(call);
        else call.reject("permission denied");
    }

    @PluginMethod
    public void stop(PluginCall call) {
        close();
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        close();
    }

    private synchronized void open(PluginCall call) {
        close();
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        // 음성 처리(자동 음량, 잡음 제거)가 없는 원음 입력을 먼저 시도하고, 안 되면 일반 마이크
        boolean unprocessed = "true".equals(am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        int[] sources = unprocessed
            ? new int[] { MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC }
            : new int[] { MediaRecorder.AudioSource.MIC };
        int[] rates = { call.getInt("sampleRate", 48000), 48000, 44100 };
        AudioRecord r = null;
        int rate = 0, source = 0;
        for (int s : sources) {
            for (int sr : rates) {
                int min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (min <= 0) continue;
                try {
                    r = new AudioRecord(s, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 4, sr / 2));
                } catch (Exception e) {
                    r = null;
                }
                if (r != null && r.getState() == AudioRecord.STATE_INITIALIZED) {
                    rate = sr;
                    source = s;
                    break;
                }
                if (r != null) r.release();
                r = null;
            }
            if (r != null) break;
        }
        if (r == null) {
            call.reject("mic unavailable");
            return;
        }
        // 블루투스 기기(차량 등)의 마이크가 아니라 폰 내장 마이크를 씀
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (d.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                r.setPreferredDevice(d);
                break;
            }
        }
        try {
            r.startRecording();
        } catch (Exception e) {
            r.release();
            call.reject("mic start failed");
            return;
        }
        rec = r;
        running = true;
        final AudioRecord ar = r;
        final int chunk = rate / 25; // 40ms
        reader = new Thread(() -> {
            short[] buf = new short[chunk];
            byte[] bytes = new byte[chunk * 2];
            while (running) {
                int n = ar.read(buf, 0, chunk);
                if (n <= 0) {
                    if (n < 0) break;
                    continue;
                }
                for (int i = 0; i < n; i++) {
                    bytes[2 * i] = (byte) buf[i];
                    bytes[2 * i + 1] = (byte) (buf[i] >> 8);
                }
                JSObject o = new JSObject();
                o.put("pcm", Base64.encodeToString(bytes, 0, n * 2, Base64.NO_WRAP));
                notifyListeners("pcm", o);
            }
        }, "NativeMic");
        reader.start();
        JSObject ret = new JSObject();
        ret.put("sampleRate", rate);
        ret.put("unprocessed", source == MediaRecorder.AudioSource.UNPROCESSED);
        call.resolve(ret);
    }

    private synchronized void close() {
        running = false;
        if (reader != null) {
            try {
                reader.join(500);
            } catch (InterruptedException ignored) {}
            reader = null;
        }
        if (rec != null) {
            try {
                rec.stop();
            } catch (Exception ignored) {}
            rec.release();
            rec = null;
        }
    }
}
