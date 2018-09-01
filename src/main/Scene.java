package main;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Scene
{
	private static final BasicStroke pathStroke = new BasicStroke(2f);
	private static final BasicStroke fontStroke = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Color fontColor = new Color(16,16,16);
	private static final double progressSpeed = 0.064;
	private static final int hoverAlpha = 192;
	private static final int targetX = 64;
	private static final int buttonPadding = 16;
	private static final int imagePadding = 8;
	private static final int titleX = 16;
	private static final int titleY = 64;

	private boolean[] hover = new boolean[8];
	private boolean theaterMode;
	private int[] center = new int[8];
	private String state = "menu";
	private double imageOffSet;
	private double progress;
	private float previewImageAlpha;
	private int selecting = -1;
	private int sceneChaningColor;
	private int sceneChaningAlpha;
	private int theaterChaningAlpha;
	private int diffX;

	private Main parent;
	private ArrayList<BufferedImage>[] images;
	private Path2D[] pathList;
	private Path2D[] pathListMoving;
	private Shape[] shapeList;
	private Canvas canvas;
	private Button[] buttonList;
	private Font font;
	private Theater theater;

	private int titleNow = -1;
	private String[] title = new String[]{
			"タイトル",
			"ヌードデッサンモデルの山田さん（仮）",
			"バスケ部マネージャーの吉村さん",
			"タイトル",
			"タイトル",
			"無知ムチ妹ちゃん",
			"タイトル",
			"無自覚えっちな大田友香さん",
	};

	public void setTitle(String[] s,Graphics2D g)
	{
		title = s;
		loadTitleShape(g);
	}

	private void loadTitleShape(Graphics2D g)
	{
		shapeList = new Shape[pathList.length];
		for(int x = 0; x < pathList.length; x++)
		{
			shapeList[x] = new TextLayout(title[x], font, g.getFontRenderContext()).getOutline(AffineTransform.getTranslateInstance(titleX,g.getFontMetrics(font).getAscent() + titleY));
		}
	}

	public Scene(Main parent,Path2D[] paths,Canvas c,Font f,ArrayList<BufferedImage>[] bi,Graphics2D g)
	{
		this.parent = parent;
		images = bi;
		font = f;
		pathList = paths;
		pathListMoving = new Path2D[paths.length];
		canvas = c;

		for(int x = 0; x < paths.length; x++)
		{
			center[x] = (int) paths[x].getBounds().getCenterX();
		}

		loadTitleShape(g);
	}

	public void draw(Graphics2D g)
	{
		switch(state)
		{
			case "return to menu":
			case "menu":
				titleNow = -1;
				g.setColor(new Color(0,0,0,192));
				for(int x = 0; x < pathList.length; x++)
				{
					Point p = canvas.getMousePosition();
					if(p!=null ? !pathList[x].contains(p) : true)
					{
						g.fill(pathList[x]);
					}
					else
					{
						titleNow = x;
					}
				}
				break;

			case "reshow menu":
			case "menu selected":
				g.setStroke(pathStroke);
				for(int x = 0; x < pathList.length; x++)
				{
					if(x == selecting)
					{
						if(state.equals("reshow menu"))
						{
							g.setColor(new Color(0,0,0,(int) ((1-(sceneChaningAlpha/(255.0 - hoverAlpha))) * hoverAlpha)));
							g.fill(pathList[x]);
							g.draw(pathList[x]);
						}
					}
					else
					{
						g.setColor(new Color(sceneChaningColor,sceneChaningColor,sceneChaningColor,hoverAlpha + sceneChaningAlpha));
						g.fill(pathList[x]);
						g.draw(pathList[x]);
					}
				}
				break;

			case "move back":
			case "move to left":
				drawMoving(g);
				if(state.equals("move back"))
				{
					buttonDraw(g);
					drawPreviewImage(g);
				}
				break;

			case "to theater mode":
			case "move done":
				drawMoving(g);

				drawPreviewImage(g);
				buttonDraw(g);
				break;

			case "theater mode":
				theater.draw(g);
				buttonDraw(g);
				break;
		}

		if(!theaterMode)
		{
			drawTitle(g);
		}

	}

	public void drawTop(Graphics2D g)
	{
		if(theaterChaningAlpha > 0)
		{
			g.setColor(new Color(0,0,0,theaterChaningAlpha));
			g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
		}
	}

	public void tick()
	{
		boolean resetProgress = false;
		switch(state)
		{
			case "menu selected":
				progress += progressSpeed;
				if(progress >= 1)
				{
					resetProgress = true;
					diffX = -(center[selecting] - targetX);
					progress = 1;

					for(int x = 0; x < pathList.length; x++)
					{
						pathListMoving[x] = (Path2D) pathList[x].clone();
					}

					state = "move to left";
				}

				double p = Main.genValue(-0.5,progress);
				sceneChaningColor = (int) (255 * p);
				sceneChaningAlpha = (int) ((255 - hoverAlpha) * p);
				break;

			case "move to left":
				progress += 0.024;
				if(progress >= 1)
				{
					resetProgress = true;
					progress = 1;
					state = "move done";

					buttonList = new Button[2];
					buttonList[0] = new Button(
							buttonPadding + Button.radius,
							canvas.getHeight() - buttonPadding - Button.radius,
							true,
							() ->
							{
								state = "move back";
								progress = 0;
								for(Button b : buttonList)
								{
									b.kill();
								}
							});

					buttonList[1] = new Button(
							canvas.getWidth() - buttonPadding - Button.radius,
							canvas.getHeight() - buttonPadding - Button.radius,
							false,
							()->{
								theater = new Theater(canvas,images[selecting]);
								state = "to theater mode";
								progress = 0;
							});
				}

				double curveValue0 = Main.genValue(-0.5,progress);
				imageOffSet = (int) (diffX * curveValue0);
				for(int x = 0; x < pathList.length; x++)
				{
					pathListMoving[x] = (Path2D) pathList[x].clone();
					pathListMoving[x].transform(AffineTransform.getTranslateInstance(diffX * curveValue0,0));
				}
				break;

			case "move done":
				buttonTick();
				if(progress < 1)
				{
					progress += 0.064;
					previewImageAlpha = (float) progress;
					if(progress >= 1)
					{
						previewImageAlpha = 1;
					}
				}
				break;

			case "move back":
				progress += 0.024;
				if(progress >= 1)
				{
					resetProgress = true;
					progress = 1;
					state = "reshow menu";
				}

				float tempAlpha = (float) (1 - progress * 3);
				previewImageAlpha = tempAlpha < 0 ? 0 : tempAlpha;

				double curveValue1 = Main.genValue(0.5,1 - progress);
				imageOffSet = (int) (diffX * curveValue1);
				for(int x = 0; x < pathList.length; x++)
				{
					pathListMoving[x] = (Path2D) pathList[x].clone();
					pathListMoving[x].transform(AffineTransform.getTranslateInstance(diffX * curveValue1,0));
				}
				buttonTick();
				break;

			case "reshow menu":
				progress += progressSpeed;
				if(progress >= 1)
				{
					resetProgress = true;
					progress = 1;
					state = "menu";
				}

				double curveValue2 = Main.genValue(-0.5,1-progress);
				sceneChaningColor = (int) (255 * curveValue2);
				sceneChaningAlpha = (int) ((255 - hoverAlpha) * curveValue2);
				break;

			case "to theater mode":
				progress += 0.032;
				if(progress >= 1)
				{
					resetProgress = true;
					theaterMode = true;
					previewImageAlpha = 0;
					theaterChaningAlpha = 0;
					state = "theater mode";
					parent.getGraphics().setBackground(Color.BLACK);

					buttonList = new Button[3];
					buttonList[0] = new Button(
							buttonPadding + Button.radius,
							canvas.getHeight() - buttonPadding - Button.radius,
							true,
							() ->
							{
								imageOffSet = 0;
								theaterChaningAlpha = 255;
								parent.getGraphics().setBackground(Main.backColor);
								state = "return to menu";
								theaterMode = false;
							});

					buttonList[1] = new Button(
							canvas.getWidth() - 2 * buttonPadding - 3 * Button.radius,
							canvas.getHeight() - buttonPadding - Button.radius,
							true,
							()->{
								theater.movePage(false);
							});

					buttonList[2] = new Button(
							canvas.getWidth() - buttonPadding - Button.radius,
							canvas.getHeight() - buttonPadding - Button.radius,
							false,
							()->{
								theater.movePage(true);
							});
					break;
				}
				theaterChaningAlpha = (int) (progress * 255);
				break;

			case "theater mode":
				buttonTick();
				buttonList[1].setVisible(theater.hasNext(false));
				buttonList[2].setVisible(theater.hasNext(true));
				break;

			case "return to menu":
				progress += 0.032;

				if(progress >= 1)
				{
					progress = 1;
					resetProgress = true;
					state = "menu";
				}

				theaterChaningAlpha = (int) (255 - (progress * 255));
				break;
		}

		if(resetProgress)
		{
			progress = 0;
			resetProgress = false;
		}
	}

	private void buttonDraw(Graphics2D g)
	{
		for(Button b : buttonList)
		{
			b.draw(g);
		}
	}

	private void buttonTick()
	{
		for(Button b : buttonList)
		{
			b.tick(canvas.getMousePosition());
		}
	}

	private void drawMoving(Graphics2D g)
	{
		g.setStroke(pathStroke);
		for(int x = 0; x < pathList.length; x++)
		{
			if(x != selecting)
			{
				g.setColor(new Color(255,255,255,255));
				g.fill(pathListMoving[x]);
				g.draw(pathListMoving[x]);
			}
		}
	}

	private void drawPreviewImage(Graphics2D g)
	{
		if(images[selecting].size() > 0)
		{
			BufferedImage i = images[selecting].get(0);
			double scale = (canvas.getHeight() - 2.0 * imagePadding) / i.getHeight();
			AffineTransform at = AffineTransform.getTranslateInstance(canvas.getWidth() - imagePadding - i.getWidth() * scale,imagePadding);
			at.scale(scale,scale);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, previewImageAlpha));
			g.drawImage(i,at,null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		}
	}

	private void drawTitle(Graphics2D g)
	{
		if(titleNow == -1)
		{
			return;
		}

		g.setColor(Color.white);
		g.setStroke(fontStroke);
		g.draw(shapeList[titleNow]);

		g.setColor(fontColor);
		g.drawString(title[titleNow],titleX,g.getFontMetrics(font).getAscent() + titleY);
	}

	public void onClick(MouseEvent e)
	{
		switch(state)
		{
			case "menu":
				for(int x = 0; x < pathList.length; x++)
				{
					if(pathList[x].contains(e.getPoint()))
					{
						selecting = x;
						state = "menu selected";
						break;
					}
				}
				break;
			case "theater mode":
			case "move done":
				for(Button b : buttonList)
				{
					b.click(e.getPoint());
				}
				break;
		}
	}

	public void setHover(int x, boolean b)
	{
		hover[x] = b;
	}

	public boolean getTheaterMode()
	{
		return theaterMode;
	}

	public double getImageOffSet()
	{
		return imageOffSet;
	}
}