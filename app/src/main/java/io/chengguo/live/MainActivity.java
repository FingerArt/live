package io.chengguo.live;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.net.URI;

import io.chengguo.streaming.RTSPClient;
import io.chengguo.streaming.codec.h264.H264Decoder;
import io.chengguo.streaming.codec.Mp3Decoder;
import io.chengguo.streaming.rtp.RtpPacket;
import io.chengguo.streaming.rtsp.IInterceptor;
import io.chengguo.streaming.rtsp.Request;
import io.chengguo.streaming.rtsp.Response;
import io.chengguo.streaming.transport.TransportMethod;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity implements View.OnClickListener, RTSPClient.IRTPPacketObserver, Mp3Decoder.Callback {

    private static final String TAG = "MainActivity";

    private SurfaceView mSurfaceView;
    private AudioTrack audioTrack;
    private RTSPClient rtspClient;
    private H264Decoder h264Decoder;
    private View wStart;
    private View wStop;
    private boolean isPause;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        createClient();

        mSurfaceView = findViewById(R.id.surface);
        findViewById(R.id.btn_connect).setOnClickListener(this);
        findViewById(R.id.btn_disconnect).setOnClickListener(this);
        wStart = findViewById(R.id.btn_start);
        wStart.setOnClickListener(this);
        wStop = findViewById(R.id.btn_stop);
        wStop.setOnClickListener(this);
        try {
//            int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
//            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//                mp3Decoder.start();
//                audioTrack.play();

                try {
                    h264Decoder = new H264Decoder(holder.getSurface(), 300, 300);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void createClient() {
        rtspClient = RTSPClient.create()
                .host("14.29.172.223")
                .transport(TransportMethod.TCP)
                .setRTPPacketObserver(this)
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtspClient.teardown();
        rtspClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                rtspClient.connect();
                break;
            case R.id.btn_disconnect:
                rtspClient.disconnect();
                break;
            case R.id.btn_start:
                rtspClient.play(URI.create("rtsp://14.29.172.223/bipbop-gear1-all.264"));
                break;
            case R.id.btn_stop:
                if (isPause) {
                    isPause = false;
                    rtspClient.resume();
                } else {
                    isPause = true;
                    rtspClient.pause();
                }
                break;
        }
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wStart.setEnabled(true);
                wStop.setEnabled(true);
            }
        });
    }

    @Override
    public void onConnectFailure(Throwable throwable) {
        Log.w(TAG, "onConnectFailure", throwable);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wStart.setEnabled(false);
                wStop.setEnabled(false);
            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wStart.setEnabled(false);
                wStop.setEnabled(false);
            }
        });
    }

    @Override
    public void onReceive(RtpPacket rtpPacket) {
        try {
//            mp3Decoder.input(rtpPacket.getPayload(), 0, rtpPacket.getPayload().length, rtpPacket.getTimestamp());
            h264Decoder.input(rtpPacket.getPayload(), rtpPacket.getTimestamp(), rtpPacket.isMarker());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOutput(byte[] bytes, int offset, int size) {
//        audioTrack.write(bytes, offset, size);
    }
}