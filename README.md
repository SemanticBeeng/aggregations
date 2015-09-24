# aggregations

Fast accurate low memory type-safe aggregations DSL for Spark

The intention is to add many aggregations over time.  This code was motivated by an MVP which required a way to approximate a median in a single pass and without doing a full sort.  So at the moment we just have a median estimator and it's underlying structure, a capped size bin histogram.

## MedianEstimator

The current implementation allows for the potential to try many different strategies for merging bins in the underlying CappedBinHistogram to see which performs best.  The current merge strategy is quite simple and works for the aforementioned MVP. The use case was that we have many keys and we want to compute the median of the values for each key in a map-reduce job via sparks `combineByKey`.  When the number of values is 2 - 3 orders of magnitude greater than the memory limit given to the MedianEstimator performance starts to become unpredictable; a more complex merge strategy would need to be implemented.

When this is run just as a sliding window, i.e. a non-parallelisable yet memory and speed efficient way to compute a median the accuracy is very good.

### Performance Results

#### On Real Data (as Map Reduce Job)

Experiment based on real data where the O(memory) cap is set to 1000.

```
Average Error: 7.879808917316398E-4  (0.08 %)
Worst Error: 0.05264586160108548
```

The particularly bad error spikes up once we get to ~3,000,000 values.  We start seeing errors above 1% when the number of values exceed 100,000.

#### On Dummy Data (as Sliding Window)

O(100) memory with sliding window implementation, on dummy random numbers

**Uniform distribution**

```
Error Average = 1.989344102340268E-4
Worst Error = 0.0014935776162501246
```

**Normal distribution**

```
Error Average = 0.0017381514563992078
Worst Error = 0.008912530737190746
```

### Future Improvements

As discussed, when the number of values becomes 2 - 3 orders of magnitude greater than the memory allocation we see "high" errors (depending on what is defined as acceptable by the use case).  A potentially extremly effective solution would be to change the objective function in the merge strategy to try to merge bins that have very low probability of containing the median.  

To make this clearer, suppose we have memory allocation 1000, then once we need to perform a merge we ought to have sampled at least 1000 examples.  We can then argue that there is a very low probability that the actual median will be one of the end points, so we should favour merging these endpoints.  Experiments are needed to confirm there are not any strange edge cases that could cause some kind of catestrophic error.

Only a couple full days of time has been put into this code base so there is much to be done!

## CappedBinHistogram

This is the underlying structure that is used by the `MedianEstimator`, but is a highly useful structure in of itself.  It could prove to be extremly useful at generating probability distributions from the data (whereas the common technique in Data Science is just to assume some distribution that makes the maths easy).


