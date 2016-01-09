#ifdef GL_ES
precision mediump float;
#endif

uniform float u_time;
uniform vec4 u_diffuseColor;
uniform sampler2D u_diffuseMap;
uniform sampler2D u_maskMap;

varying vec2 v_uv;

#define PI_2 1.570796

void main()
{
    vec4 diffuse = u_diffuseColor;

    vec4 mask = texture2D(u_maskMap, v_uv);

    if (mask.r <= 0.1 && mask.g <= 0.1)
        discard;

    if (mask.r > 0.1)
    {
        float scale = 0.5;
        vec2 uv = v_uv * (2.0 - scale) - vec2(0.5 * (1.0 - scale));
        vec4 diffuseMapSample = texture2D(u_diffuseMap, uv);

        if (uv.x >= 0.0 && uv.y >= 0.0 &&
            uv.x <= 1.0 && uv.y <= 1.0 &&
            diffuseMapSample.a > 0.1)
            diffuse = diffuseMapSample;

        diffuse *= mask.r;

//        diffuse.rgb += mix(
//            vec3(0.0),
//            vec3(0.5, 0.5, 0.5),
//            distance(v_uv.y, abs(sin(1.0 / 2.0 + u_time * PI_2 * 4.0))) * 1.0
//        );
    }
    else if (mask.g > 0.1)
    {
        diffuse = vec4(vec3(0.0), 0.6);
        diffuse *= mask.g;
    }

    gl_FragColor = diffuse;
}
