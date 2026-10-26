package com.classlog.sorimirror;

import android.view.WindowManager;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// 웹 쪽에서 Capacitor.Plugins.SoriScreen.keepOn({on}) 으로 화면 꺼짐 방지를 켜고 끔
@CapacitorPlugin(name = "SoriScreen")
public class ScreenPlugin extends Plugin {
    @PluginMethod
    public void keepOn(PluginCall call) {
        final boolean on = Boolean.TRUE.equals(call.getBoolean("on", false));
        getActivity().runOnUiThread(() -> {
            if (on) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        call.resolve();
    }
}
