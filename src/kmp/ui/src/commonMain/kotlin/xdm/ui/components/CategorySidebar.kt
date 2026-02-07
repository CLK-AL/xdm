package xdm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xdm.ui.screens.Category

/**
 * Category navigation sidebar.
 * Converted from WPF MainWindow.xaml left ListBox (190px wide)
 * and Java MainWindow.java SidePanel.
 *
 * WPF uses: ListBox + DataTemplate { VectorIcon + TextBlock }
 * Java uses: custom SidePanel with JLabel + icon painting
 */
@Composable
fun CategorySidebar(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column {
            // App title area
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "XDM",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider()

            // Category list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(Category.entries) { category ->
                    CategoryItem(
                        category = category,
                        isSelected = category == selectedCategory,
                        onClick = { onCategorySelected(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon = when (category) {
        Category.All -> Icons.Default.Download
        Category.Incomplete -> Icons.Default.Pending
        Category.Finished -> Icons.Default.CheckCircle
        Category.Compressed -> Icons.Default.FolderZip
        Category.Documents -> Icons.Default.Description
        Category.Music -> Icons.Default.MusicNote
        Category.Video -> Icons.Default.VideoLibrary
        Category.Programs -> Icons.Default.Apps
        Category.Other -> Icons.Default.MoreHoriz
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = category.label,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
