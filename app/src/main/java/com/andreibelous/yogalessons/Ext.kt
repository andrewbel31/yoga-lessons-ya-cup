package com.andreibelous.yogalessons

import android.content.Context
import android.view.View
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlin.math.ceil

fun <T> T?.toObservable() = if (this == null) Observable.empty<T>() else Observable.just(this)

fun <T> ((T) -> Unit).asConsumer(): Consumer<T> = Consumer { t -> this@asConsumer.invoke(t) }

fun Context.dp(value: Float): Float {
    return if (value == 0f) {
        0f
    } else ceil(resources.displayMetrics.density * value)
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}