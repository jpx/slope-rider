attribute vec2 a_position;
attribute vec2 a_uv;

uniform mat4 u_modelToWorldMatrix;
uniform mat4 u_worldToScreenMatrix;

varying vec2 v_uv;

void main()
{
    v_uv = a_uv;

    vec4 position = vec4(a_position, 0.0, 1.0);

    gl_Position = u_worldToScreenMatrix * u_modelToWorldMatrix * position;
}
