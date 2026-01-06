~~~~# 📘 QReport - Guida Sviluppo Completa

**Versione:** 1.0  
**Data:** Ottobre 2025  
**Namespace:** `net.calvuz.qreport`  
**Repository:** TBD

---

## 📋 INDICE

1. [Panoramica Progetto](#1-panoramica-progetto)
2. [Specifiche Tecniche](#2-specifiche-tecniche)
3. [Architettura Software](#3-architettura-software)
4. [Data Model Completo](#4-data-model-completo)
5. [Checklist Attività Complete](#5-checklist-attività-complete)
6. [Struttura Progetto Dettagliata](#6-struttura-progetto-dettagliata)
7. [Setup e Dipendenze](#7-setup-e-dipendenze)
8. [UI/UX Guidelines](#8-uiux-guidelines)
9. [Sistema Export Word](#9-sistema-export-word)
10. [Gestione Foto](#10-gestione-foto)
11. [Roadmap Sviluppo](#11-roadmap-sviluppo)

---

## 1. PANORAMICA PROGETTO

### 1.1 Obiettivo
Sviluppare un'applicazione Android nativa per digitalizzare il processo di check-up delle isole robotizzate industriali, sostituendo i report Word manuali con un sistema strutturato, fotografico e tracciabile.

### 1.2 Contesto d'Uso
- **Utenti:** Tecnici manutentori in trasferta presso clienti
- **Ambiente:** Stabilimenti industriali con isole robotizzate
- **Scenario:** Check-up periodici di manutenzione preventiva
- **Output:** Report Word professionali per il cliente

### 1.3 Problemi Risolti
| Problema Attuale | Soluzione QReport |
|------------------|-------------------|
| Report Word manuali lenti da compilare | Checklist digitali pre-compilate |
| Foto scattate e allegate separatamente | Foto integrate nei check item |
| Difficoltà tracciare storico interventi | Database locale con archivio completo |
| Formattazione documenti time-consuming | Export automatico formattato |
| Rischio perdita dati durante compilazione | Auto-save continuo in bozza |
| Impossibile lavorare offline | Funzionamento 100% offline |

### 1.4 Funzionalità Core (MVP)

#### ✅ Funzionalità Implementate (MVP)
1. **Gestione Check-Up**
    - Creazione check-up con selezione tipo isola
    - Checklist modulari (template base + moduli specifici)
    - Compilazione progressiva salvabile come bozza
    - Multi check-up simultanei

2. **Acquisizione Dati**
    - Check item: OK/NOK/NA/Pending
    - Note testuali illimitate per ogni item
    - Foto illimitate per ogni item
    - Classificazione criticità (Critical/Important/Routine/NA)

3. **Gestione Contenuti**
    - Intestazione compilabile in qualsiasi momento
    - Spare parts aggiungibili post check-up
    - Archiviazione locale completa

4. **Export**
    - Generazione Word (.docx) modificabile
    - Formattazione automatica professionale
    - Inclusione foto nel documento

5. **Storage**
    - Database Room (SQLite)
    - Funzionamento 100% offline
    - Storage foto in internal storage

#### 🔮 Funzionalità Future (Post-MVP)
- Annotazioni su foto (frecce, cerchi, testo, evidenziatori)
- Sincronizzazione con server centrale
- Comparazione con check-up precedenti
- Analytics e trend usura componenti
- Export PDF e Excel
- Multi-user con autenticazione
- Template personalizzabili per cliente
- Sistema predittivo manutenzione
- Firma digitale
- Integrazione ERP/CMMS

---

## 2. SPECIFICHE TECNICHE

### 2.1 Stack Tecnologico

| Componente | Tecnologia | Versione | Note |
|------------|-----------|----------|------|
| **Linguaggio** | Kotlin | 1.9.22+ | - |
| **Piattaforma** | Android Native | API 26+ | (Android 8.0 Oreo) |
| **UI Framework** | Jetpack Compose | 2024.02.00 | Material Design 3 |
| **Architettura** | Clean Architecture | - | MVVM pattern |
| **Database** | Room (SQLite) | 2.6.1+ | Offline-first |
| **DI** | Hilt (Dagger) | 2.50+ | Dependency Injection |
| **Navigation** | Compose Navigation | 2.7.6+ | Bottom Nav Bar |
| **Camera** | CameraX | 1.3.1+ | Photo capture |
| **Image Loading** | Coil | 2.5.0+ | Async image loading |
| **Word Export** | Apache POI | 5.2.5 | .docx generation |
| **JSON** | Gson | 2.10.1+ | Serialization |
| **Logging** | Timber | 5.0.1+ | Debug logging |

### 2.2 Requisiti Sistema

**Target Device:**
- **Primary:** Smartphone Android (5.5" - 6.7")
- **Secondary:** Tablet Android (7" - 10")
- **Orientamento:** Portrait (primary), Landscape (supportato)

**Requisiti Hardware:**
- Android 8.0+ (API 26+)
- RAM: 2 GB minimo, 4 GB consigliato
- Storage: 500 MB app + 2 GB foto/report
- Camera: Risoluzione minima 8 MP

**Permissions Richieste:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="28" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### 2.3 Namespace
- **Package base:** `net.calvuz.qreport`
- **Dominio:** calvuz.net (proprietà verificata)

---

## 3. ARCHITETTURA SOFTWARE

### 3.1 Clean Architecture - Diagramma

```
┌─────────────────────────────────────────────────┐
│         PRESENTATION LAYER                       │
│   (UI - Jetpack Compose + ViewModels)           │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │   Screens    │  │  ViewModels  │             │
│  │  (Compose)   │←─│  (StateFlow) │             │
│  └──────────────┘  └──────────────┘             │
└──────────────────────┬──────────────────────────┘
                       │ Uses
                       ↓
┌─────────────────────────────────────────────────┐
│            DOMAIN LAYER                          │
│   (Business Logic - Pure Kotlin)                │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │   Models     │  │  Use Cases   │             │
│  │  (Entities)  │  │  (Business)  │             │
│  └──────────────┘  └──────────────┘             │
│                                                  │
│  ┌──────────────────────────────────┐           │
│  │   Repository Interfaces          │           │
│  │   (Contracts - no implementation)│           │
│  └──────────────────────────────────┘           │
└──────────────────────┬──────────────────────────┘
                       │ Implements
                       ↓
┌─────────────────────────────────────────────────┐
│             DATA LAYER                           │
│   (Data Sources + Repository Impl)              │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │     Room     │  │ Repositories │             │
│  │   Database   │←─│     Impl     │             │
│  └──────────────┘  └──────────────┘             │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │ File System  │  │   Mappers    │             │
│  │   (Photos)   │  │ Entity↔Model │             │
│  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────┘
```

### 3.2 Dependency Rule

**REGOLA D'ORO:** Le dipendenze puntano SOLO verso l'interno

- ✅ **Presentation → Domain**: UI usa Use Cases
- ✅ **Data → Domain**: Repository implementa contratti Domain
- ✅ **Domain → Nessuno**: Business Logic puro (no dipendenze esterne)
- ❌ **Domain → Data**: MAI! Domain non conosce l'implementazione
- ❌ **Domain → Presentation**: MAI! Business logic indipendente da UI

### 3.3 MVVM Pattern - Unidirectional Data Flow

```
┌─────────────────────────────────────────┐
│           User Interaction               │
│         (Button Click, Input)            │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│          ViewModel Event                 │
│      (onEvent(ChecklistEvent))           │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│           Use Case                       │
│    (Execute Business Logic)              │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│          Repository                      │
│      (Access Data Layer)                 │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│     Data Source (Room/FileSystem)        │
│         (Persist/Retrieve)               │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│     ViewModel State Update               │
│   (emit new StateFlow value)             │
└───────────────┬─────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│       Compose Recomposition              │
│      (UI reflects new state)             │
└─────────────────────────────────────────┘
```

**Esempio Concreto:**
```kotlin
// 1. User clicks "Mark as OK"
CheckItemCard(
    item = item,
    onStatusChange = { newStatus ->
        viewModel.onEvent(
            ChecklistEvent.UpdateItemStatus(itemId, newStatus)
        )
    }
)

// 2. ViewModel processes event
fun onEvent(event: ChecklistEvent) {
    when (event) {
        is ChecklistEvent.UpdateItemStatus -> {
            viewModelScope.launch {
                updateCheckItemUseCase(
                    itemId = event.itemId,
                    status = event.status
                )
            }
        }
    }
}

// 3. Use Case executes business logic
class UpdateCheckItemUseCase(
    private val repository: CheckUpRepository
) {
    suspend operator fun invoke(itemId: String, status: CheckStatus) {
        repository.updateCheckItem(itemId, status)
    }
}

// 4. Repository persists to Room
override suspend fun updateCheckItem(itemId: String, status: CheckStatus) {
    val entity = checkItemDao.getItemById(itemId)
    entity?.let {
        checkItemDao.updateItem(
            it.copy(
                status = status.name,
                checkedAt = System.currentTimeMillis()
            )
        )
    }
}

// 5. Room triggers Flow update
@Query("SELECT * FROM check_items WHERE id = :id")
fun getItemByIdFlow(id: String): Flow<CheckItemEntity>

// 6. ViewModel collects Flow and updates State
init {
    checkUpRepository.getCheckUpById(checkUpId)
        .onEach { checkUp ->
            _state.update { it.copy(checkUp = checkUp) }
        }
        .launchIn(viewModelScope)
}

// 7. Compose recomposes with new state
val state by viewModel.state.collectAsState()
ChecklistScreen(state = state)
```

---

## 4. DATA MODEL COMPLETO

### 4.1 Domain Models (Kotlin Puro)

Vedi documento precedente per:
- Enumerations (IslandType, ReportStatus, CheckStatus, Criticality, CheckCategory, ExportFormat)
- Core Models (CheckUpReport, CheckHeader, CheckSection, CheckItem, Photo, SparePart, ExportedFile)

*(I modelli sono già definiti completamente nel messaggio precedente)*

### 4.2 Room Database Schema

**Database Name:** `qreport_db`  
**Version:** 1

**Entities:**
1. `check_up_reports` - Check-up principali
2. `check_headers` - Intestazioni
3. `check_sections` - Sezioni di controllo
4. `check_items` - Item singoli
5. `photos` - Foto allegate
6. `spare_parts` - Ricambi consigliati
7. `exported_files` - Storico export

**Relazioni:**
```
check_up_reports (1) ──┬── (0..1) check_headers
                       ├── (0..*) check_sections
                       ├── (0..*) spare_parts
                       └── (0..*) exported_files

check_sections (1) ──── (0..*) check_items

check_items (1) ──── (0..*) photos
```

---

## 5. CHECKLIST ATTIVITÀ COMPLETE

### 5.1 Struttura Template

```kotlin
data class CheckItemTemplate(
    val description: String,
    val interval: String?,  // "mensile", "bimestrale", "semestrale", "annuale", etc.
    val order: Int,
    val category: CheckCategory
)
```

### 5.2 Template Base (TUTTE LE ISOLE)

#### CONTROLLO TOOL/PINZA ROBOT (18 items)
```kotlin
val baseToolChecks = listOf(
    CheckItemTemplate("Controllo stato tubi aria e raccordi", "bimestrale", 1, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica perdite aria", "bimestrale", 2, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo integrità cono ISO / interfaccia cambio tool", "semestrale", 3, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica funzionamento aggancio cono", "semestrale", 4, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo cuscinetti interni cono ISO", "semestrale", 5, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo pistoni cambio tool (tenuta aria)", "semestrale", 6, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo cablaggi elettrici", "bimestrale", 7, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica assenza tagli/bruciature cavi", "bimestrale", 8, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo connessioni elettriche", "bimestrale", 9, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo coibentazione tool", "bimestrale", 10, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica integrità protezioni termiche", "bimestrale", 11, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo componenti meccanici pinza", "semestrale", 12, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica usura chele/gommini", "settimanale", 13, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Sostituzione gommini chele (se necessario)", "settimanale", 14, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo serraggio viti pinza", "semestrale", 15, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Controllo sistema visione (se presente)", "semestrale", 16, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Verifica integrità lenti camere visione", "semestrale", 17, CheckCategory.ROBOT_TOOL),
    CheckItemTemplate("Pulizia lenti sistema visione", "mensile", 18, CheckCategory.ROBOT_TOOL)
)
```

#### CONTROLLO ROBOT (22 items)
```kotlin
val baseRobotChecks = listOf(
    CheckItemTemplate("Controllo perdite olio riduttori", "bimestrale", 1, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica gocce olio in prossimità degli assi", "bimestrale", 2, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo guaine e festoni cavi", "bimestrale", 3, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica assenza pieghe strette guaine", "bimestrale", 4, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo tagli o danni sui cablaggi", "bimestrale", 5, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo protezioni termiche cavi", "bimestrale", 6, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo carter e vestiti robot", "bimestrale", 7, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica danni o deformazioni carter", "bimestrale", 8, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo integrità protezioni robot", "bimestrale", 9, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo soffi aria raffreddamento motori", "bimestrale", 10, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica funzionamento e regolazione flusso aria", "bimestrale", 11, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo passaggio cavi/tubi a bordo robot", "bimestrale", 12, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica stato connessioni robot", "bimestrale", 13, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo jumper point", "semestrale", 14, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica punto di presa", "semestrale", 15, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo eventuale deformazione lancia", "semestrale", 16, CheckCategory.ROBOT),
    CheckItemTemplate("Pulizia robot generale", "bimestrale", 17, CheckCategory.ROBOT),
    CheckItemTemplate("Rimozione polvere con aria compressa", "bimestrale", 18, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo ore lavoro robot", "ogni check-up", 19, CheckCategory.ROBOT),
    CheckItemTemplate("Verifica ore da ultimo cambio olio", "ogni check-up", 20, CheckCategory.ROBOT),
    CheckItemTemplate("Grassaggio punti lubrificazione (se previsto dal manuale)", "mensile", 21, CheckCategory.ROBOT),
    CheckItemTemplate("Controllo serraggio viti carter bordo robot", "bimestrale", 22, CheckCategory.ROBOT)
)
```

#### CONTROLLO SICUREZZE (11 items)
```kotlin
val baseSafetyChecks = listOf(
    CheckItemTemplate("Controllo funghi emergenza", "bimestrale", 1, CheckCategory.SAFETY),
    CheckItemTemplate("Verifica funzionamento pulsanti emergenza", "bimestrale", 2, CheckCategory.SAFETY),
    CheckItemTemplate("Test reset emergenza", "bimestrale", 3, CheckCategory.SAFETY),
    CheckItemTemplate("Controllo barriere di sicurezza", "bimestrale", 4, CheckCategory.SAFETY),
    CheckItemTemplate("Verifica funzionamento barriere", "bimestrale", 5, CheckCategory.SAFETY),
    CheckItemTemplate("Controllo allineamento barriere", "bimestrale", 6, CheckCategory.SAFETY),
    CheckItemTemplate("Verifica assenza rotture su barriere", "bimestrale", 7, CheckCategory.SAFETY),
    CheckItemTemplate("Controllo serrature porte", "bimestrale", 8, CheckCategory.SAFETY),
    CheckItemTemplate("Verifica funzionamento serrature", "bimestrale", 9, CheckCategory.SAFETY),
    CheckItemTemplate("Test apertura/chiusura sicurezza porte", "bimestrale", 10, CheckCategory.SAFETY),
    CheckItemTemplate("Controllo feedback sensori sicurezza", "bimestrale", 11, CheckCategory.SAFETY)
)
```

#### CONTROLLO IMPIANTI ISOLA (23 items)
```kotlin
val basePlantChecks = listOf(
    CheckItemTemplate("Controllo filtri recupero condensa", "bimestrale", 1, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Svuotamento serbatoio acqua condensa", "settimanale", 2, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo filtri disoelatori", "bimestrale", 3, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Verifica spia segnalazione filtri", "bimestrale", 4, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Sostituzione filtri (se necessario)", "annuale", 5, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Pulizia radiatore gruppo essicatore", "bimestrale", 6, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Soffiatura con aria compressa radiatore", "bimestrale", 7, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo chiller (se presente)", "semestrale", 8, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Verifica perdite acqua chiller", "semestrale", 9, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo livello acqua/antigelo chiller", "semestrale", 10, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo filtro rete chiller", "semestrale", 11, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Verifica flusso ritorno chiller (>30l/min per EAF)", "semestrale", 12, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Pulizia sensori induttivi", "settimanale", 13, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Pulizia fotocellule", "settimanale", 14, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Pulizia quadri elettrici", "bimestrale", 15, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Soffiatura/aspirazione polvere quadri", "bimestrale", 16, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Pulizia generale isola e container", "settimanale", 17, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo porte isola", "bimestrale", 18, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Verifica scorrimento porte", "bimestrale", 19, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo pattini/guide scorrimento porte", "bimestrale", 20, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo stato cremagliere porte", "bimestrale", 21, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Verifica distanza pistone/cremagliera porte", "bimestrale", 22, CheckCategory.PLANT_SYSTEMS),
    CheckItemTemplate("Controllo stato usura cremagliera", "bimestrale", 23, CheckCategory.PLANT_SYSTEMS)
)
```

#### PROVE FUNZIONAMENTO (7 items)
```kotlin
val baseFunctionalTests = listOf(
    CheckItemTemplate("Esecuzione ciclo riscaldo robot (se presente)", "ogni check-up", 1, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Esecuzione ciclo simulazione", "ogni check-up", 2, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Controllo comandi manuali da HMI", "ogni check-up", 3, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Verifica risposta comandi HMI", "ogni check-up", 4, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Controllo feedback sensori da HMI", "ogni check-up", 5, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Controllo funzionamento robot in produzione (se possibile)", "ogni check-up", 6, CheckCategory.FUNCTIONAL_TESTS),
    CheckItemTemplate("Verifica integrità Flex Pendant / Teach Pendant", "ogni check-up", 7, CheckCategory.FUNCTIONAL_TESTS)
)
```

**TOTALE TEMPLATE BASE: 81 items**

---

### 5.3 Moduli Specifici per Tipo Isola

#### POLY_MOVE - Sistemi Movimentazione (55 items)

##### Settimo Asse Gudel (5 items)
```kotlin
val polyMoveSeventhAxisChecks = listOf(
    CheckItemTemplate("Pulizia e lubrificazione guide scorrimento 7° asse", "settimanale", 1, CheckCategory.SEVENTH_AXIS),
    CheckItemTemplate("Ispezione generale 7° asse", "annuale", 2, CheckCategory.SEVENTH_AXIS),
    CheckItemTemplate("Sostituzione cartuccia ingrassatore automatico 7° asse", "annuale", 3, CheckCategory.SEVENTH_AXIS),
    CheckItemTemplate("Sostituzione cartucce lubrificazione guide 7° asse", "annuale", 4, CheckCategory.SEVENTH_AXIS),
    CheckItemTemplate("Controllo olio riduttori 7° asse", "biennale", 5, CheckCategory.SEVENTH_AXIS)
)
```

##### Palomat (3 items)
```kotlin
val polyMovePalomatChecks = listOf(
    CheckItemTemplate("Controllo filtro riduttore pressione Palomat", "annuale", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione guide e profili C Palomat", "semestrale", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo viti Palomat", "annuale", 3, CheckCategory.CONVEYOR_SYSTEMS)
)
```

##### Rulliera Magazzino Bancali (7 items)
```kotlin
val polyMoveWarehouseConveyorChecks = listOf(
    CheckItemTemplate("Controllo presenza detriti rulliera magazzino", "quotidiano", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia rulliera magazzino", "quotidiano", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato rivestimento rulli gommati magazzino", "settimanale", 3, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia generale rulliera + sensori magazzino", "settimanale", 4, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo tensione catene magazzino", "annuale", 5, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo viti/parti meccaniche magazzino", "semestrale", 6, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione catena magazzino", "mensile", 7, CheckCategory.CONVEYOR_SYSTEMS)
)
```

##### Rulliere 3m Inclinate (7 items)
```kotlin
val polyMove3mConveyorChecks = listOf(
    CheckItemTemplate("Controllo presenza detriti rulliere 3m", "quotidiano", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia rulliere 3m", "quotidiano", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato rulli gommati 3m", "settimanale", 3, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia generale rulliere 3m + sensori", "settimanale", 4, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo tensione catene 3m", "annuale", 5, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo viti/parti meccaniche rulliere 3m", "semestrale", 6, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione catene rulliere 3m", "mensile", 7, CheckCategory.CONVEYOR_SYSTEMS)
)
```

##### Rulliere 2m Sollevabili (10 items)
```kotlin
val polyMove2mLiftableConveyorChecks = listOf(
    CheckItemTemplate("Controllo presenza detriti rulliere 2m", "quotidiano", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia rulliere 2m", "quotidiano", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato rulli gommati 2m", "settimanale", 3, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia generale rulliere 2m + sensori", "settimanale", 4, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo tensione catene 2m", "annuale", 5, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo viti/parti meccaniche 2m", "semestrale", 6, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione catene 2m", "mensile", 7, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato pignone + cremagliera ascensore", "bimestrale", 8, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia + lubrificazione cremagliera ascensore", "mensile", 9, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia guide lineari HIWIN", "mensile", 10, CheckCategory.CONVEYOR_SYSTEMS)
)
```

##### Rulliere Basculanti Formazione Pallet (18 items)
```kotlin
val polyMoveTiltingConveyorChecks = listOf(
    CheckItemTemplate("Controllo presenza detriti rulliere basculanti", "quotidiano", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia generale rulliere basculanti", "settimanale", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia guide lineari basculanti", "mensile", 3, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato rulli basculanti", "settimanale", 4, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo giochi pistoni sponde", "semestrale", 5, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo perdite aria pistoni sponde", "semestrale", 6, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo allineamento sponde", "bimestrale", 7, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo tensione catene basculanti", "annuale", 8, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione catene basculanti", "mensile", 9, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia + lubrificazione cremagliera basculanti", "mensile", 10, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo usura cremagliera + pignone basculanti", "semestrale", 11, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Grassaggio snodi basculanti", "mensile", 12, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Grassaggio punti sensibili basculanti", "mensile", 13, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo generale viti basculanti", "semestrale", 14, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo visivo catenaria basculanti", "mensile", 15, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo visivo cavi elettrici basculanti", "mensile", 16, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo visivo tubi pneumatici basculanti", "mensile", 17, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Verifica feedback sensori basculanti", "mensile", 18, CheckCategory.CONVEYOR_SYSTEMS)
)
```

##### Rulliere Scarico Pallet (5 items)
```kotlin
val polyMoveUnloadingConveyorChecks = listOf(
    CheckItemTemplate("Controllo presenza detriti rulliere scarico", "quotidiano", 1, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Pulizia rulliere scarico", "settimanale", 2, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo stato rulli scarico", "settimanale", 3, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Controllo viti generali rulliere scarico", "semestrale", 4, CheckCategory.CONVEYOR_SYSTEMS),
    CheckItemTemplate("Lubrificazione punti sensibili scarico", "mensile", 5, CheckCategory.CONVEYOR_SYSTEMS)
)
```

**TOTALE POLY_MOVE: 55 items specifici + 81 base = 136 items**

---

#### POLY_CAST - Sistema Visione e Magazzino Utensili (16 items)

##### Sistema Visione (4 items)
```kotlin
val polyCastVisionChecks = listOf(
    CheckItemTemplate("Controllo integrità camere visione", "semestrale", 1, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Pulizia lenti camere visione", "mensile", 2, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Verifica funzionamento sistema visione", "ogni check-up", 3, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Calibrazione sistema visione (se necessario)", "annuale", 4, CheckCategory.VISION_SYSTEM)
)
```

##### Magazzino Utensili (12 items)
```kotlin
val polyCastToolWarehouseChecks = listOf(
    CheckItemTemplate("Controllo posizione utensili nel magazzino", "ogni check-up", 1, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica posizionamento corretto tutti gli utensili", "ogni check-up", 2, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Controllo integrità utensili", "ogni check-up", 3, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica assenza collisioni/deformazioni utensili", "ogni check-up", 4, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Controllo punti robot magazzino", "ogni check-up", 5, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica prelievo corretto utensili", "ogni check-up", 6, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica deposito corretto utensili", "ogni check-up", 7, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica connessioni utensili", "semestrale", 8, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Controllo cablaggi/connettori utensili", "semestrale", 9, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Controllo comunicazione utensili con robot", "ogni check-up", 10, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Verifica attivazione comandi utensili", "ogni check-up", 11, CheckCategory.TOOL_WAREHOUSE),
    CheckItemTemplate("Controllo feedback utensili", "ogni check-up", 12, CheckCategory.TOOL_WAREHOUSE)
)
```

**TOTALE POLY_CAST: 16 items specifici + 81 base = 97 items**

---

#### POLY_EBT - Magazzino Lance (9 items)

```kotlin
val polyEbtLanceStorageChecks = listOf(
    CheckItemTemplate("Controllo allineamento lance nel magazzino", "ogni check-up", 1, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica integrità lance", "ogni check-up", 2, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo assenza collisioni lance", "ogni check-up", 3, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo deformazioni lance", "ogni check-up", 4, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo connessione lance con tool robot", "ogni check-up", 5, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica punti prelievo lance", "ogni check-up", 6, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica punti deposito lance", "ogni check-up", 7, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo struttura magazzino lance", "semestrale", 8, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica integrità supporti lance", "semestrale", 9, CheckCategory.LANCE_STORAGE)
)
```

**TOTALE POLY_EBT: 9 items specifici + 81 base = 90 items**

---

#### POLY_TAG_BLE - Sistema Etichettatura BLE (26 items)

##### Macchina BLE (8 items)
```kotlin
val polyTagBleMachineChecks = listOf(
    CheckItemTemplate("Pulizia generale macchina BLE", "settimanale", 1, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia filtro aspirazione fumi stampa", "mensile", 2, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Sostituzione filtro aspirazione fumi (se necessario)", "annuale", 3, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia filtro stampante BLE", "mensile", 4, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia filtro PC macchina BLE", "bimestrale", 5, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia lente stampante BLE", "settimanale", 6, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia quadro elettrico macchina BLE", "bimestrale", 7, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo stato ventosa presa cartellino", "semestrale", 8, CheckCategory.LABELING_MACHINE)
)
```

##### Magazzino e Distribuzione (10 items)
```kotlin
val polyTagBleWarehouseChecks = listOf(
    CheckItemTemplate("Controllo revolver etichette", "ogni check-up", 1, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica funzionamento revolver", "ogni check-up", 2, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo cilindro presa cartellino", "semestrale", 3, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica movimenti presa cartellino", "ogni check-up", 4, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo vibratore circolare", "semestrale", 5, CheckCategory.VIBRATORS),
    CheckItemTemplate("Verifica frequenza vibrazione circolare", "ogni check-up", 6, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo vibratore lineare", "semestrale", 7, CheckCategory.VIBRATORS),
    CheckItemTemplate("Verifica scorrimento chiodi su guide", "ogni check-up", 8, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo scassettatore", "semestrale", 9, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo cassetto cartellino", "semestrale", 10, CheckCategory.LABELING_MACHINE)
)
```

##### Selezione e Punti Robot (8 items)
```kotlin
val polyTagBleSoffiChecks = listOf(
    CheckItemTemplate("Controllo soffi selezione chiodi", "mensile", 1, CheckCategory.VIBRATORS),
    CheckItemTemplate("Regolazione soffi aria selezione", "ogni check-up", 2, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo punto presa chiodo", "ogni check-up", 3, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo punto presa cartellino", "ogni check-up", 4, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica pick-up label sensor", "ogni check-up", 5, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo sensori buffer clip", "ogni check-up", 6, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica comunicazione sensori con HMI", "ogni check-up", 7, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Test completo ciclo etichettatura", "ogni check-up", 8, CheckCategory.LABELING_MACHINE)
)
```

**TOTALE POLY_TAG_BLE: 26 items specifici + 81 base = 107 items**

---

#### POLY_TAG_FC - Sistema Etichettatura FC (18 items)

##### Macchina Creazione Cartellino (10 items)
```kotlin
val polyTagFcMachineChecks = listOf(
    CheckItemTemplate("Pulizia macchina etichettatura FC", "settimanale", 1, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo vibratori FC", "semestrale", 2, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo soffi aria vibratori FC", "mensile", 3, CheckCategory.VIBRATORS),
    CheckItemTemplate("Verifica velocità vibrazioni FC", "ogni check-up", 4, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo scorrimento chiodi su slitte", "ogni check-up", 5, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo cassetti cartellini FC", "semestrale", 6, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo integrità pistoni cassetti FC", "semestrale", 7, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo perdite aria cassetti FC", "semestrale", 8, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo scorrimento steli pistoni cassetti", "semestrale", 9, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Lubrificazione steli pistoni (se necessario)", "annuale", 10, CheckCategory.LABELING_MACHINE)
)
```

##### Punti e Test Funzionamento (8 items)
```kotlin
val polyTagFcTestChecks = listOf(
    CheckItemTemplate("Controllo punto prelievo chiodo FC", "ogni check-up", 1, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo punto prelievo cartellino FC", "ogni check-up", 2, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo movimenti robot magazzino", "ogni check-up", 3, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo componenti macchinetta FC", "ogni check-up", 4, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Attivazione manuale componenti da HMI", "ogni check-up", 5, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica funzionamento componenti FC", "ogni check-up", 6, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica feedback HMI", "ogni check-up", 7, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Test ciclo completo etichettatura FC", "ogni check-up", 8, CheckCategory.LABELING_MACHINE)
)
```

**TOTALE POLY_TAG_FC: 18 items specifici + 81 base = 99 items**

---

#### POLY_TAG_V - Sistema Doppio Robot (35 items)

##### Robot Esterno (8 items)
```kotlin
val polyTagVExternalRobotChecks = listOf(
    CheckItemTemplate("Controllo scudi pinza robot esterno", "bimestrale", 1, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo cavi/tubi/guaine robot esterno", "bimestrale", 2, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo usura pistoni robot esterno", "semestrale", 3, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo parti meccaniche robot esterno", "semestrale", 4, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo lenti sistema visione esterno", "mensile", 5, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo carter/vestiti robot esterno", "bimestrale", 6, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo perdite olio robot esterno", "bimestrale", 7, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo ore lavoro robot esterno", "ogni check-up", 8, CheckCategory.DUAL_ROBOT)
)
```

##### Robot Interno (4 items)
```kotlin
val polyTagVInternalRobotChecks = listOf(
    CheckItemTemplate("Controllo danni braccio robot interno", "bimestrale", 1, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo tubi e cavi robot interno", "bimestrale", 2, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo sensori robot interno", "bimestrale", 3, CheckCategory.DUAL_ROBOT),
    CheckItemTemplate("Controllo pinza robot interno", "semestrale", 4, CheckCategory.DUAL_ROBOT)
)
```

##### Macchina Etichettatura (15 items)
```kotlin
val polyTagVLabelingChecks = listOf(
    CheckItemTemplate("Pulizia macchina etichettatura V", "settimanale", 1, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia/sostituzione filtro aspirazione stampa V", "mensile", 2, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Pulizia lente stampante V", "settimanale", 3, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo Flexibowl", "semestrale", 4, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo vibratore lineare V", "semestrale", 5, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo hopper", "semestrale", 6, CheckCategory.VIBRATORS),
    CheckItemTemplate("Controllo cassetti etichette V", "semestrale", 7, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo integrità pistoni cassetti V", "semestrale", 8, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica perdite aria cassetti V", "semestrale", 9, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo sensori cassetti V", "ogni check-up", 10, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica allineamento cassetti con stampante", "ogni check-up", 11, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica sensore presenza clip su pinza", "ogni check-up", 12, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica sensori buffer clip V", "ogni check-up", 13, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Controllo magazzino etichette V", "semestrale", 14, CheckCategory.LABELING_MACHINE),
    CheckItemTemplate("Verifica sistema bloccaggio magazzino", "semestrale", 15, CheckCategory.LABELING_MACHINE)
)
```

##### Sistema Visione (8 items)
```kotlin
val polyTagVVisionChecks = listOf(
    CheckItemTemplate("Controllo sistema visione etichettatura", "semestrale", 1, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Pulizia lenti camere visione V", "mensile", 2, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Verifica calibrazione sistema visione V", "annuale", 3, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Controllo illuminazione sistema visione", "semestrale", 4, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Verifica comunicazione camere con PLC", "ogni check-up", 5, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Test riconoscimento clip", "ogni check-up", 6, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Test riconoscimento etichette", "ogni check-up", 7, CheckCategory.VISION_SYSTEM),
    CheckItemTemplate("Verifica tempi risposta sistema visione", "ogni check-up", 8, CheckCategory.VISION_SYSTEM)
)
```

**TOTALE POLY_TAG_V: 35 items specifici + 81 base = 116 items**

---

#### POLY_SAMPLE - Magazzini e Sistema Lancia (52 items)

##### Lancia Robot (10 items)
```kotlin
val polySampleLanceChecks = listOf(
    CheckItemTemplate("Controllo stato tubi-raccordi acqua lancia", "bimestrale", 1, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica assenza perdite o crepe lancia", "bimestrale", 2, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo integrità lancia SAMPLE", "ogni check-up", 3, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica assenza crepe lancia", "ogni check-up", 4, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo stato cementazione lancia", "semestrale", 5, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo deformazione lancia SAMPLE", "ogni check-up", 6, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica allineamento lancia/puntale/bronzina", "ogni check-up", 7, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo parti filettate lancia", "semestrale", 8, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica ghiera/puntale/bronzina non grippati", "semestrale", 9, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica fori soffiante lancia non tappati", "mensile", 10, CheckCategory.LANCE_STORAGE)
)
```

##### Magazzini Cartucce (19 items)
```kotlin
val polySampleWarehouseChecks = listOf(
    CheckItemTemplate("Pulizia magazzini cartucce", "settimanale", 1, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Soffiatura/aspirazione detriti magazzini", "settimanale", 2, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo perdite aria magazzini", "bimestrale", 3, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Attivazione comandi manuali magazzini", "ogni check-up", 4, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Pulizia componenti mobili magazzini", "mensile", 5, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Rimozione polvere/incrostazioni pattini", "mensile", 6, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Pulizia rotaie magazzini", "mensile", 7, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Pulizia steli pistoni magazzini", "mensile", 8, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo serraggio viti magazzini", "semestrale", 9, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo gioco pattini/rotaie", "semestrale", 10, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica assenza componenti lenti", "ogni check-up", 11, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo stato rotaie magazzini", "semestrale", 12, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica assenza graffi/solchi rotaie", "semestrale", 13, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica assenza ruggine rotaie", "semestrale", 14, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo scorrimento pareggiatore", "ogni check-up", 15, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica finecorsa pareggiatore", "ogni check-up", 16, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo scorrimento centratore", "ogni check-up", 17, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica finecorsa centratore", "ogni check-up", 18, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Lubrificazione componenti mobili magazzini", "mensile", 19, CheckCategory.CARTRIDGE_SYSTEMS)
)
```

##### Cilindri e Selezione (11 items)
```kotlin
val polySampleCylindersChecks = listOf(
    CheckItemTemplate("Controllo cilindri sollevamento cartuccia", "semestrale", 1, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica finecorsa sollevamento cartuccia", "ogni check-up", 2, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo cilindri selezione cartuccia", "semestrale", 3, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica funzionamento selezione cartuccia", "ogni check-up", 4, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo perdite aria cilindri", "bimestrale", 5, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo tenuta pistoni cartucce", "semestrale", 6, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica regolazione sensori magazzini", "ogni check-up", 7, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo funzionamento sensori magazzini", "ogni check-up", 8, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo stato coltelli magazzino scarico", "semestrale", 9, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo funzionamento docce scarico (se presenti)", "semestrale", 10, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica feedback sensori magazzini", "ogni check-up", 11, CheckCategory.CARTRIDGE_SYSTEMS)
)
```

##### PolyCUT e Cambio Tool (12 items)
```kotlin
val polySamplePolyCutChecks = listOf(
    CheckItemTemplate("Controllo funzionamento PolyCUT (se presente)", "semestrale", 1, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo stato coltello cesoia PolyCUT", "semestrale", 2, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica affilatura coltello PolyCUT", "semestrale", 3, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Controllo guide scorrimento PolyCUT", "semestrale", 4, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica funzionamento cambio tool SAMPLE", "ogni check-up", 5, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo punti presa lance SAMPLE", "ogni check-up", 6, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo comunicazione lance SAMPLE", "ogni check-up", 7, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Controllo assenza giochi meccanici lance", "ogni check-up", 8, CheckCategory.LANCE_STORAGE),
    CheckItemTemplate("Verifica funzionamento magazzino automatico", "ogni check-up", 9, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica presa cartuccia con lancia SAMPLE", "ogni check-up", 10, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Verifica scarico cartuccia con lancia SAMPLE", "ogni check-up", 11, CheckCategory.CARTRIDGE_SYSTEMS),
    CheckItemTemplate("Test ciclo completo carico/scarico", "ogni check-up", 12, CheckCategory.CARTRIDGE_SYSTEMS)
)
```

**TOTALE POLY_SAMPLE: 52 items specifici + 81 base = 133 items**

---

### 5.4 Riepilogo Totali per Tipo Isola

| Tipo Isola | Items Base | Items Specifici | **TOTALE** |
|------------|-----------|-----------------|------------|
| POLY_MOVE | 81 | 55 | **136** |
| POLY_CAST | 81 | 16 | **97** |
| POLY_EBT | 81 | 9 | **90** |
| POLY_TAG_BLE | 81 | 26 | **107** |
| POLY_TAG_FC | 81 | 18 | **99** |
| POLY_TAG_V | 81 | 35 | **116** |
| POLY_SAMPLE | 81 | 52 | **133** |

---

## 6. STRUTTURA PROGETTO DETTAGLIATA

```
net.calvuz.qreport/
│
├── 📱 presentation/                    (UI Layer)
│   │
│   ├── theme/
│   │   ├── Color.kt                   (Palette colori Material 3)
│   │   ├── Type.kt                    (Typography definitions)
│   │   └── Theme.kt                   (Theme composable)
│   │
│   ├── navigation/
│   │   ├── NavGraph.kt                (Navigation graph setup)
│   │   ├── Screen.kt                  (Sealed class screens)
│   │   └── BottomNavItem.kt           (Bottom nav items)
│   │
│   ├── components/                     (Reusable UI components)
│   │   ├── common/
│   │   │   ├── QReportTopBar.kt
│   │   │   ├── QReportBottomBar.kt
│   │   │   ├── LoadingIndicator.kt
│   │   │   ├── ErrorMessage.kt
│   │   │   ├── EmptyState.kt
│   │   │   └── ConfirmDialog.kt
│   │   │
│   │   ├── checkup/
│   │   │   ├── CheckUpCard.kt         (Card lista check-up)
│   │   │   ├── IslandTypeChip.kt      (Badge tipo isola)
│   │   │   ├── StatusBadge.kt         (Badge stato Draft/Finalized)
│   │   │   ├── ProgressIndicator.kt   (Progress bar % completamento)
│   │   │   └── CriticalityBadge.kt    (Badge criticità)
│   │   │
│   │   ├── checklist/
│   │   │   ├── CheckSectionCard.kt    (Card espandibile sezione)
│   │   │   ├── CheckItemCard.kt       (Card singolo item)
│   │   │   ├── CheckStatusButton.kt   (Bottoni OK/NOK/NA/Pending)
│   │   │   ├── CriticalitySelector.kt (Selector criticità)
│   │   │   └── NotesTextField.kt      (Campo note espandibile)
│   │   │
│   │   ├── photo/
│   │   │   ├── PhotoGrid.kt           (Grid foto con lazy loading)
│   │   │   ├── PhotoThumbnail.kt      (Thumbnail cliccabile)
│   │   │   ├── PhotoViewer.kt         (Full screen viewer)
│   │   │   ├── CameraCapture.kt       (Camera UI)
│   │   │   └── PhotoActionSheet.kt    (Menu azioni foto)
│   │   │
│   │   └── spareparts/
│   │       ├── SparePartCard.kt       (Card ricambio)
│   │       └── SparePartForm.kt       (Form aggiungi/modifica)
│   │
│   └── screens/
│       │
│       ├── home/                       (Home screen - lista attivi)
│       │   ├── HomeScreen.kt
│       │   ├── HomeViewModel.kt
│       │   ├── HomeState.kt
│       │   └── HomeEvent.kt
│       │
│       ├── newcheckup/                 (Creazione nuovo check-up)
│       │   ├── NewCheckUpScreen.kt
│       │   ├── NewCheckUpViewModel.kt
│       │   ├── NewCheckUpState.kt
│       │   └── IslandTypeSelector.kt
│       │
│       ├── header/                     (Compilazione intestazione)
│       │   ├── HeaderScreen.kt
│       │   ├── HeaderViewModel.kt
│       │   ├── HeaderState.kt
│       │   └── components/
│       │       ├── ParticipantsList.kt
│       │       ├── AttachmentsList.kt
│       │       └── DatePicker.kt
│       │
│       ├── checklist/                  (Esecuzione checklist)
│       │   ├── ChecklistScreen.kt
│       │   ├── ChecklistViewModel.kt
│       │   ├── ChecklistState.kt
│       │   ├── ChecklistEvent.kt
│       │   └── sections/               (UI specifiche per sezione)
│       │       ├── SectionsList.kt
│       │       ├── SectionDetail.kt
│       │       └── ItemDetail.kt
│       │
│       ├── camera/                     (Fotocamera)
│       │   ├── CameraScreen.kt
│       │   ├── CameraViewModel.kt
│       │   └── CameraState.kt
│       │
│       ├── spareparts/                 (Gestione spare parts)
│       │   ├── SparePartsScreen.kt
│       │   ├── SparePartsViewModel.kt
│       │   ├── SparePartsState.kt
│       │   └── SparePartDetailScreen.kt
│       │
│       ├── export/                     (Export report)
│       │   ├── ExportScreen.kt
│       │   ├── ExportViewModel.kt
│       │   ├── ExportState.kt
│       │   └── ExportProgressDialog.kt
│       │
│       ├── archive/                    (Archivio check-up)
│       │   ├── ArchiveScreen.kt
│       │   ├── ArchiveViewModel.kt
│       │   ├── ArchiveState.kt
│       │   └── ArchiveFilterSheet.kt
│       │
│       └── settings/                   (Impostazioni app)
│           ├── SettingsScreen.kt
│           └── SettingsViewModel.kt
│
├── 🎯 domain/                          (Business Logic Layer)
│   │
│   ├── model/                          (Domain Models - Kotlin puro)
│   │   ├── CheckUpReport.kt
│   │   ├── CheckHeader.kt
│   │   ├── CheckSection.kt
│   │   ├── CheckItem.kt
│   │   ├── Photo.kt
│   │   ├── SparePart.kt
│   │   ├── ExportedFile.kt
│   │   ├── Participant.kt
│   │   ├── Attachment.kt
│   │   │
│   │   └── enums/
│   │       ├── IslandType.kt
│   │       ├── ReportStatus.kt
│   │       ├── CheckStatus.kt
│   │       ├── Criticality.kt
│   │       ├── CheckCategory.kt
│   │       └── ExportFormat.kt
│   │
│   ├── repository/                     (Repository Interfaces)
│   │   ├── CheckUpRepository.kt
│   │   ├── PhotoRepository.kt
│   │   ├── ExportRepository.kt
│   │   └── TemplateRepository.kt
│   │
│   └── usecase/                        (Use Cases - Business Logic)
│       │
│       ├── checkreport/
│       │   ├── CreateCheckUpUseCase.kt
│       │   ├── GetCheckUpByIdUseCase.kt
│       │   ├── GetAllCheckUpsUseCase.kt
│       │   ├── GetActiveCheckUpsUseCase.kt
│       │   ├── GetArchivedCheckUpsUseCase.kt
│       │   ├── UpdateCheckUpUseCase.kt
│       │   ├── DeleteCheckUpUseCase.kt
│       │   └── FinalizeCheckUpUseCase.kt
│       │
│       ├── header/
│       │   ├── UpdateHeaderUseCase.kt
│       │   ├── AddParticipantUseCase.kt
│       │   ├── RemoveParticipantUseCase.kt
│       │   ├── AddAttachmentUseCase.kt
│       │   └── RemoveAttachmentUseCase.kt
│       │
│       ├── checkitem/
│       │   ├── UpdateCheckItemStatusUseCase.kt
│       │   ├── UpdateCheckItemCriticalityUseCase.kt
│       │   ├── UpdateCheckItemNotesUseCase.kt
│       │   └── GetItemsByStatusUseCase.kt
│       │
│       ├── photo/
│       │   ├── AddPhotoUseCase.kt
│       │   ├── DeletePhotoUseCase.kt
│       │   ├── GetPhotosForItemUseCase.kt
│       │   └── UpdatePhotoDescriptionUseCase.kt
│       │
│       ├── spareparts/
│       │   ├── AddSparePartUseCase.kt
│       │   ├── UpdateSparePartUseCase.kt
│       │   ├── DeleteSparePartUseCase.kt
│       │   └── GetSparePartsByCheckUpUseCase.kt
│       │
│       ├── export/
│       │   ├── GenerateWordReportUseCase.kt
│       │   ├── GeneratePdfReportUseCase.kt      (Future)
│       │   └── GenerateExcelReportUseCase.kt    (Future)
│       │
│       └── template/
│           ├── GetTemplateForIslandTypeUseCase.kt
│           └── LoadCheckItemsTemplateUseCase.kt
│
├── 💾 data/                            (Data Layer)
│   │
│   ├── local/                          (Local Data Sources)
│   │   │
│   │   ├── database/
│   │   │   ├── QReportDatabase.kt     (Room Database)
│   │   │   └── Converters.kt          (Type Converters)
│   │   │
│   │   ├── dao/                        (Data Access Objects)
│   │   │   ├── CheckUpDao.kt
│   │   │   ├── CheckHeaderDao.kt
│   │   │   ├── CheckSectionDao.kt
│   │   │   ├── CheckItemDao.kt
│   │   │   ├── PhotoDao.kt
│   │   │   ├── SparePartDao.kt
│   │   │   └── ExportedFileDao.kt
│   │   │
│   │   ├── entity/                     (Room Entities)
│   │   │   ├── CheckUpEntity.kt
│   │   │   ├── CheckHeaderEntity.kt
│   │   │   ├── CheckSectionEntity.kt
│   │   │   ├── CheckItemEntity.kt
│   │   │   ├── PhotoEntity.kt
│   │   │   ├── SparePartEntity.kt
│   │   │   ├── ExportedFileEntity.kt
│   │   │   │
│   │   │   └── relations/
│   │   │       ├── CheckUpWithDetails.kt
│   │   │       ├── SectionWithItems.kt
│   │   │       └── ItemWithPhotos.kt
│   │   │
│   │   └── filesystem/
│   │       └── PhotoFileManager.kt     (Gestione file foto)
│   │
│   ├── repository/                     (Repository Implementations)
│   │   ├── CheckUpRepositoryImpl.kt
│   │   ├── PhotoRepositoryImpl.kt
│   │   ├── ExportRepositoryImpl.kt
│   │   └── TemplateRepositoryImpl.kt
│   │
│   ├── mapper/                         (Entity ↔ Model Mappers)
│   │   ├── CheckUpMapper.kt
│   │   ├── CheckHeaderMapper.kt
│   │   ├── CheckSectionMapper.kt
│   │   ├── CheckItemMapper.kt
│   │   ├── PhotoMapper.kt
│   │   └── SparePartMapper.kt
│   │
│   ├── template/                       (Check Items Templates)
│   │   ├── ChecklistTemplates.kt
│   │   ├── PolyMoveTemplate.kt
│   │   ├── PolyCastTemplate.kt
│   │   ├── PolyEbtTemplate.kt
│   │   ├── PolyTagBleTemplate.kt
│   │   ├── PolyTagFcTemplate.kt
│   │   ├── PolyTagVTemplate.kt
│   │   └── PolySampleTemplate.kt
│   │
│   └── export/                         (Export Implementations)
│       ├── WordExporter.kt             (Apache POI)
│       ├── PdfExporter.kt              (Future)
│       └── ExcelExporter.kt            (Future)
│
└── 🔧 di/                              (Dependency Injection - Hilt)
    ├── AppModule.kt                    (Application-level dependencies)
    ├── DatabaseModule.kt               (Room Database provision)
    ├── RepositoryModule.kt             (Repository bindings)
    ├── UseCaseModule.kt                (Use Case provision)
    └── FileSystemModule.kt             (File manager provision)
```

---

## 7. SETUP E DIPENDENZE

### 7.1 Gradle Setup

#### gradle/libs.versions.toml
```toml
[versions]
agp = "8.2.0"
kotlin = "1.9.22"
ksp = "1.9.22-1.0.17"
compose-bom = "2024.02.00"
compose-compiler = "1.5.8"
room = "2.6.1"
hilt = "2.50"
hilt-navigation = "1.1.0"
navigation = "2.7.6"
camerax = "1.3.1"
poi = "5.2.5"
coil = "2.5.0"
gson = "2.10.1"
timber = "5.0.1"
lifecycle = "2.7.0"
activity-compose = "1.8.2"
core-ktx = "1.12.0"

junit = "4.13.2"
androidx-junit = "1.1.5"
espresso-core = "3.5.1"

[libraries]
# Core Android
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation" }

# CameraX
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# Apache POI (Word Export)
apache-poi = { group = "org.apache.poi", name = "poi", version.ref = "poi" }
apache-poi-ooxml = { group = "org.apache.poi", name = "poi-ooxml", version.ref = "poi" }

# Image Loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# JSON
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Logging
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-junit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso-core" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

#### build.gradle.kts (Project)
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

#### build.gradle.kts (App Module)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "net.calvuz.qreport"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.calvuz.qreport"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Apache POI
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)

    // Timber
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

---

**🎯 DOCUMENTO PRONTO PER LE PROSSIME SESSIONI DI SVILUPPO**

Questo documento completo include:
✅ Panoramica e obiettivi  
✅ Stack tecnologico completo  
✅ Clean Architecture dettagliata  
✅ Data model completo (Domain + Room)  
✅ Tutti i template checklist per ogni tipo isola  
✅ Struttura progetto completa  
✅ Setup Gradle e dipendenze

Nella prossima chat continueremo con:
- UI/UX Guidelines
- Sistema Export Word (Apache POI)
- Gestione Foto e Storage
- Roadmap sviluppo MVP

Salva questo documento come riferimento! 🚀