#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;

varying vec2 v_uv;

#define PI_2 1.570796

void main()
{
    vec3 diffuse = vec3(0.0);

    diffuse.r = v_uv.x;
    diffuse.b = v_uv.y;

    float distance = distance(vec2(0.5, 0.5), v_uv);

    if (distance > 0.5)
        discard;

    float thresholdDistance = 0.4;

    if (distance > thresholdDistance)
    {
        diffuse = vec3(0.0);
    }

    gl_FragColor = vec4(diffuse, 1.0);
}
