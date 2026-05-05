package com.codex.retroplatformer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class RetroGameView extends SurfaceView implements Runnable {
    private static final int TILE = 32;
    private static final int WORLD_W = 170;
    private static final int WORLD_H = 15;
    private static final float GRAVITY = 1700f;
    private static final float MOVE_SPEED = 245f;
    private static final float JUMP_SPEED = -620f;
    private static final float MAX_FALL = 900f;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final char[][] map = new char[WORLD_H][WORLD_W];
    private final List<Coin> coins = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Player player = new Player();

    private Thread loopThread;
    private volatile boolean running;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpWasPressed;
    private float cameraX;
    private int score;
    private int lives = 3;
    private String banner = "RETRO PLATFORMER";
    private float bannerTimer = 2.2f;

    public RetroGameView(Context context) {
        super(context);
        holder = getHolder();
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        setFocusable(true);
        resetLevel();
    }

    public void resume() {
        if (running) {
            return;
        }
        running = true;
        loopThread = new Thread(this, "retro-platformer-loop");
        loopThread.start();
    }

    public void pause() {
        running = false;
        try {
            if (loopThread != null) {
                loopThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - last) / 1_000_000_000f);
            last = now;
            update(dt);
            drawFrame();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            leftPressed = false;
            rightPressed = false;
            jumpPressed = false;
            for (int i = 0; i < event.getPointerCount(); i++) {
                if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) && i == pointerIndex) {
                    continue;
                }
                float x = event.getX(i);
                float y = event.getY(i);
                if (y > getHeight() * 0.55f) {
                    if (x < getWidth() * 0.22f) {
                        leftPressed = true;
                    } else if (x < getWidth() * 0.44f) {
                        rightPressed = true;
                    } else if (x > getWidth() * 0.72f) {
                        jumpPressed = true;
                    }
                }
            }
            return true;
        }
        return true;
    }

    private void resetLevel() {
        coins.clear();
        enemies.clear();
        for (int y = 0; y < WORLD_H; y++) {
            for (int x = 0; x < WORLD_W; x++) {
                map[y][x] = '.';
            }
        }

        for (int x = 0; x < WORLD_W; x++) {
            setTile(x, 13, 'G');
            setTile(x, 14, 'G');
        }

        addPlatform(9, 10, 5);
        addPlatform(20, 8, 7);
        addPlatform(34, 10, 4);
        addPlatform(47, 7, 8);
        addPlatform(64, 11, 6);
        addPlatform(77, 9, 9);
        addPlatform(96, 8, 6);
        addPlatform(113, 10, 8);
        addPlatform(132, 7, 6);

        addBrickLine(15, 6, 4);
        addBrickLine(58, 6, 5);
        addBrickLine(89, 5, 5);
        addBrickLine(123, 6, 4);
        addQuestion(18, 5);
        addQuestion(40, 8);
        addQuestion(70, 7);
        addQuestion(103, 6);
        addQuestion(139, 5);

        for (int x = 12; x < 148; x += 7) {
            coins.add(new Coin(x * TILE + 10, 7 * TILE + (x % 3) * 22));
        }

        enemies.add(new Enemy(24 * TILE, 12 * TILE - 28, 22 * TILE, 31 * TILE));
        enemies.add(new Enemy(52 * TILE, 12 * TILE - 28, 48 * TILE, 62 * TILE));
        enemies.add(new Enemy(84 * TILE, 12 * TILE - 28, 78 * TILE, 93 * TILE));
        enemies.add(new Enemy(119 * TILE, 12 * TILE - 28, 113 * TILE, 129 * TILE));
        enemies.add(new Enemy(145 * TILE, 12 * TILE - 28, 137 * TILE, 157 * TILE));

        setTile(160, 12, 'F');
        setTile(160, 11, 'F');
        setTile(160, 10, 'F');
        setTile(161, 12, 'P');

        respawn();
    }

    private void respawn() {
        player.x = 64;
        player.y = 8 * TILE;
        player.vx = 0;
        player.vy = 0;
        player.onGround = false;
        cameraX = 0;
        banner = lives > 0 ? "GET READY" : "GAME OVER";
        bannerTimer = 1.4f;
    }

    private void addPlatform(int startX, int y, int width) {
        for (int i = 0; i < width; i++) {
            setTile(startX + i, y, 'B');
        }
    }

    private void addBrickLine(int startX, int y, int width) {
        for (int i = 0; i < width; i++) {
            setTile(startX + i, y, 'R');
        }
    }

    private void addQuestion(int x, int y) {
        setTile(x, y, 'Q');
    }

    private void setTile(int x, int y, char tile) {
        if (x >= 0 && x < WORLD_W && y >= 0 && y < WORLD_H) {
            map[y][x] = tile;
        }
    }

    private void update(float dt) {
        if (bannerTimer > 0) {
            bannerTimer -= dt;
        }

        float input = 0f;
        if (leftPressed) input -= 1f;
        if (rightPressed) input += 1f;

        player.vx = input * MOVE_SPEED;
        if (jumpPressed && !jumpWasPressed && player.onGround) {
            player.vy = JUMP_SPEED;
            player.onGround = false;
        }
        jumpWasPressed = jumpPressed;

        player.vy = Math.min(MAX_FALL, player.vy + GRAVITY * dt);
        movePlayer(player.vx * dt, 0);
        movePlayer(0, player.vy * dt);

        for (Coin coin : coins) {
            if (!coin.collected && intersects(player.rect(), coin.rect())) {
                coin.collected = true;
                score += 10;
            }
        }

        for (Enemy enemy : enemies) {
            if (!enemy.alive) continue;
            enemy.update(dt);
            if (intersects(player.rect(), enemy.rect())) {
                if (player.vy > 80 && player.y + player.h < enemy.y + enemy.h * 0.65f) {
                    enemy.alive = false;
                    player.vy = JUMP_SPEED * 0.55f;
                    score += 50;
                } else {
                    lives--;
                    if (lives <= 0) {
                        score = 0;
                        lives = 3;
                        resetLevel();
                    } else {
                        respawn();
                    }
                    return;
                }
            }
        }

        if (player.y > getHeight() + 200 || player.y > WORLD_H * TILE + 200) {
            lives--;
            if (lives <= 0) {
                score = 0;
                lives = 3;
                resetLevel();
            } else {
                respawn();
            }
        }

        if (player.x > 159 * TILE) {
            score += 200;
            resetLevel();
            banner = "LEVEL CLEAR";
            bannerTimer = 3.0f;
        }

        float target = player.x - getWidth() * 0.38f;
        cameraX += (target - cameraX) * Math.min(1f, dt * 7f);
        cameraX = clamp(cameraX, 0, WORLD_W * TILE - getWidth());
    }

    private void movePlayer(float dx, float dy) {
        player.x += dx;
        RectF r = player.rect();
        if (collidesSolid(r)) {
            if (dx > 0) {
                player.x = ((int) ((player.x + player.w) / TILE)) * TILE - player.w - 0.01f;
            } else if (dx < 0) {
                player.x = ((int) (player.x / TILE) + 1) * TILE + 0.01f;
            }
            player.vx = 0;
        }

        player.y += dy;
        player.onGround = false;
        r = player.rect();
        if (collidesSolid(r)) {
            if (dy > 0) {
                player.y = ((int) ((player.y + player.h) / TILE)) * TILE - player.h - 0.01f;
                player.vy = 0;
                player.onGround = true;
            } else if (dy < 0) {
                int hitX = (int) ((player.x + player.w * 0.5f) / TILE);
                int hitY = (int) (player.y / TILE);
                if (tileAt(hitX, hitY) == 'Q') {
                    setTile(hitX, hitY, 'U');
                    coins.add(new Coin(hitX * TILE + 10, (hitY - 1) * TILE + 8));
                    score += 25;
                }
                player.y = ((int) (player.y / TILE) + 1) * TILE + 0.01f;
                player.vy = 0;
            }
        }
        player.x = clamp(player.x, 0, WORLD_W * TILE - player.w);
    }

    private boolean collidesSolid(RectF rect) {
        int left = Math.max(0, (int) (rect.left / TILE));
        int right = Math.min(WORLD_W - 1, (int) ((rect.right - 1) / TILE));
        int top = Math.max(0, (int) (rect.top / TILE));
        int bottom = Math.min(WORLD_H - 1, (int) ((rect.bottom - 1) / TILE));
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (isSolid(map[y][x])) {
                    return true;
                }
            }
        }
        return false;
    }

    private char tileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= WORLD_W || y >= WORLD_H) return '.';
        return map[y][x];
    }

    private boolean isSolid(char tile) {
        return tile == 'G' || tile == 'B' || tile == 'R' || tile == 'Q' || tile == 'U' || tile == 'P';
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            drawGame(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGame(Canvas c) {
        int w = c.getWidth();
        int h = c.getHeight();
        c.drawColor(Color.rgb(92, 184, 255));
        drawBackground(c, w, h);

        int startX = Math.max(0, (int) (cameraX / TILE) - 1);
        int endX = Math.min(WORLD_W - 1, (int) ((cameraX + w) / TILE) + 1);
        for (int y = 0; y < WORLD_H; y++) {
            for (int x = startX; x <= endX; x++) {
                drawTile(c, map[y][x], x * TILE - cameraX, y * TILE);
            }
        }

        for (Coin coin : coins) {
            if (!coin.collected) drawCoin(c, coin.x - cameraX, coin.y);
        }
        for (Enemy enemy : enemies) {
            if (enemy.alive) drawEnemy(c, enemy.x - cameraX, enemy.y);
        }
        drawPlayer(c, player.x - cameraX, player.y);
        drawHud(c, w);
        drawControls(c, w, h);

        if (bannerTimer > 0) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(42);
            paint.setColor(Color.rgb(30, 30, 48));
            c.drawText(banner, w * 0.5f + 3, h * 0.3f + 3, paint);
            paint.setColor(Color.WHITE);
            c.drawText(banner, w * 0.5f, h * 0.3f, paint);
        }
    }

    private void drawBackground(Canvas c, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(248, 252, 255));
        for (int i = 0; i < 8; i++) {
            float x = ((i * 260) - (cameraX * 0.25f % 260));
            c.drawCircle(x, 78 + (i % 3) * 22, 24, paint);
            c.drawCircle(x + 28, 72 + (i % 2) * 18, 30, paint);
            c.drawCircle(x + 62, 82, 22, paint);
        }

        paint.setColor(Color.rgb(69, 188, 100));
        for (int i = 0; i < 10; i++) {
            float x = i * 180 - (cameraX * 0.4f % 180);
            c.drawRect(x, h - 118, x + 120, h - 90, paint);
            c.drawRect(x + 22, h - 150, x + 88, h - 90, paint);
        }
    }

    private void drawTile(Canvas c, char tile, float x, float y) {
        if (tile == '.') return;
        paint.setStyle(Paint.Style.FILL);
        if (tile == 'G') {
            paint.setColor(Color.rgb(84, 170, 74));
            c.drawRect(x, y, x + TILE, y + TILE, paint);
            paint.setColor(Color.rgb(122, 84, 54));
            c.drawRect(x, y + 8, x + TILE, y + TILE, paint);
        } else if (tile == 'B') {
            paint.setColor(Color.rgb(238, 189, 86));
            c.drawRect(x, y, x + TILE, y + TILE, paint);
            paint.setColor(Color.rgb(159, 102, 62));
            c.drawRect(x + 3, y + 3, x + TILE - 3, y + TILE - 3, paint);
        } else if (tile == 'R') {
            paint.setColor(Color.rgb(188, 86, 56));
            c.drawRect(x, y, x + TILE, y + TILE, paint);
            paint.setColor(Color.rgb(111, 54, 47));
            c.drawRect(x, y + 14, x + TILE, y + 17, paint);
            c.drawRect(x + 14, y, x + 17, y + TILE, paint);
        } else if (tile == 'Q') {
            paint.setColor(Color.rgb(249, 196, 65));
            c.drawRect(x, y, x + TILE, y + TILE, paint);
            paint.setColor(Color.rgb(83, 61, 48));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(24);
            c.drawText("?", x + TILE / 2f, y + 24, paint);
        } else if (tile == 'U') {
            paint.setColor(Color.rgb(134, 125, 116));
            c.drawRect(x, y, x + TILE, y + TILE, paint);
        } else if (tile == 'F') {
            paint.setColor(Color.WHITE);
            c.drawRect(x + 11, y, x + 16, y + TILE, paint);
            paint.setColor(Color.rgb(230, 61, 61));
            c.drawRect(x + 16, y + 2, x + 42, y + 18, paint);
        } else if (tile == 'P') {
            paint.setColor(Color.rgb(45, 75, 95));
            c.drawRect(x + 7, y, x + TILE - 7, y + TILE, paint);
        }
    }

    private void drawCoin(Canvas c, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 216, 74));
        c.drawOval(new RectF(x, y, x + 14, y + 22), paint);
        paint.setColor(Color.WHITE);
        c.drawRect(x + 5, y + 4, x + 8, y + 17, paint);
    }

    private void drawEnemy(Canvas c, float x, float y) {
        paint.setColor(Color.rgb(130, 76, 48));
        c.drawRoundRect(new RectF(x, y + 6, x + 30, y + 28), 7, 7, paint);
        paint.setColor(Color.WHITE);
        c.drawCircle(x + 9, y + 14, 4, paint);
        c.drawCircle(x + 21, y + 14, 4, paint);
        paint.setColor(Color.BLACK);
        c.drawCircle(x + 10, y + 14, 2, paint);
        c.drawCircle(x + 22, y + 14, 2, paint);
        paint.setColor(Color.rgb(55, 39, 34));
        c.drawRect(x + 3, y + 28, x + 11, y + 33, paint);
        c.drawRect(x + 19, y + 28, x + 27, y + 33, paint);
    }

    private void drawPlayer(Canvas c, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(43, 82, 196));
        c.drawRect(x + 7, y + 20, x + 25, y + 42, paint);
        paint.setColor(Color.rgb(243, 178, 115));
        c.drawRect(x + 8, y + 7, x + 24, y + 22, paint);
        paint.setColor(Color.rgb(213, 48, 49));
        c.drawRect(x + 5, y + 2, x + 27, y + 10, paint);
        paint.setColor(Color.WHITE);
        c.drawRect(x + 18, y + 12, x + 22, y + 16, paint);
        paint.setColor(Color.rgb(38, 38, 43));
        c.drawRect(x + 6, y + 42, x + 14, y + 48, paint);
        c.drawRect(x + 18, y + 42, x + 28, y + 48, paint);
    }

    private void drawHud(Canvas c, int w) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(24);
        paint.setColor(Color.rgb(31, 40, 55));
        c.drawText("SCORE " + score, 22, 34, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("LIVES " + lives, w - 22, 34, paint);
    }

    private void drawControls(Canvas c, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(96, 25, 35, 48));
        c.drawCircle(w * 0.13f, h * 0.78f, 48, paint);
        c.drawCircle(w * 0.32f, h * 0.78f, 48, paint);
        c.drawCircle(w * 0.84f, h * 0.76f, 56, paint);

        paint.setTextSize(34);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        c.drawText("<", w * 0.13f, h * 0.80f, paint);
        c.drawText(">", w * 0.32f, h * 0.80f, paint);
        c.drawText("A", w * 0.84f, h * 0.79f, paint);
    }

    private boolean intersects(RectF a, RectF b) {
        return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Player {
        float x;
        float y;
        float vx;
        float vy;
        final float w = 30;
        final float h = 48;
        boolean onGround;

        RectF rect() {
            return new RectF(x, y, x + w, y + h);
        }
    }

    private static class Coin {
        final float x;
        final float y;
        boolean collected;

        Coin(float x, float y) {
            this.x = x;
            this.y = y;
        }

        RectF rect() {
            return new RectF(x, y, x + 16, y + 22);
        }
    }

    private static class Enemy {
        float x;
        float y;
        final float minX;
        final float maxX;
        float vx = -85;
        final float w = 30;
        final float h = 33;
        boolean alive = true;

        Enemy(float x, float y, float minX, float maxX) {
            this.x = x;
            this.y = y;
            this.minX = minX;
            this.maxX = maxX;
        }

        void update(float dt) {
            x += vx * dt;
            if (x < minX || x > maxX) {
                vx *= -1;
                x = Math.max(minX, Math.min(maxX, x));
            }
        }

        RectF rect() {
            return new RectF(x, y, x + w, y + h);
        }
    }
}
