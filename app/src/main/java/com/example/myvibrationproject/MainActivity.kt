package com.example.myvibrationproject

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val selectedApps = mutableMapOf<String, VibePattern>()
    private lateinit var listLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "VibeFlow 設定"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        })

        root.addView(Button(this).apply {
            text = if (isNotificationAccessGranted()) "✅ 通知アクセス: ON" else "⚠️ 通知アクセスをONにする"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2).also { it.setMargins(0,16,0,16) }
            setBackgroundColor(0xFFDDDDDD.toInt())
        })

        root.addView(Button(this).apply {
            text = "+ 監視アプリを追加"
            setOnClickListener { showAppPicker() }
        })

        root.addView(TextView(this).apply {
            text = "監視中のアプリ"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })

        listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listLayout)

        setContentView(scroll)

        selectedApps.putAll(AppVibeSettings.getAllSettings(this))
        refreshList()
    }

    private fun showAppPicker() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString() }

        val names = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("アプリを選択")
            .setItems(names) { _, index ->
                val pkg = apps[index].packageName
                if (!selectedApps.containsKey(pkg)) {
                    selectedApps[pkg] = VibePattern.DOUBLE
                    AppVibeSettings.setPattern(this, pkg, VibePattern.DOUBLE)
                    refreshList()
                } else {
                    Toast.makeText(this, "すでに追加済みです", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun refreshList() {
        listLayout.removeAllViews()
        val pm = packageManager

        if (selectedApps.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "まだアプリが追加されていません"
                setTextColor(0xFF888888.toInt())
                setPadding(0, 16, 0, 0)
            })
            return
        }

        selectedApps.forEach { (pkg, pattern) ->
            val appName = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)

            // カード風の外枠
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(0xFFF5F5F5.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 12) }
            }

            // 上段：アプリ名 ＋ 削除ボタン
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(this).apply {
                text = appName
                textSize = 15f
                setTextColor(0xFF222222.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            topRow.addView(Button(this).apply {
                text = "削除"
                textSize = 12f
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("$appName を削除しますか？")
                        .setPositiveButton("削除") { _, _ ->
                            selectedApps.remove(pkg)
                            AppVibeSettings.removeApp(this@MainActivity, pkg)
                            refreshList()
                        }
                        .setNegativeButton("キャンセル", null)
                        .show()
                }
            })
            card.addView(topRow)

            // パッケージ名（小さく表示）
            card.addView(TextView(this).apply {
                text = pkg
                textSize = 11f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 0, 0, 8)
            })

            // 下段：パターン選択スピナー
            val patternRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            patternRow.addView(TextView(this).apply {
                text = "パターン："
                textSize = 13f
            })

            val spinner = Spinner(this)
            val patternNames = VibePattern.values().map { it.displayName }.toTypedArray()
            spinner.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, patternNames)
            spinner.setSelection(pattern.ordinal)
            spinner.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // カスタム設定エリア（CUSTOM選択時のみ表示）
            val customArea = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (pattern == VibePattern.CUSTOM) View.VISIBLE else View.GONE
                setPadding(0, 8, 0, 0)
            }

            // カスタムパターン入力
            customArea.addView(TextView(this).apply {
                text = "振動パターン（ms）例: 0,300,150,300,150,600"
                textSize = 11f
                setTextColor(0xFF666666.toInt())
            })
            val patternInput = EditText(this).apply {
                hint = "0,300,150,300,150,600"
                textSize = 13f
                setText(AppVibeSettings.getCustomPattern(this@MainActivity, pkg) ?: "")
            }
            customArea.addView(patternInput)

            // カスタム強度入力
            customArea.addView(TextView(this).apply {
                text = "強度（0〜255）例: 0,255,0,255,0,255"
                textSize = 11f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 8, 0, 0)
            })
            val amplitudeInput = EditText(this).apply {
                hint = "0,255,0,255,0,255"
                textSize = 13f
                setText(AppVibeSettings.getCustomAmplitude(this@MainActivity, pkg) ?: "")
            }
            customArea.addView(amplitudeInput)

            // 保存ボタン
            customArea.addView(Button(this).apply {
                text = "カスタムを保存"
                setOnClickListener {
                    val p = patternInput.text.toString()
                    val a = amplitudeInput.text.toString()
                    AppVibeSettings.setCustomPattern(this@MainActivity, pkg, p)
                    AppVibeSettings.setCustomAmplitude(this@MainActivity, pkg, a)
                    Toast.makeText(this@MainActivity, "保存しました", Toast.LENGTH_SHORT).show()
                }
            })

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                            pos: Int, id: Long) {
                    val newPattern = VibePattern.values()[pos]
                    selectedApps[pkg] = newPattern
                    AppVibeSettings.setPattern(this@MainActivity, pkg, newPattern)
                    customArea.visibility =
                        if (newPattern == VibePattern.CUSTOM) View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            patternRow.addView(spinner)
            card.addView(patternRow)
            card.addView(customArea)
            listLayout.addView(card)
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(packageName)
    }
}