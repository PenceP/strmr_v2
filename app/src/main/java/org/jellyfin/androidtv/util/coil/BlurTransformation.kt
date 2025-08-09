package org.jellyfin.androidtv.util.coil

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil3.size.Size
import coil3.transform.Transformation

class BlurTransformation(
	private val context: Context,
	private val radius: Float = 10f
) : Transformation() {
	init {
		require(radius in 0f..25f) { "radius must be in [0, 25]" }
	}

	override val cacheKey: String = "blur:$radius"

	override suspend fun transform(
		input: Bitmap,
		size: Size,
	): Bitmap {
		val rsContext = RenderScript.create(context)
		val output = Bitmap.createBitmap(input.width, input.height, input.config ?: Bitmap.Config.ARGB_8888)

		val inAlloc = Allocation.createFromBitmap(rsContext, input)
		val outAlloc = Allocation.createFromBitmap(rsContext, output)

		val script = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
		script.setRadius(radius)
		script.setInput(inAlloc)
		script.forEach(outAlloc)

		outAlloc.copyTo(output)

		rsContext.destroy()
		
		return output
	}
}