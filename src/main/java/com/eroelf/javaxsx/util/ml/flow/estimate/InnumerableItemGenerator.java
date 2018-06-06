package com.eroelf.javaxsx.util.ml.flow.estimate;

import java.util.Iterator;
import java.util.List;

import com.eroelf.javaxsx.util.ml.feature.BatchSample;
import com.eroelf.javaxsx.util.ml.feature.BatchScoreableRestrictedBatchSample;
import com.eroelf.javaxsx.util.ml.feature.Item;
import com.eroelf.javaxsx.util.ml.feature.RestrictedBatchSample;
import com.eroelf.javaxsx.util.ml.feature.UpdatableByItem;
import com.eroelf.javaxsx.util.ml.feature.score.Scorer;
import com.eroelf.javaxsx.util.ml.feature.strategy.Strategy;
import com.eroelf.javaxsx.util.ml.flow.controller.InnumerableFlowHandler;
import com.eroelf.javaxsx.util.ml.flow.controller.filter.ItemFilter;
import com.eroelf.javaxsx.util.ml.flow.controller.filter.ItemFilterHandler;
import com.eroelf.javaxsx.util.ml.flow.controller.filter.NaiveItemFilterHandler;
import com.eroelf.javaxsx.util.ml.flow.estimate.statistics.IdleItemGroupStatistics;
import com.eroelf.javaxsx.util.ml.flow.estimate.statistics.ItemGroupStatistics;

/**
 * Controls an modeling and scoring flow in which {@link Item} candidates are innumerable but can be given and modeled by {@link Strategy}s, and then scores them.
 * 
 * @author weikun.zhong
 *
 * @param <T> the type of the generated {@link Item} objects.
 */
public abstract class InnumerableItemGenerator<T extends Item & UpdatableByItem<T>> implements ItemGenerator<T>
{
	/**
	 * This method will be called in the {@link InnumerableItemGenerator#update(Item) update} method for checking if the given {@code item} already exists in the candidate set (has been generated by another {@link Strategy} instance).
	 * 
	 * @param item the given {@link Item} object to be checked.
	 * @return the pre-generated {@link Item} object if found, otherwise {@code null}.
	 */
	protected abstract T findExistedItem(T item);

	/**
	 * This method will be called in the {@link InnumerableItemGenerator#update(Item) update} method for saving the given {@code item} if there was no pre-generated {@link Item} object, which is identical to the given {@code item}, found in the candidate set.
	 * 
	 * @param item the given {@link Item} object to be saved.
	 */
	protected abstract void saveToExisted(T item);

	/**
	 * Traverses all {@link Item} objects after all {@link Strategy} instances finished their {@link Strategy#generate(ItemFilter) generate} methods.
	 * 
	 * @return an {@link Iterable} group of those {@link Item} objects.
	 */
	protected abstract Iterable<T> getCandidates();

	/**
	 * Creates a specified flow handler for this generator.
	 * 
	 * @return a {@link InnumerableFlowHandler} object.
	 * @see {@link InnumerableFlowHandler}
	 */
	protected abstract InnumerableFlowHandler<T> getFlowHandler();

	/**
	 * Updates information of the given {@link Item} object.
	 * Different {@link Strategy} instances may generate identical {@link Item} objects, their features and information should be combined into one.
	 * 
	 * @param item the {@link Item} object generated by a certain {@link Strategy}
	 */
	protected void update(T item)
	{
		T existed=findExistedItem(item);
		if(existed!=null)
			existed.update(item);
		else
			saveToExisted(item);
	}

	/**
	 * Creates a specified filter handler for this generator to deal with {@link Item} objects with some filtering rules which has nothing to do with the flow handler returned by the {@link InnumerableItemGenerator#getFlowHandler() getFlowHandler} method.
	 * Returns the {@link NaiveItemFilterHandler} instance by default.
	 * 
	 * @return a {@link ItemFilterHandler} object.
	 * @see {@link ItemFilterHandler}
	 */
	protected ItemFilterHandler<T> getFilterHandler()
	{
		return NaiveItemFilterHandler.get();
	}

	/**
	 * Creates a specified {@link BatchScoreableRestrictedBatchSample} object for this generator.
	 * 
	 * @param batchSize the sample batch size.
	 * @return a {@link BatchScoreableRestrictedBatchSample} object.
	 * @see {@link BatchScoreableRestrictedBatchSample}
	 * @see {@link RestrictedBatchSample}
	 * @see {@link BatchSample}
	 */
	protected BatchScoreableRestrictedBatchSample<T> createBatchSample(int batchSize)
	{
		return new RestrictedBatchSample<T>(batchSize);
	}

	/**
	 * Retains some other information during the flow.
	 * Does nothing by default.
	 * This method will be called just after the specified {@link Item} object is modeled (before it be scored).
	 * 
	 * @param item the {@link Item} objects whose other information is about to be retained.
	 */
	protected void verbose(T item)
	{}

	/**
	 * Creates a specified {@link ItemGroupStatistics} object for this generator to handle the generated {@link Item} statistics.
	 * Returns the {@link IdleItemGroupStatistics} instance by default, which does nothing.
	 * 
	 * @return a {@link ItemGroupStatistics} object.
	 * @see {@link ItemGroupStatistics}
	 */
	protected ItemGroupStatistics<T> getItemGroupStatistics()
	{
		return IdleItemGroupStatistics.get();
	}

	@Override
	public List<T> generate(List<T> destination)
	{
		int start=destination.size();

		ItemFilterHandler<T> filterHandler=getFilterHandler();
		ItemFilter<T> preFilter=filterHandler.getPreFilter();
		ItemFilter<T> innerFilter=filterHandler.getInnerFilter();

		InnumerableFlowHandler<T> flowHandler=getFlowHandler();
		Scorer scorer=flowHandler.getScorer();

		for(Strategy<T> strategy : flowHandler.getStrategies())
		{
			for(T item : strategy.generate(preFilter))
			{
				update(item);
			}
		}

		ItemGroupStatistics<T> itemGroupStatistics=getItemGroupStatistics();

		int batchSize=flowHandler.getBatchSize();
		BatchScoreableRestrictedBatchSample<T> batchSample=createBatchSample(batchSize);
		int currSize;
		boolean needScore=false;
		for(T item : getCandidates())
		{
			currSize=batchSample.add(item);
			needScore=true;
			if(currSize==batchSize)
			{
				batchSample.scoreBy(scorer);
				needScore=false;
				for(T sample : batchSample)
				{
					if(innerFilter.test(sample))
					{
						itemGroupStatistics.increaseStatistics(sample);
						verbose(sample);
						destination.add(sample);
					}
				}
			}
		}
		if(needScore)
		{
			batchSample.scoreBy(scorer);
			needScore=false;
			for(T sample : batchSample)
			{
				if(innerFilter.test(sample))
				{
					itemGroupStatistics.increaseStatistics(sample);
					verbose(sample);
					destination.add(sample);
				}
			}
		}

		itemGroupStatistics.computeStatistics(destination.subList(start, destination.size()));
		ItemFilter<T> afterFilter=filterHandler.getAfterFilter(itemGroupStatistics);
		if(afterFilter!=null)
		{
			Iterator<T> iter=destination.listIterator(start);
			while(iter.hasNext())
			{
				T item=iter.next();
				if(!afterFilter.test(item))
					iter.remove();
			}
		}
		return destination;
	}
}
