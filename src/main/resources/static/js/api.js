// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Track current user session
let currentUser = null;

// Helper function to make API calls
async function apiCall(endpoint, method = 'GET', body = null, isFormData = false) {
  try {
    const options = {
      method,
      headers: isFormData ? {} : { 'Content-Type': 'application/json' }
    };

    if (body) {
      options.body = isFormData ? body : JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`${response.status}: ${error}`);
    }

    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}

// ============ Authentication Functions ============

function signUp(fullName, email, password) {
  // Sign up creates a user session locally (no backend auth yet)
  currentUser = {
    id: generateUUID(),
    fullName,
    email,
    password
  };
  localStorage.setItem('currentUser', JSON.stringify(currentUser));
  showNotification(`Welcome, ${fullName}!`, 'success');
  return currentUser;
}

function login(email, password) {
  // For now, login accepts any email/password
  // In production, this would validate against backend
  currentUser = {
    id: generateUUID(),
    email,
    password
  };
  localStorage.setItem('currentUser', JSON.stringify(currentUser));
  showNotification(`Logged in as ${email}`, 'success');
  return currentUser;
}

function logout() {
  currentUser = null;
  localStorage.removeItem('currentUser');
  showNotification('Logged out successfully', 'info');
}

function getCurrentUser() {
  if (!currentUser) {
    currentUser = localStorage.getItem('currentUser');
    if (currentUser) {
      currentUser = JSON.parse(currentUser);
    }
  }
  return currentUser;
}

function isLoggedIn() {
  return getCurrentUser() !== null;
}

// ============ File Upload Function ============

async function uploadFile(file, password) {
  if (!isLoggedIn()) {
    showNotification('Please login first', 'error');
    return null;
  }

  const user = getCurrentUser();
  if (!file) {
    showNotification('Please select a file', 'error');
    return null;
  }

  if (!password || password.length < 4) {
    showNotification('Password must be at least 4 characters', 'error');
    return null;
  }

  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);
    formData.append('userEmail', user.email);

    const response = await apiCall('/files/upload', 'POST', formData, true);

    showNotification(`File "${file.name}" uploaded successfully!`, 'success');
    console.log('File ID:', response.fileId);

    // Save to user's file history
    saveFileToHistory(file.name, response.fileId, user.email);

    return response.fileId;
  } catch (error) {
    showNotification(`Upload failed: ${error.message}`, 'error');
    return null;
  }
}

// ============ File Download Function ============

async function downloadFile(fileId, password) {
  if (!isLoggedIn()) {
    showNotification('Please login first', 'error');
    return;
  }

  if (!fileId || !password) {
    showNotification('File ID and password are required', 'error');
    return;
  }

  try {
    const endpoint = `/files/download/${fileId}?password=${encodeURIComponent(password)}`;

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'GET'
    });

    if (!response.ok) {
      throw new Error('Download failed - invalid file ID or password');
    }

    const blob = await response.blob();

    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;

    let downloadName = `${fileId}`;

    const disposition = response.headers.get("Content-Disposition");

    if (disposition) {
      // Match filename inside quotes, capturing only non-quote characters.
      // (Old regex used ".+" which greedily consumed the closing quote too,
      // producing names like "file.pdf\"" -> saved as "file.pdf_" on Windows.)
      let match = disposition.match(/filename="([^"]+)"/);

      if (!match) {
        // Fallback: unquoted filename, or RFC 5987 filename*=UTF-8''...
        match = disposition.match(/filename\*?=(?:UTF-8''|["']?)([^;"'\n]+)/i);
      }

      if (match) {
        downloadName = decodeURIComponent(match[1]);
      }
    }

    a.download = downloadName;

    document.body.appendChild(a);
    a.click();

    document.body.removeChild(a);

    window.URL.revokeObjectURL(url);

    showNotification('File downloaded successfully', 'success');

  } catch (error) {
    showNotification(`Download failed: ${error.message}`, 'error');
  }
}

// ============ File History Management ============

function saveFileToHistory(fileName, fileId, email) {
  let history = JSON.parse(localStorage.getItem('fileHistory') || '{}');

  if (!history[email]) {
    history[email] = [];
  }

  history[email].push({
    fileName,
    fileId,
    uploadedAt: new Date().toLocaleString(),
    timestamp: Date.now()
  });

  // Keep only last 20 files
  if (history[email].length > 20) {
    history[email] = history[email].slice(-20);
  }

  localStorage.setItem('fileHistory', JSON.stringify(history));
}

function getFileHistory(email) {
  const history = JSON.parse(localStorage.getItem('fileHistory') || '{}');
  return history[email] || [];
}

// ============ Utility Functions ============

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function showNotification(message, type = 'info') {
  console.log(`[${type.toUpperCase()}] ${message}`);

  // Remove existing notifications
  const existingAlert = document.querySelector('.alert');
  if (existingAlert) {
    existingAlert.remove();
  }

  // Create alert
  const alertDiv = document.createElement('div');
  alertDiv.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show`;
  alertDiv.style.position = 'fixed';
  alertDiv.style.top = '70px';
  alertDiv.style.right = '20px';
  alertDiv.style.zIndex = '9999';
  alertDiv.style.minWidth = '300px';
  alertDiv.innerHTML = `
    ${message}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;

  document.body.appendChild(alertDiv);

  // Auto remove after 5 seconds
  setTimeout(() => {
    if (alertDiv.parentElement) {
      alertDiv.remove();
    }
  }, 5000);
}