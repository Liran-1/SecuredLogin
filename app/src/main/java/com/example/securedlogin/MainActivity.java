package com.example.securedlogin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS = 79;
    private TextInputEditText login_eTXT_passInput;
    private MaterialButton login_BTN_enter;
    private ShapeableImageView login_IMG_audioLock, login_IMG_batteryLock, login_IMG_chargeLock,
            login_IMG_brightnessLock, login_IMG_contactLock;
    private TextView login_LBL_status;
    private boolean audioUnlocked = false, inputUnlocked = false, chargeUnlocked = false, brightnessUnlocked = false, contactUnlocked = false;
    private Intent batteryStatus;

    private String checkName = "guy4444";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setBatteryManager();

        findViews();
        initViews();
    }

    private void setBatteryManager() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.getApplicationContext()
                .registerReceiver(null, ifilter);
    }

    /**
     * Find app views.
     */
    private void findViews() {
        login_eTXT_passInput = findViewById(R.id.login_eTXT_passInput);
        login_BTN_enter = findViewById(R.id.login_BTN_enter);
        login_IMG_audioLock = findViewById(R.id.login_IMG_audioLock);
        login_IMG_batteryLock = findViewById(R.id.login_IMG_batteryLock);
        login_IMG_chargeLock = findViewById(R.id.login_IMG_chargeLock);
        login_IMG_brightnessLock = findViewById(R.id.login_IMG_brightnessLock);
        login_IMG_contactLock = findViewById(R.id.login_IMG_contactLock);
        login_LBL_status = findViewById(R.id.login_LBL_status);
    }

    /**
     * Init app views.
     */
    private void initViews() {
        login_BTN_enter.setOnClickListener(view -> {
            checkChargingStatusLock();
            checkAudioLock();
            checkBrightnessLock();
            checkContactLock();
            checkInputLock();
            checkLocks();
        });
    }

    /**
     * Check if all locks are open and update UI.
     */
    private void checkLocks() {
        if (audioUnlocked && chargeUnlocked && brightnessUnlocked && contactUnlocked && inputUnlocked) {
            login_LBL_status.setText(R.string.unlocked);
            login_LBL_status.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            login_LBL_status.setTextColor(ContextCompat.getColor(this, R.color.red));
            login_LBL_status.setText(R.string.locked);
        }
    }

    /**
     * Check if input is valid.
     */
    private void checkInputLock() {
        if (!TextUtils.isEmpty(login_eTXT_passInput.getText())) {
            String passInput = login_eTXT_passInput.getText().toString();

            boolean chargingInput = checkInputChargingMethod(passInput),
                    batteryPctInput = checkInputBatteryPct(passInput);

            if (chargingInput && batteryPctInput) {
                inputUnlocked = true;
                openLock(login_IMG_batteryLock);
            } else {
                inputUnlocked = false;
                closeLock(login_IMG_batteryLock);
            }
        }
    }

    /**
     * Check if input contains correct battery percentage.
     *
     * @param passInput
     * @return boolean
     */
    private boolean checkInputBatteryPct(String passInput) {
        int batteryPct = (int) getBatteryPct();
        if (passInput.length() > 3) {
            return passInput.subSequence(passInput.length() - 2, passInput.length()).equals(String.valueOf(batteryPct));
        } else {
            return false;
        }
    }

    /**
     * Check if input contains correct charging method.
     *
     * @param passInput
     * @return boolean
     */
    private boolean checkInputChargingMethod(String passInput) {
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String usb = "usb", ac = "ac";
        if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB && passInput.length() > usb.length()) {
            return passInput.subSequence(0, 3).equals(usb);
        } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC && passInput.length() > ac.length()) {
            return passInput.subSequence(0, 2).equals(ac);
        } else {
            return false;
        }
    }

    /**
     * Returns current battery percentage.
     *
     * @return float
     */
    private float getBatteryPct() {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level * 100 / (float) scale;
    }

    /**
     * Check if a certain contact exists in phone.
     */
    private void checkContactLock() {
        if (!checkContactsPermission()) {
            requestContactsPermission();
        } else {
            ArrayList contacts = getContacts();
            if (contacts.contains(checkName)) {
                contactUnlocked = true;
                openLock(login_IMG_contactLock);
            } else {
                contactUnlocked = false;
                closeLock(login_IMG_contactLock);
            }
        }
    }

    /**
     * Request permission from user to access contacts.
     */
    private void requestContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_CONTACTS)) {
            // show UI part if you want here to show some rationale !!!
            return;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ArrayList mobileArray = getContacts();
        } else {
            // permission denied,Disable the
            // functionality that depends on this permission.
        }
        return;
    }

    private boolean checkContactsPermission() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns a list of the contacts on phone.
     *
     * @return ArrayList
     */
    @SuppressLint("Range")
    public ArrayList getContacts() {

        Uri contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        ArrayList<String> nameList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if ((cursor != null ? cursor.getCount() : 0) > 0) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                nameList.add(name.toLowerCase());
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return nameList;
    }

    /**
     * Check if brightness is in correct range
     */
    private void checkBrightnessLock() {
        try {
            int brightnessValue = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            float maxBrightnessVal = 255;
            float brightnessPct = brightnessValue * 100 / maxBrightnessVal;
            if (brightnessPct > 50 && brightnessPct < 75) {
                brightnessUnlocked = true;
                openLock(login_IMG_brightnessLock);
            } else {
                brightnessUnlocked = false;
                closeLock(login_IMG_brightnessLock);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
//        int brightnessLevel = Settings.System.SCREEN_BRIGHTNESS;


    }

    /**
     * Check if ringtone volume is in correct range
     */
    private void checkAudioLock() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int currentRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING),
                maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

        float currentRingVolumePct = currentRingVolume * 100 / (float) maxRingVolume;

        if (currentRingVolumePct > 50 && currentRingVolumePct < 80) {
            audioUnlocked = true;
            openLock(login_IMG_audioLock);
        } else {
            audioUnlocked = false;
            closeLock(login_IMG_audioLock);
        }
    }

    /**
     * Check if phone is currently charging.
     */
    private void checkChargingStatusLock() {
        if (checkBatteryCharging()) {
            chargeUnlocked = true;
            openLock(login_IMG_chargeLock);
        } else {
            chargeUnlocked = false;
            closeLock(login_IMG_chargeLock);
        }
    }

    /**
     * Check if phone is currently charged or charging.
     *
     * @return
     */
    private boolean checkBatteryCharging() {
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * Change lock image to closed.
     *
     * @param img
     */
    private void closeLock(ShapeableImageView img) {
        img.setImageResource(R.drawable.padlock_locked);
    }

    /**
     * Change lock image to open.
     *
     * @param img
     */
    private void openLock(ShapeableImageView img) {
        img.setImageResource(R.drawable.padlock_unlocked);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}