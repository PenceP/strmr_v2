package com.strmr.ai.ui.search

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Row
import com.strmr.ai.constant.QueryType
import com.strmr.ai.data.service.BackgroundService
import com.strmr.ai.ui.itemhandling.BaseRowItem
import com.strmr.ai.ui.itemhandling.ItemLauncher
import com.strmr.ai.ui.itemhandling.ItemRowAdapter
import com.strmr.ai.ui.presentation.CardPresenter
import com.strmr.ai.ui.presentation.CustomListRowPresenter
import com.strmr.ai.ui.presentation.MutableObjectAdapter

class SearchFragmentDelegate(
	private val context: Context,
	private val backgroundService: BackgroundService,
	private val itemLauncher: ItemLauncher,
) {
	val rowsAdapter = MutableObjectAdapter<Row>(CustomListRowPresenter())

	fun showResults(searchResultGroups: Collection<SearchResultGroup>) {
		rowsAdapter.clear()
		val adapters = mutableListOf<ItemRowAdapter>()
		for ((labelRes, baseItems) in searchResultGroups) {
			val adapter = ItemRowAdapter(
				context,
				baseItems.toList(),
				CardPresenter(),
				rowsAdapter,
				QueryType.Search
			).apply {
				setRow(ListRow(HeaderItem(context.getString(labelRes)), this))
			}
			adapters.add(adapter)
		}
		for (adapter in adapters) adapter.Retrieve()
	}

	val onItemViewClickedListener = OnItemViewClickedListener { _, item, _, row ->
		if (item !is BaseRowItem) return@OnItemViewClickedListener
		row as ListRow
		val adapter = row.adapter as ItemRowAdapter
		itemLauncher.launch(item as BaseRowItem?, adapter, context)
	}

	val onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
		val baseItem = item?.let { (item as BaseRowItem).baseItem }
		if (baseItem != null) {
			backgroundService.setBackground(baseItem)
		} else {
			backgroundService.clearBackgrounds()
		}
	}
}
