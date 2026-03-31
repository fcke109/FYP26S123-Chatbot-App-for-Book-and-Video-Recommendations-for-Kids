import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const YOUTUBE_API_KEY = functions.config().youtube?.key || process.env.YOUTUBE_API_KEY || "";

interface YouTubeSearchRequest {
  query: string;
}

/**
 * Cloud Function: Search YouTube safely (server-side API key)
 * Always enforces safeSearch=strict and videoEmbeddable=true
 */
export const searchYouTube = functions.https.onCall(
  async (data: YouTubeSearchRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const {query} = data;
    if (!query || typeof query !== "string" || query.trim().length === 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Search query is required"
      );
    }

    // Rate limit check
    const userId = context.auth.uid;
    const rateLimitRef = admin.firestore()
      .collection("rateLimits")
      .doc(`${userId}_youtube`);

    const now = Date.now();
    const windowMs = 10 * 60 * 1000; // 10 minutes
    const maxRequests = 30;

    const rateLimitDoc = await rateLimitRef.get();
    if (rateLimitDoc.exists) {
      const rlData = rateLimitDoc.data()!;
      const windowStart = rlData.windowStart?.toMillis?.() || rlData.windowStart || 0;
      if (now - windowStart < windowMs) {
        if ((rlData.count || 0) >= maxRequests) {
          throw new functions.https.HttpsError(
            "resource-exhausted",
            "Too many requests. Please wait a few minutes."
          );
        }
        await rateLimitRef.update({count: admin.firestore.FieldValue.increment(1)});
      } else {
        await rateLimitRef.set({count: 1, windowStart: admin.firestore.Timestamp.now()});
      }
    } else {
      await rateLimitRef.set({count: 1, windowStart: admin.firestore.Timestamp.now()});
    }

    if (!YOUTUBE_API_KEY) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "YouTube API key not configured"
      );
    }

    try {
      const encodedQuery = encodeURIComponent(query);
      const url =
        `https://www.googleapis.com/youtube/v3/search` +
        `?part=snippet` +
        `&maxResults=1` +
        `&type=video` +
        `&videoEmbeddable=true` +
        `&safeSearch=strict` +
        `&q=${encodedQuery}` +
        `&key=${YOUTUBE_API_KEY}`;

      const response = await fetch(url);
      const json = await response.json();

      if (!json.items || json.items.length === 0) {
        return {videoUrl: null, thumbnailUrl: null};
      }

      const item = json.items[0];
      const videoId = item.id?.videoId;
      const title = item.snippet?.title || "";

      if (!videoId) {
        return {videoUrl: null, thumbnailUrl: null};
      }

      return {
        videoUrl: `https://www.youtube.com/watch?v=${videoId}`,
        thumbnailUrl: `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
        title,
      };
    } catch (error) {
      console.error("YouTube search error:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to search YouTube"
      );
    }
  }
);
