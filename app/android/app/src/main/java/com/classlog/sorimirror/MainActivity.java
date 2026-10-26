package com.classlog.sorimirror;

import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 화면 꺼짐 방지는 마이크가 켜진 동안에만 (ScreenPlugin)
        registerPlugin(ScreenPlugin.class);
        // 마이크를 통화 모드가 아닌 일반 녹음으로 여는 플러그인 (차량 블루투스가 통화로 잡지 않게)
        registerPlugin(NativeMicPlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
