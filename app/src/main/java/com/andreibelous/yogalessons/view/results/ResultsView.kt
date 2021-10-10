package com.andreibelous.yogalessons.view.results

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andreibelous.yogalessons.cast
import com.andreibelous.yogalessons.dp
import com.andreibelous.yogalessons.recording.Phase
import com.andreibelous.yogalessons.recording.ProcessedResult

class ResultsView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val itemsAdapter = Adapter(listOf())

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadius = context.dp(24f)
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    (view.height + cornerRadius).toInt(),
                    cornerRadius
                )
            }
        }
        setBackgroundColor(Color.WHITE)
        adapter = itemsAdapter
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    fun bind(model: ResultsViewModel) {
        itemsAdapter.setItems(model.toItems())
    }

    private fun ResultsViewModel.toItems(): List<ResultListItem> {
        val items = mutableListOf<ResultListItem>()

        items.add(
            ResultListItem.HeaderItem(
                token = token,
                data = result.rawAmplitude
            )
        )

        items.add(ResultListItem.ShareItem(shareClickAction))

        result.phases.forEach {
            items.add(ResultListItem.PhaseItem(phase = it))
        }

        items.add(ResultListItem.ShareItem(shareClickAction))

        return items
    }
}

sealed interface ResultListItem {

    data class HeaderItem(
        val token: String,
        val data: List<Double>
    ) : ResultListItem

    data class PhaseItem(
        val phase: Phase
    ) : ResultListItem

    data class ShareItem(val clickAction: () -> Unit) : ResultListItem
}

private class Adapter(
    private var items: List<ResultListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<ResultListItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            HEADER_VIEW_TYPE -> HeaderViewHolder(HeaderView(parent.context))
            PHASE_VIEW_TYPE -> PhaseViewHolder(PhaseView(parent.context))
            SHARE_VIEW_TYPE -> ShareViewHolder(ShareView(parent.context))
            else -> throw Exception("unsupported view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ResultListItem.HeaderItem -> holder.cast<HeaderViewHolder>().bind(item)
            is ResultListItem.PhaseItem -> holder.cast<PhaseViewHolder>().bind(item)
            is ResultListItem.ShareItem -> holder.cast<ShareViewHolder>().bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int = items[position].toViewType()

    override fun getItemCount(): Int = items.size

    private fun ResultListItem.toViewType() =
        when (this) {
            is ResultListItem.HeaderItem -> HEADER_VIEW_TYPE
            is ResultListItem.PhaseItem -> PHASE_VIEW_TYPE
            is ResultListItem.ShareItem -> SHARE_VIEW_TYPE
        }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: ResultListItem.HeaderItem) {
            itemView.cast<HeaderView>().bind(model.token, model.data)
        }
    }

    private class PhaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: ResultListItem.PhaseItem) {
            itemView.cast<PhaseView>().bind(model.phase)
        }
    }

    private class ShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: ResultListItem.ShareItem) {
            itemView.cast<ShareView>().bind { model.clickAction() }
        }
    }

    private companion object {

        private const val HEADER_VIEW_TYPE = 1
        private const val PHASE_VIEW_TYPE = 2
        private const val SHARE_VIEW_TYPE = 3
    }
}


data class ResultsViewModel(
    val token: String,
    val result: ProcessedResult,
    val shareClickAction: () -> Unit
)