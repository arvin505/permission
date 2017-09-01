package demo.permission;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.wangjing.permission.annotations.PermissionsCustomRationale;
import com.wangjing.permission.annotations.PermissionsDenied;
import com.wangjing.permission.annotations.PermissionsGranted;
import com.wangjing.permission.annotations.PermissionsRationale;
import com.wangjing.permission.annotations.PermissionsRequestSync;

@PermissionsRequestSync(value = {1, 2, 3, 4, 5, 6, 7, 8}, permissions = {
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.LOCATION_HARDWARE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAPTURE_SECURE_VIDEO_OUTPUT,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_CHECKIN_PROPERTIES
})
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    @PermissionsDenied({1, 2, 3, 4})
    public void helloWorld(int code) {

    }

    @PermissionsGranted({5, 6, 7, 8})
    public void test(int code) {

    }

    @PermissionsGranted(10)
    public void hahah() {

    }

    @PermissionsRationale({12, 13, 14})
    public void 大河向东流(int code) {

    }

    @PermissionsCustomRationale({1, 2, 3, 4})
    public void 测试自定义(int code) {

    }

    @PermissionsCustomRationale(5)
    public void 测试自定义单个() {

    }
}
