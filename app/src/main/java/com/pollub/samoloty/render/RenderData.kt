package com.pollub.samoloty.render

data class RenderData(
        val targetName: String,
        val model: Model,
        val texture: Texture,
        val scale: Float = 1f,
        val rotation: Int = 0,
        val vertexMultiplier: Float = 5f
)
