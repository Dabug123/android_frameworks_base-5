/*
 * Copyright (C) 2020 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.custom;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static volatile boolean sIsGms = false;
    public static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String DEVICE = "ro.statix.device";

    private static final Map<String, Object> propsToChange;
    private static final Map<String, ArrayList<String>> propsToKeep;
    private static final String[] extraPackagesToChange = {
        "com.android.vending",
        "com.breel.wallpapers20"
    };

    private static final String[] packagesToKeep = {
        "com.google.android.GoogleCamera",
        "com.google.android.GoogleCamera.Go",
    };

    // Codenames for currently supported Pixels by Google
    private static final String[] pixelCodenames = {
        "oriole",
        "raven",
        "redfin",
        "barbet",
        "bramble",
        "sunfish",
        "coral",
        "flame"
    };

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put("com.google.android.settings.intelligence", new ArrayList<String>(Arrays.asList("FINGERPRINT")));
        propsToKeep.put("com.google.android.gms", new ArrayList<String>(Arrays.asList("MODEL")));
        propsToChange = new HashMap<>();
        propsToChange.put("BRAND", "google");
        propsToChange.put("MANUFACTURER", "Google");
        propsToChange.put("DEVICE", "redfin");
        propsToChange.put("PRODUCT", "redfin");
        propsToChange.put("MODEL", "Pixel 5");
        propsToChange.put("FINGERPRINT", "google/redfin/redfin:12/SQ3A.220605.009.A1/8643238:user/release-keys");
    }

    public static void setProps(Application app) {
        String packageName = app.getPackageName();
        if (packageName == null){
            return;
        }
        if (Arrays.asList(pixelCodenames).contains(SystemProperties.get(DEVICE))) {
            if (packageName.equals(PACKAGE_GMS) && app.getProcessName().equals("com.google.android.gms.unstable")) {
                setPropValue("MODEL", SystemProperties.get("ro.product.model") + " ");
                setPropValue("PRODUCT", SystemProperties.get(DEVICE));
                sIsGms = true;
            }
            return;
        }
        if (packageName.equals(PACKAGE_GMS) && app.getProcessName().equals("com.google.android.gms.unstable")) {
            setPropValue("MODEL", "Pixel 5" + " ");
            sIsGms = true;
        }
        if ((packageName.startsWith("com.google.") && !Arrays.asList(packagesToKeep).contains(packageName))
                || Arrays.asList(extraPackagesToChange).contains(packageName)){
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)){
                    if (DEBUG) Log.d(TAG, "Not defining " + key + " prop for: " + packageName);
                    continue;
                }
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")){
            setPropValue("FINGERPRINT", Build.DATE);
        }
    }

    private static void setPropValue(String key, Object value){
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (sIsGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}
