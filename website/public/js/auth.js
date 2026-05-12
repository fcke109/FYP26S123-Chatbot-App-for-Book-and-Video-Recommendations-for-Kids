import {
  auth,
  db,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  updateProfile,
  doc,
  setDoc,
  serverTimestamp
} from "./firebase-config.js";

const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const forgotPasswordLink = document.getElementById("forgotPasswordLink");

function showMessage(message, isError = false) {
  alert(message);
}

if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;

    try {
      await signInWithEmailAndPassword(auth, email, password);
      showMessage("Login successful!");
      window.location.href = "/catalog.html";
    } catch (error) {
      showMessage(error.message, true);
    }
  });
}

if (registerForm) {
  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("registerName").value.trim();
    const email = document.getElementById("registerEmail").value.trim();
    const age = Number(document.getElementById("registerAge").value);
    const accountType = document.getElementById("accountType").value;
    const password = document.getElementById("registerPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    if (!name || !email || !age || !accountType || !password || !confirmPassword) {
      showMessage("Please fill in all fields.", true);
      return;
    }

    if (password !== confirmPassword) {
      showMessage("Passwords do not match.", true);
      return;
    }

    if (password.length < 6) {
      showMessage("Password must be at least 6 characters.", true);
      return;
    }

    if (age < 3 || age > 18) {
      showMessage("Age must be between 3 and 18.", true);
      return;
    }

    try {
      const result = await createUserWithEmailAndPassword(auth, email, password);
      const user = result.user;

      await updateProfile(user, {
        displayName: name
      });

      await setDoc(doc(db, "users", user.uid), {
        id: user.uid,
        name: name,
        email: email,
        age: age,

        accountType: accountType,
        planType: "FREE",

        interests: [],
        readingLevel: "BEGINNER",
        parentalEmail: "",
        parentalPin: "",

        isGuest: false,
        canDownloadApk: true,

        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });

      showMessage("Account created successfully!");
      window.location.href = "/catalog.html";
    } catch (error) {
      showMessage(error.message, true);
    }
  });
}

if (forgotPasswordLink) {
  forgotPasswordLink.addEventListener("click", async (e) => {
    e.preventDefault();

    const email = document.getElementById("loginEmail").value.trim();

    if (!email) {
      showMessage("Enter your email first, then click forgot password.", true);
      return;
    }

    try {
      await sendPasswordResetEmail(auth, email);
      showMessage("Password reset email sent.");
    } catch (error) {
      showMessage(error.message, true);
    }
  });
}