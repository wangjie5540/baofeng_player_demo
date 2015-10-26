attribute vec3 vPosition;
attribute vec2 vTexCoordinate;
uniform mat4 textureTransform;
uniform mat4 vertexTransform;
varying vec2 v_TexCoordinate;

void main () {
    v_TexCoordinate = (textureTransform * vec4(vTexCoordinate, 0, 1.0)).xy;
    gl_Position = vertexTransform * vec4(vPosition, 1.0);
}