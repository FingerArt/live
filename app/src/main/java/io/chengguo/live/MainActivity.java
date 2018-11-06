package io.chengguo.live;

import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.net.URI;

import io.chengguo.streaming.RTSPClient;
import io.chengguo.streaming.rtcp.IReport;
import io.chengguo.streaming.rtp.RtpPacket;
import io.chengguo.streaming.rtsp.IResolver;
import io.chengguo.streaming.rtsp.Method;
import io.chengguo.streaming.rtsp.RTSPSession;
import io.chengguo.streaming.rtsp.Request;
import io.chengguo.streaming.rtsp.Response;
import io.chengguo.streaming.rtsp.TransportListenerWrapper;
import io.chengguo.streaming.rtsp.header.Header;
import io.chengguo.streaming.rtsp.header.TransportHeader;
import io.chengguo.streaming.transport.TransportMethod;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, RTSPClient.RTPPacketReceiver, Decoder.Callback {

    private static final String TAG = "MainActivity";

    private SurfaceView mSurfaceView;
    private AudioTrack audioTrack;
    private Decoder decoder;
    private RTSPClient rtspClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSurfaceView = findViewById(R.id.surface);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);

        try {
            int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            decoder = new Decoder();
            decoder.setCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                decoder.start();
                audioTrack.play();
                //初始化实时流解码器
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        rtspClient = RTSPClient.create()
                .host("14.29.172.223")
                .port(554)
                .transport(TransportMethod.TCP)
                .setPacketReciver(this).build();
        rtspClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtspClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                rtspClient.play(URI.create("rtsp://172.17.0.2/NeverPlay.mp3"));
                break;
            case R.id.btn_stop:
                rtspClient.pause();
                break;
        }
    }

    @Override
    public void onReceive(RtpPacket rtpPacket) {
        try {
            decoder.input(rtpPacket.getPayload(), 0, rtpPacket.getPayload().length, rtpPacket.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOutput(byte[] bytes, int offset, int size) {
        audioTrack.write(bytes, offset, size);
    }
}