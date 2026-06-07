package com.example.tarupramanata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TrendingAdapter(
    private val trendingList: List<Article>,
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder>() {

    class TrendingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgTrending: ImageView = view.findViewById(R.id.imgTrending)
        val tvTitle: TextView = view.findViewById(R.id.tvFeaturedTitle)
        val tvMeta: TextView = view.findViewById(R.id.tvFeaturedMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending, parent, false)

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return TrendingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
        // --- LOGIKA INFINITE LOOP ---
        // Gunakan operator modulo (%) untuk mengulang data list
        val actualPosition = position % trendingList.size
        val article = trendingList[actualPosition]

        holder.tvTitle.text = article.title
        holder.tvMeta.text = article.date

        holder.imgTrending.setImageDrawable(null)
        if (article.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(article.imageUrl)
                .into(holder.imgTrending)
        } else {
            Glide.with(holder.itemView.context).clear(holder.imgTrending)
        }

        holder.itemView.setOnClickListener {
            onItemClick(article)
        }
    }

    // --- BUAT SEOLAH-OLAH ITEM TAK TERBATAS ---
    override fun getItemCount(): Int = Int.MAX_VALUE
}