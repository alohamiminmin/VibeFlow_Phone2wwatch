package com.example.myvibrationproject

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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
            setBackgroundColor(0xFFF8F8F8.toInt())
        }
        scroll.addView(root)

        // ヘッダー
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 24)
        }
        header.addView(TextView(this).apply {
            text = "🤜"
            textSize = 32f
            setPadding(0, 0, 16, 0)
        })
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(TextView(this).apply {
            text = "VibeFlow"
            textSize = 22f
            setTextColor(0xFF222222.toInt())
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(this).apply {
            text = "Watch バイブカスタマイザー"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
        })
        header.addView(titleCol)
        root.addView(header)

        // 権限ボタン
        val permBtn = Button(this).apply {
            updatePermissionButton(this)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        root.addView(permBtn)
        root.addView(divider())

        // アプリ追加ボタン
        root.addView(Button(this).apply {
            text = "＋ 監視アプリを追加"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1976D2.toInt())
            setOnClickListener { showAppPicker() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 8, 0, 16) }
        })

        root.addView(TextView(this).apply {
            text = "監視中のアプリ"
            textSize = 14f
            setTextColor(0xFF555555.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 8)
        })

        listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listLayout)

        setContentView(scroll)

        selectedApps.putAll(AppVibeSettings.getAllSettings(this))
        refreshList()

        // 通知権限リクエスト（Android 13以上）
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun showAppPicker() {
        val pm = packageManager

        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .toMutableList()

        var filteredApps = allApps.toMutableList()

        val searchInput = EditText(this).apply {
            hint = "アプリ名・パッケージ名で検索..."
            setPadding(32, 16, 32, 16)
        }
        val listView = ListView(this)

        fun buildAdapter() = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            filteredApps.map { pm.getApplicationLabel(it).toString() }
        ) {
            override fun getView(position: Int, convertView: View?,
                                 parent: android.view.ViewGroup): View {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(24, 12, 24, 12)
                }
                val icon = runCatching {
                    pm.getApplicationIcon(filteredApps[position].packageName)
                }.getOrNull()
                row.addView(ImageView(context).apply {
                    setImageDrawable(icon)
                    layoutParams = LinearLayout.LayoutParams(80, 80).also {
                        it.setMargins(0, 0, 20, 0)
                    }
                })
                val col = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                col.addView(TextView(context).apply {
                    text = pm.getApplicationLabel(filteredApps[position]).toString()
                    textSize = 15f
                    setTextColor(0xFF222222.toInt())
                })
                col.addView(TextView(context).apply {
                    text = filteredApps[position].packageName
                    textSize = 10f
                    setTextColor(0xFFAAAAAA.toInt())
                })
                row.addView(col)
                return row
            }
        }

        listView.adapter = buildAdapter()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredApps = allApps.filter {
                    pm.getApplicationLabel(it).toString().lowercase().contains(query) ||
                            it.packageName.lowercase().contains(query)
                }.toMutableList()
                listView.adapter = buildAdapter()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(searchInput)
            addView(listView)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("アプリを選択")
            .setView(container)
            .setNegativeButton("キャンセル", null)
            .create()

        listView.setOnItemClickListener { _, _, index, _ ->
            val pkg = filteredApps[index].packageName
            if (!selectedApps.containsKey(pkg)) {
                selectedApps[pkg] = VibePattern.DOUBLE
                AppVibeSettings.setPattern(this, pkg, VibePattern.DOUBLE)
                AppVibeSettings.removeCandidate(this, pkg)
                refreshList()
            } else {
                Toast.makeText(this, "すでに追加済みです", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun refreshList() {
        listLayout.removeAllViews()
        val pm = packageManager

        // 通知が来た未登録アプリの候補を表示
        val candidates = AppVibeSettings.getCandidates(this)
            .filter { !selectedApps.containsKey(it) }
        if (candidates.isNotEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "📬 通知が届いたアプリ（タップして追加）"
                textSize = 13f
                setTextColor(0xFF1976D2.toInt())
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 8, 0, 8)
            })

            candidates.forEach { pkg ->
                val appName = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                val appIcon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(0xFFE3F2FD.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 0, 0, 4) }
                }
                row.addView(ImageView(this).apply {
                    setImageDrawable(appIcon)
                    layoutParams = LinearLayout.LayoutParams(64, 64).also {
                        it.setMargins(0, 0, 16, 0)
                    }
                })
                row.addView(TextView(this).apply {
                    text = appName
                    textSize = 14f
                    setTextColor(0xFF222222.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(Button(this).apply {
                    text = "追加"
                    textSize = 11f
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(0xFF1976D2.toInt())
                    setOnClickListener {
                        selectedApps[pkg] = VibePattern.DOUBLE
                        AppVibeSettings.setPattern(this@MainActivity, pkg, VibePattern.DOUBLE)
                        AppVibeSettings.removeCandidate(this@MainActivity, pkg)
                        refreshList()
                    }
                })
                row.addView(Button(this).apply {
                    text = "無視"
                    textSize = 11f
                    setTextColor(0xFF888888.toInt())
                    setBackgroundColor(0x00000000)
                    setOnClickListener {
                        AppVibeSettings.removeCandidate(this@MainActivity, pkg)
                        refreshList()
                    }
                })
                listLayout.addView(row)
            }
            listLayout.addView(divider())
        }

        // 監視中アプリリスト
        if (selectedApps.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "まだアプリが追加されていません\n上のボタンから追加してください"
                setTextColor(0xFF999999.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 0)
            })
            return
        }

        selectedApps.forEach { (pkg, pattern) ->
            val appName = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            val appIcon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 12) }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            topRow.addView(ImageView(this).apply {
                setImageDrawable(appIcon)
                layoutParams = LinearLayout.LayoutParams(80, 80).also {
                    it.setMargins(0, 0, 16, 0)
                }
            })
            val nameCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameCol.addView(TextView(this).apply {
                text = appName
                textSize = 15f
                setTextColor(0xFF222222.toInt())
                typeface = Typeface.DEFAULT_BOLD
            })
            nameCol.addView(TextView(this).apply {
                text = pkg
                textSize = 10f
                setTextColor(0xFFAAAAAA.toInt())
            })
            topRow.addView(nameCol)
            topRow.addView(Button(this).apply {
                text = "削除"
                textSize = 11f
                setTextColor(0xFFE53935.toInt())
                setBackgroundColor(0x00000000)
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
            card.addView(divider())

            val patternRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 0)
            }
            patternRow.addView(TextView(this).apply {
                text = "パターン："
                textSize = 13f
                setTextColor(0xFF444444.toInt())
            })

            val spinner = Spinner(this)
            val patternNames = VibePattern.values().map { it.displayName }.toTypedArray()
            spinner.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, patternNames)
            spinner.setSelection(pattern.ordinal)
            spinner.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val customArea = buildCustomArea(pkg, pattern)

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

            // ↓ 遅延設定エリアをここに追加（customAreaの前）
            val delayRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 0)
            }
            delayRow.addView(TextView(this).apply {
                text = "遅延(ms)："
                textSize = 13f
                setTextColor(0xFF444444.toInt())
            })
            val delayInput = EditText(this).apply {
                hint = "800"
                textSize = 13f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(AppVibeSettings.getDelay(this@MainActivity, pkg).toString())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            delayRow.addView(delayInput)
            delayRow.addView(Button(this).apply {
                text = "保存"
                textSize = 11f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1976D2.toInt())
                setOnClickListener {
                    val delay = delayInput.text.toString().toLongOrNull() ?: 800L
                    AppVibeSettings.setDelay(this@MainActivity, pkg, delay)
                    Toast.makeText(this@MainActivity, "遅延 ${delay}ms 保存しました",
                        Toast.LENGTH_SHORT).show()
                }
            })
            card.addView(delayRow)  // ← delayRowをcardに追加
            card.addView(customArea) // ← 既存のこの行はそのまま
            listLayout.addView(card)
        }  // ← selectedApps.forEachの閉じ括弧
    }

    private fun buildCustomArea(pkg: String, currentPattern: VibePattern): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (currentPattern == VibePattern.CUSTOM) View.VISIBLE else View.GONE
            setPadding(0, 12, 0, 0)

            addView(TextView(this@MainActivity).apply {
                text = "振動パターン（ms）例: 0,300,150,300,150,600"
                textSize = 11f
                setTextColor(0xFF666666.toInt())
            })
            val patternInput = EditText(this@MainActivity).apply {
                hint = "0,300,150,300,150,600"
                textSize = 13f
                setText(AppVibeSettings.getCustomPattern(this@MainActivity, pkg) ?: "")
            }
            addView(patternInput)

            addView(TextView(this@MainActivity).apply {
                text = "強度（0〜255）例: 0,255,0,255,0,255"
                textSize = 11f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 8, 0, 0)
            })
            val amplitudeInput = EditText(this@MainActivity).apply {
                hint = "0,255,0,255,0,255"
                textSize = 13f
                setText(AppVibeSettings.getCustomAmplitude(this@MainActivity, pkg) ?: "")
            }
            addView(amplitudeInput)

            addView(Button(this@MainActivity).apply {
                text = "💾 カスタムを保存"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF388E3C.toInt())
                setOnClickListener {
                    val p = patternInput.text.toString().trim()
                    val a = amplitudeInput.text.toString().trim()
                    val pCount = p.split(",").size
                    val aCount = a.split(",").size
                    if (pCount != aCount) {
                        Toast.makeText(this@MainActivity,
                            "パターンと強度の数を揃えてください（$pCount vs $aCount）",
                            Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    AppVibeSettings.setCustomPattern(this@MainActivity, pkg, p)
                    AppVibeSettings.setCustomAmplitude(this@MainActivity, pkg, a)
                    Toast.makeText(this@MainActivity, "✅ 保存しました", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        ).also { it.setMargins(0, 8, 0, 8) }
        setBackgroundColor(0xFFEEEEEE.toInt())
    }

    private fun updatePermissionButton(btn: Button) {
        if (isNotificationAccessGranted()) {
            btn.text = "✅ 通知アクセス: ON"
            btn.setTextColor(0xFF388E3C.toInt())
            btn.setBackgroundColor(0xFFE8F5E9.toInt())
        } else {
            btn.text = "⚠️ 通知アクセスをONにする（タップ）"
            btn.setTextColor(0xFFFFFFFF.toInt())
            btn.setBackgroundColor(0xFFE53935.toInt())
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(packageName)
    }
}