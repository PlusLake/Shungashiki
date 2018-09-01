package main;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Main
{
	private static final Dimension windowSize = new Dimension(720,480);
	private static final int imageWidth = 1500;
	private static final int imageHeight = 500;
	private static final double ratio = (double)windowSize.width / (double)imageWidth;
	public static final Color backColor = new Color(255,224,224);
	private static final Color edgeColor = new Color(255,192,192);
	private static final Color ballColor = new Color(255,252,252);
	private static final Color transFontColor = new Color(0,0,0,192);
	private static final Color twitterHoverColor = new Color(128,128,192);
	private static final int edgeWidth = 32;
	private static final int whiteEdgeWidth = 8;
	private static final int ballInterval = 32;
	private static final int ballSize = 10;
	private static final int ballRate = 2;
	private static final int twitterX = 512;
	private static final Cursor cursorHand = new Cursor(Cursor.HAND_CURSOR);
	private static final Cursor cursorDefault = new Cursor(Cursor.DEFAULT_CURSOR);
	private static final Rectangle twitterRect = new Rectangle(twitterX,0,250,28);
	private static final Rectangle qualityRect = new Rectangle(0,0,50,28);

	private JFrame window;
	private Canvas canvas;
	private BufferedImage background;
	private BufferedImage twitter;
	@SuppressWarnings("unchecked")
	private ArrayList<BufferedImage>[] images = (ArrayList<BufferedImage>[]) new ArrayList<?>[8];
	private Graphics2D graphics;
	private Path2D[] characterPath = new Path2D[8];
	private Scene scene;
	private ArrayList<ClickEffect> clickList = new ArrayList<ClickEffect>();
	private Font font;
	private Font twitterFont;

	private boolean initDone;
	private boolean onTwitter;
	private boolean onQuality;
	private boolean highQuality = true;
	private int imagePosY;
	private int ballAnimation;
	private int twitterFontY;
	private int[] imageFinalSize = new int[2];

	private int[] characterQuad = new int[]{
			0,
			0,
			133,
			178,
			331,
			377,
			542,
			560,
			719,
			769,
			912,
			948,
			1148,
			1146,
			1378,
			1327,
			imageWidth,
			imageWidth,
	};

	private Main()
	{
		init();
		initVariables();
		loadExternal();
		initLoop();
	}

	private synchronized void graphicsLoop(Graphics2D g)
	{
		g.clearRect(0,0,windowSize.width,windowSize.height);

		if(!scene.getTheaterMode())
		{
			if(highQuality)
			{
				g.setColor(ballColor);
				int half = ballInterval /2;
				for(int y = 0; y < 9 * ballRate; y++)
				{
					for(int x = 0; x < 16 * ballRate; x++)
					{
						g.fillOval(x * ballInterval - ballAnimation, y * ballInterval - ballAnimation, ballSize, ballSize);
						g.fillOval(x * ballInterval + half - ballAnimation, y * ballInterval + half - ballAnimation, ballSize, ballSize);
					}
				}
			}


			g.setColor(edgeColor);
			g.fillRect(0,imagePosY - edgeWidth,windowSize.width,imageFinalSize[1] + 2 * edgeWidth);
			g.setColor(Color.WHITE);
			g.fillRect(0,imagePosY - whiteEdgeWidth,windowSize.width,imageFinalSize[1] + 2 * whiteEdgeWidth);
			AffineTransform af = AffineTransform.getTranslateInstance(scene.getImageOffSet(),imagePosY);
			af.scale(ratio,ratio);
			g.drawImage(background,af,null);
		}

		scene.draw(g);

		for(int x = clickList.size() - 1; x >= 0; x--)
		{
			if(clickList.get(x).isDead())
			{
				clickList.remove(x);
				continue;
			}
			clickList.get(x).draw(g);
		}

		if(!scene.getTheaterMode())
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.64f));
			g.drawImage(twitter,twitterX,4,24,24,null);
			g.setFont(twitterFont);
			g.setColor(onTwitter ? twitterHoverColor : transFontColor);
			g.drawString("春画式 @reply0715",twitterX + 28,twitterFontY + 5);
			g.setColor(transFontColor);
			g.drawString("画質",5,twitterFontY + 5);
			g.setFont(font);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		}

		scene.drawTop(g);

		canvas.getBufferStrategy().show();
	}

	private synchronized void logicLoop()
	{
		ballAnimation++;
		if(ballAnimation >= 32)
		{
			ballAnimation = 0;
		}

		for(int x = clickList.size() - 1; x >= 0; x--)
		{
			clickList.get(x).tick();
		}

		if(!scene.getTheaterMode())
		{
			Point p = canvas.getMousePosition();
			if(p!=null)
			{
				onTwitter = twitterRect.contains(p);
				canvas.setCursor(onTwitter ? cursorHand : cursorDefault);
				if(!onTwitter)
				{
					onQuality = qualityRect.contains(p);
					canvas.setCursor(onQuality ? cursorHand : cursorDefault);
				}
			}
			else
			{
				onTwitter = onQuality = false;
			}
		}
		else
		{
			onTwitter = onQuality = false;
		}

		scene.tick();
	}

	private void initVariables()
	{
		imageFinalSize[0] = (int) (background.getWidth() * ratio);
		imageFinalSize[1] = (int) (background.getHeight() * ratio);
		imagePosY = (windowSize.height - imageFinalSize[1]) /2;

		for(int x = 0; x < characterPath.length; x++)
		{
			images[x] = new ArrayList<BufferedImage>();
			characterPath[x] = new Path2D.Double();
			Path2D p = characterPath[x];
			p.moveTo(characterQuad[x*2],x%2 == 0 ? 0 : imageHeight);
			int y = x%2 == 1 ? 0b100 : 0b011;
			for(int k = 1; k <= 3; k++)
			{
				p.lineTo(characterQuad[x*2 + k],((y >>> (k-1))&1) == 1 ? imageHeight : 0);
			}
			p.closePath();
			AffineTransform at = AffineTransform.getTranslateInstance(0,imagePosY);
			at.scale(ratio,ratio);
			p.transform(at);
		}

		scene = new Scene(this,characterPath,canvas,font,images,graphics);

		initDone = true;
	}

	private void initLoop()
	{
		makeThread(() -> graphicsLoop(graphics),60,true).start();
		makeThread(() -> logicLoop(),32,false).start();
	}

	private void init()
	{
		window = new JFrame("春画式画像ビューア");
		canvas = new Canvas();
		canvas.setPreferredSize(windowSize);
		window.add(canvas);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
		window.setVisible(true);
		window.pack();
		canvas.createBufferStrategy(2);
		graphics = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		graphics.setBackground(backColor);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		window.setLocation(d.width/2-window.getSize().width/2, d.height/2-window.getSize().height/2);
		canvas.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e)
			{
				clickList.add(new ClickEffect(e.getX(),e.getY()));
				scene.onClick(e);
				if(onTwitter)
				{
					try
					{
						Desktop.getDesktop().browse(new URI("http://twitter.com/reply0715"));
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				if(onQuality)
				{
					highQuality = !highQuality;
					if(highQuality)
					{
						graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
						graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					}
					else
					{
						graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
						graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					}
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
		});
		try
		{
			background = ImageIO.read(getInputStream("/resource/image.png"));
			twitter = ImageIO.read(getInputStream("/resource/twitter.png"));
			window.setIconImage(ImageIO.read(getInputStream("/resource/icon.jpg")));

			font = Font.createFont(Font.TRUETYPE_FONT,getInputStream("/resource/azuki.ttf")).deriveFont(32f);
			twitterFont = font.deriveFont(20f);
			twitterFontY = graphics.getFontMetrics(twitterFont).getAscent();
			graphics.setFont(font);
		}
		catch (Exception e){}
	}

	private InputStream getInputStream(String Location)
	{
		return Main.class.getResourceAsStream(Location);
	}

	private Thread makeThread(tick t,int rate,boolean maxPriority)
	{
		return new Thread()
		{
			@Override
			public synchronized void run()
			{
				try
				{
					if(maxPriority)
					{
						this.setPriority(10);
					}
					int counter = 0;
					long refTime = System.nanoTime();
					long offSet = 0;
					int fpsTarget = rate;
					int inverseFpsTarget = 1000000000 / fpsTarget;
					double fps = 0f;
					while(true)
					{
						if(!initDone)
						{
							return;
						}
						long start = System.nanoTime();
						t.run();
						counter++;
						long wait = inverseFpsTarget - System.nanoTime() + start - offSet;
						start = System.nanoTime();
						if(wait>0)
						{
							this.wait(wait/1000000,(int) wait%1000000);
						}
						else
						{
							offSet = 0;
						}
						offSet = System.nanoTime() - start - wait;
						if(counter >= fpsTarget)
						{
							fps = 1000000000.0 / (System.nanoTime() - refTime) * fpsTarget;
							refTime = System.nanoTime();
							System.out.println(fps);
							counter = 0;
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.exit(0);
				}
			}
		};
	}

	private void loadExternal()
	{
		String root = ClassLoader.getSystemClassLoader().getResource(".").getPath();
		loadImage(root + "/image/");
		loadTitle(root + "/");
	}

	private void loadImage(String dir)
	{
		File rootFile = new File(dir);
		if(!rootFile.exists())
		{

			return;
		}
		for(int x = 0; x < 8; x++)
		{
			File sub = new File(dir + x + "/");
			if(sub.exists())
			{
				for(int i = 0; i < 100; i++)
				{
					File image = new File(sub.getAbsolutePath() + String.format("\\%02d.jpg",i));
					if(!image.exists())
					{
						image = new File(sub.getAbsolutePath() + String.format("\\%02d.png",i));
					}
					if(!image.exists())
					{
						break;
					}
					BufferedImage bi = null;
					try
					{
						bi = ImageIO.read(image);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						break;
					}
					images[x].add(bi);
				}
			}
		}
	}

	private void loadTitle(String dir)
	{
		File f = new File(dir + "title.txt");
		if(!f.exists())
		{
			return;
		}
		BufferedReader br = null;
		String[] buffer = new String[8];

		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
			String temp = br.readLine();
			if (temp != null && temp.startsWith("\uFEFF"))
			{
				temp = temp.substring(1);
			}
			int x = 0;
			while(temp != null && x <= 7)
			{
				System.out.println(temp);
				buffer[x++] = temp;
				temp = br.readLine();
			}
			br.close();

			scene.setTitle(buffer,graphics);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	public static double genValue(double curve,double input)
	{
		double c = (curve >= -0.5 && curve <= 0.5) ? curve : 0;
		double x = (input >= 0 && input <= 1) ? input * Math.sqrt(2) : 0;
		double temp = x - Math.sin(Math.PI/4);
		double y = c * (temp * temp) - 0.5 * c;
		Point2D p = new Point2D.Double(x,y);
		AffineTransform.getRotateInstance(Math.PI/4).transform(p,p);
		double returnValue = p.getY();
		if(returnValue > 1)
		{
			returnValue = 1;
		}
		else if(returnValue < 0)
		{
			returnValue = 0;
		}
		return returnValue;
	}

	public Graphics2D getGraphics()
	{
		return graphics;
	}

	private interface tick
	{
		public void run();
	}

	public static void main(String[] args)
	{
		new Main();
	}
}