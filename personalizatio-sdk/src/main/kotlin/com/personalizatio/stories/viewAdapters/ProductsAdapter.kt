package com.personalizatio.stories.viewAdapters

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.base.Strings
import com.personalizatio.Product
import com.personalizatio.R
import com.personalizatio.SDK
import com.personalizatio.stories.views.StoriesView

class ProductsAdapter(private val storiesView: StoriesView) :
    RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {
    private var products: List<Product> = ArrayList()
    private var storyId = 0
    private var slideId: String = ""

    @SuppressLint("NotifyDataSetChanged")
    fun setProducts(products: List<Product>, storyId: Int, slideId: String) {
        this.products = products
        this.storyId = storyId
        this.slideId = slideId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.product, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int {
        return products.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(android.R.id.icon)
        val name: TextView = view.findViewById(android.R.id.text1)
        private val oldPrice: TextView = view.findViewById(R.id.oldprice)
        private val discount: TextView = view.findViewById(R.id.discount)
        private val price: TextView = view.findViewById(R.id.price)

        init {
            oldPrice.paintFlags = oldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        fun bind(product: Product) {
            Glide.with(image.context).load(product.image).into(image)
            name.text = product.name
            val settings = storiesView.settings
            name.typeface = settings.font_family
            discount.typeface = settings.font_family
            oldPrice.typeface = settings.font_family
            price.typeface = settings.font_family
            if (Strings.isNullOrEmpty(product.oldPrice)) {
                discount.visibility = View.GONE
                oldPrice.visibility = View.GONE
            } else {
                discount.visibility = View.VISIBLE
                oldPrice.visibility = View.VISIBLE
                oldPrice.text = product.oldPrice
                discount.text = String.format("-%s%%", product.discount)
            }
            price.text = product.price
            itemView.setOnClickListener {
                Log.d(SDK.TAG, "click: " + product.name + ", " + product.url)
                try {
                    if (storiesView.itemClickListener?.onClick(product) == true) {
                        itemView.context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(if (Strings.isNullOrEmpty(product.deeplink)) product.url else product.deeplink)
                            )
                        )
                    }
                    SDK.instance.storiesManager.trackStory("click", storiesView.code, storyId, slideId)
                } catch (e: ActivityNotFoundException) {
                    Log.e(SDK.TAG, e.message, e)
                    Toast.makeText(itemView.context, "Unknown error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
