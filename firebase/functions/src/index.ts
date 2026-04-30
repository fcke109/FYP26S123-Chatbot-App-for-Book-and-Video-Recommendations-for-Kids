import * as admin from "firebase-admin";

admin.initializeApp();

// Export all cloud functions
export {chatWithBot, getRecommendations} from "./chatbot";
export {onUserCreated, migrateExistingUsers, deleteAuthUser, cleanGhostAccounts} from "./triggers";
export {searchYouTube} from "./youtube-proxy";
export {setAdminRole} from "./admin";
export {logAuditEvent} from "./auditLog";
export {verifyPurchase} from "./billing";

// NOTE: softDeleteChildAccount lives in ./parentControls but is intentionally
// NOT exported on Spark plan — Spark blocks new function deploys. The soft-
// delete flow uses a direct Firestore write gated by the
// `isValidParentSoftDeleteChild` rule branch instead. If/when this project
// upgrades to Blaze, re-export this and the client can be switched back to
// the function call (see git history for the original AccountManager path).
// export {softDeleteChildAccount} from "./parentControls";
