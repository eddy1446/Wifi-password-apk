package com.example.wifipass;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setPadding(32, 32, 32, 32);
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);
        tv.setText(readWifi());
    }

    private String readWifi() {
        String[] files = {
            "/data/misc/wifi/wpa_supplicant.conf",   // Android 8 and below
            "/data/misc/wifi/WifiConfigStore.xml",   // Android 8.1 - 10
            "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml" // Android 11+
        };
        for (String f : files) {
            String out = runRoot("cat " + f);
            if (out != null && !out.trim().isEmpty()) {
                return f.endsWith(".conf") ? parseConf(out) : parseXml(out);
            }
        }
        return "Couldn't read Wi-Fi data. Is the phone rooted? Grant root access when prompted.";
    }

    private String runRoot(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String parseConf(String s) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("network=\\{(.*?)\\}", Pattern.DOTALL).matcher(s);
        while (m.find()) {
            String blk = m.group(1);
            String ssid = find(blk, "ssid=\"(.*?)\"");
            String psk = find(blk, "psk=\"(.*?)\"");
            if (ssid != null)
                sb.append(ssid).append("\n  ").append(psk != null ? psk : "(open / no password)").append("\n\n");
        }
        return sb.length() > 0 ? sb.toString() : "No networks found.";
    }

    private String parseXml(String s) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("<Network>(.*?)</Network>", Pattern.DOTALL).matcher(s);
        while (m.find()) {
            String blk = m.group(1);
            String ssid = find(blk, "name=\"SSID\">&quot;(.*?)&quot;");
            String psk = find(blk, "name=\"PreSharedKey\">&quot;(.*?)&quot;");
            if (ssid != null)
                sb.append(ssid).append("\n  ").append(psk != null ? psk : "(open / no password)").append("\n\n");
        }
        return sb.length() > 0 ? sb.toString() : "No networks found.";
    }

    private String find(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? m.group(1) : null;
    }
  }
