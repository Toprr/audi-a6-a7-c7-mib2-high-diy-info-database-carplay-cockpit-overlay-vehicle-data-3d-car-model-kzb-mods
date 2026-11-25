// PC test of speed_overlay.h 0.3 with GL stubbed out (run in a Linux container):
// positions of both boxes, no flicker while VehLog rewrites a file, stale and off
// handling, the fuel line rules, and RGBA dumps of the boxes for a preview.
#include <stdint.h>
#include <stdio.h>
#include <utime.h>
#include <math.h>
typedef unsigned int GLuint; typedef int GLint; typedef unsigned int GLenum; typedef float GLfloat; typedef unsigned char GLboolean;
#define GL_VERTEX_SHADER 1
#define GL_FRAGMENT_SHADER 2
#define GL_COMPILE_STATUS 3
#define GL_LINK_STATUS 4
#define GL_TEXTURE3 5
#define GL_TEXTURE0 6
#define GL_TEXTURE_2D 7
#define GL_TEXTURE_MIN_FILTER 8
#define GL_TEXTURE_MAG_FILTER 9
#define GL_LINEAR 10
#define GL_TEXTURE_WRAP_S 11
#define GL_TEXTURE_WRAP_T 12
#define GL_CLAMP_TO_EDGE 13
#define GL_RGBA 14
#define GL_UNSIGNED_BYTE 15
#define GL_BLEND 16
#define GL_SRC_ALPHA 17
#define GL_ONE_MINUS_SRC_ALPHA 18
#define GL_FLOAT 19
#define GL_FALSE 0
#define GL_TRIANGLE_FAN 20
static GLuint g_bound = 0; static int g_uploads = 0;
struct Draw { GLuint tex; float x0, y0, x1, y1; };
static Draw g_draws[8]; static int g_n = 0; static float g_cur[8];
static void glActiveTexture(GLenum){} static void glBindTexture(GLenum, GLuint t){ g_bound = t; }
static void glTexImage2D(GLenum, GLint, GLint, int, int, GLint, GLenum, GLenum, const void*){ g_uploads++; }
#define LOG(...) do{ printf("  log: " __VA_ARGS__); printf("\n"); }while(0)
#define LOGE(...) LOG(__VA_ARGS__)
static GLuint glCreateShader(GLenum){return 1;} static void glShaderSource(GLuint,int,const char**,const GLint*){}
static void glCompileShader(GLuint){} static void glGetShaderiv(GLuint,GLenum,GLint* v){*v=1;} static void glDeleteShader(GLuint){}
static GLuint glCreateProgram(){return 1;} static void glAttachShader(GLuint,GLuint){} static void glLinkProgram(GLuint){}
static void glGetProgramiv(GLuint,GLenum,GLint* v){*v=1;} static GLint glGetAttribLocation(GLuint,const char* n){return n[0]=='p'?0:1;}
static GLint glGetUniformLocation(GLuint,const char*){return 0;} static GLuint g_tex = 10; static void glGenTextures(int,GLuint* t){*t=++g_tex;}
static void glTexParameteri(GLenum,GLenum,GLint){} static void glUseProgram(GLuint){} static void glUniform1i(GLint,GLint){}
static void glEnable(GLenum){} static void glDisable(GLenum){} static void glBlendFunc(GLenum,GLenum){}
static void glVertexAttribPointer(GLint i,int,GLenum,GLboolean,int,const void* p){ if(i==0) for(int k=0;k<8;k++) g_cur[k]=((const float*)p)[k]; }
static void glEnableVertexAttribArray(GLint){} static void glDisableVertexAttribArray(GLint){}
static void glDrawArrays(GLenum,int,int){ Draw& d = g_draws[g_n++]; d.tex = g_bound;
    d.x0 = roundf((g_cur[0] + 1) / 2 * 800); d.x1 = roundf((g_cur[2] + 1) / 2 * 800);
    d.y0 = roundf((1 - g_cur[1]) / 2 * 298); d.y1 = roundf((1 - g_cur[5]) / 2 * 298); }
#include "../player/speed_overlay.h"

static int fails = 0;
static uint64_t t_us = 1;
static void put(const char* path, const char* s) { FILE* f = fopen(path, "w"); fputs(s, f); fclose(f); }
static void frame() { g_n = 0; t_us += 300000; speed_overlay_draw(800, 298, t_us); }
static const Draw* find(GLuint tex) { for (int i = 0; i < g_n; i++) if (g_draws[i].tex == tex) return &g_draws[i]; return NULL; }
static void check(const char* what, bool ok) { printf("%s %s\n", ok ? "ok  " : "FAIL", what); if (!ok) fails++; }
static void dump(const char* name) { char n[96]; snprintf(n, sizeof n, "/out/%s.rgba", name); FILE* f = fopen(n, "wb"); fwrite(so_px, 1, sizeof so_px, f); fclose(f); }

// Simulated VehLog 0.8: writes value(s) plus a counter, like the car does.
static long seq_s = 0, seq_f = 0;
static void vl_speed(int v) { char b[32]; snprintf(b, sizeof b, "%d %ld\n", v, ++seq_s); put(SO_FILE, b); }
static void vl_fuel(int c, int l) { char b[48]; snprintf(b, sizeof b, "%d %d %ld\n", c, l, ++seq_f); put(SO_FUEL_FILE, b); }
static void age(const char* path) { struct utimbuf old = { time(NULL) - 3600, time(NULL) - 3600 }; utime(path, &old); }

int main(int argc, char** argv) {
    remove(SO_OFF);
    if (argc > 1 && strcmp(argv[1], "conf") == 0) {
        put(SO_CONF, "pos=bl\r\nleft=180\r\nbottom=20\r\nfuelright=210\r\nalpha=230\r\n");
        vl_speed(52); vl_fuel(87, 58);
        frame();
        const Draw* s = find(so_tex); const Draw* f = find(so_ftex);
        check("conf: speed box at left 180, bottom 20", s && s->x0 == 180 && s->y1 == 278);
        check("conf: fuel box 210 from the right", f && f->x1 == 590 && f->y1 == 278);
        check("conf: alpha=230 used for the box", so_px[(1 * SO_TEX_W + so_bw / 2) * 4 + 3] == 230);
        return fails;
    }
    if (argc > 1 && strcmp(argv[1], "badalpha") == 0) {
        put(SO_CONF, "alpha=10\n");
        vl_speed(52); vl_fuel(87, 58);
        frame();
        check("conf: alpha=10 (too low) ignored, default 179", so_alpha == 179 && so_px[(1 * SO_TEX_W + so_bw / 2) * 4 + 3] == 179);
        return fails;
    }
    if (argc > 1 && strcmp(argv[1], "fueloff") == 0) {
        put(SO_CONF, "fuel=off\n");
        vl_speed(52); vl_fuel(87, 58);
        frame();
        check("fuel=off: only the speed box", g_n == 1 && find(so_tex));
        return fails;
    }
    if (argc > 1 && strcmp(argv[1], "oldformat") == 0) {
        remove(SO_CONF);
        put(SO_FILE, "52\n"); put(SO_FUEL_FILE, "87 58\n");
        frame();
        check("old format without counter, fresh file time: both shown", find(so_tex) && find(so_ftex));
        age(SO_FILE); age(SO_FUEL_FILE);
        for (int i = 0; i < 25; i++) frame();
        check("old format, file times 1 h old: hidden after 6 s", g_n == 0);
        return fails;
    }
    remove(SO_CONF);
    // 1. both boxes, default position
    vl_speed(52); vl_fuel(87, 58);
    frame();
    const Draw* s = find(so_tex); const Draw* f = find(so_ftex);
    if (s) printf("     speed box x %.0f..%.0f y %.0f..%.0f\n", s->x0, s->x1, s->y0, s->y1);
    if (f) printf("     fuel  box x %.0f..%.0f y %.0f..%.0f\n", f->x0, f->x1, f->y0, f->y1);
    check("speed box 175 from the left, 12 from the bottom", s && s->x0 == 175 && s->y1 == 286);
    check("fuel box mirrored by default: 175 from the right, bottom 12", f && f->x1 == 625 && f->y1 == 286);
    printf("     both boxes %d x %d (0.4: 154 x 60, 0.6: 137 x 42 and 105 x 47)\n", so_bw, so_bh);
    check("both boxes exactly the same size", s && f && s->x1 - s->x0 == f->x1 - f->x0 && s->y1 - s->y0 == f->y1 - f->y0);
    check("smaller than 0.4", so_bw < 154 && so_bh < 60);
    check("box background 70 % opaque by default (alpha 179, was 160)", so_px[(1 * SO_TEX_W + so_bw / 2) * 4 + 3] == 179);
    check("small km/h: narrower than the old one", SO_SU3_W < SO_UNIT_W);
    {
        // speed box: same size and "km/h" in the same place for every speed
        int sizes_ok = 1, unit_ok = 1, inside = 1, ref_w = so_bw, ref_h = so_bh, unit_x = -1;
        int sp[7] = {0, 4, 40, 99, 100, 140, 188};
        for (int k = 0; k < 7; k++) {
            vl_speed(sp[k]); frame();
            const Draw* d = find(so_tex);
            if (!d || d->x0 != 175 || d->x1 - d->x0 != ref_w || d->y1 - d->y0 != ref_h || d->y1 != 286) sizes_ok = 0;
            // right edge of the ink in the texture = end of "km/h"
            int r = -1;
            for (int x = 0; x < so_bw; x++) for (int y = 0; y < so_bh; y++)
                if (so_px[(y * SO_TEX_W + x) * 4 + 3] > 200 && so_px[(y * SO_TEX_W + x) * 4] > 100) r = x;
            if (unit_x < 0) unit_x = r; else if (r != unit_x) unit_ok = 0;
            if (so_sb[0] < so_sref[0] || so_sb[2] > so_sref[2] || so_sb[1] < so_sref[1] || so_sb[3] > so_sref[3]) inside = 0;
        }
        printf("     speed box %d x %d for 0, 4, 40, 99, 100, 140, 188 km/h, km/h ends at x %d\n", ref_w, ref_h, unit_x);
        check("speed box: same size and place for 0 to 188 km/h", sizes_ok);
        check("speed box: km/h does not move", unit_ok);
        check("speed box: every speed fits inside the 3-digit size", inside);
        check("box: 6 px left/right and 5 px top/bottom around the larger content",
              (so_bw - 12 == so_sref[2] - so_sref[0] || so_bw - 12 >= so_fb[2] - so_fb[0]) && so_bh - 10 >= so_sref[3] - so_sref[1]);
        {
            // fuel box: same size for every value
            int fuel_ok = 1;
            int fc[5] = {87, 124, 54, -1, 99}; int fl[5] = {50, 9, 150, 63, -1};
            for (int k = 0; k < 5; k++) {
                vl_fuel(fc[k], fl[k]); frame();
                const Draw* d = find(so_ftex);
                if (!d || d->x1 != 625 || d->x1 - d->x0 != ref_w || d->y1 - d->y0 != ref_h) fuel_ok = 0;
            }
            check("fuel box: same size and place for every value", fuel_ok);
            vl_fuel(87, 58); frame();
        }
        vl_speed(52); frame();
    }
    // 2. the car's problem: file times useless (1 h old after every write), cruise control
    //    at 120 for 10 minutes, VehLog rewriting every 2 s (every 7th frame)
    int missing = 0, frames = 0;
    for (int i = 0; i < 2000; i++) {
        if (i % 7 == 0) { vl_speed(120); vl_fuel(87, 58); age(SO_FILE); age(SO_FUEL_FILE); }
        if (i % 7 == 3) { put(SO_FILE, ""); put(SO_FUEL_FILE, "8"); }      // caught mid-write
        if (i % 7 == 4) { put(SO_FILE, "120 "); }                           // still no line end
        if (i % 7 == 5) { char b[32]; snprintf(b, sizeof b, "120 %ld\n", seq_s); put(SO_FILE, b);
                          snprintf(b, sizeof b, "87 58 %ld\n", seq_f); put(SO_FUEL_FILE, b); age(SO_FILE); }
        frame(); frames++;
        if (!find(so_tex) || !find(so_ftex)) missing++;
    }
    printf("     10 min cruise control: %d of %d frames without a box\n", missing, frames);
    check("cruise control, old file times, mid-write reads: never hidden", missing == 0 && so_speed == 120);
    // 3. changes
    int up = g_uploads;
    vl_speed(120); frame(); vl_speed(120); frame();
    check("same value again: no new texture upload", g_uploads == up);
    vl_speed(121); frame();
    check("new value 121 shown at the next read", find(so_tex) && so_speed == 121 && g_uploads == up + 1);
    // 4. fuel line rules
    vl_fuel(-1, 58); frame();
    check("consumption unknown: box with the litre line only", find(so_ftex) && so_fcons == -1 && so_flit == 58);
    vl_fuel(87, -1); frame();
    check("litres unknown: box with the consumption line only", find(so_ftex) && so_fcons == 87 && so_flit == -1);
    vl_fuel(-1, -1); frame();
    check("both unknown: no fuel box, speed still there", !find(so_ftex) && find(so_tex));
    vl_fuel(4095, 58); frame();
    check("409.5 l/100km (invalid on the car): treated as unknown", find(so_ftex) && so_fcons == -1);
    vl_fuel(87, 58); frame();
    // 5. VehLog stops: no new counter
    vl_speed(121); vl_fuel(87, 58);
    frame();
    for (int i = 0; i < 18; i++) frame();   // 5.4 s more
    check("no new counter for 5.7 s: still shown", find(so_tex) && find(so_ftex));
    for (int i = 0; i < 3; i++) frame();    // 6.6 s
    check("no new counter for 6.6 s (VehLog stopped): both hidden", g_n == 0);
    vl_speed(60); vl_fuel(87, 58); frame();
    check("VehLog writes again: both back at once", find(so_tex) && find(so_ftex) && so_speed == 60);
    put(SO_OFF, ""); frame();
    check("/tmp/speedoverlay-off: nothing drawn", g_n == 0);
    remove(SO_OFF); frame();
    check("off file removed: back", find(so_tex) && find(so_ftex));
    remove(SO_FILE);
    for (int i = 0; i < 25; i++) { vl_fuel(87, 58); frame(); }
    check("speed file gone: speed box hidden after 6 s, fuel box stays", !find(so_tex) && find(so_ftex));
    // 6. pictures for the preview: both boxes for a few value pairs, with their size
    int sp[4] = {4, 45, 128, 188}; int fc[4] = {87, 87, 124, -1}; int fl[4] = {50, 50, 9, 63};
    for (int k = 0; k < 4; k++) {
        so_speed = sp[k]; so_fcons = fc[k]; so_flit = fl[k];
        so_rasterize_fuel(fc[k], fl[k]); so_rasterize(sp[k]);
        char n[64];
        so_rasterize(sp[k]); snprintf(n, sizeof n, "s%d_%dx%d", k, so_bw, so_bh); dump(n);
        so_rasterize_fuel(fc[k], fl[k]); snprintf(n, sizeof n, "f%d_%dx%d", k, so_bw, so_bh); dump(n);
        printf("     preview %d: %d km/h, fuel %d, %d L -> boxes %d x %d\n", k, sp[k], fc[k], fl[k], so_bw, so_bh);
    }
    printf(fails ? "%d FAILED\n" : "ALL OK\n", fails);
    return fails;
}
