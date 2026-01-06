# QReport - Industrial Check-up System

**Versione:** 1.0.0  
**Piattaforma:** Android (API 26+)  
**Framework:** Jetpack Compose + Clean Architecture

## 📋 Panoramica

QReport è un'applicazione Android nativa progettata per digitalizzare il processo di check-up delle isole robotizzate industriali. L'app sostituisce i report Word manuali con un sistema strutturato, fotografico e tracciabile.

### 🎯 Obiettivi Principali

- **Digitalizzazione** dei check-up manuali
- **Acquisizione foto** integrate per ogni check item
- **Export automatico** in formato Word professionale
- **Funzionamento offline** completo
- **Template modulari** per diversi tipi di isole

## 🏗️ Architettura

Il progetto segue i principi della **Clean Architecture** con separazione in layer:

```
presentation/          # UI Layer (Jetpack Compose)
├── ui/
│   ├── components/   # Componenti riutilizzabili
│   ├── screens/      # Schermate principali
│   └── theme/        # Design system
├── navigation/       # Navigation graph
└── viewmodel/        # ViewModels

domain/               # Business Logic Layer
├── model/           # Entità di dominio
├── repository/      # Interfacce repository
└── usecase/         # Use cases

data/                # Data Layer
├── database/        # Room database
│   ├── dao/        # Data Access Objects
│   ├── entities/   # Database entities
│   └── converters/ # Type converters
├── repository/      # Implementazioni repository
├── export/          # Sistema export Word
└── photo/           # Gestione foto
```

## 🛠️ Stack Tecnologico

| Componente | Tecnologia | Versione |
|------------|-----------|----------|
| **Linguaggio** | Kotlin | 1.9.22+ |
| **UI Framework** | Jetpack Compose | 2024.02.00 |
| **Database** | Room (SQLite) | 2.6.1+ |
| **DI** | Hilt (Dagger) | 2.50+ |
| **Navigation** | Compose Navigation | 2.7.6+ |
| **Camera** | CameraX | 1.3.1+ |
| **Word Export** | Apache POI | 5.2.5 |
| **Image Loading** | Coil | 2.5.0+ |

## 📱 Funzionalità

### ✅ MVP (Minimum Viable Product)

1. **Gestione Check-up**
    - Creazione con selezione tipo isola
    - Checklist modulari (template base + specifici)
    - Salvataggio automatico come bozza
    - Multi check-up simultanei

2. **Check Item System**
    - Stati: OK/NOK/NA/Pending
    - Livelli di criticità
    - Note testuali illimitate
    - Foto multiple per item

3. **Acquisizione Foto**
    - Integrazione CameraX
    - Storage organizzato
    - Thumbnail automatici
    - Didascalie editabili

4. **Export Word**
    - Generazione .docx professionale
    - Formattazione automatica
    - Inclusione foto nel documento
    - Template personalizzabili

### 🔮 Roadmap Future

- Annotazioni su foto (frecce, evidenziatori)
- Sincronizzazione cloud
- Analytics e trend
- Export PDF/Excel
- Firma digitale
- Integrazione ERP/CMMS

## 🚀 Setup Sviluppo

### Prerequisiti

- Android Studio Hedgehog | 2023.1.1+
- JDK 8+
- Android SDK (API 26+)
- Git

### Installazione

1. **Clone del repository:**
```bash
git clone https://github.com/calvuz/qreport-android.git
cd qreport-android
```

2. **Sync Gradle:**
```bash
./gradlew build
```

3. **Run dell'app:**
```bash
./gradlew assembleDebug
```

### Struttura Database

Il database utilizza Room con le seguenti entità principali:

- `CheckUpEntity` - Check-up principali
- `CheckItemEntity` - Singoli elementi di controllo
- `PhotoEntity` - Foto associate
- `SparePartEntity` - Ricambi necessari
- `CheckItemTemplateEntity` - Template predefiniti

## 📐 Specifiche Design

### Colori Brand

- **Primary:** #1976D2 (Blue 700)
- **Secondary:** #FF8F00 (Orange 800)
- **Success:** #388E3C (Green 700)
- **Error:** #D32F2F (Red 700)

### Typography

- **Font Family:** Roboto (Material Design)
- **Scale:** Material Design Type Scale
- **Accessibilità:** WCAG 2.1 AA compliant

## 🧪 Testing

### Test Unitari
```bash
./gradlew test
```

### Test UI
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## 📦 Build e Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Code Quality
```bash
./gradlew ktlintCheck
./gradlew detekt
```

## 📚 Documentazione

- [UI/UX Guidelines](docs/ui-ux-guidelines.md)
- [Sistema Export Word](docs/export-word-system.md)
- [Gestione Foto](docs/3_photo-management.md)
- [API Documentation](docs/api-documentation.md)

## 🤝 Contributi

1. Fork del progetto
2. Crea un feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit delle modifiche (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Apertura Pull Request

## 📄 Licenza

Questo progetto è proprietario di **Calvuz** - Tutti i diritti riservati.

## 👥 Team

- **Lead Developer:** [Nome Developer]
- **UI/UX Designer:** [Nome Designer]
- **Product Owner:** [Nome PO]

## 📞 Supporto

Per supporto tecnico o domande:
- Email: support@calvuz.net
- Issue Tracker: [GitHub Issues](https://github.com/calvuz/qreport-android/issues)

---

**QReport v1.0** - Digitalizzando la manutenzione industriale, un check-up alla volta. 🏭📱