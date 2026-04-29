// Imports kid-safe books from the Google Books API into content_books.
//
// Get a free API key at: https://console.cloud.google.com → enable "Books API"
// → Credentials → Create API Key.
//
// Run with:
//   cd firebase/functions
//   npm run import:books -- --credentials="C:\path\to\serviceAccount.json" --api-key="YOUR_KEY"
//
// Optional flags:
//   --per-topic=40    Books to fetch per topic (1..40, Google's max per request).
//   --topics=dinosaur,space,animals   Override default topic list.
//   --dry-run         Print what would be written without touching Firestore.
//
// Idempotent: uses Google's volume id as the Firestore doc id (prefixed "gb_"),
// so re-runs overwrite the same doc instead of duplicating.

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

type TopicSpec = {
  keyword: string;
  category: string;
  ageMin: number;
  ageMax: number;
  tags: string[];
};

// Topics align with the personas in seedSyntheticUsers.ts so clustering stays tight.
const DEFAULT_TOPICS: TopicSpec[] = [
  {keyword: "dinosaur", category: "Dinosaurs", ageMin: 4, ageMax: 9, tags: ["dinosaur", "prehistoric"]},
  {keyword: "space", category: "Space", ageMin: 6, ageMax: 12, tags: ["space", "astronomy"]},
  {keyword: "planet", category: "Space", ageMin: 6, ageMax: 12, tags: ["space", "planet"]},
  {keyword: "animals", category: "Animals", ageMin: 3, ageMax: 10, tags: ["animal", "nature"]},
  {keyword: "ocean", category: "Animals", ageMin: 3, ageMax: 10, tags: ["ocean", "sea", "animal"]},
  {keyword: "fairy tale", category: "Fairy Tales", ageMin: 4, ageMax: 9, tags: ["fairy", "fantasy"]},
  {keyword: "princess", category: "Fairy Tales", ageMin: 4, ageMax: 9, tags: ["princess", "fantasy"]},
  {keyword: "science", category: "STEM", ageMin: 7, ageMax: 12, tags: ["science", "stem"]},
  {keyword: "math", category: "STEM", ageMin: 6, ageMax: 12, tags: ["math", "stem"]},
  {keyword: "coding", category: "STEM", ageMin: 8, ageMax: 13, tags: ["coding", "stem"]},
  {keyword: "adventure", category: "Adventure", ageMin: 6, ageMax: 12, tags: ["adventure", "explore"]},
  {keyword: "mystery", category: "Adventure", ageMin: 7, ageMax: 12, tags: ["mystery", "detective"]},
  {keyword: "art", category: "Arts & Music", ageMin: 5, ageMax: 11, tags: ["art", "creativity"]},
  {keyword: "music", category: "Arts & Music", ageMin: 5, ageMax: 11, tags: ["music"]},
  {keyword: "sports", category: "Sports", ageMin: 7, ageMax: 12, tags: ["sport"]},
  {keyword: "soccer", category: "Sports", ageMin: 7, ageMax: 12, tags: ["sport", "soccer"]},
];

type ParsedArgs = {
  apiKey?: string;
  credentials?: string;
  perTopic: number;
  dryRun: boolean;
  topicsOverride?: string[];
};

function parseArgs(argv: string[]): ParsedArgs {
  const out: ParsedArgs = {perTopic: 40, dryRun: false};
  for (const arg of argv.slice(2)) {
    const [key, value] = arg.split("=");
    if (key === "--api-key" && value) out.apiKey = value;
    if (key === "--credentials" && value) out.credentials = value;
    if (key === "--per-topic" && value) {
      const n = parseInt(value, 10);
      out.perTopic = Math.min(40, Math.max(1, n));
    }
    if (key === "--topics" && value) out.topicsOverride = value.split(",").map((s) => s.trim());
    if (key === "--dry-run") out.dryRun = true;
  }
  return out;
}

function loadDotEnv(): void {
  // Lightweight .env loader — avoids a dotenv dependency for this one use.
  // Reads firebase/functions/.env if present (path relative to cwd when run
  // via npm script). Only sets vars that aren't already in process.env.
  const candidates = [
    path.resolve(process.cwd(), ".env"),
    path.resolve(process.cwd(), "..", ".env"),
  ];
  for (const p of candidates) {
    if (!fs.existsSync(p)) continue;
    const raw = fs.readFileSync(p, "utf8");
    for (const line of raw.split(/\r?\n/)) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#")) continue;
      const eq = trimmed.indexOf("=");
      if (eq === -1) continue;
      const key = trimmed.slice(0, eq).trim();
      let value = trimmed.slice(eq + 1).trim();
      if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        value = value.slice(1, -1);
      }
      if (!(key in process.env)) process.env[key] = value;
    }
    return;
  }
}

function readFirebaseProjectId(): string | undefined {
  const candidates = [
    path.resolve(process.cwd(), "..", ".firebaserc"),
    path.resolve(process.cwd(), ".firebaserc"),
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) {
      try {
        const rc = JSON.parse(fs.readFileSync(p, "utf8")) as {projects?: {default?: string}};
        if (rc.projects?.default) return rc.projects.default;
      } catch {
        // fall through
      }
    }
  }
  return undefined;
}

function initAdmin(credentialsPath?: string): void {
  const projectId = readFirebaseProjectId();
  if (credentialsPath) {
    const absPath = path.resolve(credentialsPath);
    if (!fs.existsSync(absPath)) throw new Error(`Service account file not found: ${absPath}`);
    const serviceAccount = JSON.parse(fs.readFileSync(absPath, "utf8"));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id || projectId,
    });
    return;
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error(
      "No credentials. Pass --credentials=path\\to\\serviceAccount.json " +
        "or set GOOGLE_APPLICATION_CREDENTIALS."
    );
  }
  admin.initializeApp({credential: admin.credential.applicationDefault(), projectId});
}

type GoogleVolume = {
  id: string;
  volumeInfo?: {
    title?: string;
    authors?: string[];
    description?: string;
    categories?: string[];
    imageLinks?: {thumbnail?: string; smallThumbnail?: string};
    language?: string;
    previewLink?: string;
    infoLink?: string;
    maturityRating?: string;
    pageCount?: number;
  };
};

async function fetchVolumesForTopic(
  topic: TopicSpec,
  apiKey: string,
  perTopic: number
): Promise<GoogleVolume[]> {
  // Query: keyword + "children" to bias towards kid-appropriate results, without
  // over-constraining to the narrow BISAC "juvenile" subject. Mature content is
  // filtered client-side via maturityRating below.
  const q = encodeURIComponent(`${topic.keyword} children`);
  const url =
    `https://www.googleapis.com/books/v1/volumes?q=${q}` +
    `&maxResults=${perTopic}&printType=books&langRestrict=en` +
    `&orderBy=relevance&key=${apiKey}`;
  const resp = await fetch(url);
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`Google Books API error for "${topic.keyword}": ${resp.status} ${text}`);
  }
  const body = (await resp.json()) as {items?: GoogleVolume[]};
  return body.items ?? [];
}

function volumeToBook(vol: GoogleVolume, topic: TopicSpec): Record<string, unknown> | null {
  const info = vol.volumeInfo;
  if (!info || !info.title) return null;
  if (info.maturityRating && info.maturityRating !== "NOT_MATURE") return null;

  const cover = info.imageLinks?.thumbnail || info.imageLinks?.smallThumbnail || "";
  // Google thumbnails come over http; upgrade to https so they load inside the app.
  const coverHttps = cover.startsWith("http://") ? cover.replace("http://", "https://") : cover;

  const readerUrl = info.previewLink || info.infoLink || "";

  const difficulty =
    topic.ageMin >= 8 ? "medium" : topic.ageMin >= 6 ? "easy" : "easy";

  const bookTags = Array.from(
    new Set([
      ...topic.tags,
      topic.keyword.toLowerCase(),
      ...(info.categories ?? []).map((c) => c.toLowerCase()),
    ])
  );

  return {
    id: `gb_${vol.id}`,
    title: info.title,
    author: (info.authors ?? []).join(", "),
    ageMin: topic.ageMin,
    ageMax: topic.ageMax,
    category: topic.category,
    source: "GoogleBooks",
    language: info.language || "English",
    description: (info.description ?? "").slice(0, 2000),
    tags: bookTags,
    isKidSafe: true,
    canPlayInApp: false,
    difficulty,
    difficultyLevel: difficulty,
    bookUrl: readerUrl,
    readerUrl,
    url: readerUrl,
    coverUrl: coverHttps,
    imageUrl: coverHttps,
    videoUrl: "",
    youtubeUrl: "",
    isVideo: false,
    type: "book",
    createdAt: admin.firestore.Timestamp.now(),
  };
}

async function main(): Promise<void> {
  loadDotEnv();
  const {apiKey, credentials, perTopic, dryRun, topicsOverride} = parseArgs(process.argv);

  const key = apiKey || process.env.GOOGLE_BOOKS_API_KEY;
  if (!key) {
    console.error("Missing API key. Pass --api-key=... or set GOOGLE_BOOKS_API_KEY.");
    process.exit(1);
  }

  initAdmin(credentials);
  const db = admin.firestore();

  const topics = topicsOverride
    ? DEFAULT_TOPICS.filter((t) => topicsOverride.includes(t.keyword))
    : DEFAULT_TOPICS;

  console.log(`Importing up to ${perTopic} books each for ${topics.length} topics…`);

  let fetched = 0;
  let written = 0;
  let skipped = 0;
  const writer = db.batch();
  let pending = 0;
  const flush = async () => {
    if (pending === 0) return;
    await writer.commit();
    pending = 0;
  };
  let currentBatch = db.batch();

  for (const topic of topics) {
    try {
      const volumes = await fetchVolumesForTopic(topic, key, perTopic);
      fetched += volumes.length;
      let addedForTopic = 0;
      for (const vol of volumes) {
        const book = volumeToBook(vol, topic);
        if (!book) {
          skipped += 1;
          continue;
        }
        if (dryRun) {
          written += 1;
          addedForTopic += 1;
          continue;
        }
        currentBatch.set(db.collection("content_books").doc(book.id as string), book);
        pending += 1;
        written += 1;
        addedForTopic += 1;
        if (pending >= 400) {
          await currentBatch.commit();
          currentBatch = db.batch();
          pending = 0;
        }
      }
      console.log(`  ${topic.keyword.padEnd(12)} fetched=${volumes.length} added=${addedForTopic}`);
      // Light pacing to stay well under the Google Books per-minute quota.
      await new Promise((r) => setTimeout(r, 150));
    } catch (err) {
      console.error(`  ${topic.keyword} — failed: ${(err as Error).message}`);
    }
  }

  if (!dryRun && pending > 0) await currentBatch.commit();
  await flush();

  console.log(
    `\nDone. ${dryRun ? "(DRY RUN) " : ""}fetched=${fetched}  written=${written}  skipped=${skipped}`
  );
  console.log(
    "Note: re-run scripts/seedSyntheticUsers.ts afterwards so synthetic users get exposure to the new books."
  );
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
