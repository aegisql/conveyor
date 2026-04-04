# Conveyor High-Level Overview

This diagram is a structural overview of an `AssemblingConveyor`, not a single-message sequence. It shows the main loader entry points, the two internal queues, the inner conveyor thread, the collector of active building sites, builder and readiness processing, timeout support, consumer outputs, attached futures, eviction, and acknowledge handling.

One `BuildingSite` box in the diagram stands for many active sites stored in the collector, one per key. To keep the diagram readable, related flows are grouped together instead of showing every individual command variant. `resultConsumer()` is shown in both roles it can play: configuring the default result chain and sending per-key result-consumer carts. `scrapConsumer()` is conveyor-level configuration.

```mermaid
flowchart LR
    subgraph Entry["Loaders and Configuration"]
        PL["PartLoader: part()"]
        SPL["StaticPartLoader: staticPart()"]
        BL["BuilderLoader: build()"]
        FL["FutureLoader: future()"]
        CL["CommandLoader: command()"]
        RCL["ResultConsumerLoader: resultConsumer()"]
        SCL["ScrapConsumerLoader: scrapConsumer()"]
    end

    subgraph Runtime["AssemblingConveyor Runtime"]
        CV["Cart validation"]
        CMDV["Command validation"]
        IQ["inQueue"]
        MQ["mQueue"]
        WT["Inner conveyor thread"]
        SV["Static values store"]
        COL["Collector of building sites"]
        BS["BuildingSite per key"]
        CC["Cart consumer and label routing"]
        B["Builder"]
        RE["Readiness evaluator"]
        DP["DelayProvider"]
        TO["Timeout action"]
        EV["Key eviction"]
    end

    subgraph Outputs["Consumers, Futures, and Ack"]
        PF["Placement and command futures"]
        AF["Attached product futures"]
        RC["Result consumer chain"]
        SC["Scrap consumer chain"]
        ACK["Acknowledge action"]
    end

    PL -->|part cart| CV
    SPL -->|static part cart| CV
    BL -->|builder or create cart| CV
    FL -->|future cart| CV
    RCL -->|per-key result consumer cart| CV
    CL -->|management command| CMDV
    RCL -->|set default result chain| RC
    SCL -->|set default scrap chain| SC

    CV --> IQ
    CMDV --> MQ
    CV -.->|place future| PF
    CMDV -.->|command future| PF

    IQ --> WT
    MQ -->|processed before next part cart| WT

    WT -->|update or delete static values| SV
    WT -->|find or create site by key| COL
    COL --> BS
    SV -->|apply static parts to new sites| BS

    WT -->|apply part, future, or result-consumer cart| BS
    BS --> CC
    CC -->|invoke label-specific builder API| B
    BS --> RE
    RE -->|ready or waiting| WT

    BS -->|register or update expiration| DP
    DP -->|expired keys| WT
    WT -->|timeout processing| BS
    BS --> TO
    TO --> BS

    BS -->|ProductBin with acknowledge handle| RC
    WT -->|ScrapBin for cart or site failures, sometimes with acknowledge handle| SC
    BS -->|complete, cancel, or fail| AF
    WT -->|complete true, false, or exceptionally| PF
    WT -->|evict site by status| EV
    EV -->|auto acknowledge if enabled| ACK
```
