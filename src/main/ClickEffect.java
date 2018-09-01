package main;

import java.awt.Color;
import java.awt.Graphics2D;

public class ClickEffect
{
	private static final int finalRadius = 24;
	private static final double progressSpeed = 0.064;
	private static final int color = 128;

	private double progress;
	private boolean dead = false;
	private int radius;
	private int alpha = 255;
	private int x;
	private int y;

	public ClickEffect(int x,int y)
	{
		this.x = x;
		this.y = y;
	}

	public void draw(Graphics2D g)
	{
		g.setColor(new Color(color,color,color,alpha));
		g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
	}

	public void tick()
	{
		if(progress >= 1)
		{
			dead = true;
			return;
		}
		progress += progressSpeed;
		if(progress >= 1)
		{
			progress = 1;
		}

		alpha = 255 - (int) (255 * progress);
		radius = (int) (finalRadius * progress);
	}

	public boolean isDead()
	{
		return dead;
	}
}