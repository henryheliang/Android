package com.dalinoo.courier;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Henry on 15/7/22.
 */
public class PostParameter {

    private String mKey;
    private String mURL;
    private Context mContext;
    private Map<String, String> mMap;

    //构造函数带入当前窗口句柄:
    public PostParameter( Context context ) {
        mContext = context;

        //读取加密KEY：
        mKey = mContext.getString(R.string.app_key);

        //读取POST地址信息：
        mURL = mContext.getString(R.string.server_url);

        //创建Map实例：
        mMap = new HashMap<String, String>();
    }

    //MD5字符串加密方法：
    String getMD5(String val) throws NoSuchAlgorithmException {

        //创建MD5实例：
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        //对数据进行加密：
        md5.update(val.getBytes());

        //对加密数据数据进行16进制数转换：
        byte[] hash = md5.digest();
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        //返回加密&转换后的字符串数据：
        return hex.toString();
    }

    //生成Post Login 参数列表：
    public Map<String, String> getLoginParameters( String strDeviceToken, String strName, String strPassword ) {

        //清空Map实例：
        mMap.clear();

        //添加 设备 Token 参数：
        mMap.put(mContext.getString(R.string.url_devicetoken), strDeviceToken);

        //添加 Login 命令 参数：
        mMap.put( mContext.getString(R.string.url_command), mContext.getString(R.string.url_login) );

        //添加 用户名 参数：
        mMap.put( mContext.getString(R.string.profile_username), strName );

        //添加 密码 参数：
        mMap.put( mContext.getString(R.string.profile_password), strPassword );

        return mMap;
    }

    //读取Order List 参数列表：
    public Map<String, String> getListParameters( String strDeviceToken, String strLoginID, int nType ) {

        //清空Map实例：
        mMap.clear();

        //添加 设备 Token 参数：
        mMap.put( mContext.getString(R.string.url_devicetoken), strDeviceToken );

        //添加 List 命令 参数：
        mMap.put( mContext.getString(R.string.url_command), mContext.getString(R.string.url_list) );

        //添加 List 类型 参数：
        mMap.put( mContext.getString(R.string.url_status), String.valueOf(nType) );

        //添加 用户ID 参数：
        mMap.put( mContext.getString(R.string.url_loginid), strLoginID );

        return mMap;
    }

    //读取Update Order 参数列表：
    public Map<String, String> getUpdateParameters( String strDeviceToken, String strLoginID, String strOrderID, int nStatus ) {

        //清空Map实例：
        mMap.clear();

        //添加 设备 Token 参数：
        mMap.put(mContext.getString(R.string.url_devicetoken), strDeviceToken);

        //添加 Update 命令 参数：
        mMap.put( mContext.getString(R.string.url_command), mContext.getString(R.string.url_update) );

        //添加 更新 状态 参数：
        mMap.put( mContext.getString(R.string.url_status), String.valueOf(nStatus) );

        //添加 用户 ID 参数：
        mMap.put( mContext.getString(R.string.url_loginid), strLoginID );

        //添加 订单 ID 参数：
        mMap.put( mContext.getString(R.string.url_orderid), strOrderID );

        return mMap;
    }

    //读取Order Detail 参数列表:
    public Map<String, String> getDetailParameters( String strDeviceToken, String strLoginID, String strOrderID ) {
        //清空Map实例：
        mMap.clear();

        //添加 设备 Token 参数：
        mMap.put( mContext.getString(R.string.url_devicetoken), strDeviceToken );

        //添加 Update 命令 参数：
        mMap.put( mContext.getString(R.string.url_command), mContext.getString(R.string.url_detail) );

        //添加 用户 ID 参数：
        mMap.put( mContext.getString(R.string.url_loginid), strLoginID );

        //添加 订单 ID 参数：
        mMap.put( mContext.getString(R.string.url_orderid), strOrderID );

        return mMap;
    }

    //返回带MD5校验字符串的POST URL信息：
    public String getURL() {

        //对参数列表+加密KEY进行整体打包MD5加密：
        String strMD5;
        try{
            strMD5 = getMD5( mMap.toString() + mKey );
        } catch ( NoSuchAlgorithmException e ) {
            System.out.println("md5 error");
            e.printStackTrace();
            return null;
        }

        //返回带MD5加密字符串URL 信息：
        return mURL + "?md5=" + strMD5;
    }
}
