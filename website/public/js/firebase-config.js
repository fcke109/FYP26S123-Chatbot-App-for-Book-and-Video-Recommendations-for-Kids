import { initializeApp } from "https://www.gstatic.com/firebasejs/12.11.0/firebase-app.js";
import { getAuth, onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, sendPasswordResetEmail } from "https://www.gstatic.com/firebasejs/12.11.0/firebase-auth.js";
import { getFirestore, collection, query, where, orderBy, limit, startAfter, getDocs, doc, setDoc, serverTimestamp } from "https://www.gstatic.com/firebasejs/12.11.0/firebase-firestore.js";

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

export {
  onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword,
  signOut, sendPasswordResetEmail,
  collection, query, where, orderBy, limit, startAfter, getDocs,
  doc, setDoc, serverTimestamp
};

// Shared auth state UI updater
export function initAuthUI() {
  const loginLink = document.getElementById("nav-login");
  const logoutLink = document.getElementById("nav-logout");
  const userName = document.getElementById("nav-user-name");

  onAuthStateChanged(auth, (user) => {
    if (user) {
      if (loginLink) loginLink.style.display = "none";
      if (logoutLink) logoutLink.style.display = "inline-block";
      if (userName) {
        userName.textContent = user.displayName || user.email.split("@")[0];
        userName.style.display = "inline-block";
      }
    } else {
      if (loginLink) loginLink.style.display = "inline-block";
      if (logoutLink) logoutLink.style.display = "none";
      if (userName) userName.style.display = "none";
    }
  });

  if (logoutLink) {
    logoutLink.addEventListener("click", async (e) => {
      e.preventDefault();
      await signOut(auth);
      window.location.href = "/";
    });
  }
}
