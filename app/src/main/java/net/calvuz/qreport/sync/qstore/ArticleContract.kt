package net.calvuz.qreport.sync.qstore

import android.net.Uri

object ArticleContract {
    const val AUTHORITY       = "net.calvuz.qstore.provider"
    const val PERMISSION_READ = "net.calvuz.qstore.permission.READ_ARTICLES"
    const val ACTION_PICK     = "net.calvuz.qstore.action.PICK_ARTICLES"

    val ARTICLES_URI: Uri = Uri.parse("content://$AUTHORITY/articles")

    object Articles {
        const val UUID            = "uuid"
        const val NAME            = "name"
        const val DESCRIPTION     = "description"
        const val CATEGORY_ID     = "category_id"
        const val UNIT_OF_MEASURE = "unit_of_measure"
        const val CODE_OEM        = "code_oem"
        const val CODE_ERP        = "code_erp"
        const val CODE_BM         = "code_bm"
        const val NOTES           = "notes"
        const val UPDATED_AT      = "updated_at"
    }

    object PickerExtras {
        const val PRESELECTED_UUIDS = "preselected_uuids"
        const val SELECTED_UUIDS    = "selected_uuids"
    }
}
