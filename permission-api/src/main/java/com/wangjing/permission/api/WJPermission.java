package com.wangjing.permission.api;

import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.wangjing.permission.api.request.ActivityRequest;
import com.wangjing.permission.api.request.Requestable;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.interfaces.PBEKey;

/**
 * Created by xiaoyi on 2017-8-31.
 */

public class WJPermission {
    private static final String PERMISSION_PROXY = "$$PermissionsProxy";
    private static Map<String, PermissionsProxy> map = new HashMap<>();
    private static PermissionsProxy instance;

    public static void syncRequestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        initProxy(activity);
        syncRequest(activity);
    }

    private static void initProxy(Object object) {
        String name = object.getClass().getName();
        String proxyName = name + PERMISSION_PROXY;
        PermissionsProxy proxy = map.get(proxyName);
        if (proxy == null) {
            try {
                proxy = (PermissionsProxy) Class.forName(proxyName).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            map.put(proxyName, proxy);
            instance = proxy;
        } else {
            instance = proxy;
        }
    }

    private static void syncRequest(Object object) {
        if (object instanceof Activity
                || object instanceof android.app.Fragment
                || object instanceof Fragment) {
            instance.startSyncRequestPermissionsMethod(object);
        } else {
            throw new IllegalArgumentException(object.getClass().getName() + " not supported ");
        }
    }

    private WJPermission() {
    }


    public static void requestPermission(Activity activity, String permission, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        requestPermission(activity, ActivityRequest.getInstance(instance), permission, requestCode);
    }

    public static void requestPermission(Object object, Requestable requestable, String permission, int requestCode) {
        requestable.request(object, permission, requestCode);
    }
}
