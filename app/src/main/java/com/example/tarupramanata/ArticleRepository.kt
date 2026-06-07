package com.example.tarupramanata

import android.content.Context
import org.json.JSONArray
import java.nio.charset.Charset

object ArticleRepository {

    fun getArticles(context: Context): List<Article> {
        val jsonString = loadJSONFromAsset(context) ?: return emptyList()
        return parseJSON(jsonString, context)
    }

    private fun loadJSONFromAsset(context: Context): String? {
        return try {
            val inputStream = context.assets.open("data_artikel.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun parseJSON(jsonString: String, context: Context): List<Article> {
        val articleList = ArrayList<Article>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // --- PARSING TUTORIAL STEPS ---
                val tutorialList = ArrayList<TutorialStepModel>()
                val stepsArray = obj.optJSONArray("tutorial_steps")

                if (stepsArray != null) {
                    for (j in 0 until stepsArray.length()) {
                        val stepObj = stepsArray.getJSONObject(j)
                        tutorialList.add(
                            TutorialStepModel(
                                desc = stepObj.optString("desc"),
                                img = stepObj.optString("img")
                            )
                        )
                    }
                }
                // ---------------------------------------------

                val article = Article(
                    title = obj.getString("title"),
                    category = obj.getString("category"),
                    snippet = obj.getString("snippet"),
                    content = obj.getString("content"),
                    date = obj.getString("date"),
                    isTrending = obj.optBoolean("is_trending", false),

                    // >>> BACA DATA BARU <<<
                    bagian = obj.optString("bagian", ""),
                    bahan = obj.optString("bahan", ""),
                    tags = obj.optString("tags", ""),
                    caraPengolahan = obj.optString("cara_pengolahan", ""),
                    caraPenggunaan = obj.optString("cara_penggunaan", ""),

                    // >>> BACA LINK VIDEO DARI JSON <<<
                    videoUrl = obj.optString("video_url", ""),

                    tutorialSteps = tutorialList
                )
                articleList.add(article)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articleList
    }
}