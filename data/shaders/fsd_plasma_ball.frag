uniform vec4 u_baseColor;
uniform float u_time;
uniform float u_useInstanceTime;

varying vec2 v_texCoord;
varying vec4 v_color;
varying float v_particleTime;
varying float v_particleType;

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float basic_noise(vec2 co){
    vec2 i = floor(co);
    vec2 f = fract(co);
    vec2 u = f*f*(3.0-2.0*f);
    return mix(mix(rand(i + vec2(0.0,0.0)),
                 rand(i + vec2(1.0,0.0)), u.x),
               mix(rand(i + vec2(0.0,1.0)),
                 rand(i + vec2(1.0,1.0)), u.x), u.y);
}

void main() {
    vec2 uv = v_texCoord - 0.5;
    float dist = length(uv);
    float time = u_useInstanceTime > 0.5 ? v_particleTime * 2.0 + u_time : u_time;
    
    if (v_particleType < 0.5) {
        float core_radius = 0.25;
        float plasma_outer_radius = 0.45;
        float core_darkness = 0.4;
        
        float noise_val = basic_noise(uv * 2.0 + vec2(time * 0.2) * 0.2);
        
        vec3 final_color = vec3(0.9, 0.05, 0.05);
        if (u_useInstanceTime > 0.5) {
            final_color = vec3(v_color.r, v_color.g * 0.2, v_color.b * 0.2);
        }
        
        float alpha = u_useInstanceTime > 0.5 ? v_color.a : u_baseColor.a;
        alpha *= (1.0 - smoothstep(plasma_outer_radius - 0.05, 0.5, dist));
        
        float core_mix_factor = smoothstep(core_radius * 0.8, core_radius * 1.2, dist);
        vec3 dark_core_color = vec3(0.4, 0.01, 0.01);
        vec3 core_color = mix(dark_core_color, final_color, core_mix_factor);
        final_color = mix(core_color, final_color, smoothstep(core_radius * 0.85, core_radius * 1.1, dist));
        
        float life_fade = 1.0;
        if (v_particleTime < 0.1) {
            life_fade = v_particleTime / 0.1;
        } else if (v_particleTime > 0.9) {
            life_fade = 1.0 - (v_particleTime - 0.9) / 0.1;
        }
        
        alpha *= life_fade;
        
        gl_FragColor = vec4(final_color, clamp(alpha, 0.0, 1.0));
    } else {
        float ellipse = 1.0 - smoothstep(0.0, 1.0, length(vec2(uv.x, uv.y * 2.0)) * 2.0);
        float final_alpha = ellipse * v_color.a;
        
        float r = v_color.r;
        float g = v_color.g * 0.1;
        float b = v_color.b * 0.1;
        
        gl_FragColor = vec4(r, g, b, final_alpha);
    }
}