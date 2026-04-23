// Seeds synthetic users + interactions so item-based collaborative filtering has
// enough co-occurrence signal to produce recommendations on a cold dataset.
//
// Run with (recommended, explicit service account path):
//   cd firebase/functions
//   npm run seed:synthetic -- --credentials="C:\path\to\serviceAccount.json" --users=120
//
// Or, if GOOGLE_APPLICATION_CREDENTIALS is already exported:
//   npm run seed:synthetic -- --users=120
//
// Get a service-account JSON from Firebase Console →
//   Project Settings → Service accounts → Generate new private key.
//
// Every doc written here is tagged with isSynthetic: true and every userId is
// prefixed with "synthetic_" so admin UI / analytics can filter them out.
// Re-runs are idempotent per user id (existing docs are overwritten, not duplicated).

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

type Persona = {
  key: string;
  name: string;
  ageRange: [number, number];
  interests: string[];
  // Lowercased keyword affinity against book.category + book.tags
  affinity: string[];
  readingLevel: "Beginner" | "Intermediate" | "Advanced";
};

const PERSONAS: Persona[] = [
  {
    key: "dino",
    name: "Dino Fan",
    ageRange: [4, 7],
    interests: ["dinosaurs", "history"],
    affinity: ["dinosaur", "dino", "prehistoric", "fossil", "jurassic"],
    readingLevel: "Beginner",
  },
  {
    key: "space",
    name: "Space Explorer",
    ageRange: [6, 10],
    interests: ["space", "science"],
    affinity: ["space", "planet", "rocket", "astronaut", "star", "galaxy", "moon"],
    readingLevel: "Intermediate",
  },
  {
    key: "animals",
    name: "Animal Lover",
    ageRange: [3, 8],
    interests: ["animals", "nature"],
    affinity: ["animal", "zoo", "wild", "dog", "cat", "bird", "ocean", "jungle"],
    readingLevel: "Beginner",
  },
  {
    key: "fairy",
    name: "Fairy Tale Reader",
    ageRange: [4, 8],
    interests: ["fairy tales", "stories"],
    affinity: ["fairy", "princess", "magic", "fantasy", "dragon", "castle"],
    readingLevel: "Beginner",
  },
  {
    key: "stem",
    name: "STEM Kid",
    ageRange: [7, 12],
    interests: ["science", "math", "coding"],
    affinity: ["science", "math", "experiment", "coding", "robot", "engineer", "stem"],
    readingLevel: "Advanced",
  },
  {
    key: "adventure",
    name: "Adventurer",
    ageRange: [6, 11],
    interests: ["adventure", "mystery"],
    affinity: ["adventure", "mystery", "explore", "pirate", "treasure", "quest"],
    readingLevel: "Intermediate",
  },
  {
    key: "art",
    name: "Arts & Music",
    ageRange: [5, 10],
    interests: ["art", "music"],
    affinity: ["art", "music", "draw", "paint", "song", "dance", "craft"],
    readingLevel: "Intermediate",
  },
  {
    key: "sports",
    name: "Sports Fan",
    ageRange: [7, 12],
    interests: ["sports"],
    affinity: ["sport", "ball", "team", "soccer", "football", "basketball", "game"],
    readingLevel: "Intermediate",
  },
];

type Book = {
  id: string;
  title: string;
  category: string;
  tags: string[];
  ageMin: number;
  ageMax: number;
  isKidSafe: boolean;
  isVideo: boolean;
};

function parseArgs(argv: string[]): {
  totalUsers: number;
  dryRun: boolean;
  credentials?: string;
} {
  let totalUsers = 120;
  let dryRun = false;
  let credentials: string | undefined;
  for (const arg of argv.slice(2)) {
    const [key, value] = arg.split("=");
    if (key === "--users" && value) totalUsers = parseInt(value, 10);
    if (key === "--dry-run") dryRun = true;
    if (key === "--credentials" && value) credentials = value;
  }
  return { totalUsers, dryRun, credentials };
}

function readFirebaseProjectId(): string | undefined {
  // Project lives at repoRoot/firebase/.firebaserc. Script runs from firebase/functions,
  // so climb one level to find it.
  const candidates = [
    path.resolve(process.cwd(), "..", ".firebaserc"),
    path.resolve(process.cwd(), ".firebaserc"),
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) {
      try {
        const rc = JSON.parse(fs.readFileSync(p, "utf8")) as {
          projects?: { default?: string };
        };
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
    if (!fs.existsSync(absPath)) {
      throw new Error(`Service account file not found: ${absPath}`);
    }
    const serviceAccount = JSON.parse(fs.readFileSync(absPath, "utf8"));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id || projectId,
    });
    return;
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error(
      "No credentials. Either pass --credentials=path\\to\\serviceAccount.json " +
        "or set GOOGLE_APPLICATION_CREDENTIALS. Download a key from " +
        "Firebase Console → Project Settings → Service accounts."
    );
  }
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId,
  });
}

function rng(seed: string): () => number {
  // Tiny deterministic PRNG (mulberry32) so re-runs with the same user id produce
  // stable interactions. Keeps the user-item matrix consistent across seedings.
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) | 0;
  let a = h >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function scoreItemForPersona(book: Book, persona: Persona): number {
  const haystack = [
    book.category.toLowerCase(),
    book.title.toLowerCase(),
    ...book.tags.map((t) => t.toLowerCase()),
  ].join(" ");
  let score = 0;
  for (const kw of persona.affinity) {
    if (haystack.includes(kw)) score += 1;
  }
  // Prefer age overlap
  const [pMin, pMax] = persona.ageRange;
  const overlaps = book.ageMin <= pMax && book.ageMax >= pMin;
  if (overlaps) score += 0.5;
  return score;
}

async function loadBooks(db: admin.firestore.Firestore): Promise<Book[]> {
  const snap = await db.collection("content_books").get();
  return snap.docs
    .map((d) => {
      const raw = d.data() as Record<string, unknown>;
      return {
        id: d.id,
        title: String(raw.title ?? ""),
        category: String(raw.category ?? ""),
        tags: Array.isArray(raw.tags) ? (raw.tags as string[]) : [],
        ageMin: Number(raw.ageMin ?? 0),
        ageMax: Number(raw.ageMax ?? 15),
        isKidSafe: raw.isKidSafe !== false,
        isVideo:
          raw.isVideo === true ||
          String(raw.type ?? "").toLowerCase() === "video",
      };
    })
    .filter((b) => b.isKidSafe);
}

function pickItemsForPersona(
  books: Book[],
  persona: Persona,
  rand: () => number
): Book[] {
  const scored = books
    .map((b) => ({ book: b, score: scoreItemForPersona(b, persona) }))
    .filter((s) => s.score > 0)
    .sort((a, b) => b.score - a.score);

  // Take the top 40 that match this persona, then sample from them so two
  // synthetic users of the same persona don't end up with identical favorites.
  const pool = scored.slice(0, 40).map((s) => s.book);

  // Fallback: if affinity matches nothing, fall back to age-appropriate books
  if (pool.length === 0) {
    return books
      .filter(
        (b) => b.ageMin <= persona.ageRange[1] && b.ageMax >= persona.ageRange[0]
      )
      .slice(0, 20);
  }
  return pool.sort(() => rand() - 0.5);
}

async function seedOneUser(
  db: admin.firestore.Firestore,
  userId: string,
  persona: Persona,
  pickedPool: Book[],
  rand: () => number,
  dryRun: boolean
): Promise<{ favorites: number; history: number }> {
  const favCount = 5 + Math.floor(rand() * 11); // 5..15
  const historyCount = 3 + Math.floor(rand() * 8); // 3..10

  const favorites = pickedPool.slice(0, favCount);
  // Reading history overlaps with favorites (simulates re-reading) plus some new items
  const historyPool = pickedPool.slice(0, Math.min(pickedPool.length, favCount + historyCount));
  const history = historyPool
    .slice()
    .sort(() => rand() - 0.5)
    .slice(0, historyCount);

  const age =
    persona.ageRange[0] +
    Math.floor(rand() * (persona.ageRange[1] - persona.ageRange[0] + 1));

  const userDoc = {
    id: userId,
    name: `${persona.name} #${userId.split("_").pop()}`,
    email: `${userId}@synthetic.local`,
    age,
    interests: persona.interests,
    readingLevel: persona.readingLevel,
    planType: "FREE",
    accountType: "CHILD",
    status: "ACTIVE",
    isGuest: false,
    isSynthetic: true,
    syntheticPersona: persona.key,
    createdAt: admin.firestore.Timestamp.now(),
  };

  if (dryRun) {
    return { favorites: favorites.length, history: history.length };
  }

  const batch = db.batch();
  batch.set(db.collection("users").doc(userId), userDoc);

  batch.set(
    db.collection("favorites").doc(userId),
    { userId, isSynthetic: true, updatedAt: admin.firestore.Timestamp.now() },
    { merge: true }
  );
  for (const book of favorites) {
    batch.set(
      db
        .collection("favorites")
        .doc(userId)
        .collection("items")
        .doc(book.id),
      {
        id: book.id,
        userId,
        itemId: book.id,
        type: book.isVideo ? "VIDEO" : "BOOK",
        title: book.title,
        description: "",
        imageUrl: "",
        url: "",
        addedAt: admin.firestore.Timestamp.now(),
        isSynthetic: true,
      }
    );
  }

  batch.set(
    db.collection("readingHistory").doc(userId),
    { userId, isSynthetic: true, updatedAt: admin.firestore.Timestamp.now() },
    { merge: true }
  );
  // NB: CollaborativeFilteringService reads readingHistory/{uid}/items/{itemId}
  // (see CollaborativeFilteringService.kt line 121). The app's ReadingHistoryManager
  // writes to .../sessions/ — that is a pre-existing mismatch. Seeding into /items/
  // matches what CF actually reads.
  for (const book of history) {
    const completed = rand() > 0.35;
    batch.set(
      db
        .collection("readingHistory")
        .doc(userId)
        .collection("items")
        .doc(book.id),
      {
        itemId: book.id,
        title: book.title,
        completed,
        isVideo: book.isVideo,
        openedAt: admin.firestore.Timestamp.now(),
        isSynthetic: true,
      }
    );
  }

  await batch.commit();
  return { favorites: favorites.length, history: history.length };
}

async function main(): Promise<void> {
  const { totalUsers, dryRun, credentials } = parseArgs(process.argv);
  initAdmin(credentials);
  const db = admin.firestore();

  console.log(`Loading books from content_books...`);
  const books = await loadBooks(db);
  console.log(`Loaded ${books.length} kid-safe items.`);
  if (books.length < 10) {
    console.error(
      "Refusing to seed: fewer than 10 items in content_books — CF has nothing meaningful to correlate."
    );
    process.exit(1);
  }

  const usersPerPersona = Math.ceil(totalUsers / PERSONAS.length);
  let created = 0;
  let totalFavs = 0;
  let totalHistory = 0;

  for (const persona of PERSONAS) {
    const matched = books.filter((b) => scoreItemForPersona(b, persona) > 0).length;
    console.log(
      `Persona "${persona.key}" — matched ${matched} items, seeding ${usersPerPersona} users`
    );
    for (let i = 0; i < usersPerPersona && created < totalUsers; i++) {
      const userId = `synthetic_${persona.key}_${String(i + 1).padStart(3, "0")}`;
      const rand = rng(userId);
      const pool = pickItemsForPersona(books, persona, rand);
      if (pool.length === 0) {
        console.warn(`  Skipping ${userId} — no eligible items`);
        continue;
      }
      const result = await seedOneUser(db, userId, persona, pool, rand, dryRun);
      created += 1;
      totalFavs += result.favorites;
      totalHistory += result.history;
    }
  }

  console.log(
    `\nDone. ${dryRun ? "(DRY RUN) " : ""}Seeded ${created} synthetic users, ` +
      `${totalFavs} favorites, ${totalHistory} reading-history entries.`
  );
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
