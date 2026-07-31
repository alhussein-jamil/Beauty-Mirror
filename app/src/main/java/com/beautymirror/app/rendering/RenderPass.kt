package com.beautymirror.app.rendering

interface RenderPass {
    val name: String
    var enabled: Boolean
    fun resize(width: Int, height: Int)
    fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh)
    fun release()
}
