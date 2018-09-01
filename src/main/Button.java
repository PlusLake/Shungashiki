package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class Button
{
	public static final int radius = 24;
	private static final int[][] color = new int[][] {{255,192,192},{255,255,255}};
	private static final int maxAlpha = 224;
	private static final int triangleSize = 16;
	private static final double progressSpeed = 0.064;

	private int x;
	private int y;
	private int alpha;
	private double progress;
	private int hover;
	private boolean dead;
	private boolean visible = true;
	private Path2D triangle = new Path2D.Double();
	private ClickEvent clickEvent;

	public Button(int x, int y,boolean left,ClickEvent ce)
	{
		this.x = x;
		this.y = y;
		clickEvent = ce;

		triangle.moveTo(triangleSize,0);
		Point2D p = new Point2D.Double(triangleSize,0);
		Point2D p0 = new Point2D.Double();
		Point2D p1 = new Point2D.Double();
		AffineTransform.getRotateInstance(2.0/3.0*Math.PI).transform(p,p0);
		AffineTransform.getRotateInstance(4.0/3.0*Math.PI).transform(p,p1);
		triangle.lineTo(p0.getX(),p0.getY());
		triangle.lineTo(p1.getX(),p1.getY());
		triangle.closePath();
		if(left)
		{
			triangle.transform(AffineTransform.getScaleInstance(-1,1));
		}
		triangle.transform(AffineTransform.getTranslateInstance(x,y));
	}

	public void draw(Graphics2D g)
	{
		if(!visible)
		{
			return;
		}
		g.setColor(new Color(color[hover][0],color[hover][1],color[hover][2],alpha));
		hover = ~hover & 1;
		g.fillOval(x-radius, y-radius, radius*2, radius*2);
		g.setColor(new Color(color[hover][0],color[hover][1],color[hover][2],alpha));
		hover = ~hover & 1;
		g.fill(triangle);
	}

	public void click(Point p)
	{
		if(!visible)
		{
			return;
		}
		if(isHover(p))
		{
			clickEvent.run();
		}
	}

	private boolean isHover(Point p)
	{
		return p == null ? false : p.distance(x,y) < radius ? true : false;
	}

	public void kill()
	{
		progress = 0;
		dead = true;
	}

	public void tick(Point p)
	{
		if(dead)
		{
			if(progress < 1)
			{
				progress += progressSpeed;
				if(progress > 1)
				{
					progress = 1;
				}
				alpha =  maxAlpha - (int)(maxAlpha * Main.genValue(-0.5,progress));
			}
			return;
		}
		if(progress<1)
		{
			progress += progressSpeed;
			if(progress > 1)
			{
				progress = 1;
			}
			alpha = (int) (maxAlpha * Main.genValue(-0.5,progress));
		}

		hover = isHover(p) ? 1 : 0;
	}

	public void setVisible(boolean b)
	{
		visible = b;
		if(!b)
		{
			hover = 0;
		}
	}

	public interface ClickEvent
	{
		public void run();
	}
}