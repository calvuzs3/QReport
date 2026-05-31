package net.calvuz.qreport.client.island.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Supported robotic island types — POLY family.
 *
 * Display strings and icons are NOT stored here:
 *  - [labelResId] / [descriptionResId] are resolved via stringResource() in the UI
 *  - Icons are mapped in the presentation layer (IslandTypeIcons.kt or similar)
 *
 * [code] is the short technical identifier used in documents and DB values.
 */
@Serializable
enum class IslandType(
    val code: String,
    val labelResId: Int,
    val descriptionResId: Int
) {
    POLY_MOVE(
        code = "MOVE",
        labelResId = R.string.island_type_poly_move,
        descriptionResId = R.string.island_type_poly_move_desc
    ),
    POLY_CAST(
        code = "CAST",
        labelResId = R.string.island_type_poly_cast,
        descriptionResId = R.string.island_type_poly_cast_desc
    ),
    POLY_EBT(
        code = "EBT",
        labelResId = R.string.island_type_poly_ebt,
        descriptionResId = R.string.island_type_poly_ebt_desc
    ),
    POLY_TAG_BLE(
        code = "TAG_BLE",
        labelResId = R.string.island_type_poly_tag_ble,
        descriptionResId = R.string.island_type_poly_tag_ble_desc
    ),
    POLY_TAG_FC(
        code = "TAG_FC",
        labelResId = R.string.island_type_poly_tag_fc,
        descriptionResId = R.string.island_type_poly_tag_fc_desc
    ),
    POLY_TAG_V(
        code = "TAG_V",
        labelResId = R.string.island_type_poly_tag_v,
        descriptionResId = R.string.island_type_poly_tag_v_desc
    ),
    POLY_SAMPLE(
        code = "SAMPLE",
        labelResId = R.string.island_type_poly_sample,
        descriptionResId = R.string.island_type_poly_sample_desc
    );

    companion object
}