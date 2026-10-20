package com.example.xtreamtv

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Type(val label: String, val catAction: String, val listAction: String) {
    LIVE("TV", "get_live_categories", "get_live_streams"),
    VOD("Películas", "get_vod_categories", "get_vod_streams"),
    SERIES("Series", "get_series_categories", "get_series")
}

data class Item(val id: String, val name: String, val type: Type, val ext: String = "")
data class Episode(val id: String, val title: String, val ext: String)

class Xtream(server: String, val user: String, val pass: String) {
    private val base = server.trim().trimEnd('/').let { if (it.startsWith("http")) it else "http://$it" }

    private fun get(extra: String): String {
        val u = "$base/player_api.php?username=$user&password=$pass$extra"
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 60000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun arr(extra: String): JSONArray {
        val s = get(extra).trim()
        return if (s.startsWith("[")) JSONArray(s) else JSONArray()
    }

    fun login(): JSONObject = JSONObject(get(""))

    fun categories(t: Type): List<Item> {
        val a = arr("&action=${t.catAction}")
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Item(o.optString("category_id"), o.optString("category_name"), t)
        }
    }

    fun items(t: Type, cat: String): List<Item> {
        val q = if (cat.isEmpty()) "" else "&category_id=$cat"
        val a = arr("&action=${t.listAction}$q")
        val idKey = if (t == Type.SERIES) "series_id" else "stream_id"
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Item(o.optString(idKey), o.optString("name"), t, o.optString("container_extension"))
        }
    }

    fun episodes(seriesId: String): List<Episode> {
        val eps = JSONObject(get("&action=get_series_info&series_id=$seriesId")).opt("episodes")
        val out = ArrayList<Episode>()
        fun add(season: String, a: JSONArray) {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val n = o.optString("episode_num", "${i + 1}")
                out.add(Episode(o.optString("id"), "T$season E$n · ${o.optString("title")}",
                    o.optString("container_extension", "mkv")))
            }
        }
        when (eps) {
            is JSONObject -> eps.keys().asSequence().sortedBy { it.toIntOrNull() ?: 0 }
                .forEach { add(it, eps.getJSONArray(it)) }
            is JSONArray -> for (i in 0 until eps.length()) add("${i + 1}", eps.getJSONArray(i))
        }
        return out
    }

    private fun decode(s: String): String = try {
        String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: Exception) { s }

    fun epg(streamId: String): String {
        val o = JSONObject(get("&action=get_short_epg&stream_id=$streamId&limit=3"))
        val a = o.optJSONArray("epg_listings")
        if (a == null || a.length() == 0) return "Sin guía (EPG)"
        val f = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        for (i in 0 until a.length()) {
            val e = a.getJSONObject(i)
            val st = e.optLong("start_timestamp") * 1000
            val en = e.optLong("stop_timestamp") * 1000
            sb.append(if (i == 0) "AHORA  " else "DESPUÉS  ")
                .append(f.format(Date(st))).append("–").append(f.format(Date(en))).append("\n")
                .append(decode(e.optString("title"))).append("\n")
            if (i == 0) {
                val d = decode(e.optString("description"))
                if (d.isNotBlank()) sb.append(d.take(300)).append("\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    fun url(i: Item): String = when (i.type) {
        Type.LIVE -> "$base/live/$user/$pass/${i.id}.m3u8"
        Type.VOD -> "$base/movie/$user/$pass/${i.id}.${i.ext.ifEmpty { "mp4" }}"
        Type.SERIES -> ""
    }

    fun episodeUrl(e: Episode): String = "$base/series/$user/$pass/${e.id}.${e.ext}"
}

object PlayQueue {
    var urls: List<String> = emptyList()
    var titles: List<String> = emptyList()
    var index = 0
    var live = false
    fun set(u: List<String>, t: List<String>, i: Int, l: Boolean) {
        urls = u; titles = t; index = i; live = l
    }
}
