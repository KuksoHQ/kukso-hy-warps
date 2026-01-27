package com.kukso.hy.warps;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class KuksoWarpsConfig {
    @SerializedName("Warmup")
    public int warmup = 3;

    @SerializedName("Cooldown")
    public int cooldown = 5;
}
