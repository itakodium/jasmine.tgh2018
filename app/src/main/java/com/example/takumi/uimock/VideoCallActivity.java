package com.example.takumi.uimock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.widget.Button;
import android.view.View;
import android.view.Surface;
import android.util.SparseIntArray;
import android.util.Size;
import android.os.Handler;
import android.os.HandlerThread;
import android.Manifest;
import android.widget.Toast;
import java.util.Arrays;


public class VideoCallActivity extends AppCompatActivity {

    private final static String TAG = "AndroidCameraApi";
    private TextureView textureView;
    private final static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String camId;
    protected CameraDevice camDev;
    protected CameraCaptureSession camCaptureSession;
    protected CaptureRequest.Builder captureReqBuider;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler bgHandler;
//    private HandlerThread bgThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        Button btn = findViewById(R.id.hangup_button);
        assert btn != null;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeCamera();
                finish();
            }
        });

        textureView = findViewById(R.id.video);
        assert textureView != null;
        textureView.setSurfaceTextureListener(txtrListener);
    }

    TextureView.SurfaceTextureListener txtrListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // this might be the place to plug-in Face Detection and image transformation codes...
        }
    };

    protected void createCameraPreview() {
        try {
            SurfaceTexture txtr = textureView.getSurfaceTexture();
            assert txtr != null;
            txtr.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(txtr);
            captureReqBuider = camDev.createCaptureRequest(camDev.TEMPLATE_PREVIEW);
            captureReqBuider.addTarget(surface);
            camDev.createCaptureSession (Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (null == camDev) {
                        return;
                    }
                    camCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(VideoCallActivity.this, "Oops, Config changed!", Toast.LENGTH_SHORT).show();
                }
            },
            null);

        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened( CameraDevice camera) {
            camDev = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camDev.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camDev.close();
            camDev = null;
        }
    };

    final CameraCaptureSession.CaptureCallback capCbListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

//    protected void startBackgroundThread() {
//        bgThread = new HandlerThread("Camera Background");
//        bgThread.start();
//        bgHandler = new Handler(bgThread.getLooper());
//    }
//
//    protected void stopBackgroundThread() {
//        bgThread.quitSafely();
//        try {
//            bgThread.join();
//            bgThread = null;
//            bgHandler = null;
//        } catch (InterruptedException e){
//            e.printStackTrace();
//        }
//    }

    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] camIds = manager.getCameraIdList();
            CameraCharacteristics camChar = null;
            for (int i = 0; i < camIds.length; i++) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(camIds[i]);
                if (cc.get(CameraCharacteristics.LENS_FACING)== CameraCharacteristics.LENS_FACING_FRONT) {
                    camId = camIds[i];
                    camChar = cc;
                    break;
                }
            }
            StreamConfigurationMap map = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(VideoCallActivity.this,
                        new String[] {Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(camId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == camDev){
            Log.e(TAG, "updatePreview error");
        }
        captureReqBuider.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            camCaptureSession.setRepeatingRequest(captureReqBuider.build(), null, bgHandler);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != camDev) {
            camDev.close();
            camDev = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
}





