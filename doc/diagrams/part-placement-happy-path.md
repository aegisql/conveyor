# Part Placement Happy Path

This diagram shows the normal `AssemblingConveyor` flow for placing a new part. It intentionally leaves out rejection, timeout, and shutdown side paths so the core part-placement lifecycle stays easy to read.

The flow starts when a caller submits a new cart with `place()`. After basic pre-placement validation, the cart is queued and `place()` immediately returns the cart's `CompletableFuture<Boolean>` to the caller. The conveyor's inner worker thread later processes the cart, routes it to an existing or newly created building site for the key, and applies it through the builder API. If the build becomes ready, the conveyor builds the product, passes a `ProductBin` with an acknowledge handle to the result consumer, completes any attached result futures with the product, and then evicts the building site. During eviction, auto-acknowledge may call the configured acknowledge action for `Status.READY`. If the build is still not ready, the site stays open and waits for more parts. On this happy path, once the cart has been processed, the placement future is completed successfully.

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
    participant Consumer as Result Consumer
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

    alt New key
        Site->>Builder: create builder
    else Existing key
        Site->>Builder: reuse existing builder
    end

    Worker->>Builder: apply labeled part
    Worker->>Site: check readiness

    alt Ready to complete
        Worker->>Site: build()
        Site->>Builder: get product
        Builder-->>Site: product
        Site->>Consumer: pass ProductBin with acknowledge handle
        Site->>ResultFuture: complete(product)
        Worker->>Eviction: evict site by key
        Eviction->>Ack: auto acknowledge READY if enabled
        Worker->>Future: complete(true)
    else Not ready
        Site-->>Worker: keep waiting for more parts
        Worker->>Future: complete(true)
    end

    Future-->>User: placement processed
```
