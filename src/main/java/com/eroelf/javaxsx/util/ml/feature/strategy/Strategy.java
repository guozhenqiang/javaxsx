package com.eroelf.javaxsx.util.ml.feature.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.eroelf.javaxsx.util.ml.feature.Item;
import com.eroelf.javaxsx.util.ml.feature.model.Modeler;
import com.eroelf.javaxsx.util.ml.flow.controller.filter.ItemFilter;

/**
 * Any subclass of this class will be regarded as a strategy which generates a group of {@link Item} instances and models them according to some specified conditions and strategies.
 * 
 * @author weikun.zhong
 *
 * @param <T> the type of those {@link Item} instances generated by this strategy.
 */
public abstract class Strategy<T extends Item> implements Modeler
{
	/**
	 * Provides a set contains {@link Item} instances which correspond to this strategy but without being modeled.
	 * 
	 * @return a {@link Set} object contains all the candidates correspond to this strategy.
	 */
	protected abstract Set<T> candicates();

	/**
	 * Generates a list of modeled {@link Item} instances which correspond to this strategy.
	 * 
	 * @param preFilter a filter provide some additional restrictions which may be related to a specified situation but has nothing to do with this strategy.
	 * @return the generated {@link Item} instances list.
	 */
	public List<T> generate(ItemFilter<T> preFilter)
	{
		List<T> res=new ArrayList<>();
		for(T item : candicates())
		{
			if(preFilter.test(item))
			{
				item.modelBy(this);
				res.add(item);
			}
		}
		return res;
	}
}
