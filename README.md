<div align="center">

# 🔐 SecureFileVault

### Zero-Trust File Protection — Encrypt. Store. Deliver.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?style=for-the-badge&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue?style=for-the-badge&logo=postgresql)
![AES-256](https://img.shields.io/badge/Encryption-AES--256--GCM-red?style=for-the-badge&logo=letsencrypt)
![Render](https://img.shields.io/badge/Deployed-Render.com-46E3B7?style=for-the-badge&logo=render)

**SecureFileVault** is a production-ready REST API that compresses, encrypts, and securely stores files in the cloud — then delivers them back only to users with the correct password.

[🚀 Live Demo](#) &nbsp;|&nbsp; [📖 API Docs](#api-endpoints) &nbsp;|&nbsp; [⚙️ Setup](#setup)

</div>

---

## ✨ Features

- 🔒 **AES-256-GCM Encryption** — Military-grade encryption before any file touches cloud storage
- 🗜️ **GZIP Compression** — Files compressed before encryption for optimal storage efficiency
- ☁️ **Supabase Cloud Storage** — Scalable, reliable file persistence with private bucket access
- 🗃️ **PostgreSQL Metadata** — File records managed via Spring Data JPA with UUID-based access
- 📧 **Email Notifications** — Automated delivery of File ID + download instructions via Gmail SMTP
- 🛡️ **Zero-Trust Design** — Even if cloud storage is breached, files are unreadable without password
- 🌐 **RESTful API** — Clean, stateless endpoints testable via Postman or any HTTP client

---

## 🏗️ System Architecture

```
User (Postman / Frontend)
        │
        │  POST /api/files/upload
        │  (file + password + email)
        ▼
┌─────────────────────────────┐
│     Spring Boot Backend      │
│                             │
│  1. GZIP Compress           │
│  2. AES-256-GCM Encrypt     │
│  3. Upload to Supabase      │
│  4. Save metadata to DB     │
│  5. Send email notification │
└─────────────────────────────┘
        │                │
        ▼                ▼
  Supabase Storage   PostgreSQL
  (encrypted file)   (metadata)
        │
        ▼
  Email → User gets File ID
        │
        │  GET /api/files/download/{fileId}?password=xxx
        ▼
┌─────────────────────────────┐
│     Spring Boot Backend      │
│                             │
│  1. Lookup metadata in DB   │
│  2. Fetch from Supabase     │
│  3. AES-256-GCM Decrypt     │
│  4. GZIP Decompress         │
│  5. Stream original file    │
└─────────────────────────────┘
        │
        ▼
   Original file returned ✅
```

---

## 🔐 Security Design

| Layer | Implementation |
|---|---|
| Encryption Algorithm | AES-256-GCM (authenticated encryption) |
| Key Derivation | PBKDF2WithHmacSHA256 with random salt |
| IV Generation | Random 12-byte IV per file |
| Storage | Encrypted bytes only — plaintext never touches cloud |
| Compression Order | Compress → Encrypt (encrypted data is incompressible) |
| Password Storage | Never stored — used only as encryption key derivation input |

> **Why compress before encrypt?** Encrypted data appears as random noise — random data has no patterns and cannot be compressed. Compressing first, then encrypting gives both size reduction and security.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Cloud Storage | Supabase Storage (S3-compatible) |
| Database | PostgreSQL via Supabase |
| ORM | Spring Data JPA + Hibernate |
| Email | JavaMailSender + Gmail SMTP |
| HTTP Client | Spring RestTemplate |
| Build Tool | Maven |
| Deployment | Render.com |
| API Testing | Postman |

---

## 📡 API Endpoints

### POST `/api/files/upload`
Upload and secure a file.

**Request** — `multipart/form-data`
| Parameter | Type | Description |
|---|---|---|
| `file` | File | Any file (PDF, image, doc) up to 50MB |
| `password` | String | Password used to encrypt the file |
| `userEmail` | String | Email to receive the File ID |

**Response** — `200 OK`
```json
{
  "fileId": "dbf076b9-48de-4856-910f-25b599f38991",
  "message": "File uploaded successfully"
}
```

---

### GET `/api/files/download/{fileId}?password={password}`
Download and decrypt a file.

**Path Variable** — `fileId` (UUID received after upload)

**Query Parameter** — `password` (same password used during upload)

**Response** — `200 OK`
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="original_filename.pdf"
Body: <decrypted file bytes>
```

**Error Responses**
```json
{ "error": "File not found", "status": 404 }
{ "error": "Invalid password", "status": 400 }
{ "error": "Upload failed", "status": 500 }
```

---

## ⚙️ Setup

### Prerequisites
- Java 21+
- Maven 3.8+
- Supabase account (free tier)
- Gmail account with App Password enabled

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/SecureFileVault.git
cd SecureFileVault
```

### 2. Create Supabase project
```
1. Go to https://supabase.com → New Project
2. Storage → New Bucket → name it "secure-vault" → Private
3. SQL Editor → run:

CREATE TABLE file_metadata (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    supabase_key VARCHAR(500) NOT NULL UNIQUE,
    upload_timestamp TIMESTAMP NOT NULL,
    user_email VARCHAR(255) NOT NULL
);
```

### 3. Configure environment variables
Create `application.properties` (never commit this file):
```properties
spring.application.name=securevault
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://YOUR_SUPABASE_HOST:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# Supabase Storage
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.key=YOUR_SERVICE_ROLE_KEY
supabase.bucket=secure-vault

# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_16_DIGIT_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.from=YOUR_GMAIL@gmail.com

# File limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### 4. Run locally
```bash
./mvnw spring-boot:run
```
App starts at `http://localhost:8080`

### 5. Test with Postman
```
POST http://localhost:8080/api/files/upload
Body → form-data:
  file      → any PDF
  password  → YourPassword123
  userEmail → your@email.com
```

---

## 📁 Project Structure

```
src/main/java/com/demo/securevault/
├── controller/
│   └── FileController.java       # REST endpoints
├── service/
│   ├── FileService.java          # Upload/download logic + Supabase HTTP calls
│   ├── EncryptionService.java    # AES-256-GCM + GZIP
│   └── EmailService.java         # Gmail SMTP notification
├── repository/
│   └── FileMetadataRepository.java  # JPA repository
├── model/
│   └── FileMetadata.java         # DB entity
├── config/
│   └── SupabaseConfig.java       # RestTemplate + Supabase properties
└── SecurevaultApplication.java
```

---

## 🚀 Deployment (Render.com)

```
1. Push code to GitHub (ensure application.properties is in .gitignore)
2. Render.com → New Web Service → connect GitHub repo
3. Build Command: ./mvnw clean install -DskipTests
4. Start Command: java -jar target/securevault-0.0.1-SNAPSHOT.jar
5. Add Environment Variables in Render dashboard
```

---

## 🧠 Key Engineering Decisions

**Why AES-256-GCM over AES-CBC?**
GCM provides authenticated encryption — it detects tampering with encrypted data. CBC only encrypts; a corrupted or maliciously modified ciphertext would decrypt silently into garbage. GCM is the standard used by TLS 1.3, WhatsApp, and Signal.

**Why compress before encrypting?**
Encryption output is statistically random — random data has no repeating patterns, making it incompressible. Compressing first maximizes size reduction, then encryption secures it.

**Why not store the password?**
The password is never stored anywhere — not in the database, not in logs. It is used only to derive the AES key via PBKDF2. This means even a full database breach reveals nothing useful to an attacker.

---

## 👩‍💻 Author

**Khushi Arya**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=flat&logo=linkedin)](https://linkedin.com/in/YOUR_PROFILE)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-black?style=flat&logo=github)](https://github.com/YOUR_USERNAME)

---

<div align="center">
  <sub>Built with ❤️ using Java & Spring Boot</sub>
</div>
