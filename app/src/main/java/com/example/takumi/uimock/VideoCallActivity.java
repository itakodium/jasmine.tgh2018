package com.example.takumi.uimock;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.view.Surface;
import android.util.SparseIntArray;
import android.util.Size;
import android.os.Handler;
import android.os.HandlerThread;
import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


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
    private ImageReader imageReader = ImageReader.newInstance(1440, 720, ImageFormat.JPEG, 1);
    private Image image;
    private Image.Plane plane;
    private ByteBuffer buf;
    private byte[] b;
    private Bitmap bmp;
    private Handler mBackgroundHandler = new Handler();
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler bgHandler;
//    private HandlerThread bgThread;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        final View.OnTouchListener moving = new View.OnTouchListener() {

            private float downX;
            private float downY;

            private int downLeftMargin;
            private int downTopMargin;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // ViewGroup.MarginLayoutParamsでキャストすることで
                // FrameLayoutの子要素であっても同様に扱える。
                final ViewGroup.MarginLayoutParams param =
                        (ViewGroup.MarginLayoutParams)v.getLayoutParams();

                if( event.getAction() == MotionEvent.ACTION_DOWN ){

                    downX = event.getRawX();
                    downY = event.getRawY();

                    downLeftMargin = param.leftMargin;
                    downTopMargin = param.topMargin;

                    return true;
                }
                else if( event.getAction() == MotionEvent.ACTION_MOVE){

                    param.leftMargin = downLeftMargin + (int)(event.getRawX() - downX);
                    param.topMargin = downTopMargin + (int)(event.getRawY() - downY);

                    v.layout(
                            param.leftMargin,
                            param.topMargin,
                            param.leftMargin + v.getWidth(),
                            param.topMargin + v.getHeight());
                    return true;
                }

                return false;
            }

        };

        Button btn = findViewById(R.id.hangup_button);
        assert btn != null;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeCamera();
                finish();
            }
        });

        Button process_btn = findViewById(R.id.process_button);
        assert process_btn != null;
        process_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseVisionFaceDetectorOptions options =
                        new FirebaseVisionFaceDetectorOptions.Builder()
                                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                                .setMinFaceSize(0.15f)
                                .setTrackingEnabled(true)
                                .build();

                try {
                    if (ActivityCompat.checkSelfPermission(VideoCallActivity.this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED ) {
                        Log.d("AndroidCameraApi", "In try-catch try-if");
                        ActivityCompat.requestPermissions(VideoCallActivity.this,
                                new String[] {Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    } else {
                        Log.d("AndroidCameraApi","In try-catch try-else");
                        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
                        String cameraId = "";
                        for (String id : cameraManager.getCameraIdList()) {
                            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                            if (characteristics.get(CameraCharacteristics.LENS_FACING)
                                    == CameraCharacteristics.LENS_FACING_BACK) {
                                cameraId = id;
                            }
                        }

                        openCamera();

                        image = imageReader.acquireLatestImage();
                        Log.d("AndroidCameraApi","so far OK");
                        plane = image.getPlanes()[0];
                        Log.d("AndroidCameraApi","the line above has some problem");

                        buf = plane.getBuffer();
                        b = new byte[buf.remaining()];
                        buf.get(b);
                        bmp = BitmapFactory.decodeByteArray(b, 0, b.length);

                        if (bmp != null) {
                            showStamp();
                        }
                        Log.d("AndroidCameraApi","Before getting image for firebase");
                        FirebaseVisionImage firebase_image = FirebaseVisionImage.fromBitmap(bmp);
                        Log.d("AndroidCameraApi","After getting image for firebase");
                        //FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(imageReader.acquireLatestImage(), getRotationCompensation(cameraId, VideoCallActivity.this, VideoCallActivity.this));

                        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                                .getVisionFaceDetector(options);

                        Task<List<FirebaseVisionFace>> result =
                                detector.detectInImage(firebase_image)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                                    @Override
                                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                                        // Task completed successfully
                                                        // ...
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Task failed with an exception
                                                        // ...
                                                    }
                                                });
                        Log.d("AndroidCameraApi","Got Result");
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    Log.d("AndroidCameraApi","Catch");
                }

            }
        });

        Button stamp_btn = findViewById(R.id.stamp_button);
        assert stamp_btn != null;
        stamp_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStamp();
            }
        });

        imageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        findViewById(R.id.stamp).setOnTouchListener(moving);

        textureView = findViewById(R.id.video);
        assert textureView != null;
        textureView.setSurfaceTextureListener(txtrListener);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        // Surfaceから画像が利用できるようになった時に呼び出される
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d("AndroidCameraApi", "OnImageAvailableListener called");
            FirebaseVisionFaceDetectorOptions options =
                    new FirebaseVisionFaceDetectorOptions.Builder()
                            .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                            .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                            .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                            .setMinFaceSize(0.15f)
                            .setTrackingEnabled(true)
                            .build();
            CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            /*
            String cameraId = "";
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                }
            }
            */

            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Log.d("AndroidCameraApi", "image non null");
            }
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buf = plane.getBuffer();
            byte[] b = new byte[buf.remaining()];
            buf.get(b);
            bmp = BitmapFactory.decodeByteArray(b, 0, b.length);

            if (bmp != null) {
                showStamp();
            }

            FirebaseVisionImage firebase_image = FirebaseVisionImage.fromBitmap(bmp);
            //FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(imageReader.acquireLatestImage(), getRotationCompensation(cameraId, VideoCallActivity.this, VideoCallActivity.this));

            FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                    .getVisionFaceDetector(options);

            Task<List<FirebaseVisionFace>> result =
                    detector.detectInImage(firebase_image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<FirebaseVisionFace>>() {
                                        @Override
                                        public void onSuccess(List<FirebaseVisionFace> faces) {
                                            // Task completed successfully
                                            // ...
                                        }
                                        })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                        });
        }
    };

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
        /*
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
        */
    }

    private void showStamp() {
        findViewById(R.id.stamp).setVisibility(View.VISIBLE);
    }

    /*
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    */

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }
}





