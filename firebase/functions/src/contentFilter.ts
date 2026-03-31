/**
 * Server-side content filter for AI responses.
 * Checks for unsafe content categories before returning to client.
 */

const UNSAFE_PATTERNS: RegExp[] = [
  // Violence
  /\b(kill|murder|stab|shoot|blood|gore|violent|weapon|gun|knife|bomb|attack|assault|torture)\b/i,
  // Adult content
  /\b(sex|sexual|naked|nude|porn|explicit|xxx|erotic|fetish)\b/i,
  // Profanity
  /\b(fuck|shit|damn|hell|ass|bitch|bastard|crap|dick|piss)\b/i,
  // Self-harm
  /\b(suicide|self-harm|cut yourself|kill yourself|end your life)\b/i,
  // Drugs & alcohol
  /\b(cocaine|heroin|meth|marijuana|weed|alcohol|beer|wine|drunk|drugs|smoking|cigarette)\b/i,
  // Hate speech
  /\b(racist|sexist|homophobic|hate speech|slur|discriminat)\b/i,
  // Horror/disturbing
  /\b(horror|terrify|nightmare|demon|evil spirit|possessed|haunted)\b/i,
];

const SAFE_FALLBACK =
  "I'd love to help you find some great books and videos! " +
  "Could you tell me what topics you're interested in?";

/**
 * Check if text contains unsafe content for children
 */
export function isContentSafe(text: string): boolean {
  return !UNSAFE_PATTERNS.some((pattern) => pattern.test(text));
}

/**
 * Filter response text, replacing unsafe content with safe fallback
 */
export function filterResponse(text: string): string {
  if (isContentSafe(text)) {
    return text;
  }
  return SAFE_FALLBACK;
}
