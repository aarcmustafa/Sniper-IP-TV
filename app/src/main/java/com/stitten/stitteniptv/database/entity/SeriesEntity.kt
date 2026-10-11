package com.stitten.stitteniptv.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "series",
    indices = [
        Index("category"),
        Index("name"),
        Index("addedAt"),
        Index("sourceId")
    ]
)
data class SeriesEntity(
    @PrimaryKey val id: String,
    val name: String,
    val poster: String,
    val category: String,
    val sourceId: Long = 0,

    // ═══════════════════════════════════════════════════════════
    //  ✅ حقول جديدة
    // ═══════════════════════════════════════════════════════════

    /** صورة عريضة (16:9) */
    val backdrop: String? = null,

    /** قصة المسلسل */
    val plot: String? = null,

    /** التقييم */
    val rating: String? = null,

    /** سنة الإصدار */
    val year: String? = null,

    /** التصنيف (Action, Drama, ...) */
    val genre: String? = null,

    /** الممثلون — اسم العمود: cast_list */
    @ColumnInfo(name = "cast_list")
    val cast: String? = null,

    /** المخرج */
    val director: String? = null,

    /** عدد الحلقات */
    val episodeCount: Int = 0,

    /** عدد المواسم */
    val seasonCount: Int = 0,

    /** تاريخ الإضافة (Unix ms) */
    val addedAt: Long = 0L,
)
