// Little Dino — Collaborative Filtering FYP Defense Deck
// Run: node build.js
const pptxgen = require("pptxgenjs");
const path = require("path");

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13.3" x 7.5" — more room for code excerpts
pres.title = "Hybrid Collaborative Filtering — Little Dino";
pres.author = "Felix Eng Cheng Kang";

// Ocean Gradient palette — academic / serious
const COLORS = {
  navy: "21295C",      // primary dark
  deep: "065A82",      // accent dark blue
  teal: "1C7293",      // secondary
  light: "F4F7FA",     // bg
  white: "FFFFFF",
  text: "1F2937",      // body text
  textMute: "64748B",  // captions
  amber: "F59E0B",     // highlight (sparingly)
  green: "10B981",     // positive
};

const FONT_HEAD = "Cambria";
const FONT_BODY = "Calibri";
const FONT_CODE = "Consolas";

// helper: header bar consistent across content slides
function addHeader(slide, title, sub) {
  slide.background = { color: COLORS.light };
  // Left thick accent bar (motif)
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.18, h: 7.5,
    fill: { color: COLORS.deep }, line: { color: COLORS.deep, width: 0 }
  });
  slide.addText(title, {
    x: 0.5, y: 0.35, w: 12.5, h: 0.65,
    fontSize: 32, bold: true, color: COLORS.navy, fontFace: FONT_HEAD,
    margin: 0
  });
  if (sub) {
    slide.addText(sub, {
      x: 0.5, y: 1.0, w: 12.5, h: 0.4,
      fontSize: 14, italic: true, color: COLORS.textMute, fontFace: FONT_BODY,
      margin: 0
    });
  }
  // Footer page-marker
  slide.addText("Little Dino · FYP26S123", {
    x: 0.5, y: 7.1, w: 6, h: 0.3,
    fontSize: 9, color: COLORS.textMute, fontFace: FONT_BODY
  });
}

function addCodeBlock(slide, code, opts) {
  const { x, y, w, h, fontSize = 10 } = opts;
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h,
    fill: { color: COLORS.navy },
    line: { color: COLORS.navy, width: 0 }
  });
  slide.addText(code, {
    x: x + 0.15, y: y + 0.1, w: w - 0.3, h: h - 0.2,
    fontSize, fontFace: FONT_CODE, color: "E2E8F0",
    valign: "top", align: "left", margin: 0
  });
}

// ============================================================
// SLIDE 1 — Title
// ============================================================
{
  const s = pres.addSlide();
  s.background = { color: COLORS.navy };

  // Decorative band
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 4.5, w: 13.3, h: 0.06,
    fill: { color: COLORS.teal }, line: { color: COLORS.teal, width: 0 }
  });

  s.addText("Hybrid Collaborative Filtering", {
    x: 0.8, y: 1.6, w: 11.7, h: 1.0,
    fontSize: 54, bold: true, color: COLORS.white, fontFace: FONT_HEAD,
    margin: 0
  });
  s.addText("for Kid-Safe Book and Video Recommendations", {
    x: 0.8, y: 2.7, w: 11.7, h: 0.7,
    fontSize: 26, color: COLORS.white, fontFace: FONT_HEAD, italic: true,
    margin: 0
  });
  s.addText("Little Dino · Chatbot App for Book and Video Recommendations for Kids", {
    x: 0.8, y: 3.7, w: 11.7, h: 0.5,
    fontSize: 14, color: "CADCFC", fontFace: FONT_BODY,
    margin: 0
  });

  s.addText("Felix Eng Cheng Kang", {
    x: 0.8, y: 5.0, w: 11.7, h: 0.5,
    fontSize: 18, color: COLORS.white, fontFace: FONT_BODY, bold: true,
    margin: 0
  });
  s.addText("Final Year Project · FYP26S123", {
    x: 0.8, y: 5.5, w: 11.7, h: 0.4,
    fontSize: 13, color: "9DB4E0", fontFace: FONT_BODY,
    margin: 0
  });

  s.addNotes(
    "Open with the project context. Little Dino is an Android chatbot for kids that recommends safe books and videos. " +
    "This presentation focuses on the Collaborative Filtering recommendation engine — the core algorithmic contribution of the project. " +
    "I'll cover the problem framing, why I chose a hybrid approach, the implementation in detail (signal capture, similarity computation, score blending), and engineering decisions such as parallel data fetching."
  );
}

// ============================================================
// SLIDE 2 — Project Context & The Problem
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Why We Need Recommendations", "Three constraints unique to children's content discovery");

  const cards = [
    {
      title: "Cold start",
      body: "A 6-year-old typing 'something fun' produces almost no signal. We need to suggest items before the child knows what to ask for.",
      x: 0.6
    },
    {
      title: "Content overload",
      body: "Open Library + Gutendex + curated catalogue exposes thousands of titles. Without ranking, the chat returns the same generic 'popular' list.",
      x: 4.85
    },
    {
      title: "Safety first",
      body: "Every recommendation must be age-appropriate and kid-safe. Filtering must happen before scoring, not after — wrong content can never be visible.",
      x: 9.1
    }
  ];

  for (const card of cards) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: card.x, y: 1.7, w: 3.7, h: 4.6,
      fill: { color: COLORS.white },
      line: { color: "E2E8F0", width: 1 },
      shadow: { type: "outer", color: "000000", blur: 8, offset: 2, angle: 90, opacity: 0.08 }
    });
    s.addShape(pres.shapes.RECTANGLE, {
      x: card.x, y: 1.7, w: 3.7, h: 0.1,
      fill: { color: COLORS.deep }, line: { color: COLORS.deep, width: 0 }
    });
    s.addText(card.title, {
      x: card.x + 0.3, y: 2.0, w: 3.1, h: 0.6,
      fontSize: 22, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
    });
    s.addText(card.body, {
      x: card.x + 0.3, y: 2.7, w: 3.1, h: 3.4,
      fontSize: 13, color: COLORS.text, fontFace: FONT_BODY, valign: "top", margin: 0
    });
  }

  s.addNotes(
    "Three constraints drive the recommendation design. (1) Cold start — kids in our target age range can't formulate detailed queries. We need to populate the chat welcome screen with relevant items even when the child has typed nothing. " +
    "(2) Content overload — the app aggregates from multiple book sources plus a curated library, easily thousands of items. Without a ranker, every child gets the same boring popular list. " +
    "(3) Safety — every recommendation must be appropriate. The filter happens before scoring; we never score then hope to filter, because a single inappropriate impression is too costly."
  );
}

// ============================================================
// SLIDE 3 — Approach Positioning
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Choosing the Right Approach", "Hybrid CF vs alternatives");

  const cols = [
    {
      title: "Content-Based",
      pros: "Uses item metadata (tags, age, category). Works on day one with no users.",
      cons: "Tunnel vision — only recommends items similar to what the child already saw. No serendipity.",
      pick: false,
      x: 0.6
    },
    {
      title: "User-Based CF",
      pros: "Surfaces items popular among similar children — strong serendipity signal.",
      cons: "Needs a user community. Falls apart on Day 1. Vulnerable to noisy users.",
      pick: false,
      x: 4.85
    },
    {
      title: "Hybrid CF (chosen)",
      pros: "Combines user-based + item-based CF with cosine similarity, weighted 50/50.",
      cons: "Slightly more compute; both algorithms run per request. Mitigated by filtering and limits.",
      pick: true,
      x: 9.1
    }
  ];

  for (const col of cols) {
    const isPick = col.pick;
    s.addShape(pres.shapes.RECTANGLE, {
      x: col.x, y: 1.65, w: 3.7, h: 4.85,
      fill: { color: isPick ? COLORS.deep : COLORS.white },
      line: { color: isPick ? COLORS.deep : "E2E8F0", width: 1 },
      shadow: { type: "outer", color: "000000", blur: 10, offset: 3, angle: 90, opacity: isPick ? 0.18 : 0.06 }
    });
    s.addText(col.title, {
      x: col.x + 0.3, y: 1.85, w: 3.1, h: 0.6,
      fontSize: 20, bold: true,
      color: isPick ? COLORS.white : COLORS.navy,
      fontFace: FONT_HEAD, margin: 0
    });
    if (isPick) {
      s.addText("CHOSEN", {
        x: col.x + 0.3, y: 2.45, w: 3.1, h: 0.3,
        fontSize: 11, bold: true, color: "F9E795", fontFace: FONT_BODY, charSpacing: 4, margin: 0
      });
    }
    s.addText("Pros", {
      x: col.x + 0.3, y: 2.85, w: 3.1, h: 0.35,
      fontSize: 12, bold: true,
      color: isPick ? "9DECFF" : COLORS.green,
      fontFace: FONT_BODY, margin: 0
    });
    s.addText(col.pros, {
      x: col.x + 0.3, y: 3.2, w: 3.1, h: 1.4,
      fontSize: 12,
      color: isPick ? COLORS.white : COLORS.text,
      fontFace: FONT_BODY, margin: 0, valign: "top"
    });
    s.addText("Cons", {
      x: col.x + 0.3, y: 4.65, w: 3.1, h: 0.35,
      fontSize: 12, bold: true,
      color: isPick ? "FCB9B9" : "EF4444",
      fontFace: FONT_BODY, margin: 0
    });
    s.addText(col.cons, {
      x: col.x + 0.3, y: 5.0, w: 3.1, h: 1.4,
      fontSize: 12,
      color: isPick ? COLORS.white : COLORS.text,
      fontFace: FONT_BODY, margin: 0, valign: "top"
    });
  }

  s.addNotes(
    "I evaluated three approaches. Pure content-based filtering recommends only items similar to ones the child has already engaged with — this gives no diversity, the child stays in a bubble. " +
    "Pure user-based CF gives strong serendipity but cold-starts badly: with zero users, the app would have nothing to recommend. " +
    "I chose hybrid CF — a 50/50 blend of user-based and item-based scores. User-based brings 'children like you also liked X'; item-based brings 'this is similar to what you've already enjoyed'. They cover each other's blind spots."
  );
}

// ============================================================
// SLIDE 4 — Architecture Overview
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Where CF Sits in the App", "Data flow from interactions to UI");

  // Boxes
  const boxes = [
    { x: 0.7, y: 2.0, w: 2.3, h: 1.0, title: "User Actions", sub: "favorite · read · watch", fill: COLORS.teal, white: true },
    { x: 0.7, y: 3.4, w: 2.3, h: 1.0, title: "Firestore", sub: "favorites/{u}/items\nreadingHistory/{u}/items", fill: COLORS.white },
    { x: 4.0, y: 2.7, w: 3.2, h: 1.7, title: "CollaborativeFilteringService", sub: "loadAllInteractions()\nbuildUserItemMatrix()\nUserBased + ItemBased\nHybrid blend (0.5/0.5)", fill: COLORS.deep, white: true },
    { x: 8.4, y: 2.0, w: 2.3, h: 1.0, title: "ChatViewModel", sub: "loadRecommendations()", fill: COLORS.white },
    { x: 8.4, y: 3.4, w: 2.3, h: 1.0, title: "Welcome Screen", sub: "\"Users like you...\"", fill: COLORS.white },
    { x: 11.2, y: 2.7, w: 1.7, h: 1.7, title: "Top-N", sub: "limit = 8\n(per child)", fill: COLORS.amber, white: true }
  ];

  for (const b of boxes) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: b.x, y: b.y, w: b.w, h: b.h,
      fill: { color: b.fill },
      line: { color: b.fill === COLORS.white ? "CBD5E1" : b.fill, width: 1 },
      shadow: { type: "outer", color: "000000", blur: 6, offset: 2, angle: 90, opacity: 0.10 }
    });
    s.addText(b.title, {
      x: b.x + 0.15, y: b.y + 0.1, w: b.w - 0.3, h: 0.4,
      fontSize: 12, bold: true,
      color: b.white ? COLORS.white : COLORS.navy,
      fontFace: FONT_BODY, margin: 0, align: "center"
    });
    s.addText(b.sub, {
      x: b.x + 0.15, y: b.y + 0.5, w: b.w - 0.3, h: b.h - 0.55,
      fontSize: 10,
      color: b.white ? "DBEAFE" : COLORS.textMute,
      fontFace: b.title === "Firestore" ? FONT_CODE : FONT_BODY,
      margin: 0, align: "center", valign: "top"
    });
  }

  // Arrows (lines with colored end accents)
  const arrows = [
    { x1: 1.85, y1: 3.4, x2: 1.85, y2: 3.0 }, // user actions -> firestore
    { x1: 3.0, y1: 3.55, x2: 4.0, y2: 3.55 }, // firestore -> CF service
    { x1: 7.2, y1: 3.55, x2: 8.4, y2: 2.6 },  // CF service -> ViewModel
    { x1: 9.55, y1: 3.0, x2: 9.55, y2: 3.4 }, // ViewModel -> welcome
    { x1: 7.2, y1: 3.55, x2: 11.2, y2: 3.55 }, // CF -> Top-N
  ];
  for (const a of arrows) {
    s.addShape(pres.shapes.LINE, {
      x: a.x1, y: a.y1, w: a.x2 - a.x1, h: a.y2 - a.y1,
      line: { color: COLORS.textMute, width: 2, endArrowType: "triangle" }
    });
  }

  s.addText("Compute on demand: every chat-screen open re-runs the algorithm with fresh interaction data. No batch jobs, no precomputed tables.", {
    x: 0.7, y: 5.5, w: 12.0, h: 1.2,
    fontSize: 14, italic: true, color: COLORS.textMute, fontFace: FONT_BODY,
    align: "center", valign: "middle"
  });

  s.addNotes(
    "The data flow: user actions (tapping favorite, reading a book, watching a video) write into two Firestore subcollections — favorites and readingHistory. " +
    "When the chat opens, ChatViewModel.loadRecommendations() asks the CF service to compute fresh recommendations. " +
    "The CF service reads ALL interactions (not just the target user's), builds a user-item matrix in memory, runs both scoring algorithms, blends them, filters for kid-safety and age, and returns the top 8. " +
    "Crucially, this is computed live every time — there's no batch precomputation, which keeps the system simple at the cost of higher per-request work. We discuss the scaling implications later."
  );
}

// ============================================================
// SLIDE 5 — Signal Capture & Weighting
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Signal Capture", "How user actions translate to interaction weights");

  const code = `// loadAllInteractions() — favorites
UserInteraction(userId, itemId, weight = 3.0)

// loadAllInteractions() — readingHistory
val weight = when {
    completed && isVideo -> 2.5
    completed            -> 2.5
    else                 -> 1.5     // partial / in-progress
}

// Same user/item pair from multiple sources -> weights summed
mergeDuplicateInteractions(...) // groupBy "userId::itemId"`;

  addCodeBlock(s, code, { x: 0.6, y: 1.8, w: 6.5, h: 3.4, fontSize: 12 });

  // Weight table on right
  s.addText("Weight design rationale", {
    x: 7.4, y: 1.8, w: 5.5, h: 0.4,
    fontSize: 16, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
  });

  const rows = [
    ["3.0", "Favorite", "Explicit, deliberate signal"],
    ["2.5", "Completed read/watch", "Strong implicit endorsement"],
    ["1.5", "Started but not finished", "Mild interest, also useful"]
  ];
  let yOff = 2.4;
  for (const r of rows) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: 7.4, y: yOff, w: 5.5, h: 0.85,
      fill: { color: COLORS.white },
      line: { color: "E2E8F0", width: 1 }
    });
    s.addText(r[0], {
      x: 7.4, y: yOff, w: 0.9, h: 0.85,
      fontSize: 24, bold: true, color: COLORS.deep, fontFace: FONT_HEAD,
      align: "center", valign: "middle", margin: 0
    });
    s.addText(r[1], {
      x: 8.3, y: yOff + 0.1, w: 4.5, h: 0.35,
      fontSize: 13, bold: true, color: COLORS.navy, fontFace: FONT_BODY, margin: 0
    });
    s.addText(r[2], {
      x: 8.3, y: yOff + 0.45, w: 4.5, h: 0.35,
      fontSize: 11, color: COLORS.textMute, fontFace: FONT_BODY, margin: 0
    });
    yOff += 1.0;
  }

  s.addText(
    "Summing duplicates means a child who both favorited AND finished an item gets weight 5.5 — proportional to combined enthusiasm.",
    {
      x: 0.6, y: 5.6, w: 12.3, h: 0.9,
      fontSize: 13, italic: true, color: COLORS.textMute, fontFace: FONT_BODY,
      align: "center", margin: 0
    }
  );

  s.addNotes(
    "The CF system needs a 'rating'-like signal but children don't rate. So we infer ratings from behavior. " +
    "Three weight buckets: favorites are explicit and weighted heaviest at 3.0. Completed reads/watches imply the child stayed engaged — 2.5. Started-but-not-finished still carries useful information at 1.5. " +
    "If the same child both favorites AND finishes an item, the duplicate-merge step sums to 5.5, which I think is correct — combined behavior is stronger evidence than either alone. " +
    "These numbers were tuned empirically; the model isn't very sensitive because cosine similarity is scale-invariant within a user."
  );
}

// ============================================================
// SLIDE 6 — User-Item Matrix & Parallel Fetch
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Loading the Matrix in Parallel", "Avoiding O(N) sequential round-trips");

  // Diagram: serial vs parallel
  s.addText("Naive (sequential)", {
    x: 0.6, y: 1.7, w: 6.0, h: 0.4,
    fontSize: 16, bold: true, color: COLORS.text, fontFace: FONT_BODY
  });
  s.addText("Optimized (parallel async)", {
    x: 6.9, y: 1.7, w: 6.0, h: 0.4,
    fontSize: 16, bold: true, color: COLORS.deep, fontFace: FONT_BODY
  });

  // Sequential bars
  for (let i = 0; i < 8; i++) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: 0.6 + i * 0.65, y: 2.2, w: 0.5, h: 1.5,
      fill: { color: i === 0 ? "EF4444" : "FCA5A5" },
      line: { color: "FCA5A5", width: 0 }
    });
    s.addText(`U${i + 1}`, {
      x: 0.6 + i * 0.65, y: 3.7, w: 0.5, h: 0.3,
      fontSize: 9, color: COLORS.textMute, fontFace: FONT_CODE, align: "center", margin: 0
    });
  }
  s.addText("Total time = N × await", {
    x: 0.6, y: 4.2, w: 6.0, h: 0.4,
    fontSize: 12, italic: true, color: "DC2626", fontFace: FONT_BODY
  });

  // Parallel bars
  for (let i = 0; i < 8; i++) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: 6.9 + i * 0.18, y: 2.2, w: 0.14, h: 1.5,
      fill: { color: COLORS.green },
      line: { color: COLORS.green, width: 0 }
    });
  }
  s.addText("Total time ≈ max(await)", {
    x: 6.9, y: 4.2, w: 6.0, h: 0.4,
    fontSize: 12, italic: true, color: "059669", fontFace: FONT_BODY
  });

  const code = `// CollaborativeFilteringService.loadAllInteractions()
val favoritesPerUser = favoritesRoot.documents.map { userDoc ->
    async {                                          // launched concurrently
        db.collection("favorites").document(userId)
          .collection("items").get().await()
        ...
    }
}
val all = favoritesPerUser.awaitAll() + historyPerUser.awaitAll()`;

  addCodeBlock(s, code, { x: 0.6, y: 4.85, w: 12.3, h: 1.85, fontSize: 11 });

  s.addNotes(
    "A subtle but important engineering detail. The first version of loadAllInteractions awaited each user's items collection sequentially in a for loop. " +
    "With N users that's N round-trips end-to-end, perceptibly slow on the chat home screen — especially over mobile data. " +
    "The fix: launch all per-user fetches in parallel using async{}, then awaitAll() once at the end. Total wall-clock time becomes roughly the slowest single fetch, not the sum. " +
    "On Spark plan with no caching layer, this is the cheapest performance win available — pure client-side coroutine restructuring, no backend changes."
  );
}

// ============================================================
// SLIDE 7 — User-Based CF
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "User-Based CF", "Find similar users; recommend what they liked");

  // Formula
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 1.7, w: 12.3, h: 1.1,
    fill: { color: COLORS.white },
    line: { color: "E2E8F0", width: 1 }
  });
  s.addText("score(target, item) =  Σ  sim(target, otherUser) × rating(otherUser, item)\n                              ─────────────────────────────────────────\n                                              Σ sim(target, otherUser)", {
    x: 0.7, y: 1.75, w: 12.1, h: 1.0,
    fontSize: 14, color: COLORS.navy, fontFace: FONT_CODE, align: "center", valign: "middle", margin: 0
  });

  const code = `for ((otherUserId, otherVector) in userItemMatrix) {
    if (otherUserId == targetUserId) continue
    val similarity = cosineSimilarity(targetVector, otherVector)
    if (similarity <= 0.0) continue
    for ((itemId, rating) in otherVector) {
        if (itemId in seenItemIds) continue                         // never recommend already-seen
        scores[itemId] = (scores[itemId] ?: 0.0) + (similarity * rating)
        similaritySums[itemId] = (similaritySums[itemId] ?: 0.0) + similarity
    }
}
return scores.mapValues { (id, s) -> s / (similaritySums[id] ?: 1.0) }   // normalize`;

  addCodeBlock(s, code, { x: 0.6, y: 3.0, w: 8.0, h: 3.5, fontSize: 11 });

  // Sidebar — explanation
  s.addShape(pres.shapes.RECTANGLE, {
    x: 8.85, y: 3.0, w: 4.05, h: 3.5,
    fill: { color: COLORS.deep },
    line: { color: COLORS.deep, width: 0 }
  });
  s.addText("Key choices", {
    x: 9.05, y: 3.15, w: 3.7, h: 0.4,
    fontSize: 14, bold: true, color: "F9E795", fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "Skip self in the loop — comparing target to itself yields 1.0 and biases nothing useful.", options: { bullet: true, breakLine: true } },
    { text: "Skip non-positive cosine — saves work, removes noise from orthogonal users.", options: { bullet: true, breakLine: true } },
    { text: "Normalize by Σ sim — items rated by many similar users don't beat items rated by one extremely-similar user." , options: { bullet: true, breakLine: true } },
    { text: "seenItemIds early-exit — never recommend what the child has already engaged with." , options: { bullet: true } }
  ], {
    x: 9.05, y: 3.6, w: 3.7, h: 2.85,
    fontSize: 11, color: COLORS.white, fontFace: FONT_BODY,
    paraSpaceAfter: 8, valign: "top", margin: 0
  });

  s.addNotes(
    "User-based CF answers the question 'what do users like me enjoy?'. The formula is a weighted average: for every other user, compute similarity to the target, then accumulate that user's rating for each item, weighted by similarity. Then normalize by the sum of similarities so we don't unfairly favor popular items. " +
    "Walk through the code: the outer loop iterates over all other users; cosine similarity tells us how alike their interaction patterns are; for each item that other user has rated and the target hasn't seen, we add a weighted contribution. " +
    "Key choices on the right: skipping self (would always be perfectly similar but useless), skipping non-positive similarity (orthogonal users add noise), normalization (prevents popularity bias), and excluding items the child has already seen — that last one is a hard rule, never recommend something already engaged with."
  );
}

// ============================================================
// SLIDE 8 — Item-Based CF
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Item-Based CF", "Find items similar to what the child already likes");

  // Formula
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 1.7, w: 12.3, h: 1.1,
    fill: { color: COLORS.white },
    line: { color: "E2E8F0", width: 1 }
  });
  s.addText("score(target, candidate) =  Σ  sim(seenItem, candidate) × weight(target, seenItem)\n                                       ───────────────────────────────────────────\n                                                       Σ sim(seenItem, candidate)", {
    x: 0.7, y: 1.75, w: 12.1, h: 1.0,
    fontSize: 13, color: COLORS.navy, fontFace: FONT_CODE, align: "center", valign: "middle", margin: 0
  });

  const code = `// First: transpose the matrix so similarity is computed between ITEMS
val itemUserMatrix = buildItemUserMatrix(userItemMatrix)

for ((seenItemId, seenWeight) in targetVector) {                 // items the child liked
    val seenItemVector = itemUserMatrix[seenItemId] ?: continue
    for ((candidateItemId, candidateVector) in itemUserMatrix) { // every other item
        if (candidateItemId == seenItemId) continue
        if (candidateItemId in seenItemIds) continue
        val similarity = cosineSimilarity(seenItemVector, candidateVector)
        if (similarity <= 0.0) continue
        scores[candidateItemId] = (scores[candidateItemId] ?: 0.0) + (similarity * seenWeight)
        ...
    }
}`;

  addCodeBlock(s, code, { x: 0.6, y: 3.0, w: 8.0, h: 3.5, fontSize: 10 });

  // Why both
  s.addShape(pres.shapes.RECTANGLE, {
    x: 8.85, y: 3.0, w: 4.05, h: 3.5,
    fill: { color: COLORS.teal },
    line: { color: COLORS.teal, width: 0 }
  });
  s.addText("Why item-based ≠ user-based", {
    x: 9.05, y: 3.15, w: 3.7, h: 0.4,
    fontSize: 14, bold: true, color: "F9E795", fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "User-based: 'children like you also liked X'.", options: { breakLine: true, italic: true } },
    { text: "", options: { breakLine: true } },
    { text: "Item-based: 'this is similar to a book you already loved'.", options: { italic: true, breakLine: true } },
    { text: "", options: { breakLine: true } },
    { text: "Item similarity is more stable than user similarity because items don't change behavior — once two books co-occur in many favorite lists, they stay similar.", options: {} }
  ], {
    x: 9.05, y: 3.6, w: 3.7, h: 2.85,
    fontSize: 12, color: COLORS.white, fontFace: FONT_BODY,
    valign: "top", margin: 0
  });

  s.addNotes(
    "Item-based CF flips the perspective. Instead of finding similar users, we find similar items. The intuition: if many users who liked Book A also liked Book B, then A and B are 'similar' regardless of what specific users said. " +
    "Implementation requires transposing the user-item matrix into an item-user matrix. Then for each item the target child has already engaged with, we find candidate items with similar user-rating patterns, weight by the child's enthusiasm for the seen item, and sum. " +
    "Item-based is more stable than user-based: items don't change their interaction profile minute-to-minute. User behavior fluctuates; item co-occurrence accumulates monotonically. This is why most production recommender systems lean heavier on item-based — but for a small kid app I weight them equally to maintain serendipity."
  );
}

// ============================================================
// SLIDE 9 — Cosine Similarity
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Cosine Similarity", "Why angle, not magnitude");

  // Formula box
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 1.7, w: 7.2, h: 2.0,
    fill: { color: COLORS.navy },
    line: { color: COLORS.navy, width: 0 }
  });
  s.addText("cos(A, B)  =     A · B\n              ─────────────\n               ‖A‖ × ‖B‖", {
    x: 0.7, y: 1.85, w: 7.0, h: 1.7,
    fontSize: 22, color: COLORS.white, fontFace: FONT_CODE, align: "center", valign: "middle"
  });

  const code = `private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
    val commonKeys = a.keys.intersect(b.keys)
    if (commonKeys.isEmpty()) return 0.0
    val dot   = commonKeys.sumOf { key -> (a[key] ?: 0.0) * (b[key] ?: 0.0) }
    val normA = sqrt(a.values.sumOf { it * it })
    val normB = sqrt(b.values.sumOf { it * it })
    if (normA == 0.0 || normB == 0.0) return 0.0
    return dot / (normA * normB)
}`;

  addCodeBlock(s, code, { x: 0.6, y: 3.85, w: 7.2, h: 2.5, fontSize: 11 });

  // Why cosine sidebar
  s.addShape(pres.shapes.RECTANGLE, {
    x: 8.05, y: 1.7, w: 4.85, h: 4.65,
    fill: { color: COLORS.white },
    line: { color: "E2E8F0", width: 1 }
  });
  s.addText("Why cosine over Pearson or Euclidean?", {
    x: 8.25, y: 1.9, w: 4.45, h: 0.6,
    fontSize: 14, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "Sparse vectors", options: { bold: true, breakLine: true } },
    { text: "Most users interact with <1% of the catalogue. Cosine handles sparsity natively — only common keys contribute.", options: { breakLine: true } },
    { text: "", options: { breakLine: true } },
    { text: "Scale-invariant", options: { bold: true, breakLine: true } },
    { text: "A heavy reader with 100 favorites and a casual user with 5 can still be 'similar' if the items overlap proportionally.", options: { breakLine: true } },
    { text: "", options: { breakLine: true } },
    { text: "Bounded [0, 1] for our weights", options: { bold: true, breakLine: true } },
    { text: "All weights are non-negative, so cosine never goes negative — clean numerics, easy normalization.", options: {} }
  ], {
    x: 8.25, y: 2.55, w: 4.45, h: 3.6,
    fontSize: 11, color: COLORS.text, fontFace: FONT_BODY,
    valign: "top", margin: 0
  });

  s.addNotes(
    "Cosine similarity is the workhorse. The intuition: treat each user (or item) as a vector in interaction-space, where each dimension is one possible item. The cosine of the angle between two vectors tells you how aligned their preferences are, regardless of how many items each has rated. " +
    "Three reasons cosine over alternatives. (1) Sparsity: real user vectors are mostly zero — cosine only sums over keys that exist in both, no need to materialize full vectors. " +
    "(2) Scale-invariance: a child with 100 favorites and one with 5 favorites can be similar if their proportional preferences match. Euclidean distance would unfairly penalize the heavy user. " +
    "(3) All our weights are non-negative, so cosine stays in [0,1] — easy to compose with the normalization in the user-based formula."
  );
}

// ============================================================
// SLIDE 10 — Hybrid Score
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "The Hybrid Blend", "Combining both signals into one ranking");

  // Big stat
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 1.7, w: 12.3, h: 2.5,
    fill: { color: COLORS.deep },
    line: { color: COLORS.deep, width: 0 }
  });
  s.addText("finalScore  =  0.5 × userBasedScore  +  0.5 × itemBasedScore", {
    x: 0.7, y: 1.85, w: 12.1, h: 1.0,
    fontSize: 28, bold: true, color: COLORS.white, fontFace: FONT_HEAD,
    align: "center", valign: "middle", margin: 0
  });
  s.addText("equal weight — no a-priori reason to favor one over the other on a small dataset", {
    x: 0.7, y: 2.95, w: 12.1, h: 0.5,
    fontSize: 14, italic: true, color: "9DECFF", fontFace: FONT_BODY,
    align: "center", margin: 0
  });
  s.addText("if (finalScore <= 0.0) return null     // drops items with zero signal", {
    x: 0.7, y: 3.5, w: 12.1, h: 0.5,
    fontSize: 13, color: "F9E795", fontFace: FONT_CODE,
    align: "center", margin: 0
  });

  // Reason explainability
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 4.5, w: 12.3, h: 2.0,
    fill: { color: COLORS.white },
    line: { color: "E2E8F0", width: 1 }
  });
  s.addText("Explainability: every recommendation carries a reason", {
    x: 0.8, y: 4.65, w: 12.0, h: 0.45,
    fontSize: 14, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "userBasedScore > itemBasedScore  →  ", options: { bold: true } },
    { text: "\"similar users liked this\"", options: { italic: true, color: COLORS.deep, breakLine: true } },
    { text: "itemBasedScore > userBasedScore  →  ", options: { bold: true } },
    { text: "\"similar to items the child already liked\"", options: { italic: true, color: COLORS.deep, breakLine: true } },
    { text: "tie  →  ", options: { bold: true } },
    { text: "\"both similar users and similar items\"", options: { italic: true, color: COLORS.deep } },
  ], {
    x: 0.8, y: 5.15, w: 12.0, h: 1.3,
    fontSize: 13, color: COLORS.text, fontFace: FONT_CODE,
    valign: "top", paraSpaceAfter: 4, margin: 0
  });

  s.addNotes(
    "The blend is a simple linear combination, 50/50. There's a research literature on optimal hybrid weights — for our small dataset, equal weighting is well-justified: we have no a-priori reason to favor one signal. If the dataset grows we could tune this empirically with held-out validation. " +
    "Items with finalScore = 0 are dropped — these are items neither user-based nor item-based saw any signal for. No noise in the output. " +
    "Explainability matters for kids and parents. Each recommendation carries a reason string derived from which sub-score dominated. The UI surfaces this — 'recommended because similar users liked this video'. Not just transparency: it builds trust with parents who want to know WHY their child is being shown a particular item."
  );
}

// ============================================================
// SLIDE 11 — Filtering & Age Rules
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Safety & Age Filters", "Recommendations the child can actually consume");

  // Three filter cards
  const filters = [
    {
      title: "isKidSafe gate",
      body: "Every CFItem has an isKidSafe boolean. The first line of the algorithm filters the candidate pool — items can never reach the scoring loop if they aren't safe.",
      code: "allItems.filter { it.isKidSafe }"
    },
    {
      title: "Age band match",
      body: "ageMin ≤ childAge AND ageMax ≥ (childAge − AGE_FLOOR_SLACK).\n\nageMin is enforced strictly. ageMax is relaxed by 4 years so older kids still pull in middle-grade titles.",
      code: "AGE_FLOOR_SLACK = 4"
    },
    {
      title: "Already-seen exclusion",
      body: "Built into both scoring loops. A child never sees a recommendation for a book they already favorited or finished reading.",
      code: "if (itemId in seenItemIds) continue"
    }
  ];

  let xOff = 0.7;
  for (const f of filters) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: xOff, y: 1.7, w: 3.9, h: 4.8,
      fill: { color: COLORS.white },
      line: { color: "E2E8F0", width: 1 },
      shadow: { type: "outer", color: "000000", blur: 6, offset: 2, angle: 90, opacity: 0.08 }
    });
    s.addShape(pres.shapes.RECTANGLE, {
      x: xOff, y: 1.7, w: 0.08, h: 4.8,
      fill: { color: COLORS.deep }, line: { color: COLORS.deep, width: 0 }
    });
    s.addText(f.title, {
      x: xOff + 0.25, y: 1.9, w: 3.4, h: 0.6,
      fontSize: 18, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
    });
    s.addText(f.body, {
      x: xOff + 0.25, y: 2.55, w: 3.4, h: 2.7,
      fontSize: 12, color: COLORS.text, fontFace: FONT_BODY,
      valign: "top", margin: 0
    });
    addCodeBlock(s, f.code, { x: xOff + 0.25, y: 5.4, w: 3.4, h: 0.95, fontSize: 11 });
    xOff += 4.05;
  }

  s.addNotes(
    "Three layers of filtering, applied in order: kid-safety, age band, and seen-exclusion. " +
    "The kid-safety filter is enforced upstream — items missing isKidSafe never enter the candidate pool, so the algorithm cannot accidentally surface them. " +
    "The age band uses an asymmetric rule. The minimum age is strict — we never show content too mature for the child. But the maximum age is relaxed by AGE_FLOOR_SLACK, set to 4 years. The reason: the curated library skews young (lots of ageMax ≤ 12), so without the slack, a 15-year-old would have a tiny candidate pool. The slack lets older kids see middle-grade titles with strong signal. " +
    "Seen-exclusion is enforced inside both scoring loops, not after — saves wasted similarity computation on items we'd drop anyway."
  );
}

// ============================================================
// SLIDE 12 — Complexity & Scaling
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Complexity & Scalability", "What works at FYP scale; what needs to change");

  // Big-O table
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.6, y: 1.7, w: 6.3, h: 4.7,
    fill: { color: COLORS.white },
    line: { color: "E2E8F0", width: 1 }
  });
  s.addText("Time complexity per request", {
    x: 0.8, y: 1.85, w: 6.0, h: 0.4,
    fontSize: 14, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
  });

  const rows = [
    ["Load interactions", "O(U) parallel reads", "U = users with activity"],
    ["Build matrix", "O(I)", "I = total interactions"],
    ["User-based scoring", "O(U² × Ī)", "Ī = avg items per user"],
    ["Item-based scoring", "O(M² × Ū)", "M = unique items, Ū = avg users per item"],
    ["Sort + take(8)", "O(C log C)", "C = candidate items"]
  ];

  let yOff = 2.3;
  for (const r of rows) {
    s.addText(r[0], {
      x: 0.8, y: yOff, w: 2.0, h: 0.35,
      fontSize: 11, bold: true, color: COLORS.text, fontFace: FONT_BODY, margin: 0
    });
    s.addText(r[1], {
      x: 2.85, y: yOff, w: 1.6, h: 0.35,
      fontSize: 11, color: COLORS.deep, fontFace: FONT_CODE, margin: 0
    });
    s.addText(r[2], {
      x: 4.5, y: yOff, w: 2.3, h: 0.35,
      fontSize: 10, italic: true, color: COLORS.textMute, fontFace: FONT_BODY, margin: 0
    });
    yOff += 0.5;
  }

  s.addText(
    "User-based dominates. Quadratic in active users. " +
    "Acceptable up to a few thousand active children; beyond that, batch precomputation needed.",
    {
      x: 0.8, y: 5.5, w: 6.0, h: 0.7,
      fontSize: 11, italic: true, color: COLORS.text, fontFace: FONT_BODY,
      valign: "top", margin: 0
    }
  );

  // Scaling roadmap
  s.addShape(pres.shapes.RECTANGLE, {
    x: 7.1, y: 1.7, w: 5.8, h: 4.7,
    fill: { color: COLORS.deep },
    line: { color: COLORS.deep, width: 0 }
  });
  s.addText("Scaling roadmap", {
    x: 7.3, y: 1.85, w: 5.4, h: 0.4,
    fontSize: 14, bold: true, color: COLORS.white, fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "Phase 1 — current (FYP scale)", options: { bold: true, color: "F9E795", breakLine: true } },
    { text: "On-demand client-side compute. Parallel async fetches. Up to ~1000 active users.", options: { breakLine: true } },
    { text: "", options: { breakLine: true } },
    { text: "Phase 2 — Cloud Function batch", options: { bold: true, color: "F9E795", breakLine: true } },
    { text: "Nightly Cloud Function precomputes top-N per user, writes to a recommendations subcollection. Client reads cached. (Requires Blaze plan.)", options: { breakLine: true } },
    { text: "", options: { breakLine: true } },
    { text: "Phase 3 — matrix factorization", options: { bold: true, color: "F9E795", breakLine: true } },
    { text: "Replace cosine with SVD or ALS. Reduces dense matrix ops, enables embedding-space lookups. Worth it past ~10k users.", options: {} }
  ], {
    x: 7.3, y: 2.3, w: 5.4, h: 4.0,
    fontSize: 12, color: COLORS.white, fontFace: FONT_BODY,
    paraSpaceAfter: 6, valign: "top", margin: 0
  });

  s.addNotes(
    "Honest assessment: this implementation works at FYP scale and won't work at production scale. " +
    "The dominant term is U² × Ī from user-based scoring — quadratic in the number of active users, every time the chat opens. With a few thousand users this is fine on a modern phone; beyond that, latency becomes noticeable. " +
    "The roadmap is three phases. Phase 1 is what we have. Phase 2 moves the computation to a nightly Cloud Function that writes precomputed top-Ns into Firestore — the client just reads a tiny doc. The constraint was Spark plan: no Cloud Functions, so I couldn't ship Phase 2. Phase 3 would replace cosine with SVD or ALS-based matrix factorization, learning low-dimensional user/item embeddings — standard production-grade approach. " +
    "I want to be transparent that I prioritized algorithmic correctness and explainability over scale. The codebase is structured so any phase swap-in is a single class replacement."
  );
}

// ============================================================
// SLIDE 13 — Demo (screenshot)
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Live Demo", "Welcome screen recommendations in production");

  // Try to pull a real screenshot
  const screenshot = path.resolve("../../play-store/phone_screenshot_1.png");
  s.addImage({
    path: screenshot,
    x: 0.6, y: 1.65, w: 3.2, h: 5.8
  });

  // Right side — annotated explanation
  s.addText("What you're seeing", {
    x: 4.1, y: 1.7, w: 8.8, h: 0.45,
    fontSize: 18, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "Welcome screen — auto-loaded on chat open", options: { bullet: true, breakLine: true } },
    { text: "Title: \"Users like you have…\" (CF brand)", options: { bullet: true, breakLine: true } },
    { text: "Each card: title + age band + match %", options: { bullet: true, breakLine: true } },
    { text: "Match % = finalScore × 100, rounded — visible explainability", options: { bullet: true, breakLine: true } },
    { text: "Tap → opens reader / video", options: { bullet: true, breakLine: true } },
    { text: "Mix enforced: at least 1 BOOK and 1 VIDEO when both available", options: { bullet: true } }
  ], {
    x: 4.1, y: 2.2, w: 8.8, h: 2.2,
    fontSize: 13, color: COLORS.text, fontFace: FONT_BODY,
    paraSpaceAfter: 6, valign: "top", margin: 0
  });

  s.addShape(pres.shapes.RECTANGLE, {
    x: 4.1, y: 4.6, w: 8.8, h: 2.7,
    fill: { color: COLORS.deep },
    line: { color: COLORS.deep, width: 0 }
  });
  s.addText("Behavioral evaluation (informal)", {
    x: 4.3, y: 4.75, w: 8.4, h: 0.4,
    fontSize: 14, bold: true, color: "F9E795", fontFace: FONT_HEAD, margin: 0
  });
  s.addText([
    { text: "Cold start: with <5 interactions, falls through to popularity-by-category. Acceptable for new users.", options: { breakLine: true } },
    { text: "Sparse user (~10 interactions): user-based dominates — pulls items from peer favorites.", options: { breakLine: true } },
    { text: "Active user (~50+ interactions): item-based pulls more weight; recommendations narrow toward established taste.", options: { breakLine: true } },
    { text: "Recommendations regenerate every chat open — no staleness.", options: {} }
  ], {
    x: 4.3, y: 5.2, w: 8.4, h: 2.0,
    fontSize: 12, color: COLORS.white, fontFace: FONT_BODY,
    paraSpaceAfter: 4, valign: "top", margin: 0
  });

  s.addNotes(
    "Live demo: open the app, log in as a test child, show the welcome-screen recommendations. Point out the 'Users like you have…' header — that's the CF branding. " +
    "Each card shows title, age band, and match percentage. The match % is finalScore × 100 — directly visible explainability. " +
    "Three behavioral observations from informal testing. (1) Cold start with very few interactions, the algorithm correctly returns nothing (finalScore = 0 filter), and the UI falls through to popularity-by-category. (2) A sparse user with ~10 interactions sees user-based dominate — they get items from peer favorites. (3) An active user with 50+ interactions sees item-based take over — recommendations sharpen toward established taste. " +
    "The recommendations are recomputed every time the chat opens. No precomputation, no staleness."
  );
}

// ============================================================
// SLIDE 14 — Engineering Reflections
// ============================================================
{
  const s = pres.addSlide();
  addHeader(s, "Engineering Reflections", "What I learned building this");

  const lessons = [
    {
      title: "Algorithmic transparency wins trust",
      body: "The reason string per recommendation made parental review possible. Users (and examiners) trust what they can inspect."
    },
    {
      title: "Concurrency before caching",
      body: "Restructuring sequential awaits to parallel async cut load time more than any caching scheme would, with zero infra change."
    },
    {
      title: "Hard rules > soft scoring",
      body: "isKidSafe and seen-exclusion are filters, not score penalties. Soft penalties always have an exploit; filters can't be bypassed."
    },
    {
      title: "Plan-tier constraints shape design",
      body: "Spark plan ruled out Cloud Functions, which forced client-side computation. The architecture documents the tradeoff explicitly."
    }
  ];

  let yOff = 1.7;
  for (const l of lessons) {
    s.addShape(pres.shapes.RECTANGLE, {
      x: 0.6, y: yOff, w: 12.3, h: 1.15,
      fill: { color: COLORS.white },
      line: { color: "E2E8F0", width: 1 },
      shadow: { type: "outer", color: "000000", blur: 4, offset: 1, angle: 90, opacity: 0.06 }
    });
    s.addShape(pres.shapes.RECTANGLE, {
      x: 0.6, y: yOff, w: 0.12, h: 1.15,
      fill: { color: COLORS.deep }, line: { color: COLORS.deep, width: 0 }
    });
    s.addText(l.title, {
      x: 0.95, y: yOff + 0.15, w: 11.7, h: 0.4,
      fontSize: 16, bold: true, color: COLORS.navy, fontFace: FONT_HEAD, margin: 0
    });
    s.addText(l.body, {
      x: 0.95, y: yOff + 0.55, w: 11.7, h: 0.55,
      fontSize: 12, color: COLORS.text, fontFace: FONT_BODY, margin: 0
    });
    yOff += 1.27;
  }

  s.addNotes(
    "Four reflections worth surfacing in viva. " +
    "(1) Transparency wins trust. Adding a one-line reason string per recommendation cost almost nothing but transformed the demo from a black box into something parents and examiners could probe. " +
    "(2) Concurrency before caching. The single biggest perceived performance win was restructuring sequential awaits to parallel async — no infrastructure, just better Kotlin. " +
    "(3) Hard rules over soft scoring. Both kid-safety and seen-item exclusion are filters, not score penalties. A soft penalty always has an exploit at the right multiplier; filters can't be bypassed by the algorithm. " +
    "(4) Plan-tier constraints shape design. Being on Spark plan ruled out Cloud Functions, which is why the algorithm runs client-side. I treat that as a documented tradeoff, not a limitation hidden in the architecture."
  );
}

// ============================================================
// SLIDE 15 — Closing / Q&A
// ============================================================
{
  const s = pres.addSlide();
  s.background = { color: COLORS.navy };

  // Decorative band
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 4.5, w: 13.3, h: 0.06,
    fill: { color: COLORS.teal }, line: { color: COLORS.teal, width: 0 }
  });

  s.addText("Questions?", {
    x: 0.8, y: 1.6, w: 11.7, h: 1.2,
    fontSize: 72, bold: true, color: COLORS.white, fontFace: FONT_HEAD,
    margin: 0
  });
  s.addText("Hybrid CF · Cosine similarity · Parallel data fetch · Explainable scoring", {
    x: 0.8, y: 2.9, w: 11.7, h: 0.5,
    fontSize: 18, italic: true, color: "9DECFF", fontFace: FONT_BODY,
    margin: 0
  });

  s.addText("Source", {
    x: 0.8, y: 5.0, w: 11.7, h: 0.4,
    fontSize: 14, bold: true, color: "F9E795", fontFace: FONT_BODY, margin: 0
  });
  s.addText("app/src/main/java/com/kidsrec/chatbot/data/repository/CollaborativeFilteringService.kt", {
    x: 0.8, y: 5.4, w: 11.7, h: 0.4,
    fontSize: 13, color: COLORS.white, fontFace: FONT_CODE, margin: 0
  });

  s.addText("Felix Eng Cheng Kang · FYP26S123 · Little Dino", {
    x: 0.8, y: 6.7, w: 11.7, h: 0.4,
    fontSize: 12, color: "9DB4E0", fontFace: FONT_BODY, margin: 0
  });

  s.addNotes(
    "Closing slide. Be ready for likely viva questions: " +
    "(a) why 50/50 weighting? — see slide 10 notes. " +
    "(b) what happens if a user has no interactions yet? — algorithm returns empty, UI falls through to popularity. " +
    "(c) how do you prevent a malicious user gaming the recommendations? — every interaction is tied to authenticated userId; anonymous writes are blocked at Firestore rules level. " +
    "(d) is cosine the best choice? — discuss alternatives like Pearson (handles rating bias but we have implicit weights) or Jaccard (good for binary, loses our weight nuance). " +
    "(e) why on-device? — Spark plan constraint, plus latency win on warm cache. Phase 2 of the roadmap addresses this."
  );
}

pres.writeFile({ fileName: "LittleDino_CF_Defense.pptx" })
  .then(name => console.log("Built:", name));
