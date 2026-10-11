package com.stitten.stitteniptv.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movies",
    indices = [
        Index("category"),
        Index("name"),
        Index("addedAt"),
        Index("sourceId")
    ]
)
data class MovieEntity(
    @PrimaryKey val id: String,
    val name: String,
    val poster: String,
    val url: String,
    val category: String,
    val sourceId: Long = 0,

    // ═══════════════════════════════════════════════════════════
    //  ✅ حقول جديدة (كلها nullable لتوافق آمن مع البيانات القديمة)
    // ═══════════════════════════════════════════════════════════

    /** صورة عريضة (16:9) للـ Hero Banner */
    val backdrop: String? = null,

    /** قصة الفيلم */
    val plot: String? = null,

    /** التقييم (مثل "8.5") */
    val rating: String? = null,

    /** سنة الإصدار (مثل "2023") */
    val year: String? = null,

    /** المدة (مثل "2h 15m") */
    val duration: String? = null,

    /** التصنيف (Action, Drama, ...) */
    val genre: String? = null,

    /** الممثلون — اسم العمود في SQLite: cast_list (لأن cast كلمة محجوزة) */
    @ColumnInfo(name = "cast_list")
    val cast: String? = null,

    /** المخرج */
    val director: String? = null,

    /** امتداد البث (mp4, mkv, ...) */
    val containerExtension: String? = null,

    /** تاريخ الإضافة (Unix ms) — لترتيب "أحدث الأفلام" */
    val addedAt: Long = 0L,
)
