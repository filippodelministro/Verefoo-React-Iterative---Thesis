# Q&A Prep — An Incremental Approach to Automatic Firewall Configuration

Answers written to be spoken directly during the defense. Based on the thesis chapters (Background, Iterative Approach, Implementation & Validation, Conclusions) and the presentation.

---

## The 3 core questions

### How does VEREFOO work in detail? What is a MaxSMT problem?

VEREFOO takes two inputs: a Service Graph — a directed graph modeling the network's nodes and connectivity, independent of any security consideration — and a set of Network Security Requirements, which state which traffic flows must be allowed or denied. From the Service Graph, VEREFOO derives an Allocation Graph: it inserts an Allocation Place on every link, representing a candidate position where a firewall could be placed.

It then encodes the whole placement-and-configuration problem as a MaxSMT instance — a Maximum Satisfiability Modulo Theories problem. Concretely: each Allocation Place becomes a boolean decision variable the solver can turn on or off. The hard constraints encode correctness — they force the solver to only consider solutions where every NSR is actually satisfied, no exceptions. The soft constraints encode optimality — they're weighted, and the solver tries to maximize how many of them it satisfies, which in practice means minimizing the number of firewalls allocated and the number of filtering rules generated. This is solved with the Z3 SMT solver, and because the hard constraints are hard, any solution returned is correct by construction — that's where the formal correctness guarantee comes from.

### What are VerefooSerializer, VerefooProxy and Translator?

They're the three main components of the pipeline, and it's worth being precise about which one this thesis actually touches. VerefooSerializer is the entry point: whatever variant you're running — vanilla, React, or now iterative — the call starts there; it's what builds the Allocation Graph and decides which constructor logic to run. VerefooProxy is the layer right below it: it's what actually classifies the NSRs into added, kept, deleted, identifies the reconfigurable area, and builds the MaxSMT problem to hand to Z3. The Translator is the last step: once Z3 returns a model, the Translator converts it back from the internal representation into a concrete NFV object — the actual firewall allocation and filtering policies.

My contribution only modifies VerefooSerializer: I added a new constructor that wraps the existing call to React-VEREFOO — which itself calls VerefooProxy — inside a loop, feeding it one requirement at a time. VerefooProxy, the MaxSMT encoding, and the Translator are all used exactly as they already were — I didn't change a single line in any of them.

### Why only a qualitative test, not a quantitative one?

Two reasons, one about scope and one about what a qualitative test can actually prove that a quantitative one can't. First, the primary goal of this validation was to demonstrate correctness — that the iterative algorithm, applied to a real topology, produces a configuration where every requirement is genuinely satisfied and no previously satisfied requirement gets silently broken along the way. That's something you show by tracing the execution step by step and checking each one, not by measuring timing.

Second, a meaningful quantitative comparison needs scale — larger topologies, larger and more varied NSR sets, multiple runs to account for solver variance — to actually show a timing advantage over the monolithic baseline. CESNET, with 23 nodes and 15 requirements, is realistic but small; both the iterative and the monolithic run solve in a fraction of a second, so at this scale a timing comparison wouldn't be very informative — the difference wouldn't be stressed enough to say anything meaningful. It's explicitly the first item I list in future work: benchmarking execution time across networks and NSR sets of varying scale.

---

## Other likely questions

### Why does the iterative result have one extra firewall / rule compared to the monolithic one?

Because the monolithic solver sees the entire set of fifteen requirements at once, so it can find a single Allocation Place that happens to satisfy two different isolation needs at the same time — in my case, a point on the network that covers both Ostrava's isolation and part of Tabor's. The iterative process commits to a placement for Ostrava's isolation early, as part of P0, long before it knows Tabor's requirement even exists — so it can't retroactively relocate that firewall to also cover Tabor, and ends up needing a dedicated one, plus a small follow-up rule two iterations later. Same firewall count, one extra ALLOW rule — that's the direct cost of never having global visibility.

### Why is the iterative approach's optimality only local, not global?

Because at each step the solver only reconsiders the specific area of the network affected by the one new requirement being introduced — everything else is fixed, treated as already correct. That's the whole point: it's what keeps each subproblem small. But it also means the solver can't reshuffle a decision it already committed to in a previous iteration, even if a later requirement would make a different global arrangement more efficient. Vanilla VEREFOO, seeing everything at once, can always find that better global arrangement — that's the trade-off.

### What happens if a new NSR makes the problem UNSAT during an iteration?

The loop doesn't break immediately. If a step returns UNSAT, the current configuration — the one built from all previously successful iterations — is kept as the result, and the failure is recorded and surfaced to the caller once the loop finishes. So you don't lose the work already done for the earlier, successfully satisfied requirements; you just get told which specific NSR couldn't be enforced.

### Does your approach also support removing requirements, or only adding them?

Only additions, in this thesis. React-VEREFOO itself does support deleted requirements as a category, but the iterative loop I built only ever passes one added requirement per call. Extending it to also handle removals — and arbitrary mixes of additions and removals in the same run — is explicitly listed as future work.

### How do you choose which node to reconfigure when there are multiple equally-cheap candidates (tie-breaking)?

Currently, nothing explicit — the solver picks based on the soft constraints biasing it toward reusing existing configuration, but among genuinely equally-cheap candidates there's no additional criterion. I actually discuss this directly in the thesis: this tie can propagate unevenly into later iterations — one candidate might turn out to also cover a future requirement for free, another might not — and there's no way to know that in advance. I flag a traffic-aware ordering strategy as future work specifically to address this: ordering or choosing among ties based on anticipated future traffic patterns, to minimize the number of later reconfigurations.

### Why did you use CESNET specifically as the test topology?

It's a real, publicly documented backbone topology — the Czech national academic and research network — with a realistic mix of link capacities and a non-trivial structure: a central hub in Praha, multiple redundant paths to some destinations, and peripheral nodes reachable only through longer routes. That gives enough structural complexity to exercise interesting cases — reconfiguration, reuse, ties — without needing a synthetic topology. To be precise: the topology and link capacities are real, but the specific NSR set I used is one I constructed for this evaluation, not CESNET's actual production security policy.

### What's the extra cost/overhead of the iterative approach compared to a single batch call?

Each solver invocation carries a fixed overhead — parsing the input, building the MaxSMT encoding, initializing the solver — independent of how small the actual problem is. With N requirements introduced one at a time, that fixed cost is paid N times instead of once. So there's a real trade-off: the iterative approach keeps each individual problem minimal, but accumulates this per-call overhead linearly with the number of new requirements. Quantifying exactly where that trade-off tips in either approach's favor is, again, the main open question for the quantitative benchmark proposed as future work.
