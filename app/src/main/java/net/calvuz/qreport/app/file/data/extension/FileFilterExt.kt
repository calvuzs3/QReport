package net.calvuz.qreport.app.file.data.extension

import net.calvuz.qreport.app.file.domain.model.CoreFileInfo
import net.calvuz.qreport.app.file.domain.model.FileFilter
import kotlin.text.matches

/**
 * Extension function per FileFilter
 */
fun FileFilter.matches(fileInfo: CoreFileInfo): Boolean {
    // Check extension
    extensions?.let { allowedExt ->
        val fileExt = fileInfo.extension
        if (fileExt == null || fileExt !in allowedExt) {
            return false
        }
    }

    // Check name pattern
    namePattern?.let { pattern ->
        if (!fileInfo.name.matches(pattern.toRegex())) {
            return false
        }
    }

    // Check size constraints
    minSize?.let { min ->
        if (fileInfo.size < min) return false
    }

    maxSize?.let { max ->
        if (fileInfo.size > max) return false
    }

    // Check time constraints
    olderThan?.let { cutoff ->
        if (fileInfo.lastModified >= cutoff) return false
    }

    newerThan?.let { cutoff ->
        if (fileInfo.lastModified <= cutoff) return false
    }

    return true
}