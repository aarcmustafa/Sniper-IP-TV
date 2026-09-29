package com.stitten.stitteniptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long
) {
    fun isNow(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }
}

object EpgParser {

    suspend fun loadFromUrl(url: String): List<EpgProgram> = withContext(Dispatchers.IO) {
        try {
            val xml = URL(url).readText()
            parse(xml)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parse(xml: String): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var channelId = ""
            var title = ""
            var desc = ""
            var start = 0L
            var end = 0L
            var currentTag = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "programme") {
                            channelId = parser.getAttributeValue(null, "channel") ?: ""
                            start = parseXmltvDate(parser.getAttributeValue(null, "start"))
                            end = parseXmltvDate(parser.getAttributeValue(null, "stop"))
                            title = ""
                            desc = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        when (currentTag) {
                            "title" -> title += parser.text
                            "desc" -> desc += parser.text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && channelId.isNotEmpty()) {
                            programs.add(
                                EpgProgram(channelId, title.trim(), desc.trim(), start, end)
                            )
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) { }
        return programs
    }

    private fun parseXmltvDate(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        return try {
            val fmt = java.text.SimpleDateFormat("yyyyMMddHHmmss Z", java.util.Locale.US)
            fmt.parse(s.trim())?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
