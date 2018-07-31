"tweefill"
========
Not for public use. This is a protoyping repo for implementing various distributed rate limiting algorithms.

The main implementation on the master branch is the treefill algorithm described in the paper ["An optimal distributed trigger counting algorithm for large-scale networked systems"][1]. On the hackweek branch we have a homebrew implementation based more on real-world needs for rate limiting clients, given a better understanding of / greater interest in performance over a real network than we suppose the original author might have had, also with support for more than one event coming in at a time. 

On the hackweek branch, there is primarily an implementation of a modified version of Kim et al's Treefill. We found certain aspects of Treefill unworkable or nonintuitive, and so modified the core algorithm.

For our (hackweek) use case, we are interested in requests for some number of permits W that don't just come in one at a time (that is, we want to support acquire(n) where n can be any positive number). Our implementation both supports this easily and supports, for W <= N (where N is the number of nodes in the TweeFill cluster), falling back to a centralized (number of rounds = 1, number of nodes allocated to the request doesn't have to be huge if not needed) model. As stated in the Kim paper, for cases where W is not very much greater than N, we don't need to worry about rounds and Detects and shuffling permit counts around and can do a much simpler form of trigger counting more akin to current methods of rate limiting.

algorithms (on master branch)
----------

### treefill
- almost functional, but not quite yet

##### what works
- message passing & processing of Detect, Full, and WindowFull
- message transport layer abstracted for testing & simulation purposes
- nodes self-calculate their role in the tree (no observer/god role required)
- nodes independently increment their round counters

#### limitataions
- implementation assumes a perfect tree of nodes
- not fault tolerant yet

#### bugs / not working yet
- misunderstanding in the way the leaf node detectors are allocated 
  - too many Detects are generated before WindowFull is broadcast
  - all nodes particiapte in the detect tree
  - number of Detect slots is tied to the dimensions of the tree and cluster size and doesn't match what's really needed


##### pending refactors & features
- lots of renaming
  - karytree to karitytree
- datastructures are overly verbose to provide high visibility in initial code design and debugging; can be made far more efficient when the whole thing functions properly and passes tests
- support arbitrary cluster sizes
- support fault tolerance and rolling deployments
  - replace single root node with some form of virtualized/distributed root node
  - rearrange karitytree to be depth-first


citations & references
----------------------

[1]: https://doi.org/10.1177/0037549713485499 "An optimal distributed trigger counting algorithm for large-scale networked systems"
Seokhyun Kim, Jaeheung Lee, Yongsu Park, Yookun Cho
SIMULATION 
Vol 89, Issue 7, pp. 846 - 859
First Published May 17, 2013

[2]: https://github.com/SeokhyunKim/treefill "treefill netlogo simulation"



development
-----------
Since this is a work-in-progress, all development is happening on hackweek branch for now.

Developed with:
- Java 1.8.0_111
- Maven 3.5.2 
