package com.stitten.stitteniptv.data

object CategoriesManager {

    private val ARAB_SPORTS_KEYWORDS = listOf(
        "bein", "be in", "ssc", "alkass", "الكأس", "ad sport",
        "دوري", "الدوري", "كورة", "كرة قدم", "كرة",
        "مباريات", "بث مباشر", "رياضة", "رياضي", "رياضية",
        "الرياضية", "أبو ظبي الرياضية", "دبي الرياضية",
        "الكويت الرياضية", "السعودية الرياضية",
        "dawri", "dawry",
        "alwan", "alwan tv", "قناة ألوان", "ألوان"
    )

    private val WORLD_SPORTS_KEYWORDS = listOf(
        "sport", "sports", "football", "soccer",
        "nba", "nfl", "ufc", "f1", "formula", "motogp",
        "match", "premier", "laliga", "serie a",
        "champions", "world cup", "euro",
        "sky sport", "espn", "fox sport", "eurosport",
        "tennis", "golf", "hockey"
    )

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

    private val ARABIC_GENERAL_KEYWORDS = listOf(
        "عربي", "عربية", "arabic", "arab",
        "mbc", "rotana", "روتانا",
        "نايل", "nile",
        "aljazeera", "الجزيرة",
        "noor", "نور", "zaman", "زمان",
        "الأقصى", "aqsa", "خليجي", "gulf"
    )

    fun extractChannelCategories(channels: List<Channel>): List<String> =
        channels.map { it.group }.filter { it.isNotBlank() }.distinct()

    fun extractMovieCategories(movies: List<Movie>): List<String> =
        movies.map { it.category }.filter { it.isNotBlank() }.distinct()

    fun extractSeriesCategories(series: List<Series>): List<String> =
        series.map { it.category }.filter { it.isNotBlank() }.distinct()

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        val lower = text.lowercase().trim()
        return keywords.any { lower.contains(it.lowercase()) }
    }

    fun isArabSports(category: String): Boolean = containsAny(category, ARAB_SPORTS_KEYWORDS)

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

    // ============== الأولوية (1-4) ==============
    /**
     * 1 = رياضية عربية
     * 2 = دول عربية
     * 3 = رياضية عالمية
     * 4 = باقي القنوات
     */
    fun getPriorityOrder(category: String): Int {
        return when {
            isArabSports(category) -> 1
            isArabCountry(category) || isArabicGeneral(category) -> 2
            isWorldSports(category) -> 3
            else -> 4
        }
    }

    fun detectTypeFromChannelNames(channelNames: List<String>): String {
        if (channelNames.isEmpty()) return ""
        var arabSports = 0
        var worldSports = 0
        var arabCountry = 0
        var arabicGeneral = 0

        channelNames.forEach { name ->
            when {
                isArabSports(name) -> arabSports++
                isArabCountry(name) -> arabCountry++
                isWorldSports(name) -> worldSports++
                isArabicGeneral(name) -> arabicGeneral++
            }
        }

        val max = maxOf(arabSports, worldSports, arabCountry, arabicGeneral)
        val threshold = (channelNames.size * 30) / 100
        if (max < threshold || max == 0) return ""

        return when (max) {
            arabSports -> "رياضية عربية"
            arabCountry -> "قنوات عربية"
            worldSports -> "رياضية عالمية"
            arabicGeneral -> "عربية"
            else -> ""
        }
    }

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

        return arabSports.sorted() + arabCountries.sorted() +
                worldSports.sorted() + arabicGeneral.sorted() + others.sorted()
    }
}
