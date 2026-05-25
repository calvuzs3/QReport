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