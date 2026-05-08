package com.example.myvibrationproject

enum class VibePattern(val displayName: String) {
    NONE("なし"),
    SHORT("短く1回"),
    DOUBLE("短く2回"),
    LONG("長く1回"),
    STRONG("強く3回"),
    CALL("電話パターン（ループ）"),
    WECHAT("WeChat（トントン）"),  // ← 追加
    CUSTOM("カスタム")
}