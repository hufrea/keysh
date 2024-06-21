package io.github.hufrea.keysh.actions;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class ActionTorch {
    static public void turnFlashlight(Context context, boolean mode) {
        try {
            CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null;

            for (String camID : camManager.getCameraIdList()) {
                CameraCharacteristics camCharacter = camManager.getCameraCharacteristics(camID);
                if (camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != null) {
                    cameraId = camID;
                    break;
                }
            }
            if (cameraId != null) {
                camManager.setTorchMode(cameraId, mode);
            }
        } catch (CameraAccessException e) {
            Log.e("turnFlashlight", e.toString());
        }
    }
}
