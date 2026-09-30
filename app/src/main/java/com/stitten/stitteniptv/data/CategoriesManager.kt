package com.stitten.stitteniptv.data

object CategoriesManager {

    // ============== كلمات الرياضة العربية ==============
    private val ARAB_SPORTS_KEYWORDS = listOf(
        "bein", "be in", "ssc", "alkass", "الكأس", "ad sport",
        "دوري", "الدوري", "كورة", "كرة قدم", "كرة",
        "مباريات", "بث مباشر", "رياضة", "رياضي", "رياضية",
        "الرياضية", "أبو ظبي الرياضية", "دبي الرياضية",
        "الكويت الرياضية", "السعودية الرياضية",
        "dawri", "dawry"
    )

    // ============== كلمات الرياضة العالمية ==============
    private val WORLD_SPORTS_KEYWORDS = listOf(
        "sport", "sports", "football", "soccer",
        "nba", "nfl", "ufc", "f1", "formula", "motogp",
        "match", "premier", "laliga", "serie a",
        "champions", "world cup", "euro",
        "sky sport", "espn", "fox sport", "eurosport",
        "tennis", "golf", "hockey"
    )

    // ============== الدول العربية ==============
    private val ARAB_COUNTRIES_KEYWORDS = listOf(
        "مصر", "egypt", "مصرية",
        "السعودية", "saudi", "سعودي", "ksa",
        "الإمارات", "امارات", "emirates", "uae",
        "دبي", "dubai", "أبو ظبي", "abu dhabi",
        "قطر", "qatar", "قطري",
        "الكويت", "kuwait", "كويتي",
        "البحرين", "bahrain", "بحريني",
        "عمان", "oman", "عماني",
        "الأردن", "jordan", "أردني",
        "لبنان", "lebanon", "لبناني",
        "سوريا", "syria", "سوري",
        "العراق", "iraq", "عراقي",
        "فلسطين", "palestine", "فلسطيني",
        "اليمن", "yemen", "يمني",
        "ليبيا", "libya", "ليبي",
        "تونس", "tunisia", "تونسي",
        "الجزائر", "algeria", "جزائري",
        "المغرب", "morocco", "مغربي",
        "السودان", "sudan", "سوداني",
        "موريتانيا", "mauritania",
        "الصومال", "somalia",
        "جيبوتي", "djibouti",
        "جزر القمر", "comoros"
    )

    // ============== عربية عامة ==============
    private val ARABIC_GENERAL_KEYWORDS = listOf(
        "عربي", "عربية", "arabic", "arab",
        "mbc", "rotana", "روتانا",
        "نايل", "nile",
        "aljazeera", "الجزيرة",
        "noor", "نور", "zaman", "زمان",
        "الأقصى", "aqsa",
        "خليجي", "gulf"
    )

    // ============== استخراج التصنيفات ==============
    fun extractChannelCategories(channels: List<Channel>): List<String> =
        channels.map { it.group }
            .filter { it.isNotBlank() }
            .distinct()

    fun extractMovieCategories(movies: List<Movie>): List<String> =
        movies.map { it.category }
            .filter { it.isNotBlank() }
            .distinct()

    fun extractSeriesCategories(series: List<Series>): List<String> =
        series.map { it.category }
            .filter { it.isNotBlank() }
            .distinct()

    // ============== كشف الفئات ==============
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        val lower = text.lowercase().trim()
        return keywords.any { lower.contains(it.lowercase()) }
    }

    fun isArabSports(category: String): Boolean =
        containsAny(category, ARAB_SPORTS_KEYWORDS)

    fun isWorldSports(category: String): Boolean {
        if (isArabSports(category)) return false
        return containsAny(category, WORLD_SPORTS_KEYWORDS)
    }

    fun isArabCountry(category: String): Boolean {
        if (isArabSports(category)) return false
        return containsAny(category, ARAB_COUNTRIES_KEYWORDS)
    }

    fun isArabicGeneral(category: String): Boolean {
        if (isArabSports(category)) return false
        if (isArabCountry(category)) return false
        return containsAny(category, ARABIC_GENERAL_KEYWORDS)
    }

    // ============== الترتيب بالأولوية ==============
    /**
     * ترتيب التصنيفات حسب الأولوية:
     * 1. الرياضية العربية (beIN, SSC, الكأس)
     * 2. قنوات الدول العربية (مصر، السعودية، قطر...)
     * 3. الرياضية العالمية (NBA, Premier League)
     * 4. العربية العامة (MBC, Rotana)
     * 5. باقي القنوات (باقة X, أخبار, أفلام)
     */
    fun sortCategories(categories: List<String>): List<String> {
        val arabSports = mutableListOf<String>()
        val arabCountries = mutableListOf<String>()
        val worldSports = mutableListOf<String>()
        val arabicGeneral = mutableListOf<String>()
        val others = mutableListOf<String>()

        categories.forEach { cat ->
            when {
                isArabSports(cat) -> arabSports.add(cat)
                isArabCountry(cat) -> arabCountries.add(cat)
                isWorldSports(cat) -> worldSports.add(cat)
                isArabicGeneral(cat) -> arabicGeneral.add(cat)
                else -> others.add(cat)
            }
        }

        return arabSports.sorted() +
                arabCountries.sorted() +
                worldSports.sorted() +
                arabicGeneral.sorted() +
                others.sorted()
    }
}
