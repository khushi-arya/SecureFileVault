// Initialize app on page load
function initializeApp() {
  // Check if user is already logged in
  const user = getCurrentUser();
  if (user) {
    updateAuthUI(user);
  }

  setupScrollReveal();
  setupNavigation();
}

// Set up scroll reveal animation
function setupScrollReveal() {
  const revealItems = document.querySelectorAll('.reveal');
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.15 }
  );

  revealItems.forEach((item) => observer.observe(item));
}

// Setup navigation
function setupNavigation() {
  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener('click', () => {
      const navbarCollapse = document.querySelector('.navbar-collapse');
      if (navbarCollapse && navbarCollapse.classList.contains('show')) {
        const toggle = document.querySelector('.navbar-toggler');
        toggle?.click();
      }
    });
  });
}

// ============ Authentication Handlers ============

function handleLogin(event) {
  event.preventDefault();
  
  const email = document.getElementById('loginEmail').value;
  const password = document.getElementById('loginPassword').value;
  const button = event.target.querySelector('button[type="submit"]');
  
  // Show loading state
  const originalText = button.innerHTML;
  button.disabled = true;
  button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Logging in...';

  try {
    const user = login(email, password);
    
    // Update UI
    setTimeout(() => {
      updateAuthUI(user);
      document.getElementById('loginEmail').value = '';
      document.getElementById('loginPassword').value = '';
      button.disabled = false;
      button.innerHTML = originalText;
    }, 800);
  } catch (error) {
    button.disabled = false;
    button.innerHTML = originalText;
    showNotification('Login failed: ' + error.message, 'error');
  }
}

function handleSignUp(event) {
  event.preventDefault();
  
  const fullName = document.getElementById('signupName').value;
  const email = document.getElementById('signupEmail').value;
  const password = document.getElementById('signupPassword').value;
  const button = event.target.querySelector('button[type="submit"]');
  
  if (password.length < 4) {
    showNotification('Password must be at least 4 characters', 'error');
    return;
  }

  // Show loading state
  const originalText = button.innerHTML;
  button.disabled = true;
  button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating account...';

  try {
    const user = signUp(fullName, email, password);
    
    // Update UI
    setTimeout(() => {
      document.getElementById('signupSection').style.display = 'none';
      document.getElementById('signupSuccessSection').style.display = 'block';
      document.getElementById('signupUserEmail').textContent = email;
      
      // Reset form
      document.getElementById('signupName').value = '';
      document.getElementById('signupEmail').value = '';
      document.getElementById('signupPassword').value = '';
    }, 800);
  } catch (error) {
    button.disabled = false;
    button.innerHTML = originalText;
    showNotification('Sign up failed: ' + error.message, 'error');
  }
}

function updateAuthUI(user) {
  // Hide login and signup forms
  document.getElementById('loginSection').style.display = 'none';
  document.getElementById('signupSection').style.display = 'none';
  document.getElementById('loginSuccessSection').style.display = 'block';
  document.getElementById('signupSuccessSection').style.display = 'none';
  
  document.getElementById('loginUserEmail').textContent = user.email;
  
  // Show upload section
  showUploadSection();
  
  // Load file history
  loadFileHistory(user.email);
}

function handleLogout() {
  logout();
  location.reload();
}

// ============ File Upload Handler ============

async function handleFileUpload(event) {
  event.preventDefault();
  
  const file = document.getElementById('fileInput').files[0];
  const password = document.getElementById('uploadPassword').value;
  const button = event.target.querySelector('button[type="submit"]');
  
  if (!file) {
    showNotification('Please select a file', 'error');
    return;
  }

  if (file.size > 50 * 1024 * 1024) {
    showNotification('File size exceeds 50 MB limit', 'error');
    return;
  }

  // Show loading state
  const originalText = button.innerHTML;
  button.disabled = true;
  button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Uploading...';

  try {
    const fileId = await uploadFile(file, password);
    
    if (fileId) {
      // Show success with file ID
      showUploadSuccess(fileId, file.name);
      
      // Reset form
      document.getElementById('uploadForm').reset();
      
      // Reload history
      const user = getCurrentUser();
      if (user) {
        loadFileHistory(user.email);
      }
    }
  } catch (error) {
    showNotification('Upload failed: ' + error.message, 'error');
  } finally {
    button.disabled = false;
    button.innerHTML = originalText;
  }
}

function showUploadSuccess(fileId, fileName) {
  const successDiv = document.createElement('div');
  successDiv.className = 'alert alert-success alert-dismissible fade show mt-3';
  successDiv.innerHTML = `
    <h5 class="alert-heading"><i class="bi bi-check-circle me-2"></i>Upload Successful!</h5>
    <p class="mb-2"><strong>File ID:</strong></p>
    <input type="text" class="form-control mb-2" value="${fileId}" readonly />
    <p class="text-muted small mb-0">
      <strong>File:</strong> ${fileName}<br>
      Share the File ID above with recipients. They can use it to download and decrypt the file with the password you set.
    </p>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
  
  const uploadForm = document.getElementById('uploadForm');
  uploadForm.parentElement.insertBefore(successDiv, uploadForm.nextSibling);
}

// ============ File Download Handler ============

async function handleDownload(event) {
  event.preventDefault();
  
  const fileId = document.getElementById('downloadFileId').value;
  const password = document.getElementById('downloadPassword').value;
  const button = event.target.querySelector('button[type="submit"]');
  
  if (!fileId || !password) {
    showNotification('Please enter both File ID and password', 'error');
    return;
  }

  // Show loading state
  const originalText = button.innerHTML;
  button.disabled = true;
  button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Downloading...';

  try {
    await downloadFile(fileId, password);
    document.getElementById('downloadFileId').value = '';
    document.getElementById('downloadPassword').value = '';
  } catch (error) {
    showNotification('Download failed: ' + error.message, 'error');
  } finally {
    button.disabled = false;
    button.innerHTML = originalText;
  }
}

// ============ File History Display ============

function showUploadSection() {
  document.getElementById('upload').style.display = 'block';
  const uploadSection = document.getElementById('upload');
  uploadSection.scrollIntoView({ behavior: 'smooth' });
}

function loadFileHistory(email) {
  const history = getFileHistory(email);
  
  if (history.length === 0) {
    document.getElementById('fileHistorySection').style.display = 'none';
    return;
  }

  document.getElementById('fileHistorySection').style.display = 'block';
  const historyList = document.getElementById('fileHistoryList');
  historyList.innerHTML = '';

  history.reverse().forEach((file, index) => {
    const fileItem = document.createElement('div');
    fileItem.className = 'mb-3 p-3 border rounded';
    fileItem.innerHTML = `
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <p class="mb-1"><strong><i class="bi bi-file me-2"></i>${file.fileName}</strong></p>
          <small class="text-muted">ID: ${file.fileId.substring(0, 12)}...</small><br>
          <small class="text-muted">${file.uploadedAt}</small>
        </div>
        <button class="btn btn-sm btn-outline-primary" onclick="copyToClipboard('${file.fileId}')">
          <i class="bi bi-clipboard me-1"></i>Copy ID
        </button>
      </div>
    `;
    historyList.appendChild(fileItem);
  });
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    showNotification('File ID copied to clipboard!', 'success');
  }).catch(err => {
    showNotification('Failed to copy', 'error');
  });
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', initializeApp);
