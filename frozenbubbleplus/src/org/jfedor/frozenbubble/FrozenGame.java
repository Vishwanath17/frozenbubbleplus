/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright � 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright � 2003 Glenn Sanson.
 * Additional source - Copyright � 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or later, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to:
 * Free Software Foundation, Inc.
 * 675 Mass Ave
 * Cambridge, MA 02139, USA
 *
 * Artwork:
 *    Alexis Younes <73lab at free.fr>
 *      (everything but the bubbles)
 *    Amaury Amblard-Ladurantie <amaury at linuxfr.org>
 *      (the bubbles)
 *
 * Soundtrack:
 *    Matthias Le Bidan <matthias.le_bidan at caramail.com>
 *      (the three musics and all the sound effects)
 *
 * Design & Programming:
 *    Guillaume Cottenceau <guillaume.cottenceau at free.fr>
 *      (design and manage the project, whole Perl sourcecode)
 *
 * Java version:
 *    Glenn Sanson <glenn.sanson at free.fr>
 *      (whole Java sourcecode, including JIGA classes
 *             http://glenn.sanson.free.fr/jiga/)
 *
 * Android port:
 *    Pawel Aleksander Fedorynski <pfedor@fuw.edu.pl>
 *    Eric Fortin <videogameboy76 at yahoo.com>
 *    Copyright � Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

package org.jfedor.frozenbubble;

import java.util.Random;
import java.util.Vector;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.efortin.frozenbubble.HighscoreManager;

public class FrozenGame extends GameScreen {
  public final static int HORIZONTAL_MOVE = 0;
  public final static int FIRE            = 1;

  public final static int KEY_UP    = 38;
  public final static int KEY_LEFT  = 37;
  public final static int KEY_RIGHT = 39;
  public final static int KEY_SHIFT = 16;

  public static final int GAME_PLAYING   = 1;
  public static final int GAME_LOST      = 2;
  public static final int GAME_WON       = 3;
  public static final int GAME_NEXT_LOST = 4;
  public static final int GAME_NEXT_WON  = 5;

  public static int play_result = GAME_PLAYING;
  public static int game_result = GAME_LOST;

  public static String PARAMETER_PLAYER  = "player";
  public static String PARAMETER_OFFLINE = "offline";

  // Change mode (normal/colorblind)
  public final static int KEY_M = 77;
  // Toggle sound on/off
  public final static int KEY_S = 83;
  boolean modeKeyPressed, soundKeyPressed;

  BmpWrap background;
  BmpWrap[] bubbles;
  BmpWrap[] bubblesBlind;
  BmpWrap[] frozenBubbles;
  BmpWrap[] targetedBubbles;
  Random random;

  LaunchBubbleSprite launchBubble;
  double launchBubblePosition;

  PenguinSprite penguin;
  Compressor compressor;

  ImageSprite nextBubble;
  int currentColor, nextColor;

  BubbleSprite movingBubble;
  BubbleManager bubbleManager;
  LevelManager levelManager;

  HighscoreManager highscoreManager;

  Vector<Sprite> jumping;
  Vector<Sprite> falling;

  BubbleSprite[][] bubblePlay;

  BmpWrap gameWon, gameLost, gamePaused;

  BmpWrap bubbleBlink;
  int blinkDelay;

  ImageSprite hurrySprite;
  int hurryTime;

  ImageSprite pausedSprite;

  SoundManager soundManager;

  boolean endOfGame;
  boolean frozenify;
  boolean swapPressed;
  int fixedBubbles;
  int frozenifyX, frozenifyY;
  int nbBubbles;
  double moveDown;

  Drawable launcher;
  BmpWrap penguins;

  public FrozenGame(BmpWrap background_arg,
                    BmpWrap[] bubbles_arg,
                    BmpWrap[] bubblesBlind_arg,
                    BmpWrap[] frozenBubbles_arg,
                    BmpWrap[] targetedBubbles_arg,
                    BmpWrap bubbleBlink_arg,
                    BmpWrap gameWon_arg,
                    BmpWrap gameLost_arg,
                    BmpWrap gamePaused_arg,
                    BmpWrap hurry_arg,
                    BmpWrap penguins_arg,
                    BmpWrap compressorHead_arg,
                    BmpWrap compressor_arg,
                    Drawable launcher_arg,
                    SoundManager soundManager_arg,
                    LevelManager levelManager_arg,
                    HighscoreManager highscoreManager_arg) {
    random               = new Random(System.currentTimeMillis());
    launcher             = launcher_arg;
    penguins             = penguins_arg;
    background           = background_arg;
    bubbles              = bubbles_arg;
    bubblesBlind         = bubblesBlind_arg;
    frozenBubbles        = frozenBubbles_arg;
    targetedBubbles      = targetedBubbles_arg;
    bubbleBlink          = bubbleBlink_arg;
    gameWon              = gameWon_arg;
    gameLost             = gameLost_arg;
    gamePaused           = gamePaused_arg;
    soundManager         = soundManager_arg;
    levelManager         = levelManager_arg;
    highscoreManager     = highscoreManager_arg;
    play_result          = GAME_PLAYING;
    game_result          = GAME_LOST;
    launchBubblePosition = 20;
    swapPressed          = false;

    penguin = new PenguinSprite(penguins_arg, random);
    this.addSprite(penguin);
    compressor  = new Compressor(compressorHead_arg, compressor_arg);
    hurrySprite = new ImageSprite(new Rect(203, 265, 203 + 240, 265 + 90),
                                  hurry_arg);

    jumping = new Vector<Sprite>();
    falling = new Vector<Sprite>();

    bubblePlay    = new BubbleSprite[8][13];
    bubbleManager = new BubbleManager(bubbles);
    byte[][] currentLevel = levelManager.getCurrentLevel();

    if (currentLevel == null) {
      //Log.i("frozen-bubble", "Level not available.");
      return;
    }

    for (int j=0 ; j<12 ; j++) {
      for (int i=j%2 ; i<8 ; i++) {
        if (currentLevel[i][j] != -1) {
          BubbleSprite newOne = new BubbleSprite(
            new Rect(190+i*32-(j%2)*16, 44+j*28, 32, 32),
            currentLevel[i][j],
            bubbles[currentLevel[i][j]], bubblesBlind[currentLevel[i][j]],
            frozenBubbles[currentLevel[i][j]], bubbleBlink, bubbleManager,
            soundManager, this);
          bubblePlay[i][j] = newOne;
          this.addSprite(newOne);
        }
      }
    }

    currentColor = bubbleManager.nextBubbleIndex(random);
    nextColor    = bubbleManager.nextBubbleIndex(random);

    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
      nextBubble = new ImageSprite(new Rect(302, 440, 302 + 32, 440 + 32),
                                   bubbles[nextColor]);
    }
    else {
      nextBubble = new ImageSprite(new Rect(302, 440, 302 + 32, 440 + 32),
                                   bubblesBlind[nextColor]);
    }

    this.addSprite(nextBubble);
    launchBubble = new LaunchBubbleSprite(currentColor, 
                                          (int)launchBubblePosition,
                                          launcher, bubbles, bubblesBlind);
    this.spriteToBack(launchBubble);
    nbBubbles = 0;
  }

  public void cleanUp() {
    //
    //   If the pause bitmap is displayed, remove it.
    //
    //
    resume();
  }

  public void saveState(Bundle map) {
    cleanUp();
    Vector<Sprite> savedSprites = new Vector<Sprite>();
    saveSprites(map, savedSprites);
    for (int i = 0; i < jumping.size(); i++) {
      ((Sprite)jumping.elementAt(i)).saveState(map, savedSprites);
      map.putInt(String.format("jumping-%d", i),
                 ((Sprite)jumping.elementAt(i)).getSavedId());
    }
    map.putInt("numJumpingSprites", jumping.size());
    for (int i = 0; i < falling.size(); i++) {
      ((Sprite)falling.elementAt(i)).saveState(map, savedSprites);
      map.putInt(String.format("falling-%d", i),
                 ((Sprite)falling.elementAt(i)).getSavedId());
    }
    map.putInt("numFallingSprites", falling.size());
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        if (bubblePlay[i][j] != null) {
          bubblePlay[i][j].saveState(map, savedSprites);
          map.putInt(String.format("play-%d-%d", i, j),
                     bubblePlay[i][j].getSavedId());
        }
        else {
          map.putInt(String.format("play-%d-%d", i, j), -1);
        }
      }
    }
    launchBubble.saveState(map, savedSprites);
    map.putInt("launchBubbleId", launchBubble.getSavedId());
    map.putDouble("launchBubblePosition", launchBubblePosition);
    penguin.saveState(map, savedSprites);
    compressor.saveState(map);
    map.putInt("penguinId", penguin.getSavedId());
    nextBubble.saveState(map, savedSprites);
    map.putInt("nextBubbleId", nextBubble.getSavedId());
    map.putInt("currentColor", currentColor);
    map.putInt("nextColor", nextColor);
    if (movingBubble != null) {
      movingBubble.saveState(map, savedSprites);
      map.putInt("movingBubbleId", movingBubble.getSavedId());
    }
    else {
      map.putInt("movingBubbleId", -1);
    }
    bubbleManager.saveState(map);
    map.putInt("fixedBubbles", fixedBubbles);
    map.putDouble("moveDown", moveDown);
    map.putInt("nbBubbles", nbBubbles);
    map.putInt("blinkDelay", blinkDelay);
    hurrySprite.saveState(map, savedSprites);
    map.putInt("hurryId", hurrySprite.getSavedId());
    map.putInt("hurryTime", hurryTime);
    map.putBoolean("endOfGame", endOfGame);
    map.putBoolean("frozenify", frozenify);
    map.putInt("frozenifyX", frozenifyX);
    map.putInt("frozenifyY", frozenifyY);
    map.putInt("numSavedSprites", savedSprites.size());

    for (int i = 0; i < savedSprites.size(); i++) {
      ((Sprite)savedSprites.elementAt(i)).clearSavedId();
    }
  }

  private Sprite restoreSprite(Bundle map, Vector<BmpWrap> imageList, int i) {
    int left = map.getInt(String.format("%d-left", i));
    int right = map.getInt(String.format("%d-right", i));
    int top = map.getInt(String.format("%d-top", i));
    int bottom = map.getInt(String.format("%d-bottom", i));
    int type = map.getInt(String.format("%d-type", i));
    if (type == Sprite.TYPE_BUBBLE) {
      int color = map.getInt(String.format("%d-color", i));
      double moveX = map.getDouble(String.format("%d-moveX", i));
      double moveY = map.getDouble(String.format("%d-moveY", i));
      double realX = map.getDouble(String.format("%d-realX", i));
      double realY = map.getDouble(String.format("%d-realY", i));
      boolean fixed = map.getBoolean(String.format("%d-fixed", i));
      boolean blink = map.getBoolean(String.format("%d-blink", i));
      boolean released = map.getBoolean(String.format("%d-released", i));
      boolean checkJump = map.getBoolean(String.format("%d-checkJump", i));
      boolean checkFall = map.getBoolean(String.format("%d-checkFall", i));
      int fixedAnim = map.getInt(String.format("%d-fixedAnim", i));
      boolean frozen = map.getBoolean(String.format("%d-frozen", i));
      Point lastOpenPosition =
        new Point(map.getInt(String.format("%d-lastOpenPosition.x", i)),
                  map.getInt(String.format("%d-lastOpenPosition.y", i)));
      return new BubbleSprite(new Rect(left, top, right, bottom),
                              color, moveX, moveY, realX, realY,
                              fixed, blink, released, checkJump, checkFall,
                              fixedAnim,
                              (frozen ? frozenBubbles[color] : bubbles[color]),
                              lastOpenPosition,
                              bubblesBlind[color],
                              frozenBubbles[color],
                              targetedBubbles, bubbleBlink,
                              bubbleManager, soundManager, this);
    }
    else if (type == Sprite.TYPE_IMAGE) {
      int imageId = map.getInt(String.format("%d-imageId", i));
      return new ImageSprite(new Rect(left, top, right, bottom),
                             (BmpWrap)imageList.elementAt(imageId));
    }
    else if (type == Sprite.TYPE_LAUNCH_BUBBLE) {
      int currentColor = map.getInt(String.format("%d-currentColor", i));
      int currentDirection =
        map.getInt(String.format("%d-currentDirection", i));
      return new LaunchBubbleSprite(currentColor, currentDirection,
                                    launcher, bubbles, bubblesBlind);
    }
    else if (type == Sprite.TYPE_PENGUIN) {
      int currentPenguin = map.getInt(String.format("%d-currentPenguin", i));
      int count = map.getInt(String.format("%d-count", i));
      int finalState = map.getInt(String.format("%d-finalState", i));
      int nextPosition = map.getInt(String.format("%d-nextPosition", i));
      return new PenguinSprite(penguins, random, currentPenguin, count,
                               finalState, nextPosition);
    }
    else {
      Log.e("frozen-bubble", "Unrecognized sprite type: " + type);
      return null;
    }
  }

  public void pause() {
    resume();
    pausedSprite = new ImageSprite(new Rect(152, 190, 337, 116), gamePaused );
    this.addSprite(pausedSprite);
  }

  public void resume() {
    this.removeSprite(pausedSprite);
  }

  public void restoreState(Bundle map, Vector<BmpWrap> imageList) {
    Vector<Sprite> savedSprites = new Vector<Sprite>();
    int numSavedSprites = map.getInt("numSavedSprites");
    for (int i = 0; i < numSavedSprites; i++) {
      savedSprites.addElement(restoreSprite(map, imageList, i));
    }

    restoreSprites(map, savedSprites);
    jumping = new Vector<Sprite>();
    int numJumpingSprites = map.getInt("numJumpingSprites");
    for (int i = 0; i < numJumpingSprites; i++) {
      int spriteIdx = map.getInt(String.format("jumping-%d", i));
      jumping.addElement(savedSprites.elementAt(spriteIdx));
    }
    falling = new Vector<Sprite>();
    int numFallingSprites = map.getInt("numFallingSprites");
    for (int i = 0; i < numFallingSprites; i++) {
      int spriteIdx = map.getInt(String.format("falling-%d", i));
      falling.addElement(savedSprites.elementAt(spriteIdx));
    }
    bubblePlay = new BubbleSprite[8][13];
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        int spriteIdx = map.getInt(String.format("play-%d-%d", i, j));
        if (spriteIdx != -1) {
          bubblePlay[i][j] = (BubbleSprite)savedSprites.elementAt(spriteIdx);
        }
        else {
          bubblePlay[i][j] = null;
        }
      }
    }
    int launchBubbleId = map.getInt("launchBubbleId");
    launchBubble = (LaunchBubbleSprite)savedSprites.elementAt(launchBubbleId);
    launchBubblePosition = map.getDouble("launchBubblePosition");
    int penguinId = map.getInt("penguinId");
    penguin = (PenguinSprite)savedSprites.elementAt(penguinId);
    compressor.restoreState(map);
    int nextBubbleId = map.getInt("nextBubbleId");
    nextBubble = (ImageSprite)savedSprites.elementAt(nextBubbleId);
    currentColor = map.getInt("currentColor");
    nextColor = map.getInt("nextColor");
    int movingBubbleId = map.getInt("movingBubbleId");
    if (movingBubbleId == -1) {
      movingBubble = null;
    }
    else {
      movingBubble = (BubbleSprite)savedSprites.elementAt(movingBubbleId);
    }
    bubbleManager.restoreState(map);
    fixedBubbles = map.getInt("fixedBubbles");
    moveDown = map.getDouble("moveDown");
    nbBubbles = map.getInt("nbBubbles");
    blinkDelay = map.getInt("blinkDelay");
    int hurryId = map.getInt("hurryId");
    hurrySprite = (ImageSprite)savedSprites.elementAt(hurryId);
    hurryTime = map.getInt("hurryTime");
    endOfGame = map.getBoolean("endOfGame");
    frozenify = map.getBoolean("frozenify");
    frozenifyX = map.getInt("frozenifyX");
    frozenifyY = map.getInt("frozenifyY");
  }

  private void initFrozenify() {
    ImageSprite freezeLaunchBubble =
      new ImageSprite(new Rect(301, 389, 34, 42), frozenBubbles[currentColor]);
    ImageSprite freezeNextBubble =
      new ImageSprite(new Rect(301, 439, 34, 42), frozenBubbles[nextColor]);

    this.addSprite(freezeLaunchBubble);
    this.addSprite(freezeNextBubble);

    frozenifyX = 7;
    frozenifyY = 12;
    frozenify  = true;
  }

  private void frozenify() {
    frozenifyX--;
    if (frozenifyX < 0) {
      frozenifyX = 7;
      frozenifyY--;

      if (frozenifyY<0) {
        frozenify = false;
        this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
                                       gameLost));
        soundManager.playSound(FrozenBubble.SOUND_NOH);
        return;
      }
    }

    while (bubblePlay[frozenifyX][frozenifyY] == null && frozenifyY >=0) {
      frozenifyX--;
      if (frozenifyX < 0) {
        frozenifyX = 7;
        frozenifyY--;

        if (frozenifyY<0) {
          frozenify = false;
          this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
                                         gameLost));
          soundManager.playSound(FrozenBubble.SOUND_NOH);
          return;
        }
      }
    }

    this.spriteToBack(bubblePlay[frozenifyX][frozenifyY]);
    bubblePlay[frozenifyX][frozenifyY].frozenify();

    this.spriteToBack(launchBubble);
  }

  public BubbleSprite[][] getGrid() {
    return bubblePlay;
  }

  public void addFallingBubble(BubbleSprite sprite) {
    spriteToFront(sprite);
    falling.addElement(sprite);
  }

  public void deleteFallingBubble(BubbleSprite sprite) {
    removeSprite(sprite);
    falling.removeElement(sprite);
  }

  public void addJumpingBubble(BubbleSprite sprite) {
    spriteToFront(sprite);
    jumping.addElement(sprite);
  }

  public void deleteJumpingBubble(BubbleSprite sprite) {
    removeSprite(sprite);
    jumping.removeElement(sprite);
  }

  public Random getRandom() {
    return random;
  }

  public double getMoveDown() {
    return moveDown;
  }

  private void sendBubblesDown() {
    soundManager.playSound(FrozenBubble.SOUND_NEWROOT);

    for (int i=0 ; i<8 ; i++) {
      for (int j=0 ; j<12 ; j++) {
        if (bubblePlay[i][j] != null) {
          bubblePlay[i][j].moveDown();

          if (bubblePlay[i][j].getSpritePosition().y>=380) {
            penguin.updateState(PenguinSprite.STATE_GAME_LOST);
            endOfGame = true;
            initFrozenify();
            soundManager.playSound(FrozenBubble.SOUND_LOST);
          }
        }
      }
    }

    moveDown += 28.;
    compressor.moveDown();
  }

  private void blinkLine(int number) {
    int move = number % 2;
    int column = (number+1) >> 1;

    for (int i=move ; i<13 ; i++) {
      if (bubblePlay[column][i] != null) {
        bubblePlay[column][i].blink();
      }
    }
  }

  public int play(boolean key_left, boolean key_right,
                  boolean key_fire, boolean key_swap,
                  double trackball_dx,
                  boolean touch_fire, double touch_x, double touch_y,
                  boolean ats_touch_fire, double ats_touch_dx) {
    boolean ats = FrozenBubble.getAimThenShoot();

    if ((ats && ats_touch_fire) || (!ats && touch_fire)) {
      key_fire = true;
    }

    int[] move = new int[2];

    if (key_left && !key_right) {
      move[HORIZONTAL_MOVE] = KEY_LEFT;
    }
    else if (key_right && !key_left) {
      move[HORIZONTAL_MOVE] = KEY_RIGHT;
    }
    else {
      move[HORIZONTAL_MOVE] = 0;
    }

    if (key_fire) {
      move[FIRE] = KEY_UP;
    }
    else {
      move[FIRE] = 0;
    }

    if (key_swap) {
      if (!swapPressed) {
        swapNextLaunchBubble();
        swapPressed = true;
      }
    }
    else {
      swapPressed = false;
    }

    if (!ats && touch_fire && movingBubble == null) {
      double xx = touch_x - 318;
      double yy = 406 - touch_y;
      launchBubblePosition = (Math.PI - Math.atan2(yy, xx)) * 40.0 / Math.PI;
      if (launchBubblePosition < 1) {
        launchBubblePosition = 1;
      }
      if (launchBubblePosition > 39) {
        launchBubblePosition = 39;
      }
    }

    if (FrozenBubble.getDontRushMe()) {
      hurryTime = 1;
    }

    if (endOfGame) {
      if (move[FIRE] == KEY_UP) {
        //
        //   If endOfGame is set, then the play result absolutely
        //   cannot be GAME_PLAYING.
        //
        //   TODO: Figure out how play_result is arriving here with a
        //         value of GAME_PLAYING.
        //
        //         If this issue is addressed, then game_result can be
        //         eliminated.  The good news is that this issue only
        //         occurs when the level was lost, so this redundant
        //         variable workaround works, although it is suboptimal
        //         as the root cause remains unknown.
        //
        //
        if (play_result == GAME_PLAYING)
          play_result = game_result;

        if (play_result == GAME_WON) {
          levelManager.goToNextLevel();
          play_result = GAME_NEXT_WON;
        }
        else
          play_result = GAME_NEXT_LOST;

        return play_result;
      }
      else {
        penguin.updateState(PenguinSprite.STATE_VOID);

        if (frozenify) {
          frozenify();
        }
      }
    }
    else {
      if (move[FIRE] == KEY_UP || hurryTime > 480) {
        if (movingBubble == null) {
          nbBubbles++;

          movingBubble = new BubbleSprite(new Rect(302, 390, 32, 32),
                                          (int)launchBubblePosition,
                                          currentColor,
                                          bubbles[currentColor],
                                          bubblesBlind[currentColor],
                                          frozenBubbles[currentColor],
                                          targetedBubbles, bubbleBlink,
                                          bubbleManager, soundManager, this);
          this.addSprite(movingBubble);

          currentColor = nextColor;
          nextColor = bubbleManager.nextBubbleIndex(random);

          if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
            nextBubble.changeImage(bubbles[nextColor]);
          }
          else {
            nextBubble.changeImage(bubblesBlind[nextColor]);
          }
          launchBubble.changeColor(currentColor);
          penguin.updateState(PenguinSprite.STATE_FIRE);
          soundManager.playSound(FrozenBubble.SOUND_LAUNCH);
          hurryTime = 0;
          removeSprite(hurrySprite);
        }
        else {
          penguin.updateState(PenguinSprite.STATE_VOID);
        }
      }
      else {
        double dx = 0;
        if (move[HORIZONTAL_MOVE] == KEY_LEFT) {
          dx -= 1;
        }
        if (move[HORIZONTAL_MOVE] == KEY_RIGHT) {
          dx += 1;
        }
        dx += trackball_dx;
        if (ats) {
          dx += ats_touch_dx;
        }
        launchBubblePosition += dx;
        if (launchBubblePosition < 1) {
          launchBubblePosition = 1;
        }
        if (launchBubblePosition > 39) {
          launchBubblePosition = 39;
        }
        launchBubble.changeDirection((int)launchBubblePosition);
        if (dx < 0) {
          penguin.updateState(PenguinSprite.STATE_TURN_LEFT);
        }
        else if (dx > 0) {
          penguin.updateState(PenguinSprite.STATE_TURN_RIGHT);
        }
        else {
          penguin.updateState(PenguinSprite.STATE_VOID);
        }
      }
    }

    if (movingBubble != null) {
      movingBubble.move();
      if (movingBubble.fixed()) {
        if (movingBubble.getSpritePosition().y>=380 &&
            !movingBubble.released()) {
          penguin.updateState(PenguinSprite.STATE_GAME_LOST);
          highscoreManager.lostLevel();
          game_result = GAME_LOST;
          play_result = GAME_LOST;
          endOfGame = true;
          initFrozenify();
          soundManager.playSound(FrozenBubble.SOUND_LOST);
        }
        else if (bubbleManager.countBubbles() == 0) {
          penguin.updateState(PenguinSprite.STATE_GAME_WON);
          this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
                                         gameWon));
          highscoreManager.endLevel(nbBubbles);
          game_result = GAME_WON;
          play_result = GAME_WON;
          endOfGame = true;
          soundManager.playSound(FrozenBubble.SOUND_WON);
        }
        else {
          fixedBubbles++;
          blinkDelay = 0;

          if (fixedBubbles == 8) {
            fixedBubbles = 0;
            sendBubblesDown();
          }
        }
        movingBubble = null;
      }

      if (movingBubble != null) {
        movingBubble.move();
        if (movingBubble.fixed()) {
          if (movingBubble.getSpritePosition().y>=380 &&
              !movingBubble.released()) {
            penguin.updateState(PenguinSprite.STATE_GAME_LOST);
            highscoreManager.lostLevel();
            game_result = GAME_LOST;
            play_result = GAME_LOST;
            endOfGame = true;
            initFrozenify();
            soundManager.playSound(FrozenBubble.SOUND_LOST);
          }
          else if (bubbleManager.countBubbles() == 0) {
            penguin.updateState(PenguinSprite.STATE_GAME_WON);
            this.addSprite(new ImageSprite(new Rect(152, 190,
                                                    152 + 337,
                                                    190 + 116), gameWon));
            highscoreManager.endLevel(nbBubbles);
            game_result = GAME_WON;
            play_result = GAME_WON;
            endOfGame = true;
            soundManager.playSound(FrozenBubble.SOUND_WON);
          }
          else {
            fixedBubbles++;
            blinkDelay = 0;

            if (fixedBubbles == 8) {
              fixedBubbles = 0;
              sendBubblesDown();
            }
          }
          movingBubble = null;
        }
      }
    }

    if (movingBubble == null && !endOfGame) {
      hurryTime++;
      // If hurryTime == 2 (1 + 1) we could be in the "Don't rush me"
      // mode.  Remove the sprite just in case the user switched
      // to this mode when the "Hurry" sprite was shown, to make it
      // disappear.
      if (hurryTime == 2) {
        removeSprite(hurrySprite);
      }
      if (hurryTime>=240) {
        if (hurryTime % 40 == 10) {
          addSprite(hurrySprite);
          soundManager.playSound(FrozenBubble.SOUND_HURRY);
        }
        else if (hurryTime % 40 == 35) {
          removeSprite(hurrySprite);
        }
      }
    }

    if (fixedBubbles == 6) {
      if (blinkDelay < 15) {
        blinkLine(blinkDelay);
      }

      blinkDelay++;
      if (blinkDelay == 40) {
        blinkDelay = 0;
      }
    }
    else if (fixedBubbles == 7) {
      if (blinkDelay < 15) {
        blinkLine(blinkDelay);
      }

      blinkDelay++;
      if (blinkDelay == 25) {
        blinkDelay = 0;
      }
    }

    for (int i=0 ; i<falling.size() ; i++) {
      ((BubbleSprite)falling.elementAt(i)).fall();
    }

    for (int i=0 ; i<jumping.size() ; i++) {
      ((BubbleSprite)jumping.elementAt(i)).jump();
    }

    return GAME_PLAYING;
  }

  public void paint(Canvas c, double scale, int dx, int dy) {
    compressor.paint(c, scale, dx, dy);
    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
      nextBubble.changeImage(bubbles[nextColor]);
    }
    else {
      nextBubble.changeImage(bubblesBlind[nextColor]);
    }
    super.paint(c, scale, dx, dy);
  }

  public void setPosition(double value) {
    launchBubblePosition = value;
    if (launchBubblePosition < 1) {
      launchBubblePosition = 1;
    }
    if (launchBubblePosition > 39) {
      launchBubblePosition = 39;
    }
    launchBubble.changeDirection((int)launchBubblePosition);
    penguin.updateState(PenguinSprite.STATE_VOID);
  }

  public void swapNextLaunchBubble() {
    if (currentColor != nextColor) {
      int tempColor = currentColor;
      currentColor  = nextColor;
      nextColor     = tempColor;

      launchBubble.changeColor(currentColor);

      if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL)
        nextBubble.changeImage(bubbles[nextColor]);
      else
        nextBubble.changeImage(bubblesBlind[nextColor]);

      soundManager.playSound(FrozenBubble.SOUND_WHIP);
    }
  }
}
