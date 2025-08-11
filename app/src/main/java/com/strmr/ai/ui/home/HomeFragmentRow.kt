package com.strmr.ai.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import com.strmr.ai.ui.presentation.CardPresenter
import com.strmr.ai.ui.presentation.MutableObjectAdapter

interface HomeFragmentRow {
	fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>)
}
