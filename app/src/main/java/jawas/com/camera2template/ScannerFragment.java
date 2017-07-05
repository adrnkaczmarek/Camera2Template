package jawas.com.camera2template;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

public class ScannerFragment extends Fragment implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback
{
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            ScannerFragment.this.cameraDevice = cameraDevice;
            prepareSurface();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice)
        {
            cameraDevice.close();
            ScannerFragment.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error)
        {
            cameraDevice.close();
            ScannerFragment.this.cameraDevice = null;
        }
    };
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
    };
    private CameraCaptureSession.StateCallback cameraCaptureSession = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
        {
            captureSession = cameraCaptureSession;

            try
            {
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession)
        {

        }
    };
    private TextureView.SurfaceTextureListener onTextureAvailableListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1)
        {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1)
        {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
        {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
        {

        }
    };
    private TextureView textureView;


    public static ScannerFragment newInstance()
    {
        return new ScannerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.picture).setOnClickListener(this);
        textureView = view.findViewById(R.id.texture);

        textureView.setSurfaceTextureListener(onTextureAvailableListener);
    }

    @Override
    public void onClick(View view)
    {
        takePicture();
    }

    private void takePicture()
    {

    }


    public void openCamera()
    {
        Activity activity = getActivity();
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
            {
                FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
            else
            {
                FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
            return;
        }
        initHandler();
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try
        {
            for (String cameraId: cameraManager.getCameraIdList())
            {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler);
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void prepareSurface()
    {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);

        try
        {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), cameraCaptureSession, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void initHandler()
    {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if (grantResults.length != REQUEST_CAMERA_PERMISSION || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Log.e("[Message permission]", getString(R.string.request_permission));
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (textureView.isAvailable())
        {
            openCamera();
        }
        else
        {
            textureView.setSurfaceTextureListener(onTextureAvailableListener);
        }
    }
}
