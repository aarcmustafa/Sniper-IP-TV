package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity

// ═══════════════════════════════════════════════════════════════
//  Channel
// ═══════════════════════════════════════════════════════════════

fun ChannelEntity.toModel(): Channel = Channel(
    id = id,
    name = name,
    logo = logo,
    url = url,
    group = groupName,
)

fun Channel.toEntity(sourceId: Long, priority: Int = 4): ChannelEntity = ChannelEntity(
    id = "${sourceId}_$id",
    name = name,
    logo = logo,
    url = url,
    groupName = group,
    groupPriority = priority,
    sourceId = sourceId,
)

// ═══════════════════════════════════════════════════════════════
//  Movie
// ═══════════════════════════════════════════════════════════════

fun MovieEntity.toModel(): Movie = Movie(
    id = id,
    name = name,
    poster = poster,
    url = url,
    category = category,
    backdrop = backdrop,
    plot = plot,
    rating = rating,
    year = year,
    duration = duration,
    genre = genre,
    cast = cast,
    director = director,
    containerExtension = containerExtension,
    addedAt = addedAt,
    sourceId = sourceId,
)

fun Movie.toEntity(sourceId: Long): MovieEntity = MovieEntity(
    id = if (id.startsWith("${sourceId}_")) id else "${sourceId}_$id",
    name = name,
    poster = poster,
    url = url,
    category = category,
    sourceId = sourceId,
    backdrop = backdrop,
    plot = plot,
    rating = rating,
    year = year,
    duration = duration,
    genre = genre,
    cast = cast,
    director = director,
    containerExtension = containerExtension,
    addedAt = if (addedAt > 0L) addedAt else System.currentTimeMillis(),
)

// ═══════════════════════════════════════════════════════════════
//  Series
// ═══════════════════════════════════════════════════════════════

fun SeriesEntity.toModel(): Series = Series(
    id = id,
    name = name,
    poster = poster,
    category = category,
    backdrop = backdrop,
    plot = plot,
    rating = rating,
    year = year,
    genre = genre,
    cast = cast,
    director = director,
    episodeCount = episodeCount,
    seasonCount = seasonCount,
    addedAt = addedAt,
    sourceId = sourceId,
)

fun Series.toEntity(sourceId: Long): SeriesEntity = SeriesEntity(
    id = if (id.startsWith("${sourceId}_")) id else "${sourceId}_$id",
    name = name,
    poster = poster,
    category = category,
    sourceId = sourceId,
    backdrop = backdrop,
    plot = plot,
    rating = rating,
    year = year,
    genre = genre,
    cast = cast,
    director = director,
    episodeCount = episodeCount,
    seasonCount = seasonCount,
    addedAt = if (addedAt > 0L) addedAt else System.currentTimeMillis(),
)
