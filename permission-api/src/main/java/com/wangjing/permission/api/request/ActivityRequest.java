package com.wangjing.permission.api.request;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.wangjing.permission.api.PermissionsProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoyi on 2017-9-1.
 */

public class ActivityRequest implements Requestable {
    private ActivityRequest(PermissionsProxy proxy) {
        this.proxy = proxy;
    }

    private static Map<PermissionsProxy, ActivityRequest> map = new HashMap<>();

    private PermissionsProxy proxy;


    public static ActivityRequest getInstance(PermissionsProxy proxy) {
        ActivityRequest request = map.get(proxy);
        if (request == null) {
            request = new ActivityRequest(proxy);
        }
        return request;
    }

    @Override
    public void request(Object object, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(((Activity) object), permission)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) object, permission)) {
                if (!proxy.customRationale(object, requestCode)) {
                    proxy.rationale(object, requestCode);
                    ActivityCompat.requestPermissions((Activity) object,
                            new String[]{permission}, requestCode);
                }
            } else {
                ActivityCompat.requestPermissions((Activity) object, new String[]{permission}, requestCode);
            }
        } else {
            proxy.granted(object, requestCode);
        }
    }
}
