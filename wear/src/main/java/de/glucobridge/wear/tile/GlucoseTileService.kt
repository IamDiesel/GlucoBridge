package de.glucobridge.wear.tile

import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import de.glucobridge.wear.GlucoseStore
import de.glucobridge.wear.ageShort
import de.glucobridge.wear.arrowOf
import de.glucobridge.wear.zoneColorInt

/** Wear-Tile mit dem aktuellen Glukosewert (liest den lokalen Cache). */
class GlucoseTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val store = GlucoseStore(this)
        val big: String
        val sub: String
        val color: Int
        if (store.has()) {
            big = "${store.display()} ${arrowOf(store.trend)}".trim()
            sub = "${store.unitLabel()} · ${ageShort(store.ts)}"
            color = zoneColorInt(store.zone())
        } else {
            big = "—"; sub = "keine Daten"; color = 0xFFFFFFFF.toInt()
        }

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                Text.Builder(this, big)
                    .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                    .setColor(ColorBuilders.argb(color))
                    .build()
            )
            .addContent(
                Text.Builder(this, sub)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(0xFFCCCCCC.toInt()))
                    .build()
            )
            .build()

        val openApp = ModifiersBuilders.Clickable.Builder()
            .setId("open_app")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName("de.glucobridge.wear.MainActivity")
                            .build()
                    ).build()
            ).build()

        val root = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder().setClickable(openApp).build()
            )
            .addContent(column)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RES_VERSION)
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
            .build()
        return immediate(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        immediate(ResourceBuilders.Resources.Builder().setVersion(RES_VERSION).build())

    private fun <T> immediate(v: T): ListenableFuture<T> =
        ResolvableFuture.create<T>().apply { set(v) }

    companion object { private const val RES_VERSION = "1" }
}
