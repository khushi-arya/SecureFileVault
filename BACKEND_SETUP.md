# SecureFileVault - Backend & Frontend Integration Guide

## ✅ Status: Backend is Running

**Backend Server:** `http://localhost:8080`
- Spring Boot Application started successfully
- Connected to Supabase PostgreSQL Database
- CORS enabled (allows requests from any origin)

---

## 📋 API Endpoints

### 1. **Upload File**
```
POST /api/files/upload
```
**Parameters (FormData):**
- `file` - The file to upload (multipart)
- `password` - Encryption password (string)
- `userEmail` - User's email address (string)

**Response:**
```json
{
  "fileId": "uuid-of-file",
  "message": "File uploaded successfully"
}
```

**Example:**
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('password', 'myPassword123');
formData.append('userEmail', 'user@example.com');

const response = await fetch('http://localhost:8080/api/files/upload', {
  method: 'POST',
  body: formData
});
```

### 2. **Download File**
```
GET /api/files/download/{fileId}?password={password}
```
**Parameters:**
- `fileId` - UUID of the file (in URL path)
- `password` - Decryption password (in query string)

**Returns:** Binary file content

---

## 🔧 Backend Configuration

**File:** `src/main/resources/application.properties`

```properties
# Server runs on port 8080
server.port=8080

# Database: Supabase PostgreSQL
spring.datasource.url=jdbc:postgresql://db.ldyniaufwwllzbafxffx.supabase.co:5432/postgres
spring.datasource.username=postgres

# Supabase Storage
supabase.url=https://ldyniaufwwllzbafxffx.supabase.co
supabase.bucket=secure-vailtBucket

# Gmail SMTP for email notifications
spring.mail.host=smtp.gmail.com
spring.mail.port=587
```

---

## 🎨 Frontend Integration

### New Files Created:
1. **`js/api.js`** - API communication layer
   - `apiCall()` - Generic HTTP request handler
   - `uploadFile()` - Upload encrypted files
   - `downloadFile()` - Download and decrypt files
   - `login()` / `signUp()` - User authentication
   - `localStorage` - Session management

2. **Updated `js/app.js`** - Frontend logic
   - `handleLogin()` - Login form handler
   - `handleSignUp()` - Sign-up form handler
   - `handleFileUpload()` - File upload handler
   - `handleDownload()` - File download handler
   - File history management

3. **Updated `index.html`** - UI Components
   - Login/Sign Up forms (now functional)
   - File Upload section
   - File Download section
   - File history display

### Key Features:
- ✅ User login/signup with localStorage persistence
- ✅ File upload with encryption password
- ✅ File download with decryption
- ✅ File ID display and copy to clipboard
- ✅ Upload history tracking
- ✅ Real-time notifications
- ✅ CORS-enabled API calls

---

## 🚀 How to Use

### Step 1: Navigate to Homepage
Open `http://localhost:8080` in your browser

### Step 2: Sign Up or Login
- **Sign Up:** Fill in Full Name, Email, Password → Click "Create Account"
- **Login:** Fill in Email, Password → Click "Login"

### Step 3: Upload a File
1. Click "Go to Upload" button
2. Select a file (max 50 MB)
3. Set an encryption password
4. Click "Encrypt & Upload"
5. **File ID will be displayed** - Share this with recipients

### Step 4: Download a File
1. Enter the File ID you received
2. Enter the password that was used for encryption
3. Click "Download File"
4. File will be decrypted and downloaded

---

## 🆔 User ID & Session Management

### How ID is Generated:
- Each user gets a unique UUID when they sign up/login
- ID is stored in `localStorage` under `currentUser`
- Format: `{ id, email, password, fullName }`

### View Current User:
```javascript
// In browser console:
const user = JSON.parse(localStorage.getItem('currentUser'));
console.log(user.id);  // Your unique ID
```

### Session Persistence:
- User stays logged in even after page refresh
- Clear localStorage to logout

---

## 📁 File ID System

### What is File ID?
- **UUID** assigned when file is uploaded
- **Not the filename** - it's the unique database identifier
- Used to retrieve the encrypted file from Supabase

### Where to Find:
1. **After upload:** Displayed in green success box
2. **In File History:** Listed under "Your Uploads"
3. **Copy Button:** Click "Copy ID" in file history

### Format:
```
Example File ID:
550e8400-e29b-41d4-a716-446655440000
```

---

## 🔐 Security Features

1. **AES-256 Encryption** - All files encrypted before upload
2. **Password Protection** - Files require password to decrypt
3. **CORS Enabled** - Frontend can communicate with backend
4. **HTTPS Ready** - Can be deployed with SSL certificates
5. **Email Notifications** - Users notified when files are shared

---

## 🐛 Troubleshooting

### Frontend Not Connecting to Backend?
- Verify backend is running on `localhost:8080`
- Check browser console for CORS errors
- Clear browser cache and localStorage

### File Upload Fails?
- Check file size (max 50 MB)
- Verify password is at least 4 characters
- Check browser console for error details

### Login Not Working?
- Try signing up first (creates new account)
- Ensure email is valid format
- Check localStorage: `localStorage.getItem('currentUser')`

---

## 📦 Services

### Available Backend Services:
- **FileService** - Upload/download to Supabase
- **EncryptionService** - AES-256 encryption/decryption
- **EmailService** - Send file share notifications
- **FileMetadataRepository** - Database operations

---

## 🔄 Data Flow

```
User Signs Up
    ↓
Generate UUID (User ID)
    ↓
Store in localStorage
    ↓
User Selects File + Password
    ↓
Send to Backend (/api/files/upload)
    ↓
Backend Encrypts File + Stores in Supabase
    ↓
Returns File ID
    ↓
Display File ID to User
    ↓
User Shares File ID + Password with Recipients
    ↓
Recipients Enter File ID + Password
    ↓
Backend Decrypts + Returns File (/api/files/download/{id})
```

---

## ✨ Next Steps

1. **Test the frontend:** Open `http://localhost:8080` in browser
2. **Sign up** with a test email
3. **Upload a test file** with a password
4. **Copy the File ID** that appears
5. **Logout and login** with another email
6. **Try to download** using the File ID from step 4

---

## 📞 Support

All backend endpoints are CORS-enabled and ready for cross-origin requests.
The frontend is now fully integrated with a complete working backend!

**Backend Port:** 8080
**Frontend Port:** 8080 (served as static files)
**Database:** Supabase PostgreSQL (online)
**Storage:** Supabase Storage Bucket
