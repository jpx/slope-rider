#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_size;
uniform vec2 u_anchorPosition;
uniform float u_limit;
uniform vec2 u_limitMask;

varying vec2 v_uv;

#define OUTLINE_SIZE 0.2

void main()
{
    vec4 diffuse = vec4(0.0);

    float distanceFromAnchor = distance(u_anchorPosition * u_limitMask, v_uv * u_limitMask);
    float distanceFromLimit = distanceFromAnchor - u_limit;

    diffuse += mix(
        vec4(0.0, 0.7, 0.0, 0.0),
        vec4(1.0, 0.1, 0.1, 0.0),
        distanceFromLimit / 0.1
    );

    diffuse += mix(
        vec4(0.0, 0.0, 0.0, 0.4),
        vec4(0.0, 0.0, 0.0, 0.0),
        OUTLINE_SIZE / u_size.x - v_uv.x
    );

    if (v_uv.x < OUTLINE_SIZE / u_size.x || v_uv.x > 1.0 - OUTLINE_SIZE / u_size.x ||
        v_uv.y < OUTLINE_SIZE / u_size.y || v_uv.y > 1.0 - OUTLINE_SIZE / u_size.y)
    {
//        diffuse = vec4(vec3(0.0), 0.6);
    }
    else
    {
//        diffuse.a = 0.3;
    }

    gl_FragColor = diffuse;
}
