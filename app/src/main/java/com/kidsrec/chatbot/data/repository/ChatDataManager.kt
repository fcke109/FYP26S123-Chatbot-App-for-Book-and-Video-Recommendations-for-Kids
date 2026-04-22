package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.remote.GeminiService
import com.kidsrec.chatbot.data.remote.OpenAIMessage
import com.kidsrec.chatbot.data.remote.OpenAIRequest
import com.kidsrec.chatbot.data.remote.OpenAIService
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.remote.YouTubeService
import com.kidsrec.chatbot.util.TopicExtractor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType

@Singleton
class ChatDataManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val openAIService: OpenAIService,
    private val geminiService: GeminiService,
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager,
    private val openLibraryService: OpenLibraryService,
    private val youTubeService: YouTubeService,
    private val learningProgressManager: LearningProgressManager,
    private val gamificationManager: GamificationManager
) {

    private data class ApprovedVideo(
        val id: String,
        val title: String,
        val description: String,
        val reason: String,
        val url: String,
        val imageUrl: String,
        val category: String,
        val tags: List<String> = emptyList()
    )

    private val approvedVideos = listOf(
        // =========================
        // ANIMALS
        // =========================
        ApprovedVideo(
            id = "vid_baby_shark",
            title = "Baby Shark Dance",
            description = "A fun and catchy shark song for kids.",
            reason = "Perfect for children who enjoy singing and ocean animals.",
            url = "https://www.youtube.com/watch?v=XqZsoesa55w",
            imageUrl = "https://img.youtube.com/vi/XqZsoesa55w/hqdefault.jpg",
            category = "animals",
            tags = listOf("baby shark", "shark", "ocean", "sea", "fish", "animals", "song", "dance")
        ),
        ApprovedVideo(
            id = "vid_old_macdonald",
            title = "Old MacDonald Had a Farm",
            description = "A classic nursery rhyme with farm animals.",
            reason = "Great for kids who enjoy animal sounds and sing-along songs.",
            url = "https://www.youtube.com/watch?v=_6HzoUcx3eo",
            imageUrl = "https://img.youtube.com/vi/_6HzoUcx3eo/hqdefault.jpg",
            category = "animals",
            tags = listOf("farm", "animals", "cow", "pig", "chicken", "nursery rhyme", "song")
        ),
        ApprovedVideo(
            id = "vid_ocean_animals",
            title = "What Animals Live in the Ocean?",
            description = "Discover fun facts about amazing sea creatures.",
            reason = "A great pick for children who love ocean life.",
            url = "https://www.youtube.com/watch?v=NCq8AedHvZQ",
            imageUrl = "https://www.youtube.com/watch?v=NCq8AedHvZQ/hqdefault.jpg",
            category = "animals",
            tags = listOf("ocean", "sea", "marine", "animals", "dolphin", "whale", "fish")
        ),
        ApprovedVideo(
            id = "vid_dinosaurs",
            title = "Dinosaurs for Kids",
            description = "A fun learning video about dinosaurs and prehistoric life.",
            reason = "Perfect for children who love dinosaurs.",
            url = "https://www.youtube.com/watch?v=dktnOPfE7Dc",
            imageUrl = "https://www.youtube.com/watch?v=dktnOPfE7Dc/hqdefault.jpg",
            category = "animals",
            tags = listOf("dinosaurs", "dinosaur", "t-rex", "prehistoric", "animals", "science")
        ),
        ApprovedVideo(
            id = "vid_animal_sounds",
            title = "Animal Sounds For Kids",
            description = "Learn animal sounds in a fun and playful way.",
            reason = "Helps children connect animal names with their sounds.",
            url = "https://www.youtube.com/watch?v=HqKZT61xkNk",
            imageUrl = "https://img.youtube.com/vi/HqKZT61xkNk/hqdefault.jpg",
            category = "animals",
            tags = listOf("animal sounds", "animals", "sound", "farm", "wild animals", "learning")
        ),
        ApprovedVideo(
            id = "vid_shark_song",
            title = "Shark Song for Kids",
            description = "A playful song about sharks and ocean fun.",
            reason = "Great for children who enjoy sea creatures.",
            url = "https://www.youtube.com/watch?v=XqZsoesa55w",
            imageUrl = "https://img.youtube.com/vi/XqZsoesa55w/hqdefault.jpg",
            category = "animals",
            tags = listOf("shark", "sharks", "ocean", "sea", "fish", "song")
        ),
        ApprovedVideo(
            id = "vid_farm_animals_song",
            title = "Farm Animals Song for Kids",
            description = "A simple and cheerful song about farm animals.",
            reason = "Helps younger children learn animal names.",
            url = "https://www.youtube.com/watch?v=_6HzoUcx3eo",
            imageUrl = "https://img.youtube.com/vi/_6HzoUcx3eo/hqdefault.jpg",
            category = "animals",
            tags = listOf("farm animals", "animals", "song", "cow", "pig", "horse")
        ),
        ApprovedVideo(
            id = "vid_marine_animals",
            title = "Sea Animals for Kids",
            description = "Learn about sea animals in a simple kid-friendly way.",
            reason = "A fun choice for children who love the ocean.",
            url = "https://www.youtube.com/watch?v=Oxw6FoUNeT4",
            imageUrl = "https://www.youtube.com/watch?v=Oxw6FoUNeT4/hqdefault.jpg",
            category = "animals",
            tags = listOf("marine", "ocean", "animals", "sea", "fish", "whale", "vocabulary")
        ),
        ApprovedVideo(
            id = "vid_prehistoric_animals",
            title = "Extinct Animals for Kids",
            description = "Explore giant creatures from long ago.",
            reason = "A fun pick for children interested in prehistoric life.",
            url = "https://www.youtube.com/watch?v=KZ55sIWY_Gc",
            imageUrl = "https://www.youtube.com/watch?v=KZ55sIWY_Gc/hqdefault.jpg",
            category = "animals",
            tags = listOf("prehistoric", "dinosaurs", "animals", "science", "Educational")
        ),
        ApprovedVideo(
            id = "vid_farm_song_animals",
            title = "Fun Farm Song",
            description = "A cheerful farm-themed song with friendly animals.",
            reason = "Good for little kids who enjoy singing about animals.",
            url = "https://www.youtube.com/watch?v=_6HzoUcx3eo",
            imageUrl = "https://img.youtube.com/vi/_6HzoUcx3eo/hqdefault.jpg",
            category = "animals",
            tags = listOf("farm", "song", "animals", "nursery rhyme")
        ),

        // =========================
        // SPACE
        // =========================
        ApprovedVideo(
            id = "vid_solar_system",
            title = "Solar System for Kids",
            description = "Learn about the planets in our solar system.",
            reason = "A fun way to explore planets and space.",
            url = "https://www.youtube.com/watch?v=Qd6nLM2QlWw",
            imageUrl = "https://img.youtube.com/vi/Qd6nLM2QlWw/hqdefault.jpg",
            category = "space",
            tags = listOf("space", "solar system", "planets", "earth", "mars", "science")
        ),
        ApprovedVideo(
            id = "vid_planets_song",
            title = "Planets Song for Kids",
            description = "A fun song to help children remember the planets.",
            reason = "Makes planet learning easier and more memorable.",
            url = "https://www.youtube.com/watch?v=mQrlgH97v94&list=RDmQrlgH97v94&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=mQrlgH97v94&list=RDmQrlgH97v94&start_radio=1/hqdefault.jpg",
            category = "space",
            tags = listOf("planets", "space", "song", "solar system", "learning")
        ),
        ApprovedVideo(
            id = "vid_stars_moon",
            title = "Stars and Moon for Kids",
            description = "A simple video introducing stars and the moon.",
            reason = "A calm and interesting introduction to the night sky.",
            url = "https://www.youtube.com/watch?v=yCjJyiqpAuU",
            imageUrl = "https://img.youtube.com/vi/yCjJyiqpAuU/hqdefault.jpg",
            category = "space",
            tags = listOf("moon", "stars", "night sky", "space", "bedtime")
        ),
        ApprovedVideo(
            id = "vid_rocket_learning",
            title = "Rocket Learning for Kids",
            description = "A simple learning video about rockets and space travel.",
            reason = "A fun choice for children who love rockets.",
            url = "https://www.youtube.com/watch?v=QCj66nGiKpU",
            imageUrl = "https://www.youtube.com/watch?v=QCj66nGiKpU/hqdefault.jpg",
            category = "space",
            tags = listOf("rocket", "space", "science", "planets", "astronaut")
        ),
        ApprovedVideo(
            id = "vid_astronauts_kids",
            title = "Astronauts for Kids",
            description = "A child-friendly introduction to astronauts and space missions.",
            reason = "Great for curious children who dream about space travel.",
            url = "https://www.youtube.com/watch?v=lndTOMgdRAU",
            imageUrl = "https://www.youtube.com/watch?v=lndTOMgdRAU/hqdefault.jpg",
            category = "space",
            tags = listOf("astronaut", "space", "rocket", "science", "learning")
        ),
        ApprovedVideo(
            id = "vid_planet_earth_kids",
            title = "Planet Earth for Kids",
            description = "Learn about Earth as part of the solar system.",
            reason = "Helps children understand where we live in space.",
            url = "https://www.youtube.com/watch?v=uAwTWAC0vt0",
            imageUrl = "https://www.youtube.com/watch?v=uAwTWAC0vt0/hqdefault.jpg",
            category = "space",
            tags = listOf("earth", "planet", "space", "solar system", "science")
        ),
        ApprovedVideo(
            id = "vid_space_adventure",
            title = "Space Adventure for Kids",
            description = "A simple and fun introduction to planets and stars.",
            reason = "Good for children who want an exciting space video.",
            url = "https://www.youtube.com/watch?v=osSkJuohjM8",
            imageUrl = "https://www.youtube.com/watch?v=osSkJuohjM8/hqdefault.jpg",
            category = "space",
            tags = listOf("space", "adventure", "stars", "planets", "science")
        ),
        ApprovedVideo(
            id = "vid_space_science",
            title = "Space Science for Kids",
            description = "A child-friendly science video about space.",
            reason = "A great pick for curious young learners.",
            url = "https://www.youtube.com/watch?v=7XseVrmpkUUw",
            imageUrl = "https://www.youtube.com/watch?v=7XseVrmpkUU/hqdefault.jpg",
            category = "space",
            tags = listOf("space", "science", "planets", "solar system")
        ),

        // =========================
        // BEDTIME
        // =========================
        ApprovedVideo(
            id = "vid_twinkle",
            title = "Twinkle Twinkle Little Star",
            description = "A gentle nursery rhyme for quiet moments.",
            reason = "A calm and familiar bedtime favorite.",
            url = "https://www.youtube.com/watch?v=yCjJyiqpAuU",
            imageUrl = "https://img.youtube.com/vi/yCjJyiqpAuU/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("bedtime", "nursery rhyme", "star", "sleep", "calm", "song")
        ),
        ApprovedVideo(
            id = "vid_happy_song",
            title = "My Happy Song",
            description = "A fun and gentle movement song for kids.",
            reason = "Cheerful without being too overwhelming.",
            url = "https://www.youtube.com/watch?v=RE29EUeJAb0",
            imageUrl = "https://img.youtube.com/vi/RE29EUeJAb0/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("happy song", "music", "kids song", "gentle", "fun")
        ),
        ApprovedVideo(
            id = "vid_seven_steps",
            title = "Seven Steps",
            description = "A playful but simple children’s song.",
            reason = "Easy and fun for younger children.",
            url = "https://www.youtube.com/watch?v=-CJUvZM3Ix8",
            imageUrl = "https://img.youtube.com/vi/-CJUvZM3Ix8/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("song", "nursery rhyme", "gentle", "kids", "simple")
        ),
        ApprovedVideo(
            id = "vid_bedtime_bus",
            title = "Wheels on the Bus",
            description = "A classic sing-along children’s song.",
            reason = "A familiar and comforting choice for young kids.",
            url = "https://www.youtube.com/watch?v=e_04ZrNroTo",
            imageUrl = "https://img.youtube.com/vi/e_04ZrNroTo/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("bedtime", "bus", "song", "nursery rhyme", "transport")
        ),
        ApprovedVideo(
            id = "vid_lullaby_star",
            title = "Star Light, Star Bright",
            description = "A calm star-themed song for bedtime.",
            reason = "A soothing option for nighttime routines.",
            url = "https://www.youtube.com/watch?v=OWip7yvXukI&list=RDOWip7yvXukI&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=OWip7yvXukI&list=RDOWip7yvXukI&start_radio=1/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("lullaby", "bedtime", "star", "moon", "sleep")
        ),
        ApprovedVideo(
            id = "vid_quiet_shapes",
            title = "Quiet Shapes Song",
            description = "A calm learning song about basic shapes.",
            reason = "A gentle educational video for winding down.",
            url = "https://www.youtube.com/watch?v=OEbRDtCAFdU",
            imageUrl = "https://img.youtube.com/vi/OEbRDtCAFdU/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("bedtime", "shapes", "learning", "quiet", "song")
        ),
        ApprovedVideo(
            id = "vid_quiet_colors",
            title = "Quiet Colors Song",
            description = "A simple and colorful learning song.",
            reason = "Easy and calm for younger children.",
            url = "https://www.youtube.com/watch?v=SLZcWGQQsmg",
            imageUrl = "https://img.youtube.com/vi/SLZcWGQQsmg/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("bedtime", "colors", "song", "learning", "calm")
        ),
        ApprovedVideo(
            id = "vid_bedtime_abc",
            title = "ABC Song with Calm Melody",
            description = "A softer alphabet song for younger children.",
            reason = "Nice for children who want something simple before bed.",
            url = "https://www.youtube.com/watch?v=75p-N9YKqNo",
            imageUrl = "https://img.youtube.com/vi/75p-N9YKqNo/hqdefault.jpg",
            category = "bedtime",
            tags = listOf("abc", "alphabet", "bedtime", "song", "calm")
        ),

        // =========================
        // LEARNING
        // =========================
        ApprovedVideo(
            id = "vid_abc_song",
            title = "ABC Song for Kids",
            description = "A cheerful alphabet song for early learners.",
            reason = "Great for learning letters in a fun way.",
            url = "https://www.youtube.com/watch?v=ccEpTTZW34g&list=RDccEpTTZW34g&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=ccEpTTZW34g&list=RDccEpTTZW34g&start_radio=1/hqdefault.jpg",
            category = "learning",
            tags = listOf("alphabet", "abc", "letters", "phonics", "preschool")
        ),
        ApprovedVideo(
            id = "vid_counting_song",
            title = "Counting Song for Children",
            description = "A fun video for learning numbers.",
            reason = "Helps with basic counting and number recognition.",
            url = "https://www.youtube.com/watch?v=DR-cfDsHCGA",
            imageUrl = "https://img.youtube.com/vi/DR-cfDsHCGA/hqdefault.jpg",
            category = "learning",
            tags = listOf("counting", "numbers", "math", "learning", "preschool")
        ),
        ApprovedVideo(
            id = "vid_shapes_song",
            title = "Shapes Song for Kids",
            description = "A bright and fun song introducing basic shapes.",
            reason = "Great for children learning circles, squares, and triangles.",
            url = "https://www.youtube.com/watch?v=OEbRDtCAFdU",
            imageUrl = "https://img.youtube.com/vi/OEbRDtCAFdU/hqdefault.jpg",
            category = "learning",
            tags = listOf("shapes", "geometry", "learning", "preschool", "school")
        ),
        ApprovedVideo(
            id = "vid_colors_song",
            title = "Colors Song for Children",
            description = "A colorful learning video for early learners.",
            reason = "Helps children learn color names in a fun way.",
            url = "https://www.youtube.com/watch?v=zxIpA5nF_LY&list=RDzxIpA5nF_LY&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=zxIpA5nF_LY&list=RDzxIpA5nF_LY&start_radio=1/hqdefault.jpg",
            category = "learning",
            tags = listOf("colors", "learning", "preschool", "school")
        ),
        ApprovedVideo(
            id = "vid_phonics",
            title = "Phonics Song for Kids",
            description = "Learn letter sounds with a playful phonics video.",
            reason = "Great for helping early reading skills grow.",
            url = "https://www.youtube.com/watch?v=BELlZKpi1Zs",
            imageUrl = "https://img.youtube.com/vi/BELlZKpi1Zs/hqdefault.jpg",
            category = "learning",
            tags = listOf("phonics", "reading", "letters", "school", "learning")
        ),
        ApprovedVideo(
            id = "vid_five_senses",
            title = "Five Senses for Kids",
            description = "A simple science video about the five senses.",
            reason = "Makes body science easy to understand.",
            url = "https://www.youtube.com/watch?v=q1xNuU7gaAQ",
            imageUrl = "https://img.youtube.com/vi/q1xNuU7gaAQ/hqdefault.jpg",
            category = "learning",
            tags = listOf("five senses", "body", "science", "learning", "health")
        ),
        ApprovedVideo(
            id = "vid_body_parts",
            title = "Body Parts for Kids",
            description = "A simple introduction to body part names.",
            reason = "Useful for very young learners.",
            url = "https://www.youtube.com/watch?v=SUt8q0EKbms",
            imageUrl = "https://www.youtube.com/watch?v=SUt8q0EKbms/hqdefault.jpg",
            category = "learning",
            tags = listOf("body parts", "body", "learning", "science", "kids")
        ),
        ApprovedVideo(
            id = "vid_numbers_practice",
            title = "Numbers Practice for Kids",
            description = "A simple number learning video for children.",
            reason = "Good for practicing counting and number recognition.",
            url = "https://www.youtube.com/watch?v=Yt8GFgxlITs&list=RDYt8GFgxlITs&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=Yt8GFgxlITs&list=RDYt8GFgxlITs&start_radio=1/hqdefault.jpg",
            category = "learning",
            tags = listOf("numbers", "counting", "math", "school", "learning")
        ),
        ApprovedVideo(
            id = "vid_alphabet_letters",
            title = "Alphabet Letters for Kids",
            description = "A playful alphabet video for early learners.",
            reason = "A great way to practice letters again and again.",
            url = "https://www.youtube.com/watch?v=EVTB8xIHWU0",
            imageUrl = "https://www.youtube.com/watch?v=EVTB8xIHWU0/hqdefault.jpg",
            category = "learning",
            tags = listOf("alphabet", "letters", "abc", "phonics")
        ),
        ApprovedVideo(
            id = "vid_color_learning",
            title = "Learn Colors with Wonderville Friends",
            description = "A bright and simple video about basic colors.",
            reason = "Useful for preschool children learning color names.",
            url = "https://www.youtube.com/watch?v=HrDl_1Ov8gc",
            imageUrl = "https://www.youtube.com/watch?v=HrDl_1Ov8gc/hqdefault.jpg",
            category = "learning",
            tags = listOf("colors", "preschool", "learning", "school")
        ),

        // =========================
        // SONGS & NURSERY RHYMES
        // =========================
        ApprovedVideo(
            id = "vid_wheels_bus",
            title = "Wheels on the Bus",
            description = "A classic children’s song about a fun bus ride.",
            reason = "A favorite sing-along for young children.",
            url = "https://www.youtube.com/watch?v=e_04ZrNroTo",
            imageUrl = "https://img.youtube.com/vi/e_04ZrNroTo/hqdefault.jpg",
            category = "songs",
            tags = listOf("bus", "wheels", "nursery rhyme", "song", "vehicles", "transport")
        ),
        ApprovedVideo(
            id = "vid_transport_song",
            title = "Driving In My Car",
            description = "A simple song about buses and transport.",
            reason = "Nice for children who love vehicles.",
            url = "https://www.youtube.com/watch?v=BdrZWu2dZ4c&list=RDBdrZWu2dZ4c&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=BdrZWu2dZ4c&list=RDBdrZWu2dZ4c&start_radio=1/hqdefault.jpg",
            category = "songs",
            tags = listOf("transport", "vehicles", "bus", "song", "kids")
        ),
        ApprovedVideo(
            id = "vid_music_happy",
            title = "Happy Music for Kids",
            description = "A bright and cheerful children’s music video.",
            reason = "Fun for movement, dancing, and smiles.",
            url = "https://www.youtube.com/watch?v=FiXCxfWWwPo&list=RDFiXCxfWWwPo&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=FiXCxfWWwPo&list=RDFiXCxfWWwPo&start_radio=1/hqdefault.jpg",
            category = "songs",
            tags = listOf("music", "dance", "happy", "kids songs")
        ),
        ApprovedVideo(
            id = "vid_nursery_star",
            title = "Star Nursery Rhyme",
            description = "A gentle nursery rhyme with a star theme.",
            reason = "A classic choice many children already know.",
            url = "https://www.youtube.com/watch?v=SNeI2PMcRdA&list=RDSNeI2PMcRdA&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=SNeI2PMcRdA&list=RDSNeI2PMcRdA&start_radio=1/hqdefault.jpg",
            category = "songs",
            tags = listOf("nursery rhyme", "star", "song", "bedtime")
        ),
        ApprovedVideo(
            id = "vid_count_song_music",
            title = "Counting Music for Kids",
            description = "A sing-along style counting video.",
            reason = "Combines music with early number practice.",
            url = "https://www.youtube.com/watch?v=S84fcGdEULk&list=RDS84fcGdEULk&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=S84fcGdEULk&list=RDS84fcGdEULk&start_radio=1/hqdefault.jpg",
            category = "songs",
            tags = listOf("counting", "music", "numbers", "song")
        ),
        ApprovedVideo(
            id = "vid_abc_music",
            title = "Alphabet Music for Kids",
            description = "An alphabet sing-along for younger children.",
            reason = "Good for combining music and learning.",
            url = "https://www.youtube.com/watch?v=-1jxqVy5SlA&list=RD-1jxqVy5SlA&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=-1jxqVy5SlA&list=RD-1jxqVy5SlA&start_radio=1/hqdefault.jpg",
            category = "songs",
            tags = listOf("alphabet", "abc", "music", "song")
        ),

        // =========================
        // STORIES
        // =========================
        ApprovedVideo(
            id = "vid_story_animals",
            title = "Animal Story for Kids",
            description = "A story-style animal video for children.",
            reason = "Nice for kids who want a simple animal story.",
            url = "https://www.youtube.com/watch?v=tUjOL_Nk6uo",
            imageUrl = "https://www.youtube.com/watch?v=tUjOL_Nk6uo/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "animals", "kids story", "farm")
        ),
        ApprovedVideo(
            id = "vid_story_space",
            title = "Space Story for Kids",
            description = "A story-style learning video about space.",
            reason = "A fun choice for children who like planets and stars.",
            url = "https://www.youtube.com/watch?v=PqrZ-Q38xuc",
            imageUrl = "https://www.youtube.com/watch?v=PqrZ-Q38xuc/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "space", "planets", "stars")
        ),
        ApprovedVideo(
            id = "vid_story_dinosaurs",
            title = "Dinosaur Story for Kids",
            description = "A child-friendly dinosaur-themed story video.",
            reason = "Great for children who enjoy dinosaurs and adventures.",
            url = "http://youtube.com/watch?v=msFtC8qyrsQ",
            imageUrl = "http://youtube.com/watch?v=msFtC8qyrsQ/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "dinosaurs", "adventure", "kids")
        ),
        ApprovedVideo(
            id = "vid_story_ocean",
            title = "The Whale Who Wanted to Fly",
            description = "A story-style ocean learning video for children.",
            reason = "Good for children who want a sea-themed story.",
            url = "https://www.youtube.com/watch?v=-F0ANWRHkQ0",
            imageUrl = "https://www.youtube.com/watch?v=-F0ANWRHkQ0/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "ocean", "sea", "animals")
        ),
        ApprovedVideo(
            id = "vid_story_bedtime",
            title = "Bedtime Story Song",
            description = "A calmer story-style song for bedtime.",
            reason = "Useful when children want a softer video before sleep.",
            url = "https://www.youtube.com/watch?v=zmUzTZhbNh4&list=RDzmUzTZhbNh4&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=zmUzTZhbNh4&list=RDzmUzTZhbNh4&start_radio=1/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "bedtime", "sleep", "calm")
        ),
        ApprovedVideo(
            id = "vid_story_learning",
            title = "Learning Story for Kids",
            description = "A simple story-style educational video.",
            reason = "A nice mix of learning and storytelling.",
            url = "https://www.youtube.com/watch?v=uwzViw-T0-A",
            imageUrl = "https://www.youtube.com/watch?v=uwzViw-T0-A/hqdefault.jpg",
            category = "stories",
            tags = listOf("story", "learning", "abc", "kids")
        ),

        // =========================
        // SCIENCE
        // =========================
        ApprovedVideo(
            id = "vid_science_body",
            title = "Science About the Body for Kids",
            description = "A child-friendly science video about how the body works.",
            reason = "Great for curious children learning basic science.",
            url = "https://www.youtube.com/watch?v=q1xNuU7gaAQ",
            imageUrl = "https://img.youtube.com/vi/q1xNuU7gaAQ/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "body", "health", "five senses", "learning")
        ),
        ApprovedVideo(
            id = "vid_science_ocean",
            title = "Science About Ocean Animals",
            description = "Learn about sea creatures in a simple kid-friendly way.",
            reason = "A strong choice for children who like science and animals.",
            url = "https://www.youtube.com/watch?v=BiElE2aNDTk",
            imageUrl = "https://www.youtube.com/watch?v=BiElE2aNDTk/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "ocean", "animals", "marine", "nature")
        ),
        ApprovedVideo(
            id = "vid_science_dino",
            title = "Science About Dinosaurs",
            description = "A simple dinosaur learning video for children.",
            reason = "Good for kids who enjoy science and prehistoric creatures.",
            url = "https://www.youtube.com/watch?v=XinAZXVlgkcQ",
            imageUrl = "https://www.youtube.com/watch?v=XinAZXVlgkc/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "dinosaurs", "prehistoric", "animals", "learning")
        ),
        ApprovedVideo(
            id = "vid_science_space",
            title = "Science About the Solar System",
            description = "A child-friendly introduction to planets and space science.",
            reason = "Perfect for curious young learners.",
            url = "https://www.youtube.com/watch?v=oHahGWzLpl0",
            imageUrl = "https://www.youtube.com/watch?v=oHahGWzLpl0/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "space", "solar system", "planets", "learning")
        ),
        ApprovedVideo(
            id = "vid_science_shapes",
            title = "Science Shapes for Kids",
            description = "A simple video that helps children learn shapes.",
            reason = "A useful early-learning science-style video.",
            url = "https://www.youtube.com/watch?v=0B6Ge0FzHG0&list=RD0B6Ge0FzHG0&start_radio=1",
            imageUrl = "https://www.youtube.com/watch?v=0B6Ge0FzHG0&list=RD0B6Ge0FzHG0&start_radio=1/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "shapes", "geometry", "learning")
        ),
        ApprovedVideo(
            id = "vid_science_colors",
            title = "Science Colors for Kids",
            description = "A colorful learning video for young children.",
            reason = "Great for children practicing color recognition.",
            url = "https://www.youtube.com/watch?v=o0ukljOhV-c",
            imageUrl = "https://www.youtube.com/watch?v=o0ukljOhV-c/hqdefault.jpg",
            category = "science",
            tags = listOf("science", "colors", "learning", "preschool")
        )
    )

    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            val validationError = com.kidsrec.chatbot.util.InputSanitizer.validateMessage(message)
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }

            val sanitizedMessage = com.kidsrec.chatbot.util.InputSanitizer.sanitizeChatMessage(message)

            val exploredTopic = TopicExtractor.extractTopic(sanitizedMessage)
            val trackingResult = learningProgressManager.trackTopicExplored(
                childUserId = userId,
                topic = exploredTopic
            )
            if (trackingResult.isFailure) {
                Log.e(
                    "ChatDataManager",
                    "Topic tracking failed: ${trackingResult.exceptionOrNull()?.message}",
                    trackingResult.exceptionOrNull()
                )
            } else {
                Log.d("ChatDataManager", "Tracked topic: $exploredTopic for userId=$userId")

                val gamificationResult = gamificationManager.refreshGamification(userId)
                if (gamificationResult.isFailure) {
                    Log.e(
                        "ChatDataManager",
                        "Gamification refresh failed: ${gamificationResult.exceptionOrNull()?.message}",
                        gamificationResult.exceptionOrNull()
                    )
                } else {
                    Log.d("ChatDataManager", "Gamification refreshed for userId=$userId")
                }
            }

            val curatedBooks = bookDataManager.getCuratedBooks().getOrDefault(emptyList())

            val curatedBooksContext = if (curatedBooks.isNotEmpty()) {
                buildString {
                    appendLine("Available curated books:")
                    curatedBooks.forEachIndexed { index, book ->
                        appendLine("${index + 1}. ${book.title} by ${book.author}")
                    }
                }
            } else {
                "No curated books are currently available."
            }

            val approvedVideosContext = if (approvedVideos.isNotEmpty()) {
                buildString {
                    appendLine("Available approved kid-safe videos:")
                    approvedVideos.forEachIndexed { index, video ->
                        appendLine("${index + 1}. ${video.title} [${video.category}] - ${video.description}")
                    }
                }
            } else {
                "No approved videos are currently available."
            }

            val userMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.USER,
                content = sanitizedMessage,
                timestamp = Timestamp.now()
            )

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(userMessage.id)
                .set(userMessage)
                .await()

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .update(
                    mapOf(
                        "lastUpdated" to Timestamp.now(),
                        "preview" to message.take(80)
                    )
                )
                .await()

            val messagesSnapshot = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(12)
                .get()
                .await()

            val conversationHistory = messagesSnapshot.documents.mapNotNull { doc ->
                val msg = doc.toObject(ChatMessage::class.java)
                msg?.let {
                    OpenAIMessage(
                        role = if (it.role == MessageRole.USER) "user" else "assistant",
                        content = it.content
                    )
                }
            }

            val systemPrompt = """
You are Little Dino, a friendly dinosaur helping kids find books and videos.

$curatedBooksContext

$approvedVideosContext

CRITICAL RULES:
1. ALWAYS recommend content that DIRECTLY matches what the child asked about. Relevance is the #1 priority.
2. For BOOKS: If a curated book matches the child's topic, use that exact title. Otherwise, suggest a real, well-known children's book about their specific topic.
3. For VIDEOS: ONLY use titles from the approved kid-safe video list above.
4. NEVER invent a new video title outside the approved video list.
5. Prefer approved videos whose title, category, or tags clearly match the child's request.
6. If there is no perfect video match, choose the closest safe approved video in the same topic area.
7. For any recommendation, provide a reason why it is fun for the child.
8. Always include a mix of BOTH:
   - at least 1 BOOK
   - at least 1 VIDEO
9. Keep the response friendly and short for children.

Response format:
1. Friendly message (1-2 sentences).
2. End with this EXACT block:

[RECOMMENDATIONS]
[
  {"type":"BOOK","title":"Exact Book Title","description":"1 short sentence","reason":"Why it is fun"},
  {"type":"VIDEO","title":"Exact Approved Video Title","description":"1 short sentence","reason":"Why it is fun"}
]
[/RECOMMENDATIONS]

RULES FOR JSON:
- type must be BOOK or VIDEO
- for BOOK, if a curated book directly matches the child's interest, use that exact title. Otherwise, use a real, well-known children's book title about what they asked for.
- for VIDEO, use ONLY an exact title from the approved videos listed above
- do NOT include url
- do NOT include imageUrl
- keep descriptions short
""".trimIndent()

            val messagesList = mutableListOf(
                OpenAIMessage(role = "system", content = systemPrompt)
            )
            messagesList.addAll(conversationHistory)
            messagesList.add(OpenAIMessage(role = "user", content = sanitizedMessage))

            val rawResponse = try {
                geminiService.chat(systemPrompt, conversationHistory, sanitizedMessage)
            } catch (e: Exception) {
                Log.w("ChatDataManager", "Gemini failed, trying OpenAI: ${e.message}")
                val openAIResponse = openAIService.createChatCompletion(
                    OpenAIRequest(messages = messagesList)
                )
                openAIResponse.choices.firstOrNull()?.message?.content
                    ?: "Let's find some fun stories and videos!"
            }

            val botResponse = com.kidsrec.chatbot.util.ContentFilter.sanitizeResponse(rawResponse)
            val (cleanContent, parsedRecs) = parseRecommendations(botResponse)

            val withContentUrls = attachContentUrls(
                recommendations = parsedRecs,
                curatedBooks = curatedBooks,
                approvedVideos = approvedVideos
            )

            val ensuredMix = ensureBookAndVideoMix(
                originalMessage = sanitizedMessage,
                recommendations = withContentUrls,
                curatedBooks = curatedBooks,
                approvedVideos = approvedVideos
            )

            val collaborativelyRanked = applyCollaborativeFilteringRanking(
                userId = userId,
                recommendations = ensuredMix
            )

            val recommendations = scoreWithANN(
                recommendations = collaborativelyRanked,
                curatedBooks = curatedBooks,
                userId = userId
            )

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = cleanContent.ifBlank { "Here are some fun picks for you!" },
                timestamp = Timestamp.now(),
                recommendations = recommendations
            )

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(botMessage.id)
                .set(botMessage)
                .await()

            Result.success(botMessage)
        } catch (e: Exception) {
            Log.e("ChatDataManager", "sendMessage failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getMessagesFlow(userId: String, conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chatHistory")
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatDataManager", "Error loading messages: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.toObjects(ChatMessage::class.java)) }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createConversation(userId: String): Result<String> {
        return try {
            val ref = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document()

            ref.set(Conversation(id = ref.id, userId = userId)).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestConversation(userId: String): Result<Conversation?> {
        return try {
            val snapshot = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val conversation = snapshot.toObjects(Conversation::class.java).firstOrNull()
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getConversationsFlow(userId: String, limit: Int = 20): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("chatHistory")
            .document(userId)
            .collection("conversations")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot?.toObjects(Conversation::class.java) ?: emptyList()
                trySend(conversations)
            }

        awaitClose { listener.remove() }
    }

    private suspend fun parseRecommendations(response: String): Pair<String, List<Recommendation>> {
        val recommendations = mutableListOf<Recommendation>()
        var cleanContent = response

        try {
            val startTag = "[RECOMMENDATIONS]"
            val endTag = "[/RECOMMENDATIONS]"
            val startIndex = response.indexOf(startTag)
            val endIndex = response.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1) {
                val jsonString = response
                    .substring(startIndex + startTag.length, endIndex)
                    .trim()

                cleanContent = response.substring(0, startIndex).trim()

                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val type = if (obj.optString("type").uppercase() == "VIDEO") {
                        RecommendationType.VIDEO
                    } else {
                        RecommendationType.BOOK
                    }

                    val title = obj.optString("title").trim()
                    val stableId = "rec_" + (title + type.name).hashCode().toString()

                    if (title.isNotBlank()) {
                        recommendations.add(
                            Recommendation(
                                id = stableId,
                                type = type,
                                title = title,
                                description = obj.optString("description"),
                                imageUrl = "",
                                reason = obj.optString("reason"),
                                relevanceScore = 0.0,
                                url = "",
                                isCurated = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDataManager", "Failed to parse recommendations", e)
        }

        return Pair(cleanContent, recommendations)
    }

    private suspend fun attachContentUrls(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        approvedVideos: List<ApprovedVideo>
    ): List<Recommendation> {
        return recommendations.mapNotNull { rec ->
            when (rec.type) {
                RecommendationType.BOOK -> {
                    val matchingBook = curatedBooks.firstOrNull { book ->
                        titlesMatch(book.title, rec.title)
                    }

                    if (matchingBook != null) {
                        val bookUrl = matchingBook.readerUrl.ifBlank { matchingBook.bookUrl }
                        rec.copy(
                            id = matchingBook.id,
                            title = matchingBook.title,
                            description = if (rec.description.isBlank()) matchingBook.description else rec.description,
                            imageUrl = matchingBook.coverUrl,
                            url = bookUrl,
                            isCurated = true
                        )
                    } else {
                        try {
                            val searchResult = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                openLibraryService.searchBooks(rec.title, limit = 3)
                            }
                            val found = searchResult.docs.firstOrNull { it.canReadOnline() }
                            if (found != null) {
                                rec.copy(
                                    url = found.getReadUrl() ?: "",
                                    imageUrl = found.getCoverUrl("M") ?: "",
                                    isCurated = false
                                )
                            } else {
                                val anyResult = searchResult.docs.firstOrNull()
                                if (anyResult != null) {
                                    rec.copy(
                                        url = anyResult.getOpenLibraryUrl(),
                                        imageUrl = anyResult.getCoverUrl("M") ?: "",
                                        isCurated = false
                                    )
                                } else {
                                    null
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ChatDataManager", "Open Library search failed for: ${rec.title}", e)
                            null
                        }
                    }
                }

                RecommendationType.VIDEO -> {
                    val matchingVideo = approvedVideos.firstOrNull { video ->
                        titlesMatch(video.title, rec.title)
                    }

                    if (matchingVideo != null) {
                        rec.copy(
                            id = matchingVideo.id,
                            title = matchingVideo.title,
                            description = if (rec.description.isBlank()) matchingVideo.description else rec.description,
                            imageUrl = matchingVideo.imageUrl,
                            reason = if (rec.reason.isBlank()) matchingVideo.reason else rec.reason,
                            url = matchingVideo.url,
                            isCurated = true
                        )
                    } else {
                        val fallbackVideo = pickBestFallbackVideo(rec.title, approvedVideos)
                        if (fallbackVideo != null) {
                            rec.copy(
                                id = fallbackVideo.id,
                                type = RecommendationType.VIDEO,
                                title = fallbackVideo.title,
                                description = if (rec.description.isBlank()) fallbackVideo.description else rec.description,
                                imageUrl = fallbackVideo.imageUrl,
                                reason = if (rec.reason.isBlank()) fallbackVideo.reason else rec.reason,
                                url = fallbackVideo.url,
                                isCurated = true
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    private fun extractSearchTopic(message: String): String {
        val fillerWords = setOf(
            "i", "me", "my", "like", "love", "want", "show", "find", "get",
            "watch", "see", "please", "can", "you", "the", "a", "an", "some",
            "about", "tell", "more", "really", "very", "so", "would", "could",
            "recommend", "suggest", "something", "anything", "videos", "video",
            "books", "book", "stories", "story", "to", "of", "for", "and",
            "is", "are", "was", "were", "do", "does", "did", "have", "has",
            "know", "think", "looking", "interested", "in", "on", "with"
        )
        val words = message.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in fillerWords }

        return words.joinToString(" ").ifBlank { message }
    }

    private suspend fun ensureBookAndVideoMix(
        originalMessage: String,
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        approvedVideos: List<ApprovedVideo>
    ): List<Recommendation> {
        val mutable = recommendations.toMutableList()

        val hasBook = mutable.any { it.type == RecommendationType.BOOK && it.url.isNotBlank() }
        val hasVideo = mutable.any { it.type == RecommendationType.VIDEO && it.url.isNotBlank() }

        if (!hasBook && curatedBooks.isNotEmpty()) {
            val fallbackBook = curatedBooks.first()
            val fallbackUrl = fallbackBook.readerUrl.ifBlank { fallbackBook.bookUrl }

            mutable.add(
                Recommendation(
                    id = fallbackBook.id,
                    type = RecommendationType.BOOK,
                    title = fallbackBook.title,
                    description = fallbackBook.description,
                    imageUrl = fallbackBook.coverUrl,
                    reason = "A nice story to read.",
                    relevanceScore = 0.0,
                    url = fallbackUrl
                )
            )
        }

        if (!hasVideo && approvedVideos.isNotEmpty()) {
            val fallbackVideo = pickBestFallbackVideo(originalMessage, approvedVideos)
            if (fallbackVideo != null) {
                mutable.add(
                    Recommendation(
                        id = fallbackVideo.id,
                        type = RecommendationType.VIDEO,
                        title = fallbackVideo.title,
                        description = fallbackVideo.description,
                        imageUrl = fallbackVideo.imageUrl,
                        reason = fallbackVideo.reason,
                        relevanceScore = 0.0,
                        url = fallbackVideo.url,
                        isCurated = true
                    )
                )
            } else {
                val topic = extractSearchTopic(originalMessage)
                val categoryFallback = approvedVideos.firstOrNull { video ->
                    video.category.contains(topic.lowercase()) ||
                            video.tags.any { tag -> tag.contains(topic.lowercase()) }
                } ?: approvedVideos.first()

                mutable.add(
                    Recommendation(
                        id = categoryFallback.id,
                        type = RecommendationType.VIDEO,
                        title = categoryFallback.title,
                        description = categoryFallback.description,
                        imageUrl = categoryFallback.imageUrl,
                        reason = categoryFallback.reason,
                        relevanceScore = 0.0,
                        url = categoryFallback.url,
                        isCurated = true
                    )
                )
            }
        }

        return mutable
    }

    private fun pickBestFallbackVideo(
        message: String,
        videos: List<ApprovedVideo>
    ): ApprovedVideo? {
        val lowerMessage = message.lowercase()
        val messageWords = lowerMessage.split(Regex("\\s+")).filter { it.length > 2 }

        val scored = videos.map { video ->
            var score = 0
            score += video.tags.count { tag -> lowerMessage.contains(tag.lowercase()) } * 3
            val titleWords = video.title.lowercase().split(Regex("\\s+"))
            score += messageWords.count { word -> titleWords.any { it.contains(word) } } * 2
            score += messageWords.count { word -> video.description.lowercase().contains(word) }
            score += messageWords.count { word -> video.category.lowercase().contains(word) } * 2
            video to score
        }

        val best = scored.maxByOrNull { it.second }
        return if (best != null && best.second > 0) best.first else null
    }

    private fun titlesMatch(a: String, b: String): Boolean {
        val x = normalizeTitle(a)
        val y = normalizeTitle(b)
        return x == y || x.contains(y) || y.contains(x)
    }

    private fun normalizeTitle(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }


    private data class UserItemInteraction(
        val userId: String,
        val itemKey: String,
        val weight: Double
    )

    private suspend fun applyCollaborativeFilteringRanking(
        userId: String,
        recommendations: List<Recommendation>
    ): List<Recommendation> {
        return try {
            if (recommendations.isEmpty()) return recommendations

            val allInteractions = loadAllUserItemInteractions()
            Log.d("CF_TEST", "Interactions size = ${allInteractions.size}")
            if (allInteractions.isEmpty()) return recommendations

            val userItemMatrix = buildUserItemMatrix(allInteractions)
            val targetUserVector = userItemMatrix[userId].orEmpty()
            Log.d("CF_TEST", "Target user vector size = ${targetUserVector.size}")

            if (targetUserVector.isEmpty()) return recommendations

            val candidateKeys = recommendations.associateBy { recommendationKey(it) }.keys

            val userBasedScores = computeUserBasedScores(
                targetUserId = userId,
                userItemMatrix = userItemMatrix,
                candidateKeys = candidateKeys
            )

            val itemBasedScores = computeItemBasedScores(
                targetUserId = userId,
                userItemMatrix = userItemMatrix,
                candidateKeys = candidateKeys
            )
            Log.d("CF_TEST", "User-based scores = $userBasedScores")
            Log.d("CF_TEST", "Item-based scores = $itemBasedScores")

            recommendations
                .map { rec ->
                    val key = recommendationKey(rec)
                    val userScore = userBasedScores[key] ?: 0.0
                    val itemScore = itemBasedScores[key] ?: 0.0

                    // Give slightly more weight to user-based similarity for a clearer demo effect.
                    val collaborativeScore = (0.6 * userScore) + (0.4 * itemScore)

                    val cfReason = when {
                        userScore > 0.0 && itemScore > 0.0 ->
                            "Recommended because similar users liked it and it matches content you explored."
                        userScore > 0.0 ->
                            "Recommended because children with similar interests liked it."
                        itemScore > 0.0 ->
                            "Recommended because it is similar to content you already liked."
                        else -> rec.reason
                    }

                    rec.copy(
                        relevanceScore = collaborativeScore,
                        reason = if (rec.reason.isBlank()) cfReason else rec.reason
                    )
                }
                .sortedByDescending { it.relevanceScore }
        } catch (e: Exception) {
            Log.e("ChatDataManager", "Collaborative filtering ranking failed: ${e.message}", e)
            recommendations
        }
    }

    private suspend fun loadAllUserItemInteractions(): List<UserItemInteraction> {
        val interactions = mutableListOf<UserItemInteraction>()

        try {
            val favoriteItems = firestore.collectionGroup("items").get().await()
            Log.d("CF_TEST", "Favorites collectionGroup raw count = ${favoriteItems.size()}")

            for (doc in favoriteItems.documents) {
                val path = doc.reference.path
                val segments = path.split("/")

                // Only use favorites/{userId}/items/{itemId}
                if (!(segments.size >= 4 && segments[0] == "favorites" && segments[2] == "items")) {
                    continue
                }

                Log.d("CF_TEST", "Favorite doc path = $path")

                val userId = segments[1]
                val title = doc.getString("title")
                    ?: doc.getString("name")
                    ?: doc.getString("bookTitle")
                    ?: doc.getString("videoTitle")
                    ?: doc.id

                val key = normalizeTitle(title)
                if (key.isNotBlank()) {
                    interactions.add(
                        UserItemInteraction(
                            userId = userId,
                            itemKey = key,
                            weight = 3.0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CF_TEST", "Could not load favorites interactions: ${e.message}", e)
        }

        try {
            val historySessions = firestore.collectionGroup("sessions").get().await()
            Log.d("CF_TEST", "Reading history collectionGroup raw count = ${historySessions.size()}")

            for (doc in historySessions.documents) {
                val path = doc.reference.path
                val segments = path.split("/")

                // Only use readingHistory/{userId}/sessions/{sessionId}
                if (!(segments.size >= 4 && segments[0] == "readingHistory" && segments[2] == "sessions")) {
                    continue
                }

                Log.d("CF_TEST", "History doc path = $path")

                val userId = segments[1]
                val title = doc.getString("title")
                    ?: doc.getString("name")
                    ?: doc.getString("bookTitle")
                    ?: doc.getString("videoTitle")
                    ?: doc.id

                val completed = doc.getBoolean("completed") ?: false
                val weight = if (completed) 2.5 else 1.5

                val key = normalizeTitle(title)
                if (key.isNotBlank()) {
                    interactions.add(
                        UserItemInteraction(
                            userId = userId,
                            itemKey = key,
                            weight = weight
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CF_TEST", "Could not load reading history interactions: ${e.message}", e)
        }

        val merged = mergeDuplicateInteractions(interactions)
        Log.d("CF_TEST", "Total interactions loaded = ${merged.size}")
        return merged
    }

    private fun mergeDuplicateInteractions(
        interactions: List<UserItemInteraction>
    ): List<UserItemInteraction> {
        return interactions
            .groupBy { "${it.userId}::${it.itemKey}" }
            .map { (_, grouped) ->
                UserItemInteraction(
                    userId = grouped.first().userId,
                    itemKey = grouped.first().itemKey,
                    weight = grouped.sumOf { it.weight }
                )
            }
    }

    private fun buildUserItemMatrix(
        interactions: List<UserItemInteraction>
    ): Map<String, Map<String, Double>> {
        return interactions
            .groupBy { it.userId }
            .mapValues { (_, values) ->
                values.associate { it.itemKey to it.weight }
            }
    }

    private fun computeUserBasedScores(
        targetUserId: String,
        userItemMatrix: Map<String, Map<String, Double>>,
        candidateKeys: Set<String>
    ): Map<String, Double> {
        val targetVector = userItemMatrix[targetUserId].orEmpty()
        if (targetVector.isEmpty()) return emptyMap()

        val weightedScores = mutableMapOf<String, Double>()
        val similaritySums = mutableMapOf<String, Double>()

        for ((otherUserId, otherVector) in userItemMatrix) {
            if (otherUserId == targetUserId) continue

            val similarity = cosineSimilarity(targetVector, otherVector)
            if (similarity <= 0.0) continue

            for ((itemKey, weight) in otherVector) {
                if (itemKey !in candidateKeys) continue
                if (itemKey in targetVector.keys) continue

                weightedScores[itemKey] = (weightedScores[itemKey] ?: 0.0) + (similarity * weight)
                similaritySums[itemKey] = (similaritySums[itemKey] ?: 0.0) + similarity
            }
        }

        return weightedScores.mapValues { (itemKey, score) ->
            val denom = similaritySums[itemKey] ?: 1.0
            if (denom == 0.0) 0.0 else score / denom
        }
    }

    private fun computeItemBasedScores(
        targetUserId: String,
        userItemMatrix: Map<String, Map<String, Double>>,
        candidateKeys: Set<String>
    ): Map<String, Double> {
        val targetVector = userItemMatrix[targetUserId].orEmpty()
        if (targetVector.isEmpty()) return emptyMap()

        val itemUserMatrix = buildItemUserMatrix(userItemMatrix)
        val scores = mutableMapOf<String, Double>()
        val similaritySums = mutableMapOf<String, Double>()

        for (candidateKey in candidateKeys) {
            val candidateVector = itemUserMatrix[candidateKey] ?: continue

            for ((seenItemKey, seenWeight) in targetVector) {
                val seenVector = itemUserMatrix[seenItemKey] ?: continue

                val similarity = cosineSimilarity(candidateVector, seenVector)
                if (similarity <= 0.0) continue

                scores[candidateKey] = (scores[candidateKey] ?: 0.0) + (similarity * seenWeight)
                similaritySums[candidateKey] = (similaritySums[candidateKey] ?: 0.0) + similarity
            }
        }

        return scores.mapValues { (itemKey, score) ->
            val denom = similaritySums[itemKey] ?: 1.0
            if (denom == 0.0) 0.0 else score / denom
        }
    }

    private fun buildItemUserMatrix(
        userItemMatrix: Map<String, Map<String, Double>>
    ): Map<String, Map<String, Double>> {
        val itemUserMatrix = mutableMapOf<String, MutableMap<String, Double>>()

        for ((userId, itemMap) in userItemMatrix) {
            for ((itemKey, weight) in itemMap) {
                val userWeights = itemUserMatrix.getOrPut(itemKey) { mutableMapOf() }
                userWeights[userId] = weight
            }
        }

        return itemUserMatrix
    }

    private fun cosineSimilarity(
        a: Map<String, Double>,
        b: Map<String, Double>
    ): Double {
        val commonKeys = a.keys.intersect(b.keys)
        if (commonKeys.isEmpty()) return 0.0

        val dotProduct = commonKeys.sumOf { key ->
            (a[key] ?: 0.0) * (b[key] ?: 0.0)
        }

        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })

        if (normA == 0.0 || normB == 0.0) return 0.0

        return dotProduct / (normA * normB)
    }

    private fun recommendationKey(recommendation: Recommendation): String {
        return normalizeTitle(recommendation.title)
    }

    private suspend fun scoreWithANN(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        userId: String
    ): List<Recommendation> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: return recommendations
            val favorites = favoritesManager.getFavorites(userId)

            val maxCfScore = recommendations.maxOfOrNull { it.relevanceScore }?.takeIf { it > 0.0 } ?: 1.0

            recommendations.map { rec ->
                val matchingBook = curatedBooks.firstOrNull { book ->
                    titlesMatch(book.title, rec.title)
                }

                val annScore = if (matchingBook != null) {
                    recommendationEngine.scoreBook(matchingBook, user, favorites)
                } else {
                    recommendationEngine.scoreRecommendation(rec, user, favorites)
                }

                val cfNormalized = (rec.relevanceScore / maxCfScore).coerceIn(0.0, 1.0)

                // Important: combine ANN + CF instead of overwriting CF.
                val finalScore = ((0.45 * annScore) + (0.55 * cfNormalized)).coerceIn(0.0, 1.0)

                val updatedReason = when {
                    cfNormalized >= 0.6 && rec.reason.isNotBlank() ->
                        "${rec.reason} Similar users also engaged with this."
                    cfNormalized >= 0.6 ->
                        "Recommended because similar users liked it and it matches your interests."
                    else -> rec.reason
                }

                rec.copy(
                    relevanceScore = finalScore,
                    reason = updatedReason
                )
            }.sortedByDescending { it.relevanceScore }
        } catch (e: Exception) {
            recommendations
        }
    }
}