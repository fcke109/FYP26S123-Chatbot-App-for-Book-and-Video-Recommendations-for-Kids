import * as admin from "firebase-admin";

admin.initializeApp();

// Export all cloud functions
export {chatWithBot, getRecommendations} from "./chatbot";
export {onUserCreated, migrateExistingUsers, deleteAuthUser, cleanGhostAccounts} from "./triggers";
