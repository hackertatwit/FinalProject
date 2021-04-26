package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SpaceInvaders extends Application {
	

	private static final Random RAND = new Random(); // Random number generator
	private static final int WIDTH = 800; // Canvas dimensions
	private static final int HEIGHT = 600;
	private static final int PLAYER_SIZE = 60; // Player & Enemy hitbox size
	
	static final Image PLAYER_SPRITE = new Image("file:/images/rocket.png");
	static final Image EXPLOSION_SPRITE = new Image("file:/images/explosion2.png");
	static final Image BACKGROUND_SPRITE = new Image("file:/images/asteroid.png");

	static final int EXPLOSION_WIDTH = 128;
	static final int EXPLOSION_ROWS = 3;
	static final int EXPLOSION_COL = 3;
	static final int EXPLOSION_HEIGHT = 128;
	static final int EXPLOSION_STEPS = 15;
	final int MAX_ENEMIES = 10,  MAX_SHOTS = MAX_ENEMIES * 2;
	// Enemy icons
	static final Image ENEMIES_SPRITE[] = {
			new Image("file:/images/bomber1.png"),
			new Image("file:/images/bomber2.png"),
			new Image("file:/images/bomber3.png"),
			new Image("file:/images/bomber4.png"),
			new Image("file:/images/bomber5.png"),
			new Image("file:/images/bomber6.png"),
			new Image("file:/images/bomber7.png"),
			new Image("file:/images/bomber8.png"),
			new Image("file:/images/bomber9.png"),
			new Image("file:/images/bomber10.png")
	};

	boolean gameStarted = false;
	boolean gameOver = false;
	private GraphicsContext gc;
	
	Player player;
	List<Shot> shots;
	List<Background> bg;
	List<Enemy> enemies;
	
	private double mouseX;
	private int score; // global score tracker

	// Creates canvas and animation cycle
	public void start(Stage stage) throws Exception {
		Canvas canvas = new Canvas(WIDTH, HEIGHT);	
		gc = canvas.getGraphicsContext2D();
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
		canvas.setCursor(Cursor.MOVE);
		canvas.setOnMouseMoved(e -> mouseX = e.getX());
		canvas.setOnMouseClicked(e -> {
			if(shots.size() < MAX_SHOTS) shots.add(player.shoot());
			if(!gameStarted) {
				gameStarted = true;
				setup();
			}
			else if(gameOver) {
				gameOver = false;
				setup();
			}
		});
		setup();
		stage.setScene(new Scene(new StackPane(canvas)));
		stage.setTitle("Space Invaders");
		stage.show();
		
	}

	// Initializes player's score, possible enemies per wave, and makes ArrayLists to hold repeating items
	private void setup() {
		bg = new ArrayList<>();
		shots = new ArrayList<>();
		enemies = new ArrayList<>();
		player = new Player(WIDTH / 2, HEIGHT - PLAYER_SIZE, PLAYER_SIZE, PLAYER_SPRITE);
		score = 0;
		IntStream.range(0, MAX_ENEMIES).mapToObj(i -> this.newEnemy()).forEach(enemies::add);
	}
	
	// run separate graphics for:
	// 1. Game waiting to start
	// 2. Game in progress
	// 3. Play again prompt
	private void run(GraphicsContext gc) {

		gc.setFill(Color.grayRgb(20));
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.setTextAlign(TextAlignment.CENTER);

		if(gameStarted) {
			gc.setFont(Font.font(20));
			gc.setFill(Color.WHITE);
			gc.fillText("Score: " + score, 60,  20);
		}
		else
		{
			gc.setFont(Font.font(35));
			gc.setFill(Color.YELLOW);
			gc.fillText("Click to play", WIDTH / 2, HEIGHT /2.5);
		}
		
		if(gameOver) {
			gc.setFont(Font.font(35));
			gc.setFill(Color.YELLOW);
			gc.fillText("Game Over \n Your Score is: " + score + " \n Click to play again", WIDTH / 2, HEIGHT /2.5);
		}

		bg.forEach(Background::draw);
	
		player.update();
		player.draw();
		player.posX = (int) mouseX;
		
		enemies.stream().peek(Player::update).peek(Player::draw).forEach(e -> {
			if(player.collide(e) && !player.exploding) {
				player.explode();
			}
		});
		
		
		for (int i = shots.size() - 1; i >=0 ; i--) {
			Shot shot = shots.get(i);
			if(shot.posY < 0 || shot.toRemove)  { 
				shots.remove(i);
				continue;
			}
			shot.update();
			shot.draw();
			for (Enemy enemy : enemies) {
				if(shot.collide(enemy) && !enemy.exploding) {
					score++;
					enemy.explode();
					shot.toRemove = true;
				}
			}
		}
		
		for (int i = enemies.size() - 1; i >= 0; i--){
			if(enemies.get(i).destroyed)  {
				enemies.set(i, newEnemy());
			}
		}
	
		gameOver = player.destroyed;
		if(RAND.nextInt(10) > 2) {
			bg.add(new Background());
		}
		for (int i = 0; i < bg.size(); i++) {
			if(bg.get(i).posY > HEIGHT)
				bg.remove(i);
		}
	}

	// Defines methods dealing with player's actions i.e., shooting, collisions, explosions. Also inherited by Enemy class
	public class Player {

		int posX, posY, size;
		boolean exploding, destroyed;
		Image img;
		int explosionStep = 0;
		
		public Player(int posX, int posY, int size,  Image image) {
			this.posX = posX;
			this.posY = posY;
			this.size = size;
			img = image;
		}
		
		public Shot shoot() {
			return new Shot(posX + size / 2 - Shot.size / 2, posY - Shot.size);
		}

		public void update() {
			if(exploding) explosionStep++;
			destroyed = explosionStep > EXPLOSION_STEPS;
		}
		
		public void draw() {
			if(exploding) {
				gc.drawImage(EXPLOSION_SPRITE, explosionStep % EXPLOSION_COL * EXPLOSION_WIDTH, (explosionStep / EXPLOSION_ROWS) * EXPLOSION_HEIGHT + 1,
						EXPLOSION_WIDTH, EXPLOSION_HEIGHT,
						posX, posY, size-20, size-20);
			}
			else {
				gc.drawImage(img, posX, posY, size-10, size);
			}
		}
	
		public boolean collide(Player other) {
			int d = distance(this.posX + size / 2, this.posY + size /2, 
							other.posX + other.size / 2, other.posY + other.size / 2);
			return d < other.size / 2 + this.size / 2 ;
		}
		
		public void explode() {
			exploding = true;
			explosionStep = -1;
		}

	}
	
	// Enemy is the same as Player, but it moves. The higher the score, the faster it goes.
	public class Enemy extends Player {
		
		int SPEED = (score/5)+2;
		
		public Enemy(int posX, int posY, int size, Image image) {
			super(posX, posY, size, image);
		}
		
		public void update() {
			super.update();
			if(!exploding && !destroyed) posY += SPEED;
			if(posY > HEIGHT) destroyed = true;
		}
	}

	// Detects collision and increases shot speed when score hits 50
	public class Shot {
		
		public boolean toRemove;

		int posX, posY, speed = 10;
		static final int size = 6;
			
		public Shot(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}

		public void update() {
			posY-=speed;
		}
		

		public void draw() {
			gc.setFill(Color.YELLOWGREEN);
			if (score >=50 && score<=70 || score>=120) {
				gc.setFill(Color.YELLOWGREEN);
				speed = 50;
				gc.fillRect(posX-5, posY-10, size+10, size+30);
			} else {
			gc.fillOval(posX, posY, size, size);
			}
		}
		
		public boolean collide(Player Player) {
			int distance = distance(this.posX + size / 2, this.posY + size / 2,
					Player.posX + Player.size / 2, Player.posY + Player.size / 2);
			return distance  < Player.size / 2 + size / 2;
		} 
		
		
	}

	// Creates the background, populated with random objects
	public class Background {
		int posX, posY;
		private int h, w, r, g, b;
		private double opacity;
		
		public Background() {
			posX = RAND.nextInt(WIDTH);
			posY = 0;
			w = RAND.nextInt(5) + 1;
			h =  RAND.nextInt(5) + 1;
			r = RAND.nextInt(100) + 150;
			g = RAND.nextInt(100) + 150;
			b = RAND.nextInt(100) + 150;
			opacity = RAND.nextFloat();
			if(opacity < 0) opacity *=-1;
			if(opacity > 0.5) opacity = 0.5;
		}
		
		public void draw() {
			if(opacity > 0.8) opacity-=0.01;
			if(opacity < 0.1) opacity+=0.01;
			gc.setFill(Color.rgb(r, g, b, opacity));

			/* uncomment to include asteroids in background
				if(posX % 20 == 0)
				{
					gc.drawImage(BACKGROUND_IMG, posX, posY, w+20, h+20);

				}
			*/
			gc.fillOval(posX, posY, w, h);

			posY+=20;
		}
	}
	
	// Generates new enemies
	Enemy newEnemy() {
		return new Enemy(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, ENEMIES_SPRITE[RAND.nextInt(ENEMIES_SPRITE.length)]);
	}

	// Distance formula
	int distance(int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
	}
	
	
	public static void main(String[] args) {
		launch();
	}
}

