import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

import {
  getFirestore,
  collection,
  query,
  where,
  orderBy,
  limit,
  startAfter,
  getDocs,
  getDoc,
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch,
  increment,
  serverTimestamp,
  onSnapshot
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

import {
  getStorage,
  ref as storageRef,
  getBlob,
  getMetadata
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js";

const firebaseConfig = {
  apiKey: "AIzaSyAsZfL8NaSDAPuO2JgC5q1ogJcTbZge0xk",
  authDomain: "fyp-chatbot-81a50.firebaseapp.com",
  projectId: "fyp-chatbot-81a50",
  storageBucket: "fyp-chatbot-81a50.firebasestorage.app",
  messagingSenderId: "698763257857",
  appId: "1:698763257857:web:a32596abe40098a00f3800",
  measurementId: "G-WEZ9YEJ6SF"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);

export {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
  collection,
  query,
  where,
  orderBy,
  limit,
  startAfter,
  getDocs,
  getDoc,
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch,
  increment,
  serverTimestamp,
  onSnapshot,
  storageRef,
  getBlob,
  getMetadata
};

// Shared auth state UI updater. Toggles a consistent set of nav elements
// across every page so logged-in users always see their name + Logout, and
// logged-out users always see the Login button. Sets data-auth-state on
// <body> so CSS can drive the loading/signed-in/signed-out states without a
// flash of the wrong UI.
export function initAuthUI({ logoutRedirect = "/" } = {}) {
  const loginLink =
    document.getElementById("nav-login-link") ||
    document.getElementById("nav-login");
  const logoutBtn =
    document.getElementById("nav-logout-btn") ||
    document.getElementById("nav-logout");
  const userName = document.getElementById("nav-user-name");

  onAuthStateChanged(auth, (user) => {
    if (user) {
      document.body.setAttribute("data-auth-state", "signed-in");
      const displayName =
        user.displayName || user.email?.split("@")[0] || "User";

      if (loginLink) loginLink.style.display = "none";
      if (logoutBtn) logoutBtn.style.display = "inline-block";
      if (userName) {
        userName.textContent = displayName;
        userName.style.display = "inline-block";
      }
    } else {
      document.body.setAttribute("data-auth-state", "signed-out");

      if (loginLink) loginLink.style.display = "inline-flex";
      if (logoutBtn) logoutBtn.style.display = "none";
      if (userName) userName.style.display = "none";
    }
  });

  if (logoutBtn && !logoutBtn.dataset.authBound) {
    logoutBtn.dataset.authBound = "1";
    logoutBtn.addEventListener("click", async (e) => {
      e.preventDefault();
      try {
        await signOut(auth);
      } catch (err) {
        console.warn("Logout failed:", err?.message || err);
      }
      window.location.href = logoutRedirect;
    });
  }
}
