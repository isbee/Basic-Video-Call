package io.agora.openvcall.service;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

import io.agora.openvcall.AGApplication;
import io.agora.openvcall.R;
import io.agora.openvcall.model.AGEventHandler;
import io.agora.openvcall.model.ConstantApp;
import io.agora.openvcall.model.CurrentUserSettings;
import io.agora.openvcall.model.EngineConfig;
import io.agora.openvcall.model.RtcChannelToken;
import io.agora.openvcall.model.RtcChannelTokenService;
import io.agora.openvcall.model.RtcChannelTokenServiceClient;
import io.agora.openvcall.ui.CallActivity;
import io.agora.openvcall.ui.layout.GridVideoViewContainer;
import io.agora.propeller.Constant;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OverlayService extends Service {

    private final static Logger log = LoggerFactory.getLogger(CallActivity.class);

    private GridVideoViewContainer mGridVideoViewContainer;
    // should only be modified under UI thread
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid
    private boolean mIsLandscape = false;

    WindowManager wm;
    View mView;

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 현재 RtcEngine은 application level에서 접근 가능하므로, activity에서 binder를 활용하지 않아도 됨.
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                /*ViewGroup.LayoutParams.MATCH_PARENT*/300,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.TOP;
        mView = inflate.inflate(R.layout.overlay_service, null);
        wm.addView(mView, params);

        mGridVideoViewContainer = (GridVideoViewContainer) mView.findViewById(R.id.overlay_grid_video_view_container);

        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        preview(true, surfaceV, 0);
        surfaceV.setZOrderOnTop(false);
        surfaceV.setZOrderMediaOverlay(false);

        mUidsList.put(0, surfaceV); // get first surface view

        mGridVideoViewContainer.initViewContainer(inflate, this, 0, mUidsList, mIsLandscape); // first is now full view
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wm != null) {
            if(mView != null) {
                wm.removeView(mView);
                mView = null;
            }
            wm = null;
        }
    }

    protected void permissionGranted() {
    }

    public final void closeIME(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(v.getWindowToken(), 0); // 0 force close IME
        v.clearFocus();
    }

    protected AGApplication application() {
        return (AGApplication) getApplication();
    }

    protected RtcEngine rtcEngine() {
        return application().rtcEngine();
    }

    protected EngineConfig config() {
        return application().config();
    }

    protected void addEventHandler(AGEventHandler handler) {
        application().addEventHandler(handler);
    }

    protected void removeEventHandler(AGEventHandler handler) {
        application().remoteEventHandler(handler);
    }

    protected CurrentUserSettings vSettings() {
        return application().userSettings();
    }

    /**
     *
     * Starts/Stops the local video preview
     *
     * Before calling this method, you must:
     * Call the enableVideo method to enable the video.
     *
     * @param start Whether to start/stop the local preview
     * @param view The SurfaceView in which to render the preview
     * @param uid User ID.
     */
    protected void preview(boolean start, SurfaceView view, int uid) {
        if (start) {
            rtcEngine().setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            rtcEngine().startPreview();
        } else {
            rtcEngine().stopPreview();
        }
    }

    /**
     * Allows a user to join a channel.
     *
     * Users in the same channel can talk to each other, and multiple users in the same channel can start a group chat. Users with different App IDs cannot call each other.
     *
     * You must call the leaveChannel method to exit the current call before joining another channel.
     *
     * A successful joinChannel method call triggers the following callbacks:
     *
     * The local client: onJoinChannelSuccess.
     * The remote client: onUserJoined, if the user joining the channel is in the Communication profile, or is a BROADCASTER in the Live Broadcast profile.
     *
     * When the connection between the client and Agora's server is interrupted due to poor
     * network conditions, the SDK tries reconnecting to the server. When the local client
     * successfully rejoins the channel, the SDK triggers the onRejoinChannelSuccess callback
     * on the local client.
     *
     * @param channel The unique channel name for the AgoraRTC session in the string format.
     * @param uid User ID.
     */
    public final void joinChannel(final String channel, int uid) {
        String accessToken = getApplicationContext().getString(R.string.agora_access_token);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "<#YOUR ACCESS TOKEN#>")) {
            accessToken = null; // default, no token
        }

        rtcEngine().joinChannel(accessToken, channel, "OpenVCall", uid);
        config().mChannel = channel;
        enablePreProcessor();
        log.debug("joinChannel " + channel + " " + uid);
    }

    public final void joinRtcChannelWithToken(final String channel, int uid) {
        RtcChannelTokenService tokenService = RtcChannelTokenServiceClient.getClient();
        // TODO channelId 문자로 받을 것
        tokenService.getRtcChannelToken(channel, String.valueOf(uid)).enqueue(new Callback<RtcChannelToken>() {
            @Override
            public void onResponse(Call<RtcChannelToken> call, Response<RtcChannelToken> response) {
                RtcChannelToken token = response.body();

                rtcEngine().joinChannel(token.getToken(), channel, "OpenVCall", uid);
                config().mChannel = channel;
                enablePreProcessor();
                log.debug("joinChannel " + channel + " " + uid);
            }

            @Override
            public void onFailure(Call<RtcChannelToken> call, Throwable t) {
                System.out.println("Shit");
            }
        });
    }

    /**
     * Allows a user to leave a channel.
     *
     * After joining a channel, the user must call the leaveChannel method to end the call before
     * joining another channel. This method returns 0 if the user leaves the channel and releases
     * all resources related to the call. This method call is asynchronous, and the user has not
     * exited the channel when the method call returns. Once the user leaves the channel,
     * the SDK triggers the onLeaveChannel callback.
     *
     * A successful leaveChannel method call triggers the following callbacks:
     *
     * The local client: onLeaveChannel.
     * The remote client: onUserOffline, if the user leaving the channel is in the
     * Communication channel, or is a BROADCASTER in the Live Broadcast profile.
     *
     * @param channel Channel Name
     */
    public final void leaveChannel(String channel) {
        log.debug("leaveChannel " + channel);
        config().mChannel = null;
        disablePreProcessor();
        rtcEngine().leaveChannel();
        config().reset();
    }

    /**
     * Enables image enhancement and sets the options.
     */
    protected void enablePreProcessor() {
        if (Constant.BEAUTY_EFFECT_ENABLED) {
            rtcEngine().setBeautyEffectOptions(true, Constant.BEAUTY_OPTIONS);
        }
    }

    public final void setBeautyEffectParameters(float lightness, float smoothness, float redness) {
        Constant.BEAUTY_OPTIONS.lighteningLevel = lightness;
        Constant.BEAUTY_OPTIONS.smoothnessLevel = smoothness;
        Constant.BEAUTY_OPTIONS.rednessLevel = redness;
    }


    /**
     * Disables image enhancement.
     */
    protected void disablePreProcessor() {
        // do not support null when setBeautyEffectOptions to false
        rtcEngine().setBeautyEffectOptions(false, Constant.BEAUTY_OPTIONS);
    }

    protected void configEngine(VideoEncoderConfiguration.VideoDimensions videoDimension, VideoEncoderConfiguration.FRAME_RATE fps, String encryptionKey, String encryptionMode) {
        if (!TextUtils.isEmpty(encryptionKey)) {
            rtcEngine().setEncryptionMode(encryptionMode);
            rtcEngine().setEncryptionSecret(encryptionKey);
        }

        log.debug("configEngine " + videoDimension + " " + fps + " " + encryptionMode);
        // Set the Resolution, FPS. Bitrate and Orientation of the video
        rtcEngine().setVideoEncoderConfiguration(new VideoEncoderConfiguration(videoDimension,
                fps,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }
}
