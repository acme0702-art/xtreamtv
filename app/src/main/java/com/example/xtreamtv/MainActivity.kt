package com.example.xtreamtv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var api: Xtream
    private lateinit var favs: Favs
    private lateinit var catList: ListView
    private lateinit var itemList: ListView
    private lateinit var epgView: TextView
    private lateinit var search: EditText

    private val handler = Handler(Looper.getMainLooper())
    private val tabTypes: List<Type?> = listOf(Type.LIVE, Type.VOD, Type.SERIES, null)
    private val tabButtons = ArrayList<Button>()
    private val catCache = HashMap<Type, List<Item>>()

    private var tab: Type? = Type.LIVE
    private var cats: List<Item> = emptyList()
    private var all: List<Item> = emptyList()
    private var shown: List<Item> = emptyList()

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun lp(w: Float) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("xt", MODE_PRIVATE)
        api = Xtream(sp.getString("s", "")!!, sp.getString("u", "")!!, sp.getString("p", "")!!)
        favs = Favs(this)

        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        tabTypes.forEach { t ->
            val btn = Button(this).apply {
                text = t?.label ?: "★ Favoritos"
                setOnClickListener { selectTab(t) }
            }
            tabButtons.add(btn); bar.addView(btn)
        }
        search = EditText(this).apply {
            hint = "Buscar…"; setSingleLine(); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { applyFilter() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        bar.addView(search, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(Button(this).apply { text = "Cuenta"; setOnClickListener { finish() } })

        catList = styledList(); itemList = styledList()
        epgView = TextView(this).apply {
            setTextColor(0xFFCCCCCC.toInt()); textSize = 16f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val body = LinearLayout(this).apply {
            addView(catList, lp(1f)); addView(itemList, lp(2f)); addView(epgView, lp(1.2f))
        }
        val hint = TextView(this).apply {
            text = "OK: reproducir  ·  Mantén OK: añadir/quitar favorito  ·  En directo: ▲▼ cambia de canal"
            setTextColor(Color.GRAY); textSize = 13f; setPadding(dp(16), dp(4), dp(16), dp(6))
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101018.toInt())
            addView(bar)
            addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(hint)
        })

        catList.setOnItemClickListener { _, _, i, _ -> loadItems(cats[i]) }
        itemList.setOnItemClickListener { _, _, i, _ -> open(shown[i]) }
        itemList.setOnItemLongClickListener { _, _, i, _ ->
            val on = favs.toggle(shown[i])
            toast(if (on) "Añadido a favoritos" else "Quitado de favoritos")
            if (tab == null) { all = favs.all(); applyFilter() }
            else (itemList.adapter as BaseAdapter).notifyDataSetChanged()
            true
        }
        itemList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                showEpg(shown.getOrNull(pos))
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        selectTab(Type.LIVE)
    }

    private fun styledList() = ListView(this).apply {
        selector = ColorDrawable(0x663D5AFE)
        divider = null
    }

    private fun adapter(list: List<Item>, showFav: Boolean) =
        object : ArrayAdapter<Item>(this@MainActivity, android.R.layout.simple_list_item_1, list) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val t = super.getView(position, convertView, parent) as TextView
                val item = getItem(position)!!
                var s = item.name
                if (showFav) {
                    if (tab == null) s = "[${item.type.label}] $s"
                    if (favs.isFav(item)) s = "★ $s"
                }
                t.text = s
                t.setTextColor(Color.WHITE); t.textSize = 20f; t.maxLines = 2
                t.setPadding(dp(16), dp(12), dp(16), dp(12))
                return t
            }
        }

    private fun selectTab(t: Type?) {
        tab = t
        tabButtons.forEachIndexed { i, b -> b.alpha = if (tabTypes[i] == t) 1f else 0.55f }
        search.setText("")
        epgView.text = ""
        all = emptyList(); applyFilter()
        if (t == null) {
            catList.visibility = View.GONE
            all = favs.all(); applyFilter()
            itemList.requestFocus()
        } else {
            catList.visibility = View.VISIBLE
            loadCats(t)
        }
    }

    private fun loadCats(t: Type) {
        catCache[t]?.let { setCats(it); return }
        toast("Cargando…")
        thread {
            try {
                val c = listOf(Item("", "Todos", t)) + api.categories(t)
                catCache[t] = c
                runOnUiThread { if (tab == t) setCats(c) }
            } catch (e: Exception) { err(e) }
        }
    }

    private fun setCats(c: List<Item>) {
        cats = c
        catList.adapter = adapter(c, false)
        catList.requestFocus()
    }

    private fun loadItems(cat: Item) {
        toast("Cargando…")
        thread {
            try {
                val list = api.items(cat.type, cat.id)
                runOnUiThread {
                    if (tab != cat.type) return@runOnUiThread
                    all = list; applyFilter(); itemList.requestFocus()
                }
            } catch (e: Exception) { err(e) }
        }
    }

    private fun applyFilter() {
        val q = search.text.toString().trim().lowercase()
        shown = if (q.isEmpty()) all else all.filter { it.name.lowercase().contains(q) }
        itemList.adapter = adapter(shown, true)
    }

    private fun showEpg(item: Item?) {
        handler.removeCallbacksAndMessages(null)
        if (item == null || item.type != Type.LIVE) { epgView.text = ""; return }
        epgView.text = item.name
        handler.postDelayed({
            thread {
                val t = try { api.epg(item.id) } catch (e: Exception) { "Sin guía (EPG)" }
                runOnUiThread {
                    if (shown.getOrNull(itemList.selectedItemPosition) == item)
                        epgView.text = item.name + "\n\n" + t
                }
            }
        }, 400)
    }

    private fun open(item: Item) {
        when (item.type) {
            Type.LIVE -> {
                val q = shown.filter { it.type == Type.LIVE }
                PlayQueue.set(q.map { api.url(it) }, q.map { it.name }, q.indexOf(item), true)
                startActivity(Intent(this, PlayerActivity::class.java))
            }
            Type.VOD -> {
                PlayQueue.set(listOf(api.url(item)), listOf(item.name), 0, false)
                startActivity(Intent(this, PlayerActivity::class.java))
            }
            Type.SERIES -> showEpisodes(item)
        }
    }

    private fun showEpisodes(item: Item) {
        toast("Cargando episodios…")
        thread {
            try {
                val eps = api.episodes(item.id)
                runOnUiThread {
                    if (eps.isEmpty()) { toast("Sin episodios"); return@runOnUiThread }
                    AlertDialog.Builder(this)
                        .setTitle(item.name)
                        .setItems(eps.map { it.title }.toTypedArray()) { _, i ->
                            PlayQueue.set(eps.map { api.episodeUrl(it) }, eps.map { it.title }, i, false)
                            startActivity(Intent(this, PlayerActivity::class.java))
                        }.show()
                }
            } catch (e: Exception) { err(e) }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun err(e: Exception) = runOnUiThread {
        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
