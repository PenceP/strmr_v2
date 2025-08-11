package com.strmr.ai.di

import com.strmr.ai.util.ImageHelper
import org.koin.dsl.module

val utilsModule = module {
	single { ImageHelper(get()) }
}
