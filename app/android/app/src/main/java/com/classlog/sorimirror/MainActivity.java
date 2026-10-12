package com.classlog.sorimirror;

import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 마이크를 통화 모드가 아닌 일반 녹음으로 여는 플러그인 (차량 블루투스가 통화로 잡지 않게)
        registerPlugin(NativeMicPlugin.class);
        super.onCreate(savedInstanceState);
        // 연주하는 동안 화면이 꺼지지 않게 함
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
