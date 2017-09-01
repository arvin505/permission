package com.wangjing.permission.api.request;

/**
 * Created by xiaoyi on 2017-9-1.
 */

public interface Requestable {
    void request(Object object, String permission, int requestCode);
}
