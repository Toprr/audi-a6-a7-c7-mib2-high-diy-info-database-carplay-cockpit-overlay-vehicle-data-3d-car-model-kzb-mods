/*
 * Speed overlay for the mib-cluster-map stream player (our addition, GPL-3.0 like the player).
 *
 * Draws two small boxes over the phone map in the cockpit:
 *   left:  the speed, "52 km/h", from VehLog's /tmp/vehlog_speed ("52 <counter>\n")
 *   right: long term consumption and estimated fuel, "8,7 l/100km" over "58 L" and a
 *          jerrycan, from VehLog's /tmp/vehlog_fuel ("87 58 <counter>\n", -1 = unknown)
 * VehLog (Java HMI, read-only) rewrites both files every 2 s, the speed also on change,
 * and counts up the counter on every write.
 *
 * Safety: everything here only draws. No file -> nothing drawn. No new counter for
 * SO_STALE_S seconds (VehLog stopped) -> nothing drawn. /tmp/speedoverlay-off ->
 * nothing drawn.
 * If the shader does not compile, the overlay switches itself off for good and the
 * player draws the map exactly as before.
 *
 * 0.3: a read that finds the file empty or without its line end (VehLog is rewriting
 * it at that moment) keeps the value on screen instead of hiding it. That was the
 * flicker of 0.2. Both boxes have the same fixed size.
 * 0.4: on the car 0.3 still showed the boxes only for a moment every 2 s: it judged
 * "VehLog still alive" by the file time. Now it uses VehLog's counter and its own
 * clock, and logs every hide and show with the reason.
 * 0.5: the boxes fit their content with a small margin (SO_PAD_X, SO_PAD_Y), both the
 * same size, anchored on their outer side; the fuel lines are closer together.
 * 0.6: not the same size any more. The speed box has one fixed size that fits three
 * digits ("888 km/h"), the digits are right-aligned so "km/h" never moves. The fuel
 * box fits its own content.
 * 0.7: both boxes the same fixed size again (the larger of "888 km/h" and the widest
 * fuel content), "km/h" in the small unit size, left box at 175.
 * 0.8: box opacity is a setting (alpha=, 0 to 255), default 179 (70 %, was 160).
 *
 * Position: bottom left by default, SO_LEFT_DEFAULT pixels from the left edge (on the
 * A6/A7 C7 the rev counter covers about the left 180 of 800 pixels) and
 * SO_BOTTOM_DEFAULT pixels above the bottom edge. The right box mirrors it: the same
 * distance from the right edge, at the same height. /mnt/persist/speedoverlay.conf
 * can change it (read when the player starts):
 *   pos=bl|br|tl|tr   left=<px>   right=<px>   top=<px>   bottom=<px>
 *   fuel=on|off       fuelright=<px>
 *   alpha=<40..255>   box background opacity (255 = solid black)
 */
#pragma once
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/stat.h>
#include "speed_font.h"
#include "speed_font_small.h"

#define SO_FILE "/tmp/vehlog_speed"
#define SO_FUEL_FILE "/tmp/vehlog_fuel"
#define SO_OFF "/tmp/speedoverlay-off"
#define SO_STALE_S 6
#define SO_TEX_W 176
#define SO_TEX_H 60
#define SO_PAD_X 6                   /* 0.5: box edge to the ink, left and right */
#define SO_PAD_Y 5                   /* 0.5: box edge to the ink, top and bottom */
#define SO_RADIUS 6
#define SO_MARGIN 12
#define SO_BOTTOM_DEFAULT 12
#define SO_LEFT_DEFAULT 175
#define SO_CONF "/mnt/persist/speedoverlay.conf"
#define SO_GREY 180                  /* units and the jerrycan */
#define SO_ALPHA_DEFAULT 179         /* 0.8: box background, 70 % opaque (0.7: 160) */

static int so_state = 0;            /* 0 = not set up, 1 = ready, -1 = off for good */
static GLuint so_prog, so_tex, so_ftex;
static GLint so_pos, so_uv, so_smp;
static int so_speed = -1;           /* value in the texture, -1 = none */
static int so_fcons = -2, so_flit = -2;  /* values in the fuel texture, -2 = none yet */
static uint64_t so_last_check = 0;
static bool so_visible = false;
static bool so_fvisible = false;
static bool so_fuel_on = true;
static int so_corner = 2;            /* 0 top left, 1 top right, 2 bottom left, 3 bottom right */
static int so_bottom = SO_BOTTOM_DEFAULT;
static int so_leftpx = SO_LEFT_DEFAULT;
static int so_rightpx = SO_MARGIN;
static int so_toppx = SO_MARGIN;
static int so_fuelright = -1;        /* -1 = same as left */
static int so_alpha = SO_ALPHA_DEFAULT;
static bool so_logged = false;
static bool so_flogged = false;
static uint8_t so_px[SO_TEX_W * SO_TEX_H * 4];
/* 0.5: the content of each box (grey, alpha) without the box, and its ink bounds */
static uint8_t so_cs[SO_TEX_W * SO_TEX_H * 2];
static uint8_t so_cf[SO_TEX_W * SO_TEX_H * 2];
static int so_sb[4] = {0, 0, 0, 0};     /* speed ink x0, y0, x1, y1 (x1, y1 exclusive) */
static int so_fb[4] = {0, 0, 0, 0};     /* fuel ink */
static int so_bw = 0, so_bh = 0;        /* 0.7: size of both boxes (fixed) */
static int so_sref[4] = {0, 0, 0, 0};   /* 0.6: ink bounds of the widest speed, "888 km/h" */

static const char* so_vs =
    "attribute vec2 p; attribute vec2 t; varying vec2 v;"
    "void main(){ gl_Position = vec4(p, 0.0, 1.0); v = t; }";
static const char* so_fs =
    "precision mediump float; varying vec2 v; uniform sampler2D s;"
    "void main(){ gl_FragColor = texture2D(s, v); }";

static GLuint so_shader(GLenum type, const char* src) {
    GLuint s = glCreateShader(type);
    glShaderSource(s, 1, &src, NULL);
    glCompileShader(s);
    GLint ok = 0;
    glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
    if (!ok) { glDeleteShader(s); return 0; }
    return s;
}

static void so_texture(GLuint* t) {
    glGenTextures(1, t);
    glBindTexture(GL_TEXTURE_2D, *t);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
}

static void so_read_conf() {
    FILE* cf = fopen(SO_CONF, "r");
    if (!cf) return;
    char line[64];
    while (fgets(line, sizeof(line), cf)) {
        if (strncmp(line, "pos=tl", 6) == 0) so_corner = 0;
        else if (strncmp(line, "pos=tr", 6) == 0) so_corner = 1;
        else if (strncmp(line, "pos=bl", 6) == 0) so_corner = 2;
        else if (strncmp(line, "pos=br", 6) == 0) so_corner = 3;
        else if (strncmp(line, "fuel=off", 8) == 0) so_fuel_on = false;
        else if (strncmp(line, "fuel=on", 7) == 0) so_fuel_on = true;
        else if (strncmp(line, "bottom=", 7) == 0) {
            int b = atoi(line + 7);
            if (b >= 0 && b <= 400) so_bottom = b;
        }
        else if (strncmp(line, "left=", 5) == 0) {
            int b = atoi(line + 5);
            if (b >= 0 && b <= 700) so_leftpx = b;
        }
        else if (strncmp(line, "right=", 6) == 0) {
            int b = atoi(line + 6);
            if (b >= 0 && b <= 700) so_rightpx = b;
        }
        else if (strncmp(line, "top=", 4) == 0) {
            int b = atoi(line + 4);
            if (b >= 0 && b <= 400) so_toppx = b;
        }
        else if (strncmp(line, "fuelright=", 10) == 0) {
            int b = atoi(line + 10);
            if (b >= 0 && b <= 700) so_fuelright = b;
        }
        else if (strncmp(line, "alpha=", 6) == 0) {
            int b = atoi(line + 6);
            if (b >= 40 && b <= 255) so_alpha = b;
        }
    }
    fclose(cf);
}

static void so_init() {
    so_state = -1;
    so_read_conf();
    if (so_fuelright < 0) so_fuelright = so_leftpx;
    GLuint vs = so_shader(GL_VERTEX_SHADER, so_vs);
    GLuint fs = so_shader(GL_FRAGMENT_SHADER, so_fs);
    if (!vs || !fs) { LOGE("speed overlay: shader compile failed, overlay off"); return; }
    so_prog = glCreateProgram();
    glAttachShader(so_prog, vs);
    glAttachShader(so_prog, fs);
    glLinkProgram(so_prog);
    GLint ok = 0;
    glGetProgramiv(so_prog, GL_LINK_STATUS, &ok);
    if (!ok) { LOGE("speed overlay: shader link failed, overlay off"); return; }
    so_pos = glGetAttribLocation(so_prog, "p");
    so_uv = glGetAttribLocation(so_prog, "t");
    so_smp = glGetUniformLocation(so_prog, "s");
    glActiveTexture(GL_TEXTURE3);
    so_texture(&so_tex);
    so_texture(&so_ftex);
    glActiveTexture(GL_TEXTURE0);
    so_state = 1;
    LOG("speed overlay 0.8: ready (corner %d, left %d, right %d, top %d, bottom %d, fuel %s right %d, alpha %d)",
        so_corner, so_leftpx, so_rightpx, so_toppx, so_bottom, so_fuel_on ? "on" : "off", so_fuelright, so_alpha);
}

/* Reads the first line of a VehLog file and parses up to max integers from it.
 * Returns how many were parsed; 0 when the file is missing or the line is not
 * complete yet (VehLog rewrites it in place). *mt gets the file time. */
static int so_read_ints(const char* path, long* v, int max, time_t* mt) {
    struct stat st;
    if (stat(path, &st) != 0) return 0;
    *mt = st.st_mtime;
    FILE* f = fopen(path, "r");
    if (!f) return 0;
    char buf[48];
    memset(buf, 0, sizeof(buf));
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0 || memchr(buf, '\n', n) == NULL) return 0;
    char* p = buf;
    int k = 0;
    while (k < max) {
        char* e = NULL;
        long x = strtol(p, &e, 10);
        if (e == p) break;
        v[k++] = x;
        p = e;
    }
    return k;
}

/* 0.4: is VehLog still writing this file? VehLog 0.8 ends each line with a write
 * counter; a new counter is a sign of life, timed with the player's own clock.
 * File times are only used for the older format without a counter: on the car the
 * boxes were hidden most of the time with them. */
struct so_life { long seq; uint64_t seen_us; bool fresh; };

static void so_alive(so_life* l, int parsed, int with_seq, long seq, time_t mt, uint64_t nowUs) {
    if (parsed >= with_seq) {
        if (seq != l->seq || l->seen_us == 0) { l->seq = seq; l->seen_us = nowUs; }
    } else if (parsed > 0) {
        time_t now = time(NULL);
        if (now - mt <= SO_STALE_S && mt - now <= SO_STALE_S) l->seen_us = nowUs;
    }
    l->fresh = l->seen_us != 0 && nowUs - l->seen_us <= (uint64_t)SO_STALE_S * 1000000ULL;
}

static so_life so_slife = { -1, 0, false };
static so_life so_flife = { -1, 0, false };
static int so_logs = 0;

/* Logs when a box appears or disappears, with the reason (at most 40 lines). */
static void so_note(const char* box, bool shown, bool off, const so_life* l, uint64_t nowUs) {
    if (so_logs >= 40) return;
    so_logs++;
    if (shown) LOG("speed overlay: %s box shown", box);
    else if (off) LOG("speed overlay: %s box hidden (/tmp/speedoverlay-off)", box);
    else if (l->seen_us == 0) LOG("speed overlay: %s box hidden (no value from VehLog yet)", box);
    else if (l->fresh) LOG("speed overlay: %s box hidden (VehLog has no valid value)", box);
    else LOG("speed overlay: %s box hidden (no update from VehLog for %llu ms)", box,
             (unsigned long long)((nowUs - l->seen_us) / 1000ULL));
}
/* Clears so_px and draws the box: w x h, rounded, black at alpha 160. */
static void so_box(int w, int h) {
    memset(so_px, 0, sizeof(so_px));
    const int r = SO_RADIUS;
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int dx = x < r ? r - x : (x >= w - r ? x - (w - r - 1) : 0);
            int dy = y < r ? r - y : (y >= h - r ? y - (h - r - 1) : 0);
            if (dx * dx + dy * dy > r * r) continue;
            uint8_t* p = &so_px[(y * SO_TEX_W + x) * 4];
            p[0] = p[1] = p[2] = 0; p[3] = (uint8_t)so_alpha;
        }
    }
}

/* Blends an alpha bitmap (w x h) at (x0, y0) in colour grey into a content layer. */
static void so_blit(uint8_t* layer, const uint8_t* g, int w, int h, int x0, int y0, int grey) {
    for (int y = 0; y < h; y++) {
        int ty = y0 + y;
        if (ty < 0 || ty >= SO_TEX_H) continue;
        for (int x = 0; x < w; x++) {
            int tx = x0 + x;
            if (tx < 0 || tx >= SO_TEX_W) continue;
            int a = g[y * w + x];
            if (!a) continue;
            uint8_t* p = &layer[(ty * SO_TEX_W + tx) * 2];
            int ba = p[1];
            int oa = a + ba * (255 - a) / 255;
            p[0] = (uint8_t)(oa ? (grey * a + p[0] * ba * (255 - a) / 255) / oa : 0);
            p[1] = (uint8_t)oa;
        }
    }
}

/* Ink bounds of a content layer (pixels with visible alpha). */
static void so_bounds(const uint8_t* layer, int* b) {
    int x0 = SO_TEX_W, y0 = SO_TEX_H, x1 = 0, y1 = 0;
    for (int y = 0; y < SO_TEX_H; y++)
        for (int x = 0; x < SO_TEX_W; x++)
            if (layer[(y * SO_TEX_W + x) * 2 + 1] > 8) {
                if (x < x0) x0 = x;
                if (y < y0) y0 = y;
                if (x >= x1) x1 = x + 1;
                if (y >= y1) y1 = y + 1;
            }
    if (x1 <= x0) { x0 = y0 = x1 = y1 = 0; }
    b[0] = x0; b[1] = y0; b[2] = x1; b[3] = y1;
}

static void so_upload(GLuint tex) {
    glActiveTexture(GL_TEXTURE3);
    glBindTexture(GL_TEXTURE_2D, tex);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, SO_TEX_W, SO_TEX_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, so_px);
    glActiveTexture(GL_TEXTURE0);
}

/* Box of size w x h with the ink of bounds b centred in it, into so_px and tex. */
static void so_put(const uint8_t* layer, const int* b, int w, int h, GLuint tex) {
    so_box(w, h);
    int ox = (w - (b[2] - b[0])) / 2 - b[0];
    int oy = (h - (b[3] - b[1])) / 2 - b[1];
    for (int y = 0; y < SO_TEX_H; y++) {
        int ty = y + oy;
        if (ty < 0 || ty >= h) continue;
        for (int x = 0; x < SO_TEX_W; x++) {
            int tx = x + ox;
            if (tx < 0 || tx >= w) continue;
            const uint8_t* c = &layer[(y * SO_TEX_W + x) * 2];
            int a = c[1];
            if (!a) continue;
            uint8_t* p = &so_px[(ty * SO_TEX_W + tx) * 4];
            int ba = p[3];
            int oa = a + ba * (255 - a) / 255;
            p[0] = p[1] = p[2] = (uint8_t)(oa ? c[0] * a / oa : 0);
            p[3] = (uint8_t)oa;
        }
    }
    so_upload(tex);
}

/* Draws the speed into so_cs: digits right-aligned in a 3-digit slot, then "km/h",
 * so "km/h" always sits in the same place. */
static void so_draw_speed(const char* digits) {
    int nd = (int)strlen(digits);
    int gap = 5;
    memset(so_cs, 0, sizeof(so_cs));
    int top = (SO_TEX_H - SO_DIG_H) / 2 + 2;
    int x0 = 2;
    for (int i = nd; i < 3; i++) x0 += so_dig_w[8] - 4;   /* empty places on the left */
    for (int i = 0; i < nd; i++) {
        int d = digits[i] - '0';
        so_blit(so_cs, &so_dig_px[so_dig_off[d]], so_dig_w[d], SO_DIG_H, x0, top, 255);
        x0 += so_dig_w[d] - 4;
    }
    /* 0.7: "km/h" in the small unit size of the fuel box, on the digits' baseline */
    so_blit(so_cs, so_su3_px, SO_SU3_W, SO_SU3_H, x0 + gap, top + SO_DIG_BASE - SO_SU_BASE, SO_GREY);
}

static void so_draw_fuel(int cons, int lit);

/* 0.7: one size for both boxes, fixed: the larger of "888 km/h" and the widest fuel
 * content ("88,8 l/100km" over "150 L"), plus SO_PAD_X / SO_PAD_Y. Worked out once. */
static void so_size() {
    if (so_bw > 0) return;
    int fref[4];
    so_draw_speed("888");
    so_bounds(so_cs, so_sref);
    so_draw_fuel(888, 150);
    so_bounds(so_cf, fref);
    int w = so_sref[2] - so_sref[0], h = so_sref[3] - so_sref[1];
    if (fref[2] - fref[0] > w) w = fref[2] - fref[0];
    if (fref[3] - fref[1] > h) h = fref[3] - fref[1];
    so_bw = w + 2 * SO_PAD_X;
    so_bh = h + 2 * SO_PAD_Y;
    if (so_bw > SO_TEX_W) so_bw = SO_TEX_W;
    if (so_bh > SO_TEX_H) so_bh = SO_TEX_H;
}

/* Left box: the place of the text comes from "888 km/h", so "km/h" never moves,
 * whether 4, 40 or 140. */
static void so_rasterize(int speed) {
    so_size();
    char digits[12];
    snprintf(digits, sizeof(digits), "%d", speed < 0 ? 0 : (speed > 999 ? 999 : speed));
    so_draw_speed(digits);
    so_bounds(so_cs, so_sb);
    so_put(so_cs, so_sref, so_bw, so_bh, so_tex);
}

/* Width of a string of small digits and ',' (advance = glyph width - 2). */
static int so_small_w(const char* s) {
    int w = 0;
    for (; *s; s++) w += so_sd_w[*s == ',' ? 10 : *s - '0'] - 2;
    return w;
}

/* Draws small digits with their baseline at y = base, from x; returns the new x. */
static int so_small(const char* s, int x, int base) {
    for (; *s; s++) {
        int i = *s == ',' ? 10 : *s - '0';
        so_blit(so_cf, &so_sd_px[so_sd_off[i]], so_sd_w[i], SO_SD_H, x - 1, base - SO_SD_BASE, 255);
        x += so_sd_w[i] - 2;
    }
    return x;
}

/* Right box content: "8,7 l/100km" over "58 L" + jerrycan, each line centred.
 * 0.5: lines 20 px apart (baseline to baseline), was 25. */
#define SO_FL1 24
#define SO_FL2 44
static void so_draw_fuel(int cons, int lit) {
    memset(so_cf, 0, sizeof(so_cf));
    if (cons >= 0) {
        char t[12];
        snprintf(t, sizeof(t), "%d,%d", cons / 10, cons % 10);
        int w = so_small_w(t) + 4 + SO_SU1_W;
        int x = so_small(t, (SO_TEX_W - w) / 2, SO_FL1);
        so_blit(so_cf, so_su1_px, SO_SU1_W, SO_SU1_H, x + 4 - 1, SO_FL1 - SO_SU_BASE, SO_GREY);
    }
    if (lit >= 0) {
        char t[12];
        snprintf(t, sizeof(t), "%d", lit);
        int w = so_small_w(t) + 3 + SO_SU2_W + 4 + SO_ICON_W;
        int x = so_small(t, (SO_TEX_W - w) / 2, SO_FL2);
        so_blit(so_cf, so_su2_px, SO_SU2_W, SO_SU2_H, x + 3 - 1, SO_FL2 - SO_SU_BASE, SO_GREY);
        x += 3 + SO_SU2_W + 4;
        so_blit(so_cf, so_icon_px, SO_ICON_W, SO_ICON_H, x, SO_FL2 - SO_ICON_H + 1, SO_GREY);
    }
}

/* Right box: its lines centred in the shared box size. */
static void so_rasterize_fuel(int cons, int lit) {
    so_size();
    so_draw_fuel(cons, lit);
    so_bounds(so_cf, so_fb);
    if (so_fb[2] <= so_fb[0]) return;   /* nothing to show, the box stays hidden */
    so_put(so_cf, so_fb, so_bw, so_bh, so_ftex);
}

static void so_quad(GLuint tex, float x0, float y0, int bw, int bh, int winW, int winH) {
    if (x0 < 0.0f) x0 = 0.0f;
    if (x0 > (float)(winW - bw)) x0 = (float)(winW - bw);
    if (y0 < 0.0f) y0 = 0.0f;
    if (y0 > (float)(winH - bh)) y0 = (float)(winH - bh);
    float x1 = x0 + bw, y1 = y0 + bh;
    float l = x0 / winW * 2.0f - 1.0f, rgt = x1 / winW * 2.0f - 1.0f;
    float t = 1.0f - y0 / winH * 2.0f, b = 1.0f - y1 / winH * 2.0f;
    float u1 = (float)bw / SO_TEX_W, v1 = (float)bh / SO_TEX_H;
    GLfloat pos[8] = { l, t,  rgt, t,  rgt, b,  l, b };
    GLfloat uv[8] = { 0.0f, 0.0f,  u1, 0.0f,  u1, v1,  0.0f, v1 };
    glBindTexture(GL_TEXTURE_2D, tex);
    glVertexAttribPointer(so_pos, 2, GL_FLOAT, GL_FALSE, 0, pos);
    glVertexAttribPointer(so_uv, 2, GL_FLOAT, GL_FALSE, 0, uv);
    glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
}

/* Called once per drawn frame, after the map and before eglSwapBuffers. */
static void speed_overlay_draw(int winW, int winH, uint64_t nowUs) {
    if (so_state == 0) so_init();
    if (so_state != 1 || winW <= 0 || winH <= 0) return;
    if (nowUs - so_last_check >= 300000ULL || so_last_check == 0) {
        so_last_check = nowUs;
        struct stat ost;
        bool off = stat(SO_OFF, &ost) == 0;
        long v[3];
        time_t mt = 0;
        int n = so_read_ints(SO_FILE, v, 2, &mt);
        so_alive(&so_slife, n, 2, n >= 2 ? v[1] : 0, mt, nowUs);
        if (n >= 1 && v[0] >= 0 && v[0] <= 400 && v[0] != so_speed) {
            so_speed = (int)v[0];
            so_rasterize(so_speed);
        }
        bool vis = !off && so_slife.fresh && so_speed >= 0;
        if (vis != so_visible) so_note("speed", vis, off, &so_slife, nowUs);
        so_visible = vis;
        if (so_fuel_on) {
            n = so_read_ints(SO_FUEL_FILE, v, 3, &mt);
            so_alive(&so_flife, n, 3, n >= 3 ? v[2] : 0, mt, nowUs);
            if (n >= 2) {
                int c = (v[0] >= 10 && v[0] <= 500) ? (int)v[0] : -1;
                int l = (v[1] >= 0 && v[1] <= 150) ? (int)v[1] : -1;
                if (c != so_fcons || l != so_flit) {
                    so_fcons = c; so_flit = l;
                    so_rasterize_fuel(c, l);
                }
            }
            bool fvis = !off && so_flife.fresh && (so_fcons >= 0 || so_flit >= 0);
            if (fvis != so_fvisible) so_note("fuel", fvis, off, &so_flife, nowUs);
            so_fvisible = fvis;
        }
    }
    bool sp = so_visible && so_speed >= 0;
    bool fu = so_fvisible;
    if (!sp && !fu) return;

    if (sp && !so_logged) {
        so_logged = true;
        LOG("speed overlay: window %dx%d, first value %d km/h", winW, winH, so_speed);
    }
    if (fu && !so_flogged) {
        so_flogged = true;
        LOG("speed overlay: first fuel values %d (l/100km x10), %d l", so_fcons, so_flit);
    }
    glUseProgram(so_prog);
    glActiveTexture(GL_TEXTURE3);
    glUniform1i(so_smp, 3);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnableVertexAttribArray(so_pos);
    glEnableVertexAttribArray(so_uv);
    if (sp) {
        bool left = so_corner == 0 || so_corner == 2;
        bool bottom = so_corner >= 2;
        float x0 = left ? (float)so_leftpx : (float)(winW - so_rightpx - so_bw);
        float y0 = bottom ? (float)(winH - so_bottom - so_bh) : (float)so_toppx;
        so_quad(so_tex, x0, y0, so_bw, so_bh, winW, winH);
    }
    if (fu) {
        so_quad(so_ftex, (float)(winW - so_fuelright - so_bw), (float)(winH - so_bottom - so_bh), so_bw, so_bh, winW, winH);
    }
    glDisableVertexAttribArray(so_pos);
    glDisableVertexAttribArray(so_uv);
    glDisable(GL_BLEND);
    glActiveTexture(GL_TEXTURE0);
}
