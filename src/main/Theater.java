package main;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Theater
{
	private ArrayList<BufferedImage> imageList;
	private int now = -1;

	private AffineTransform[] trans;

	public Theater(Canvas c,ArrayList<BufferedImage> list)
	{
		imageList = list;
		if(list.size() > 1)
		{
			now = 1;
		}

		trans = new AffineTransform[list.size()];
		for(int x = 1; x < list.size(); x++)
		{
			BufferedImage bi = list.get(x);
			double scale = (double)c.getHeight() / bi.getHeight();
			trans[x] = AffineTransform.getTranslateInstance(c.getWidth() / 2 - bi.getWidth() * scale/2,0);
			trans[x].scale(scale,scale);
		}
	}

	public void draw(Graphics2D g)
	{
		if(now != -1)
		{
			g.drawImage(imageList.get(now),trans[now],null);
		}
	}

	public boolean hasNext(boolean next)
	{
		return next ? now != -1 && now + 1 < imageList.size() : now - 1 > 0;
	}

	public void movePage(boolean next)
	{
		if(next)
		{
			if(hasNext(true))
			{
				now++;
			}
		}
		else
		{
			if(hasNext(false))
			{
				now--;
			}
		}
	}
}