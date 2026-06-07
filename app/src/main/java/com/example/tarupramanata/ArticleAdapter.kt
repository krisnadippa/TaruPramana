package com.example.tarupramanata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ArticleAdapter(
    private var articleList: List<Article>,
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgArticle: ImageView = view.findViewById(R.id.imgArticle)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSnippet: TextView = view.findViewById(R.id.tvSnippet)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article_row, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articleList[position]

        holder.tvTitle.text = article.title
        holder.tvCategory.text = article.category
        holder.tvSnippet.text = article.snippet
        holder.tvTime.text = article.date

        holder.imgArticle.setImageDrawable(null)
        if (article.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(article.imageUrl)
                .into(holder.imgArticle)
        } else {
            Glide.with(holder.itemView.context).clear(holder.imgArticle)
        }

        holder.itemView.setOnClickListener {
            onItemClick(article)
        }
    }

    override fun getItemCount(): Int = articleList.size

    // --- FUNGSI BARU UNTUK SEARCH ---
    fun updateData(newList: List<Article>) {
        articleList = newList
        notifyDataSetChanged()
    }
}