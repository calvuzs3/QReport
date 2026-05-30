# Il piano è:
Internet → dominio.tld:443
→ Router (port forward 443 → IP NPM)
→ Nginx Proxy Manager (SSL termination)
→ VM QReport (IP interno:8080)
→ Ktor (HTTP)
→ PostgreSQL (porta 5432, solo locale)
PostgreSQL non è mai esposto fuori dalla VM — Ktor ci parla direttamente su localhost.

Procediamo per step. Inizia creando la VM su Proxmox:
Specifiche consigliate:

OS: Debian 12
CPU: 2 vCPU
RAM: 2GB
Disco: 20GB (PostgreSQL + logs + JAR Ktor)
Rete: bridge sulla stessa LAN del NPM

Dopo aver creato la VM e fatto il primo boot, esegui questi comandi per preparare l'ambiente:
bash# System update
apt update && apt upgrade -y

## Java 21 (Ktor gira su JVM)
apt install -y curl gnupg
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list
apt update && apt install -y temurin-21-jdk

## PostgreSQL
apt install -y postgresql postgresql-contrib

## Verifica
java -version
psql --version


# Switch all'utente postgres
su - postgres

# Crea utente applicativo (scegli una password sicura)
createuser --pwprompt qreport_user [vedi .local.properties]

# Crea il database
createdb --owner=qreport_user qreport_db

# Entra nella console SQL
psql


Dentro psql:
sql-- Verifica
\l
-- Deve comparire qreport_db

-- Esci
\q
Poi esci dall'utente postgres:
bashexit

Step 2 — Schema del database
bash# Connettiti come utente applicativo
psql -U qreport_user -d qreport_db -h localhost
Dentro psql, crea le tabelle del gruppo client management:
sql-- Clients
CREATE TABLE clients (
id TEXT PRIMARY KEY,
company_name TEXT NOT NULL,
notes TEXT,
headquarters_json TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_clients_is_deleted ON clients(is_deleted);
CREATE INDEX idx_clients_updated_at ON clients(updated_at);

-- Contacts
CREATE TABLE contacts (
id TEXT PRIMARY KEY,
client_id TEXT NOT NULL,
first_name TEXT NOT NULL,
last_name TEXT,
title TEXT,
role TEXT,
department TEXT,
phone TEXT,
mobile_phone TEXT,
email TEXT,
alternative_email TEXT,
is_primary BOOLEAN NOT NULL DEFAULT FALSE,
preferred_contact_method TEXT,
notes TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_contacts_client_id ON contacts(client_id);
CREATE INDEX idx_contacts_is_deleted ON contacts(is_deleted);
CREATE INDEX idx_contacts_updated_at ON contacts(updated_at);

-- Contracts
CREATE TABLE contracts (
id TEXT PRIMARY KEY,
client_id TEXT NOT NULL,
name TEXT,
description TEXT,
start_date BIGINT NOT NULL,
end_date BIGINT NOT NULL,
has_priority BOOLEAN NOT NULL DEFAULT TRUE,
has_remote_assistance BOOLEAN NOT NULL DEFAULT TRUE,
has_maintenance BOOLEAN NOT NULL DEFAULT TRUE,
notes TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_contracts_client_id ON contracts(client_id);
CREATE INDEX idx_contracts_is_deleted ON contracts(is_deleted);
CREATE INDEX idx_contracts_updated_at ON contracts(updated_at);

-- Facilities
CREATE TABLE facilities (
id TEXT PRIMARY KEY,
client_id TEXT NOT NULL,
name TEXT NOT NULL,
code TEXT,
notes TEXT,
facility_type TEXT NOT NULL,
address_json TEXT,
is_primary BOOLEAN NOT NULL DEFAULT FALSE,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_facilities_client_id ON facilities(client_id);
CREATE INDEX idx_facilities_is_deleted ON facilities(is_deleted);
CREATE INDEX idx_facilities_updated_at ON facilities(updated_at);

-- Facility islands
CREATE TABLE facility_islands (
id TEXT PRIMARY KEY,
facility_id TEXT NOT NULL,
commissioning_number TEXT,
island_type TEXT NOT NULL,
serial_number TEXT NOT NULL,
model_number TEXT,
model TEXT,
installation_date BIGINT,
warranty_expiration BIGINT,
operating_hours BIGINT NOT NULL DEFAULT 0,
cycle_count BIGINT NOT NULL DEFAULT 0,
last_maintenance_date BIGINT,
next_scheduled_maintenance BIGINT,
custom_name TEXT,
location TEXT,
notes TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_facility_islands_facility_id ON facility_islands(facility_id);
CREATE INDEX idx_facility_islands_is_deleted ON facility_islands(is_deleted);
CREATE INDEX idx_facility_islands_updated_at ON facility_islands(updated_at);

-- Mechanical units
CREATE TABLE mechanical_units (
id TEXT PRIMARY KEY,
island_id TEXT NOT NULL,
unit_type TEXT NOT NULL,
name TEXT NOT NULL,
serial_number TEXT,
model TEXT,
notes TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL,
updated_at BIGINT NOT NULL,
synced_at BIGINT,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mechanical_units_island_id ON mechanical_units(island_id);
CREATE INDEX idx_mechanical_units_is_deleted ON mechanical_units(is_deleted);
CREATE INDEX idx_mechanical_units_updated_at ON mechanical_units(updated_at);

-- Sync log (traccia le sessioni di sync per ogni device)
CREATE TABLE sync_log (
id SERIAL PRIMARY KEY,
device_id TEXT NOT NULL,
synced_at BIGINT NOT NULL,
records_pushed INTEGER NOT NULL DEFAULT 0,
records_pulled INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_sync_log_device_id ON sync_log(device_id);

-- Auth users (tecnici autorizzati)
CREATE TABLE auth_users (
id SERIAL PRIMARY KEY,
username TEXT NOT NULL UNIQUE,
password_hash TEXT NOT NULL,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at BIGINT NOT NULL
);

-- Verifica
\dt

-- Esci
\q



Passiamo al progetto Ktor. Sul tuo PC di sviluppo (non sulla VM):

Step 1 — Crea il progetto
Vai su start.ktor.io e configura:

Project name: qreport-server
Build system: Gradle (Kotlin DSL)
Engine: Netty
Kotlin version: ultima stabile

Plugin da selezionare:

Routing
Content Negotiation
kotlinx.serialization
Authentication → Authentication JWT
Status Pages

Scarica lo zip, estrailo e aprilo con IntelliJ IDEA.

Step 2 — Dipendenze aggiuntive
Nel build.gradle.kts aggiungile alle dependencies esistenti:
kotlindependencies {
// ... quelle generate da start.ktor.io ...

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.3")

    // Exposed ORM (query type-safe in Kotlin)
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")

    // Connection pool
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

Step 3 — Struttura cartelle
Crea questa struttura dentro src/main/kotlin/net/calvuz/qreport/:
├── Application.kt
├── database/
│   └── DatabaseFactory.kt      ← connessione HikariCP + Exposed
├── model/
│   └── SyncDto.kt              ← tutti i DTO di sync
├── repository/
│   └── SyncServerRepository.kt ← query SQL con Exposed
├── routes/
│   ├── AuthRoutes.kt           ← POST /auth/login
│   └── SyncRoutes.kt           ← POST /sync/push, GET /sync/pull
└── plugins/
├── Security.kt             ← JWT config
├── Routing.kt              ← registrazione routes
└── Serialization.kt        ← JSON config



Partiamo dal basso e saliamo. Prima il database, poi i DTO, poi le route.
File 1 — DatabaseFactory.ktHikariCP + Exposed database factory for Ktor serverHikariCP + Exposed database factory for Ktor serverFile 2 — SyncDto.kt (allineato esattamente allo schema PostgreSQL)Sync DTOs matching the exact PostgreSQL schema columnsSync DTOs matching the exact PostgreSQL schema columnsFile 3 — SyncServerRepository.ktServer repository handling push and pull SQL operations with ExposedServer repository handling push and pull SQL operations with ExposedFile 4-6 — plugins e routesCreati 6 file, file lettoCreati 6 file, file lettoNove file, struttura completa. Tre cose da fare prima di compilare:
1. Verifica application.conf — il file generato da start.ktor.io deve avere:
   hoconktor {
   deployment {
   port = 8080
   }
   application {
   modules = [ net.calvuz.qreport.ApplicationKt.module ]
   }
   }
2. Aggiungi com.auth0:java-jwt al build.gradle.kts — serve per firmare i token JWT, start.ktor.io non la include automaticamente:
   kotlinimplementation("com.auth0:java-jwt:4.4.0")
3. Crea il primo utente — prima di testare il login, inserisci un utente nella tabella auth_users sulla VM:
   psql -U qreport_user -d qreport_db -h localhost
   sqlINSERT INTO auth_users (username, password_hash, is_active, created_at)
   VALUES ('admin', '$2a$10$...', TRUE, extract(epoch from now()) * 1000);



Edit Configurations → QReport Server → Environment variables, aggiungi:
DB_URL=jdbc:postgresql://localhost:5432/qreport_db
DB_USER=qreport_user
DB_PASSWORD=la_password_che_hai_scelto
JWT_SECRET=una_stringa_lunga_e_casuale_almeno_32_caratteri
JWT_ISSUER=qreport-server
JWT_AUDIENCE=qreport-android





#
#Sostituisci <IP_DELLA_VM> con l'IP locale della tua VM Debian su Proxmox, ad esempio 192.168.1.xxx.
#Prima però devi permettere a PostgreSQL sulla VM di accettare connessioni esterne, perché di default ascolta solo su localhost. Sulla VM esegui:
#bash# Trova il file postgresql.conf
#nano /etc/postgresql/15/main/postgresql.conf
#Cambia la riga:
##listen_addresses = 'localhost'
#in:
#listen_addresses = '*'
#Poi modifica pg_hba.conf per permettere la connessione dal tuo PC:
#bashnano /etc/postgresql/15/main/pg_hba.conf
#Aggiungi in fondo:
#host    qreport_db    qreport_user    192.168.1.0/24    md5
#Sostituisci 192.168.1.0/24 con il tuo range di rete locale reale.
#Poi riavvia PostgreSQL:
#bashsystemctl restart postgresql
#
#


Se risponde su localhost, prova anche dal tuo IP locale Windows (non dalla VM):
powershell# Trova il tuo IP Windows
ipconfig | findstr IPv4

# Poi testa con quell'IP
curl.exe http://<TUO_IP_WINDOWS>:8080/sync/pull?since=0 `
-H "Authorization: Bearer eyJ..."
Se risponde anche sull'IP locale allora 0.0.0.0 funziona e il messaggio del log è solo cosmetic.




# DEPLOY SU VM

IN GRADLE:
application {
mainClass = "net.calvuz.qreport.ApplicationKt"
}

Dobbiamo buildare un JAR eseguibile e copiarlo sulla VM. Prima verifica che nel build.gradle.kts ci sia il plugin per generare il fat JAR:
kotlin

plugins {
alias(libs.plugins.kotlin.jvm)
alias(ktorLibs.plugins.ktor)          // ← questo include già il task shadowJar
alias(libs.plugins.kotlin.serialization)
}

Il plugin Ktor include già shadowJar. Verifica anche che ci sia:
kotlin

application {
mainClass = "net.calvuz.qreport.ApplicationKt"  // ← deve puntare al nostro main
}

Se è così, buildalo da IntelliJ con il terminale integrato:
bash

./gradlew buildFatJar

Su Windows:
powershell

.\gradlew.bat buildFatJar

Il JAR viene generato in build/libs/. Dimmi come si chiama il file generato.

Parentesi se dovessi sewttare JAVA_HOME
{
 installa java
}


## Ora sulla VM crea il file con le variabili d'ambiente e avvia il server:
bash# Crea il file di configurazione environment
nano /opt/qreport/.env
Inserisci:
DB_URL=jdbc:postgresql://localhost:5432/qreport_db
DB_USER=qreport_user
DB_PASSWORD=la_tua_password
JWT_SECRET=una_stringa_lunga_e_casuale_almeno_32_caratteri
JWT_ISSUER=qreport-server
JWT_AUDIENCE=qreport-android


## Ottimo!Ottimo! Ora rendiamolo un servizio systemd così parte automaticamente al boot della VM.
bash# Crea il service file
nano /etc/systemd/system/qreport.service
Inserisci:
ini[Unit]
Description=QReport Server
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/qreport
EnvironmentFile=/opt/qreport/.env
ExecStart=java -jar /opt/qreport/qreport-server-all.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
Poi:
bash# Abilita e avvia il servizio
systemctl daemon-reload
systemctl enable qreport
systemctl start qreport

# Verifica che sia in esecuzione
systemctl status qreport


### Ora aggiornare i record CNAME su cloudflare e aggiungere il servizio al proxymanager
testare con:
curl.exe https://qreport.calvuz.net/auth/login `
  -X POST `
-H "Content-Type: application/json" `
-d '{\"username\":\"admin\",\"password\":\"admin\"}'

Se restituisce un token tutto OK.

Fase due completata.




# Phase 3

Nel frattempo aggiungi al build.gradle.kts Android le dipendenze mancanti:
kotlin// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Encrypted SharedPreferences
implementation("androidx.security:security-crypto:1.1.0-alpha06")QreportapiKT ScaricaRemotedtosKT ScaricaTokenstorageKT ScaricaRemotedatasourceKT ScaricaRetrofitremotedatasourceKT ScaricaNetworkmoduleKT ScaricaLoginusecaseKT ScaricaSyncusecaseKT ScaricaSyncresultKT ScaricaScarica tutto
Phase 3 completata ✅

✅ QReportApi — Retrofit interface
✅ RemoteDtos — DTO allineati al server
✅ TokenStorage — JWT in EncryptedSharedPreferences
✅ RemoteDataSource + RetrofitRemoteDataSource
✅ NetworkModule — Hilt DI
✅ SyncMapper — entity ↔ DTO
✅ LoginUseCase + SyncUseCase
✅ Compila senza errori




# Phase 4 
## UI sync è il prossimo passo:

Tre file da creare/aggiornare:

SyncLoginScreen + SyncLoginViewModel — schermata login
SyncSettingsScreen aggiornato — pulsante sync + stato login
SyncSettingsViewModel aggiornato — triggerSync() + isLoggedIn

Navigation graph — aggiungere route sync_login

In QReportRoutes:
kotlinconst val SYNC_LOGIN = "sync_login"

E nella route sync_settings aggiungi il parametro di navigazione:




# Phase 5 

No WorkManager — solo un LifecycleObserver sul ProcessLifecycleOwner
Token expiry — già parzialmente gestito in SyncSettingsViewModel, serve solo un interceptor OkHttp per centralizzarlo
Conflict resolution — già risolto con last-write-wins, niente da fare

Tre file in totale:

SyncForegroundObserver — si attiva quando l'app torna in foreground
TokenExpiryInterceptor — OkHttp interceptor che intercetta i 401 e pulisce il token
QReportApplication aggiornato — registra l'observer
