# 🎨 QReport - UI/UX Guidelines

**Versione:** 1.0
**Data:** Ottobre 2025
**Target:** QReport Android App
**Framework:** Jetpack Compose + Material Design 3

---

## 📋 INDICE

1. [Design Philosophy](#1-design-philosophy)
2. [Material Design 3 System](#2-material-design-3-system)
3. [Color System](#3-color-system)
4. [Typography](#4-typography)
5. [Component Library](#5-component-library)
6. [Navigation Structure](#6-navigation-structure)
7. [Screen Layouts](#7-screen-layouts)
8. [Interactive Patterns](#8-interactive-patterns)
9. [Responsive Design](#9-responsive-design)
10. [Accessibility](#10-accessibility)
11. [Implementation Guide](#11-implementation-guide)

---

## 1. DESIGN PHILOSOPHY

### 1.1 Principi Core

#### 🎯 **Efficiency First**
- **Task-focused:** Ogni schermata supporta un obiettivo specifico del tecnico
- **Quick Actions:** Accesso rapido alle funzioni più utilizzate
- **Progress Clarity:** Stato avanzamento sempre visibile
- **One-Hand Operation:** Utilizzo prevalentemente con una mano

#### 🏭 **Industrial Context**
- **Robust UI:** Elementi touch grandi e facilmente selezionabili
- **High Contrast:** Leggibilità ottimale in ambienti industriali
- **Dirt Resistance:** Design che nasconde impronte e polvere
- **Glove-Friendly:** Interazioni compatibili con guanti da lavoro

#### 📱 **Mobile-First**
- **Portrait Priority:** Progettato per uso verticale primario
- **Thumb Zone:** Controlli principali nell'area raggiungibile dal pollice
- **Landscape Support:** Funzionale ma non prioritario

#### 🔄 **Offline Reliability**
- **Local State:** Feedback immediato per ogni azione
- **Progress Indicators:** Stato di sincronizzazione chiaro
- **Error Recovery:** Gestione graceful degli errori

### 1.2 User Personas

#### 👨‍🔧 **Primary: Marco - Tecnico Manutentore**
- **Età:** 35-50 anni
- **Contesto:** In movimento tra isole robotizzate
- **Device:** Smartphone 6" (preferred), a volte tablet
- **Pain Points:** Tempo limitato, ambiente rumoroso, mani spesso occupate
- **Goals:** Completare check-up velocemente e accuratamente

#### 👩‍💼 **Secondary: Elena - Supervisore Tecnico**
- **Età:** 40-55 anni
- **Contesto:** Ufficio + occasionalmente sul campo
- **Device:** Tablet 10" (preferred), smartphone
- **Pain Points:** Revisione multipli report, coordinamento team
- **Goals:** Overview rapida, approvazione report, analisi trend

---

## 2. MATERIAL DESIGN 3 SYSTEM

### 2.1 Design Tokens Architecture

```kotlin
// QReportTheme.kt - Struttura base theme
object QReportDesignTokens {

    // Spacing Scale (4dp base unit)
    object Spacing {
        val xs = 4.dp      // Padding minimo
        val sm = 8.dp      // Spacing standard
        val md = 16.dp     // Spacing sezioni
        val lg = 24.dp     // Margini grandi
        val xl = 32.dp     // Separatori principali
        val xxl = 48.dp    // Header spacing
    }

    // Corner Radius
    object Corners {
        val none = 0.dp
        val sm = 4.dp      // Input fields
        val md = 8.dp      // Cards, buttons
        val lg = 12.dp     // Dialogs
        val xl = 16.dp     // Bottom sheets
        val full = 50.dp   // Pills, FAB
    }

    // Elevation
    object Elevation {
        val none = 0.dp
        val sm = 1.dp      // Cards
        val md = 3.dp      // Raised elements
        val lg = 6.dp      // Navigation
        val xl = 12.dp     // Dialogs
    }
}
```

### 2.2 Motion Design

```kotlin
// Animazioni standard per QReport
object QReportMotion {

    // Durate (Material 3 standard)
    const val DURATION_SHORT = 150L     // Micro-interactions
    const val DURATION_MEDIUM = 300L    // Navigazione
    const val DURATION_LONG = 500L      // Large transitions

    // Easing curves
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    // Transizioni comuni
    fun slideUpTransition() = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EmphasizedEasing)
    )
}
```

---

## 3. COLOR SYSTEM

### 3.1 Brand Colors

```kotlin
// QReportColors.kt - Sistema colori industriale
object QReportColors {

    // Primary: Blu industriale sicuro e professionale
    val Primary = Color(0xFF1976D2)        // Blue 700
    val PrimaryContainer = Color(0xFFD1E7FF) // Light blue
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryContainer = Color(0xFF001A41)

    // Secondary: Grigio metallico
    val Secondary = Color(0xFF455A64)       // Blue Grey 700
    val SecondaryContainer = Color(0xFFCFD8DC) // Blue Grey 100
    val OnSecondary = Color(0xFFFFFFFF)
    val OnSecondaryContainer = Color(0xFF263238)

    // Status Colors - Semantici per check items
    val Success = Color(0xFF2E7D32)         // Green 800
    val SuccessContainer = Color(0xFFE8F5E8)
    val Warning = Color(0xFFED6C02)         // Orange 800
    val WarningContainer = Color(0xFFFFF4E6)
    val Error = Color(0xFFD32F2F)           // Red 700
    val ErrorContainer = Color(0xFFFFE6E6)
    val Pending = Color(0xFF1976D2)         // Same as primary
    val PendingContainer = Color(0xFFE3F2FD)

    // Neutral Colors
    val Surface = Color(0xFFFAFAFA)         // Grey 50
    val SurfaceVariant = Color(0xFFF5F5F5)  // Grey 100
    val OnSurface = Color(0xFF1C1B1F)
    val OnSurfaceVariant = Color(0xFF49454F)
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)
}
```

### 3.2 Status Color Usage

#### 🟢 **Success (OK)**
- **Uso:** Check items completati con successo
- **Contesto:** Componenti funzionanti, test passati
- **UI:** Icone, badges, progress indicators

#### 🟡 **Warning (NOK)**
- **Uso:** Problemi rilevati che richiedono attenzione
- **Contesto:** Anomalie, usura, parametri fuori range
- **UI:** Alert cards, warning indicators

#### 🔴 **Error (Critical)**
- **Uso:** Problemi critici che richiedono intervento immediato
- **Contesto:** Guasti, blocchi sistema, errori gravi
- **UI:** Error dialogs, critical alerts

#### 🔵 **Pending**
- **Uso:** Check items non ancora completati
- **Contesto:** Attività in corso, da verificare
- **UI:** Progress indicators, placeholder states

#### ⚫ **NA (Not Applicable)**
- **Uso:** Check items non applicabili al contesto
- **Contesto:** Opzioni non pertinenti, moduli non installati
- **UI:** Disabled states, greyed out items

---

## 4. TYPOGRAPHY

### 4.1 Type Scale

```kotlin
// QReportTypography.kt - Scala tipografica ottimizzata per mobile
val QReportTypography = Typography(

    // Headers
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),

    headlineLarge = TextStyle(         // Screen titles
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),

    headlineMedium = TextStyle(        // Section headers
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),

    headlineSmall = TextStyle(         // Subsection headers
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // Titles
    titleLarge = TextStyle(            // Card titles, dialogs
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    titleMedium = TextStyle(           // Check item titles
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),

    titleSmall = TextStyle(            // Component labels
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body
    bodyLarge = TextStyle(             // Main content
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),

    bodyMedium = TextStyle(            // Secondary content
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),

    bodySmall = TextStyle(             // Supporting text
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // Labels
    labelLarge = TextStyle(            // Button text
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    labelMedium = TextStyle(           // Input labels
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),

    labelSmall = TextStyle(            // Tags, chips
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

### 4.2 Typography Usage

| Element | Style | Context |
|---------|-------|---------|
| **Screen Title** | headlineLarge | "Check-up Robot Saldatura" |
| **Section Header** | headlineMedium | "Sistema Pneumatico" |
| **Card Title** | titleLarge | "Stato Generale" |
| **Check Item** | titleMedium | "Pressione aria compressa" |
| **Description** | bodyMedium | Note e descrizioni dettagliate |
| **Button** | labelLarge | "Completa Check-up" |
| **Input Label** | labelMedium | "Note aggiuntive" |
| **Status Badge** | labelSmall | "OK", "NOK", "NA" |

---

## 5. COMPONENT LIBRARY

### 5.1 Core Components

#### 5.1.1 CheckItemCard

```kotlin
@Composable
fun CheckItemCard(
    item: CheckItem,
    onStatusChange: (CheckItemStatus) -> Unit,
    onNoteChange: (String) -> Unit,
    onPhotoAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                CheckItemStatus.OK -> QReportColors.SuccessContainer
                CheckItemStatus.NOK -> QReportColors.WarningContainer
                CheckItemStatus.CRITICAL -> QReportColors.ErrorContainer
                CheckItemStatus.PENDING -> QReportColors.PendingContainer
                CheckItemStatus.NA -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(QReportDesignTokens.Spacing.md)
        ) {
            // Header con titolo e status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                CriticalityBadge(criticality = item.criticality)
            }

            Spacer(height = QReportDesignTokens.Spacing.sm)

            // Status buttons
            StatusButtonRow(
                currentStatus = item.status,
                onStatusChange = onStatusChange
            )

            // Note section se presente
            if (item.note.isNotEmpty() || item.status == CheckItemStatus.NOK) {
                Spacer(height = QReportDesignTokens.Spacing.sm)

                OutlinedTextField(
                    value = item.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // Photo section
            if (item.photos.isNotEmpty()) {
                Spacer(height = QReportDesignTokens.Spacing.sm)
                PhotoThumbnailRow(
                    photos = item.photos,
                    onPhotoAdd = onPhotoAdd
                )
            } else {
                Spacer(height = QReportDesignTokens.Spacing.sm)
                IconButton(onClick = onPhotoAdd) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Aggiungi foto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

#### 5.1.2 StatusButtonRow

```kotlin
@Composable
fun StatusButtonRow(
    currentStatus: CheckItemStatus,
    onStatusChange: (CheckItemStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.sm)
    ) {
        StatusButton(
            text = "OK",
            icon = Icons.Outlined.CheckCircle,
            isSelected = currentStatus == CheckItemStatus.OK,
            color = QReportColors.Success,
            onClick = { onStatusChange(CheckItemStatus.OK) },
            modifier = Modifier.weight(1f)
        )

        StatusButton(
            text = "NOK",
            icon = Icons.Outlined.Warning,
            isSelected = currentStatus == CheckItemStatus.NOK,
            color = QReportColors.Warning,
            onClick = { onStatusChange(CheckItemStatus.NOK) },
            modifier = Modifier.weight(1f)
        )

        StatusButton(
            text = "NA",
            icon = Icons.Outlined.Block,
            isSelected = currentStatus == CheckItemStatus.NA,
            color = MaterialTheme.colorScheme.outline,
            onClick = { onStatusChange(CheckItemStatus.NA) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outline
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(width = 4.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
```

#### 5.1.3 ProgressHeader

```kotlin
@Composable
fun ProgressHeader(
    checkupTitle: String,
    completedItems: Int,
    totalItems: Int,
    criticalIssues: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(QReportDesignTokens.Spacing.md)
        ) {
            Text(
                text = checkupTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(height = QReportDesignTokens.Spacing.sm)

            // Progress bar
            LinearProgressIndicator(
                progress = { completedItems.toFloat() / totalItems },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(height = QReportDesignTokens.Spacing.sm)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$completedItems/$totalItems completati",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (criticalIssues > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = QReportColors.Error
                        )
                        Spacer(width = 4.dp)
                        Text(
                            text = "$criticalIssues critici",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QReportColors.Error
                        )
                    }
                }
            }
        }
    }
}
```

### 5.2 Specialized Components

#### 5.2.1 IslandTypeSelector

```kotlin
@Composable
fun IslandTypeSelector(
    selectedType: IslandType?,
    onTypeSelected: (IslandType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Tipo Isola Robotizzata",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = QReportDesignTokens.Spacing.md)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.sm)
        ) {
            items(IslandType.values()) { type ->
                IslandTypeCard(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

@Composable
private fun IslandTypeCard(
    type: IslandType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(QReportDesignTokens.Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(height = QReportDesignTokens.Spacing.sm)

            Text(
                text = type.displayName,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

---

## 6. NAVIGATION STRUCTURE

### 6.1 Navigation Architecture

```kotlin
// QReportDestinations.kt - Route definitions
object QReportDestinations {
    const val HOME = "home"
    const val CHECKUP_LIST = "checkup_list"
    const val CHECKUP_DETAIL = "checkup_detail/{checkupId}"
    const val CHECKUP_CREATE = "checkup_create"
    const val SETTINGS = "settings"
    const val EXPORT = "export"

    fun checkupDetail(checkupId: String) = "checkup_detail/$checkupId"
}

// Bottom Navigation Items
enum class QReportTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
) {
    HOME(
        title = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        route = QReportDestinations.HOME
    ),
    CHECKUPS(
        title = "Check-up",
        icon = Icons.Outlined.Assignment,
        selectedIcon = Icons.Filled.Assignment,
        route = QReportDestinations.CHECKUP_LIST
    ),
    EXPORT(
        title = "Export",
        icon = Icons.Outlined.FileDownload,
        selectedIcon = Icons.Filled.FileDownload,
        route = QReportDestinations.EXPORT
    ),
    SETTINGS(
        title = "Impostazioni",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        route = QReportDestinations.SETTINGS
    )
}
```

### 6.2 Navigation Component

```kotlin
@Composable
fun QReportBottomNavigation(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = QReportDesignTokens.Elevation.sm
    ) {
        QReportTab.values().forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == tab.route)
                            tab.selectedIcon
                        else
                            tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
```

### 6.3 Screen Transitions

```kotlin
// QReportNavigation.kt - Navigation with transitions
@Composable
fun QReportNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = QReportDestinations.HOME,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(QReportMotion.DURATION_MEDIUM)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(QReportMotion.DURATION_MEDIUM)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(QReportMotion.DURATION_MEDIUM)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(QReportMotion.DURATION_MEDIUM)
            )
        }
    ) {
        composable(QReportDestinations.HOME) {
            HomeScreen(
                onNavigateToCheckupCreate = {
                    navController.navigate(QReportDestinations.CHECKUP_CREATE)
                },
                onNavigateToCheckupDetail = { checkupId ->
                    navController.navigate(QReportDestinations.checkupDetail(checkupId))
                }
            )
        }

        composable(QReportDestinations.CHECKUP_CREATE) {
            CheckupCreateScreen(
                onNavigateBack = { navController.popBackStack() },
                onCheckupCreated = { checkupId ->
                    navController.navigate(QReportDestinations.checkupDetail(checkupId)) {
                        popUpTo(QReportDestinations.HOME)
                    }
                }
            )
        }

        composable(
            route = QReportDestinations.CHECKUP_DETAIL,
            arguments = listOf(navArgument("checkupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val checkupId = backStackEntry.arguments?.getString("checkupId") ?: ""
            CheckupDetailScreen(
                checkupId = checkupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Altri screen...
    }
}
```

---

## 7. SCREEN LAYOUTS

### 7.1 Home Screen

```kotlin
@Composable
fun HomeScreen(
    onNavigateToCheckupCreate: () -> Unit,
    onNavigateToCheckupDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(QReportDesignTokens.Spacing.md)
    ) {
        // Header con stats
        HomeStatsCard(
            activeCheckups = viewModel.activeCheckups,
            completedToday = viewModel.completedToday,
            pendingIssues = viewModel.pendingIssues
        )

        Spacer(height = QReportDesignTokens.Spacing.lg)

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.md)
        ) {
            PrimaryActionCard(
                title = "Nuovo Check-up",
                description = "Inizia un nuovo controllo",
                icon = Icons.Outlined.Add,
                onClick = onNavigateToCheckupCreate,
                modifier = Modifier.weight(1f)
            )

            SecondaryActionCard(
                title = "Foto Recenti",
                description = "Visualizza le ultime",
                icon = Icons.Outlined.PhotoLibrary,
                onClick = { /* Navigate to photo gallery */ },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(height = QReportDesignTokens.Spacing.lg)

        // Recent checkups
        Text(
            text = "Check-up Recenti",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = QReportDesignTokens.Spacing.md)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.sm)
        ) {
            items(viewModel.recentCheckups) { checkup ->
                CheckupSummaryCard(
                    checkup = checkup,
                    onClick = { onNavigateToCheckupDetail(checkup.id) }
                )
            }
        }
    }
}
```

### 7.2 Checkup Detail Screen

```kotlin
@Composable
fun CheckupDetailScreen(
    checkupId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-up") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export */ }) {
                        Icon(Icons.Outlined.FileDownload, "Esporta")
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Outlined.MoreVert, "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Complete checkup */ },
                icon = { Icon(Icons.Outlined.Check, "Completa") },
                text = { Text("Completa") },
                expanded = viewModel.showCompleteFAB
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(QReportDesignTokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.md)
        ) {
            // Progress header
            item {
                ProgressHeader(
                    checkupTitle = viewModel.checkup.title,
                    completedItems = viewModel.completedItems,
                    totalItems = viewModel.totalItems,
                    criticalIssues = viewModel.criticalIssues
                )
            }

            // Check item sections
            viewModel.checkItemSections.forEach { section ->
                item {
                    SectionHeader(
                        title = section.title,
                        completedItems = section.completedItems,
                        totalItems = section.totalItems
                    )
                }

                items(section.items) { item ->
                    CheckItemCard(
                        item = item,
                        onStatusChange = { status ->
                            viewModel.updateItemStatus(item.id, status)
                        },
                        onNoteChange = { note ->
                            viewModel.updateItemNote(item.id, note)
                        },
                        onPhotoAdd = {
                            viewModel.openCameraForItem(item.id)
                        }
                    )
                }
            }
        }
    }
}
```

---

## 8. INTERACTIVE PATTERNS

### 8.1 Touch Targets

#### Minimum Touch Areas
- **Primary Actions:** 48dp minimum (preferito 56dp)
- **Secondary Actions:** 44dp minimum
- **Status Buttons:** 40dp height, full width
- **Navigation Items:** 56dp height
- **FAB:** 56dp standard, 40dp mini

#### Touch Feedback
```kotlin
// Standard touch feedback for all clickable elements
Modifier.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = rememberRipple(
        bounded = true,
        radius = 24.dp,
        color = MaterialTheme.colorScheme.primary
    ),
    onClick = onClick
)
```

### 8.2 Swipe Gestures

```kotlin
// Swipe to mark as OK/NOK
@Composable
fun SwipeableCheckItem(
    item: CheckItem,
    onStatusChange: (CheckItemStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(
        -200f to -1, // Left swipe = NOK
        0f to 0,     // Center = current
        200f to 1    // Right swipe = OK
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Background indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // NOK indicator (left)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(QReportColors.Warning),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "NOK",
                    tint = Color.White,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // OK indicator (right)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(QReportColors.Success),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "OK",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }

        // Foreground card
        CheckItemCard(
            item = item,
            onStatusChange = onStatusChange,
            modifier = Modifier.offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
        )
    }

    // Handle swipe completion
    LaunchedEffect(swipeableState.targetValue) {
        when (swipeableState.targetValue) {
            -1 -> {
                onStatusChange(CheckItemStatus.NOK)
                swipeableState.snapTo(0)
            }
            1 -> {
                onStatusChange(CheckItemStatus.OK)
                swipeableState.snapTo(0)
            }
        }
    }
}
```

### 8.3 Pull-to-Refresh

```kotlin
@Composable
fun RefreshableCheckupList(
    checkups: List<Checkup>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier.pullRefresh(pullRefreshState)
    ) {
        LazyColumn {
            items(checkups) { checkup ->
                CheckupCard(checkup = checkup)
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
```

---

## 9. RESPONSIVE DESIGN

### 9.1 Breakpoints

```kotlin
// QReportBreakpoints.kt - Responsive breakpoints
object QReportBreakpoints {
    val Compact = 0.dp..599.dp      // Smartphone portrait
    val Medium = 600.dp..839.dp     // Smartphone landscape, small tablet
    val Expanded = 840.dp..1199.dp  // Large tablet
    val Large = 1200.dp..1599.dp    // Desktop (future)
    val ExtraLarge = 1600.dp..Int.MAX_VALUE.dp
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return WindowSizeClass.calculateFromSize(
        DpSize(
            configuration.screenWidthDp.dp,
            configuration.screenHeightDp.dp
        )
    )
}
```

### 9.2 Adaptive Layouts

```kotlin
@Composable
fun AdaptiveCheckupGrid(
    checkups: List<Checkup>,
    onCheckupClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = rememberWindowSizeClass()

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        WindowWidthSizeClass.Expanded -> 3
        else -> 1
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(QReportDesignTokens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.md)
    ) {
        items(checkups) { checkup ->
            CheckupCard(
                checkup = checkup,
                onClick = { onCheckupClick(checkup.id) }
            )
        }
    }
}
```

### 9.3 Orientation Handling

```kotlin
@Composable
fun OrientationAwareCheckupDetail(
    checkup: Checkup,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape && configuration.screenWidthDp >= 700) {
        // Two-pane layout for landscape tablets
        Row(modifier = modifier.fillMaxSize()) {
            // Left pane: Checklist
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(QReportDesignTokens.Spacing.md)
            ) {
                // Check items...
            }

            // Right pane: Details/Photos
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(QReportDesignTokens.Spacing.md)
            ) {
                // Item details, photos, notes...
            }
        }
    } else {
        // Single pane layout for portrait/small screens
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(QReportDesignTokens.Spacing.md)
        ) {
            // All content in single column...
        }
    }
}
```

---

## 10. ACCESSIBILITY

### 10.1 Content Descriptions

```kotlin
// Semantic descriptions per tutti gli elementi interattivi
Icon(
    imageVector = Icons.Outlined.CheckCircle,
    contentDescription = "Segna come completato con successo",
    modifier = Modifier.semantics {
        role = Role.Button
        stateDescription = if (isSelected) "Selezionato" else "Non selezionato"
    }
)

// Gruppi semantici per check items
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Check item: ${item.title}. " +
            "Stato: ${item.status.displayName}. " +
            "Criticità: ${item.criticality.displayName}."
    }
) {
    // Content...
}
```

### 10.2 Focus Management

```kotlin
@Composable
fun AccessibleStatusButtonRow(
    currentStatus: CheckItemStatus,
    onStatusChange: (CheckItemStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableIntStateOf(currentStatus.ordinal) }

    Row(
        modifier = modifier
            .selectableGroup()
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.DirectionRight -> {
                        selectedIndex = (selectedIndex + 1) % 3
                        onStatusChange(CheckItemStatus.values()[selectedIndex])
                        true
                    }
                    Key.DirectionLeft -> {
                        selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else 2
                        onStatusChange(CheckItemStatus.values()[selectedIndex])
                        true
                    }
                    else -> false
                }
            }
    ) {
        CheckItemStatus.values().forEachIndexed { index, status ->
            StatusButton(
                text = status.displayName,
                isSelected = currentStatus == status,
                onClick = {
                    selectedIndex = index
                    onStatusChange(status)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(if (index == selectedIndex) focusRequester else FocusRequester())
                    .semantics {
                        selected = currentStatus == status
                        role = Role.RadioButton
                    }
            )
        }
    }
}
```

### 10.3 Dynamic Type Support

```kotlin
@Composable
fun ScalableText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    val scaledStyle = style.copy(
        fontSize = style.fontSize * minOf(fontScale, 1.3f) // Max 130% scaling
    )

    Text(
        text = text,
        style = scaledStyle,
        modifier = modifier,
        maxLines = if (fontScale > 1.15f) Int.MAX_VALUE else style.lineHeight.value.toInt()
    )
}
```

---

## 11. IMPLEMENTATION GUIDE

### 11.1 Theme Setup

```kotlin
// MainActivity.kt - App theme setup
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QReportTheme {
                QReportApp()
            }
        }
    }
}

// QReportTheme.kt - Complete theme definition
@Composable
fun QReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> QReportDarkColorScheme
        else -> QReportLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QReportTypography,
        shapes = QReportShapes,
        content = content
    )
}
```

### 11.2 Component Usage Examples

```kotlin
// Esempio utilizzo CheckItemCard in screen
@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(QReportDesignTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(QReportDesignTokens.Spacing.md)
    ) {
        items(uiState.checkItems) { item ->
            CheckItemCard(
                item = item,
                onStatusChange = { status ->
                    viewModel.onEvent(ChecklistEvent.UpdateItemStatus(item.id, status))
                },
                onNoteChange = { note ->
                    viewModel.onEvent(ChecklistEvent.UpdateItemNote(item.id, note))
                },
                onPhotoAdd = {
                    viewModel.onEvent(ChecklistEvent.AddPhoto(item.id))
                }
            )
        }
    }
}
```

### 11.3 Performance Optimizations

```kotlin
// Lazy composition per grandi liste
@Composable
fun OptimizedCheckItemCard(
    item: CheckItem,
    onStatusChange: (CheckItemStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize expensive calculations
    val cardColor by remember(item.status) {
        derivedStateOf {
            when (item.status) {
                CheckItemStatus.OK -> QReportColors.SuccessContainer
                CheckItemStatus.NOK -> QReportColors.WarningContainer
                CheckItemStatus.CRITICAL -> QReportColors.ErrorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        }
    }

    // Use key for stable composition
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        // Content...
    }
}

// Image loading con placeholder
@Composable
fun OptimizedPhotoThumbnail(
    photoUri: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(photoUri)
            .crossfade(true)
            .size(120.dp.value.toInt()) // Optimize loading size
            .build(),
        contentDescription = null,
        modifier = modifier.size(60.dp),
        placeholder = painterResource(R.drawable.ic_photo_placeholder),
        error = painterResource(R.drawable.ic_photo_error),
        contentScale = ContentScale.Crop
    )
}
```

---

## 🎯 NEXT STEPS

Questo documento UI Guidelines fornisce:

✅ **Design System completo** con Material 3
✅ **Component library** specializzata per QReport
✅ **Navigation pattern** ottimizzato per mobile
✅ **Responsive design** per smartphone e tablet
✅ **Accessibility** completa per tutti gli utenti
✅ **Implementation guide** pronta per sviluppo

### Prossimi documenti da sviluppare:

1. **🔧 Sistema Export Word** - Apache POI implementation
2. **📸 Gestione Foto** - CameraX integration e storage
3. **📊 Analytics & Performance** - Metriche e ottimizzazioni

**Vuoi procedere con il Sistema Export Word o preferisci approfondire qualche aspetto delle UI Guidelines?** 🚀