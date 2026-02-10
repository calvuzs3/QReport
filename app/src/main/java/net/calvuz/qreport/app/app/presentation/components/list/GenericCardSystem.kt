package net.calvuz.qreport.app.app.presentation.components.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * Generic Card Variants System
 *
 * Allows any domain entity to implement FULL, COMPACT, MINIMAL variants
 * using composable content lambdas instead of inheritance
 */

/**
 * Card variant types
 */
enum class CardVariant {
    FULL,     // Complete information with all details
    COMPACT,  // Essential information only
    MINIMAL   // Just core identifier + basic info
}

/**
 * Content provider interface for different card variants
 * Each domain entity implements this to provide composable content for each variant
 */
interface CardContentProvider<T> {

    /**
     * Full variant content - shows all available information
     */
    @Composable
    fun FullContent(
        item: T,
        isSelected: Boolean,
        modifier: Modifier
    )

    /**
     * Compact variant content - shows essential information only
     */
    @Composable
    fun CompactContent(
        item: T,
        isSelected: Boolean,
        modifier: Modifier
    )

    /**
     * Minimal variant content - shows just core information
     */
    @Composable
    fun MinimalContent(
        item: T,
        modifier: Modifier
    )
}

/**
 * Generic Card Composable
 * Handles card styling, elevation, selection state - delegates content to provider
 */
@Composable
fun <T> GenericCard(
    item: T,
    variant: ListViewMode,
    contentProvider: CardContentProvider<T>,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            // Content based on variant
            when (variant) {
                ListViewMode.FULL -> {
                    contentProvider.FullContent(
                        item = item,
                        isSelected = isSelected,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                ListViewMode.COMPACT -> {
                    contentProvider.CompactContent(
                        item = item,
                        isSelected = isSelected,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                ListViewMode.MINIMAL -> {
                    contentProvider.MinimalContent(
                        item = item,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Extension function for easier usage
 */
@Composable
fun <T> T.asCard(
    variant: ListViewMode,
    contentProvider: CardContentProvider<T>,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    GenericCard(
        item = this,
        variant = variant,
        contentProvider = contentProvider,
        isSelected = isSelected,
        isLoading = isLoading,
        modifier = modifier
    )
}

/**
 * Builder pattern for complex card configurations
 */
class CardBuilder<T>(private val item: T) {
    private var variant: ListViewMode = ListViewMode.FULL
    private var isSelected: Boolean = false
    private var isLoading: Boolean = false
    private var modifier: Modifier = Modifier

    fun variant(variant: ListViewMode) = apply { this.variant = variant }
    fun selected(selected: Boolean) = apply { this.isSelected = selected }
    fun loading(loading: Boolean) = apply { this.isLoading = loading }
    fun modifier(modifier: Modifier) = apply { this.modifier = modifier }

    @Composable
    fun build(contentProvider: CardContentProvider<T>) {
        GenericCard(
            item = item,
            variant = variant,
            contentProvider = contentProvider,
            isSelected = isSelected,
            isLoading = isLoading,
            modifier = modifier
        )
    }
}

/**
 * Builder extension function
 */
fun <T> T.cardBuilder() = CardBuilder(this)

/**
 * Common content components that can be reused across different card implementations
 */
object CardComponents {

    @Composable
    fun HeaderRow(
        title: String,
        trailing: @Composable (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }

    @Composable
    fun InfoRow(
        label: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    fun FooterRow(
        leading: @Composable (() -> Unit)? = null,
        trailing: @Composable (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            leading?.invoke() ?: Spacer(Modifier.width(0.dp))
            trailing?.invoke() ?: Spacer(Modifier.width(0.dp))
        }
    }
}

/**
 * Abstract composable functions for common patterns
 */
abstract class BaseCardContentProvider<T> : CardContentProvider<T> {

    /**
     * Common layout for full content - subclasses override content sections
     */
    @Composable
    override fun FullContent(item: T, isSelected: Boolean, modifier: Modifier) {
        Column(modifier = modifier) {
            HeaderSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            MainSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            DetailsSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            FooterSection(item)
        }
    }

    /**
     * Common layout for compact content
     */
    @Composable
    override fun CompactContent(item: T, isSelected: Boolean, modifier: Modifier) {
        Column(modifier = modifier) {
            HeaderSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            MainSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            FooterSection(item)
        }
    }

    /**
     * Common layout for minimal content
     */
    @Composable
    override fun MinimalContent(item: T, modifier: Modifier) {
        Column(modifier = modifier) {
            HeaderSection(item)
        }
    }

    // Abstract sections that subclasses must implement
    @Composable
    protected abstract fun HeaderSection(item: T)

    @Composable
    protected abstract fun MainSection(item: T)

    @Composable
    protected abstract fun DetailsSection(item: T)

    @Composable
    protected abstract fun FooterSection(item: T)
}