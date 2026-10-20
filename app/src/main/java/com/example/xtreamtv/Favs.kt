package com.example.xtreamtv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Favs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("xt", Context.MODE_PRIVATE)
    private var cache: MutableList<Item> = load()

    private fun load(): MutableList<Item> {
        val a = JSONArray(sp.getString("favs", "[]"))
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Item(o.getString("id"), o.getString("name"), Type.valueOf(o.getString("type")), o.optString("ext"))
        }.toMutableList()
    }

    private fun save() {
        val a = JSONArray()
        cache.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name)
                .put("type", it.type.name).put("ext", it.ext))
        }
        sp.edit().putString("favs", a.toString()).apply()
    }

    fun all(): List<Item> = cache.toList()
    fun isFav(i: Item) = cache.any { it.id == i.id && it.type == i.type }

    /** Devuelve true si queda como favorito. */
    fun toggle(i: Item): Boolean {
        val existing = cache.firstOrNull { it.id == i.id && it.type == i.type }
        return if (existing != null) { cache.remove(existing); save(); false }
        else { cache.add(i); save(); true }
    }
}
