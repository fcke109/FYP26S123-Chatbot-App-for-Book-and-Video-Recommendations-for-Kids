import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import OpenAI from "openai";

const openai = new OpenAI({
  apiKey: functions.config().openai?.key || process.env.OPENAI_API_KEY,
});

interface ChatRequest {
  userId: string;
  conversationId: string;
  message: string;
}

interface UserProfile {
  name: string;
  age: number;
  interests: string[];
  readingLevel: string;
}

/**
 * Cloud Function: Chat with the AI bot
 * Receives user message and returns AI response
 */
export const chatWithBot = functions.https.onCall(
  async (data: ChatRequest, context) => {
    // Verify authentication
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const {userId, conversationId, message} = data;

    if (!userId || !conversationId || !message) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing required parameters"
      );
    }

    try {
      // Get user profile for personalization
      const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();

      const userProfile = userDoc.data() as UserProfile;

      // Get conversation history
      const messagesSnapshot = await admin
        .firestore()
        .collection("chatHistory")
        .doc(userId)
        .collection("conversations")
        .doc(conversationId)
        .collection("messages")
        .orderBy("timestamp", "asc")
        .limit(10)
        .get();

      const conversationHistory = messagesSnapshot.docs.map((doc) => {
        const msg = doc.data();
        return {
          role: msg.role === "USER" ? "user" : "assistant",
          content: msg.content,
        };
      });

      // Create system prompt based on user age
      const systemPrompt = createSystemPrompt(userProfile);

      // Call OpenAI API
      const completion = await openai.chat.completions.create({
        model: "gpt-3.5-turbo",
        messages: [
          {role: "system", content: systemPrompt},
          ...conversationHistory,
          {role: "user", content: message},
        ],
        temperature: 0.7,
        max_tokens: 500,
      });

      const response = completion.choices[0]?.message?.content ||
        "I'm sorry, I couldn't process that. Can you try again?";

      // Check if response contains recommendations
      const recommendations = await generateRecommendations(
        message,
        response,
        userProfile
      );

      return {
        response,
        recommendations,
      };
    } catch (error) {
      console.error("Error in chatWithBot:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to process chat message"
      );
    }
  }
);

/**
 * Cloud Function: Get personalized recommendations
 */
export const getRecommendations = functions.https.onCall(
  async (data: {userId: string}, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const {userId} = data;

    try {
      const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();

      const userProfile = userDoc.data() as UserProfile;

      // Get books from content library
      const booksSnapshot = await admin
        .firestore()
        .collection("content")
        .doc("library")
        .collection("books")
        .limit(20)
        .get();

      const books = booksSnapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      // Get videos from content library
      const videosSnapshot = await admin
        .firestore()
        .collection("content")
        .doc("library")
        .collection("videos")
        .limit(20)
        .get();

      const videos = videosSnapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      // Filter based on user profile
      const filteredBooks = books.filter((book: any) =>
        isAppropriateForAge(book.ageRange, userProfile.age)
      );

      const filteredVideos = videos.filter((video: any) =>
        isAppropriateForAge(video.ageRange, userProfile.age)
      );

      return {
        books: filteredBooks.slice(0, 5),
        videos: filteredVideos.slice(0, 5),
      };
    } catch (error) {
      console.error("Error in getRecommendations:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to get recommendations"
      );
    }
  }
);

/**
 * Helper: Create age-appropriate system prompt
 */
function createSystemPrompt(userProfile: UserProfile): string {
  const ageGroup = getAgeGroup(userProfile.age);
  const interests = userProfile.interests.join(", ");

  return `You are a friendly and enthusiastic children's book and video recommendation assistant called "Book Buddy".

User Profile:
- Name: ${userProfile.name}
- Age: ${userProfile.age} years old (${ageGroup})
- Interests: ${interests}
- Reading Level: ${userProfile.readingLevel}

Your role:
1. Recommend age-appropriate books and educational videos
2. Be encouraging and supportive about reading and learning
3. Use simple, clear language appropriate for ${ageGroup}
4. Make learning fun and exciting
5. Always ensure content is safe and appropriate for children
6. Ask questions to understand what they're interested in
7. Explain why you're recommending specific books or videos

Guidelines:
- Keep responses concise (2-3 sentences)
- Be enthusiastic and positive
- Use emojis occasionally to make it fun
- Focus on educational content
- Never recommend inappropriate content
- Encourage curiosity and learning`;
}

/**
 * Helper: Determine age group
 */
function getAgeGroup(age: number): string {
  if (age <= 5) return "preschooler";
  if (age <= 8) return "early elementary";
  if (age <= 12) return "middle elementary";
  return "young teen";
}

/**
 * Helper: Check if content is age-appropriate
 */
function isAppropriateForAge(ageRange: string, userAge: number): boolean {
  // Parse age range like "AGES_6_8" or "AGES_3_5"
  if (!ageRange) return true;

  const match = ageRange.match(/AGES_(\d+)_(\d+)/);
  if (!match) return true;

  const minAge = parseInt(match[1]);
  const maxAge = parseInt(match[2]);

  return userAge >= minAge && userAge <= maxAge;
}

/**
 * Helper: Generate recommendations based on conversation
 */
async function generateRecommendations(
  userMessage: string,
  botResponse: string,
  userProfile: UserProfile
): Promise<any[]> {
  // This is a simplified version
  // In production, you would use AI to analyze the conversation
  // and fetch relevant books/videos from the database

  const keywords = extractKeywords(userMessage.toLowerCase());
  const recommendations: any[] = [];

  // Check if user is asking for recommendations
  if (keywords.some((k) =>
    ["book", "video", "read", "watch", "recommend"].includes(k)
  )) {
    // Fetch relevant content from Firestore
    // This is a placeholder - implement actual logic
  }

  return recommendations;
}

/**
 * Helper: Extract keywords from text
 */
function extractKeywords(text: string): string[] {
  return text
    .toLowerCase()
    .split(/\s+/)
    .filter((word) => word.length > 3);
}
