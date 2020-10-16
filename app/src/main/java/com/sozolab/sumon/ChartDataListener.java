package com.sozolab.sumon;

import com.sozolab.sumon.io.esense.esenselib.ESenseEvent;

public interface ChartDataListener {

    public void update(ESenseEvent e);
}
