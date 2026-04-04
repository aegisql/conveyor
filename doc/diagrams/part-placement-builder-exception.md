# Part Placement Builder Exception Path

This diagram shows a focused failure path for `AssemblingConveyor`: a new part is placed, the builder API throws while applying that part, the current build is marked invalid, the failed build is sent to the scrap consumer, and the placement `CompletableFuture<Boolean>` completes exceptionally.

The diagram stays intentionally narrow. It does not include timeout, keep-running, recovery, or shutdown branches. On this path, the cart scrap handling completes the placement future exceptionally, the site scrap path sends a `ScrapBin` with an acknowledge handle to the scrap consumer, any result futures attached to the building site are completed exceptionally, and the site is evicted. During eviction, auto-acknowledge may call the configured acknowledge action for `Status.INVALID`.

```mermaid
sequenceDiagram
    autonumber

    actor User
    participant API as place()
    participant Future as CompletableFuture<Boolean>
    participant Queue as Input Queue
    participant Worker as Inner Conveyor Thread
    participant Site as Building Site (per key)
    participant Builder as Builder API
    participant CartScrap as Cart Scrap Path
    participant Scrap as Scrap Consumer
    participant ResultFuture as Attached Result Future(s)
    participant Eviction as Eviction
    participant Ack as Acknowledge Action

    User->>API: place(new part cart)
    API->>API: run pre-placement validation
    API->>Queue: enqueue cart
    API-->>User: return placement future

    Worker->>Queue: poll next cart
    Queue-->>Worker: cart
    Worker->>Site: find or create building site by key
    Worker->>Builder: apply labeled part
    Builder-->>Worker: throw exception

    Worker->>Site: mark build invalid and terminate processing
    Worker->>CartScrap: emit cart scrap with error
    CartScrap->>Future: complete exceptionally
    Worker->>Scrap: emit failed building site ScrapBin with acknowledge handle
    Site->>ResultFuture: complete exceptionally
    Worker->>Eviction: evict site by key
    Eviction->>Ack: auto acknowledge INVALID if enabled
```
