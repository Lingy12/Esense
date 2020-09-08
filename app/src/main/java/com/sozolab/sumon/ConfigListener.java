package com.sozolab.sumon;

import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;

public interface ConfigListener {
    void onNewSettingListener(ESenseConfig config);
}
