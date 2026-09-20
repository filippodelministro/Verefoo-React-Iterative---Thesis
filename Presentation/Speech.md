# Presentation Speech — "An Incremental Approach to Automatic Firewall Configuration"

Target duration: **~12:00**, leaving **~3 min** for questions within the 15' total.
Each block is marked with its slide and the expected cumulative time at the end of the block.

---

## SLIDE 1 - Title — 0:00 → 0:30

Good morning everyone, and thank you for being here. My name is Filippo Del Ministro, and today I'll be presenting my thesis work, titled "An Incremental Approach to Automatic Firewall Configuration," carried out under the supervision of professors Fulvio Valenza, Daniele Bringhenti, Francesco Pizzato, and Riccardo Sisto.

## SLIDE 2 - Background — 0:30 → 1:45

- modern network are complex
- for this reason we tend to use microsegmentation rather than perimetral FW; this allow to have finer-grained control over traffic, containment of lateral movement and and general much more control over the network.
- If micorsegmentation represent a huge enhancement in terms of security, could be a real challenge in terms of FW placement and configuration: in this context, manual configruation is prone to error potentially blocking legitimate traffic or, worse, allowing illegitimate one


Let's start with the context. Nowadyas the growing complexity of modern networks is exactly why security is shifting from a single perimeter firewall to a distributed approach — multiple packet-filtering firewalls spread across the topology, what we call microsegmentation. This shift brings well-known benefits: finer-grained control over traffic, containment of lateral movement if one segment is compromised, and policies tailored to each part of the network — for instance, a stricter policy for the data centre than for a general office segment. But it also complicates configuration: there are more firewall instances to manage, and if that configuration is kept manual, it becomes error-prone and offers no guarantee of correctness — a single mistake can silently violate a security requirement, or block legitimate traffic.

## SLIDE 3 - What is VEREFOO? — 1:45 → 2:45

- In this context, automated tools for FW placement and configuration, such as VEREFOO, can help network admin to administrate their network without any configuration errors.
- Given netwrok topology and a set of security requirements, VEREFOO not only place the FW, but also configure it to reache the desired status. VEREFOO also guarantee optimatily and formal correctness

Before looking at the incremental variants, let's briefly look at VEREFOO itself. Given a network topology, modeled as a Service Graph, and a set of Network Security Requirements — the NSRs, describing which traffic flows must be allowed or denied — VEREFOO automates the whole placement and configuration process. Internally, it builds an Allocation Graph, marking every link of the network as a candidate firewall position, an Allocation Place, and encodes the placement and configuration problem as a MaxSMT instance. Solving it produces a firewall allocation scheme and a filtering policy for each allocated firewall, minimizing the number of firewalls and rules while guaranteeing that every requirement is satisfied by construction.

## SLIDE 4 - VEREFOO & React-VEREFOO — 2:45 → 3:45

- in modern context, where networks are highly dynamic and need to changes through time, VEREFOO is still a valid tool, but its monolitic approach woould trigger a complete full recomputation from scratch over the already enforced rules. This is potentically not optimal, due to the fact that in real life world only a few part of the network are really changing, but frequently.
- React-VEREFOO solves this issue by reconfiguring only the FW which needs to be changes by matching the new requiremtns 
- Speaking of performances, React-VEREFOO is much faster than the monolothic version, but this difference actually massively relies on the number of changes that are needed to be implemented: the fewer the changes, the faster the execution; the larger the changes, the close to the monolithic it gets.
- On this very last assumption, there is the key focus of this thesis: pushing this obeservation to its extreme and add only one new requirement at each time rather than a batch of multiple requirement

VEREFOO, as we just saw, guarantees correctness and optimality by construction, but it's monolithic: any change to the NSR set triggers a full recomputation from scratch. React-VEREFOO was developed to overcome this: it classifies each NSR as added, kept, or deleted, and reconfigures only the portion of the network actually affected by the change. This gives a big speedup when only a small fraction of requirements changes — but that advantage shrinks as the batch of new requirements grows. That's the gap this thesis closes.

## SLIDE 5 - Research Question — 3:45 → 4:45

- On this very last assumption, there is the key focus of this thesis: pushing this obeservation to its extreme and add only one new requirement at each time rather than a batch of multiple requirement

And it's exactly this observation that motivates the question at the heart of my thesis: if React-VEREFOO is fastest when only a few new requirements are involved, why not always give it just one at a time? Instead of feeding the engine a batch of requests, which progressively degrades its performance, why not push the incremental principle to its extreme, keeping the problem minimal at every step, regardless of how many requirements must ultimately be incorporated?

## SLIDE 6 - Thesis Contribution: Iterative VEREFOO — 4:45 → 6:30

This is exactly the contribution of this thesis: the Iterative VEREFOO approach. The idea is simple to state but effective in practice: instead of passing React-VEREFOO a batch of new requirements, we introduce them one at a time, invoking React-VEREFOO internally at each step to compute the minimal update to the current configuration. To do this, the algorithm maintains two structures that evolve across iterations: consideredProps, the set of already-enforced requirements, passed as the initial set to the next step; and currentGraph, the network configuration updated after the most recently verified requirement. This way, every iteration always receives the smallest possible incremental problem — just one new requirement on top of an already stable, verified configuration. And, crucially, correctness is always guaranteed: every NSR in the final set is satisfied by the resulting configuration.

## SLIDE 7 - Structural Comparison — 6:30 → 7:15

To frame the differences more clearly, this table summarizes how the three variants behave. Vanilla VEREFOO makes a single solver call with all requirements together, achieving the global optimum. React-VEREFOO also makes a single call, but on a batch, reusing the existing configuration. The iterative approach instead makes one call per single requirement, reuses the configuration at every step, and reaches a local — greedy, so to speak — optimum, but always with the advantage of working on the smallest possible portion of the network at each step.

## SLIDE 8 - Implementation — 7:15 → 8:15

From an implementation standpoint, the approach was integrated by modifying VerefooSerializer, the entry point shared by all three algorithm variants. The new constructor wraps the call to React-VEREFOO in a loop that introduces one requirement at a time, updating consideredProps and currentGraph between iterations. The rest of the pipeline — the MaxSMT encoding, VerefooProxy, the Translator — remains unchanged: a targeted modification, not a rewrite.

## SLIDE 9 - Validation: CESNET Topology — 8:15 → 9:30

To validate the approach, I worked on a real topology: CESNET, the Czech academic backbone, 23 nodes and 28 links. Starting from an initial configuration with ten NSRs already enforced by three firewalls, I introduced five new requirements one at a time, tracing each iteration step by step to confirm that every new requirement was satisfied without breaking any of the previous ones. The final result: fifteen requirements all satisfied, with five firewalls in the final configuration.

## SLIDE 10 - Validation: Iterative vs Monolithic — 9:30 → 10:30

To gauge how good this configuration was, I compared it against the one obtained by running vanilla VEREFOO, in a single monolithic solve, over the same fifteen requirements. Both approaches allocate exactly five firewalls, three of them in the exact same positions. The iterative configuration, however, needs one additional rule: the monolithic solver can avoid it because it sees all the requirements at once, and can make a single firewall serve two purposes at the same time — something the iterative process cannot do, since it has to commit to each placement before knowing the later requirements. This is the price of incrementality: nearly negligible, and in any case offset by the fact that the iterative approach never relocates an already-valid firewall, thereby avoiding the operational cost and the transient insecure states typical of a full monolithic re-run.

## SLIDE 11 - Conclusions & Future Work — 10:30 → 11:45

To conclude: the iterative approach was validated qualitatively on a realistic topology, and reaches a result that almost exactly matches the monolithic baseline in firewall count — with just one extra rule — despite never having visibility beyond the single requirement it's processing at that moment. I expect scalability to be the main advantage of this approach: recomputing the entire configuration from scratch becomes increasingly expensive compared to solving one requirement at a time, as topologies and reconfigurations grow larger. As future work, the natural next steps are: a quantitative evaluation of execution time against the monolithic baseline at larger scale, a traffic-based ordering of requirements to reduce tie-driven reconfigurations, and support for NSR removal, alongside the additions handled in this thesis.

## SLIDE 12 - Thank you — 11:45 → 12:00

Thank you for your attention, and I'm happy to take any questions.

---

## Notes for Q&A (~3 min buffer)

Likely questions, with a ready 1-2 sentence answer:

- **Why not the global optimum?** Because each step only sees the current requirement: it's a deliberate trade-off in exchange for a problem that's always minimal to solve, consistent with how requirements actually accumulate over time in a production network.
- **Overhead of so many solver calls?** Each call carries a fixed cost for parsing, encoding, and solver initialization that accumulates linearly with the number of iterations — this is the main trade-off mentioned, to be quantified in future work.
- **Why only a qualitative validation and not a quantitative benchmark?** For this thesis, the primary goal was to demonstrate structural correctness and feasibility of the approach on a realistic case; systematically measuring execution time is the first item in future work.
- **Is the extra rule a problem?** No: it's a single isolated case out of fifteen requirements, and the stability advantage (never relocating already-valid firewalls) largely offsets it in a real operational context.
