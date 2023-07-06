package org.apache.cordova.baidumap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 百度定位cordova插件android端
 *
 * @author aruis
 * @author KevinWang15
 */
public class BaiduMapLocation extends CordovaPlugin {

    /**
     * LOG TAG
     */
    private static final String LOG_TAG = BaiduMapLocation.class.getSimpleName();

    /**
     * JS回调接口对象
     */
    public static CallbackContext cbCtx = null;

    /**
     * 百度定位客户端
     */
    public LocationClient mLocationClient = null;

    private LocationClientOption mOption;

    /**
     * 安卓6以上动态权限相关
     */

    private static final int REQUEST_CODE = 100001;

    /**
     * 百度定位监听
     */
    // BDAbstractLocationListener为7.2版本新增的Abstract类型的监听接口
    // 原有BDLocationListener接口暂时同步保留。具体介绍请参考后文第四步的说明
    private MyLocationListener myListener = new MyLocationListener();

    private boolean needsToAlertForRuntimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    || !cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            return false;
        }
    }

    /**
     * 申请权限
     */
    private void requestPermission() {
        ArrayList<String> permissionsToRequire = new ArrayList<String>();

        if (!cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionsToRequire.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (!cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsToRequire.add(Manifest.permission.ACCESS_FINE_LOCATION);

        String[] _permissionsToRequire = new String[permissionsToRequire.size()];
        _permissionsToRequire = permissionsToRequire.toArray(_permissionsToRequire);
        cordova.requestPermissions(this, REQUEST_CODE, _permissionsToRequire);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        if (cbCtx == null || requestCode != REQUEST_CODE)
            return;
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                String error = "定位权限请求被拒绝，请打开定位权限。";
                LOG.e(LOG_TAG, "权限请求被拒绝");
                cbCtx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, error));
                return;
            }
        }

        performGetLocation();
    }

    /**
     * 插件主入口
     */
    @Override
    public boolean execute(String action, final JSONArray args, CallbackContext callbackContext) throws JSONException {
        cbCtx = callbackContext;
        if ("getCurrentPosition".equalsIgnoreCase(action)) {
            if (!needsToAlertForRuntimePermission()) {
                performGetLocation();
            } else {
                requestPermission();
                // 会在onRequestPermissionResult时performGetLocation
            }
            return true;
        }

        return false;
    }

    /**
     * 权限获得完毕后进行定位
     */
    private void performGetLocation() {
        LocationClient.setAgreePrivacy(true);
        try {
            if (mLocationClient == null) {
                mLocationClient = new LocationClient(this.webView.getContext());
                mLocationClient.setAgreePrivacy(true);
                mLocationClient.registerLocationListener(myListener);
                mLocationClient.setLocOption(getDefaultLocationClientOption());
            }
            mLocationClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            mOption.setLocationMode(LocationMode.Hight_Accuracy);// 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption.setCoorType("bd09ll");// 可选，设置返回经纬度坐标类型，默认GCJ02。GCJ02：国测局坐标；BD09ll：百度经纬度坐标；BD09：百度墨卡托坐标；海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
            mOption.setScanSpan(0);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
            mOption.setOpenGps(true); // 可选，默认false,设置是否使用gps
            mOption.setNeedDeviceDirect(false);// 可选，设置是否需要设备方向结果
            mOption.setLocationNotify(false);// 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(true);// 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            mOption.setIsNeedLocationDescribe(true);// 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            mOption.setIsNeedLocationPoiList(false);// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            mOption.SetIgnoreCacheException(false);// 可选，默认false，设置是否收集CRASH信息，默认收集
            mOption.setIsNeedAltitude(false);// 可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
            mOption.setWifiCacheTimeOut(5 * 60 * 1000);// 可选，V7.2版本新增能力，如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
            mOption.setEnableSimulateGps(false);// 可选，设置是否需要过滤卫星定位仿真结果，默认需要，即参数为false
            mOption.setNeedNewVersionRgc(true);// 可选，设置是否需要最新版本的地址信息。默认需要，即参数为true
            // mOption.setFirstLocType(FirstLocTypefirstLocType);//可选，首次定位时可以选择定位的返回是准确性优先还是速度优先，默认为速度优先。可以搭配setOnceLocation(Boolean
            // isOnceLocation)单次定位接口使用，当设置为单次定位时，setFirstLocType接口中设置的类型即为单次定位使用的类型;FirstLocType.SPEED_IN_FIRST_LOC:速度优先，首次定位时会降低定位准确性，提升定位速度;FirstLocType.ACCUARACY_IN_FIRST_LOC:准确性优先，首次定位时会降低速度，提升定位准确性；
        }
        return mOption;
    }

    class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            try {
                JSONObject json = new JSONObject();

                json.put("time", location.getTime());
                json.put("locType", location.getLocType());
                json.put("locTypeDescription", location.getLocTypeDescription());
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
                json.put("radius", location.getRadius());

                json.put("countryCode", location.getCountryCode());
                json.put("country", location.getCountry());
                json.put("cityCode", location.getCityCode());
                json.put("city", location.getCity());
                json.put("district", location.getDistrict());
                json.put("street", location.getStreet());
                json.put("address", location.getAddrStr());
                json.put("province", location.getProvince());

                json.put("userIndoorState", location.getUserIndoorState());
                json.put("direction", location.getDirection());
                json.put("locationDescribe", location.getLocationDescribe());

                PluginResult pluginResult;
                if (location.getLocType() == BDLocation.TypeServerError
                        || location.getLocType() == BDLocation.TypeNetWorkException
                        || location.getLocType() == BDLocation.TypeCriteriaException) {

                    json.put("describe", "定位失败");
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, json);
                } else {
                    pluginResult = new PluginResult(PluginResult.Status.OK, json);
                }

                cbCtx.sendPluginResult(pluginResult);
            } catch (JSONException e) {
                String errMsg = e.getMessage();
                LOG.e(LOG_TAG, errMsg, e);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
                cbCtx.sendPluginResult(pluginResult);
            } finally {
                mLocationClient.stop();
            }
        }
    }
}
