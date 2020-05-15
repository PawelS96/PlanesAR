package com.pollub.samoloty.render

import java.nio.Buffer

abstract class Model {

    abstract fun getVertices(): Buffer

    abstract fun getTexCoords(): Buffer

   // abstract fun getNormals(): Buffer

   // abstract fun getIndices(): Buffer

    abstract fun getVertexCount(): Int
}
