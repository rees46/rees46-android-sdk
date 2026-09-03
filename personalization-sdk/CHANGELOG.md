# personalization-sdk changelog

## Unreleased

### Features

* Purchase tracking: optional `isGiftPackage` on `PurchaseTrackingRequest` — sent as `gift_package` only when `true`, the way `tax_free` is.
* Strict purchase tracking: `SDK.trackPurchase(PurchaseTrackingRequest, …)` with `PurchaseItemRequest` / `PurchaseTrackingRequest` (camelCase in public API; wire keys snake_case inside serialization). Client validation before network; `tax_free` only when `isTaxFree` is true; optional fields omitted when unset. Demo app and Espresso e2e taps for minimal and full payloads.

### Deprecations

* `TrackEvent.PURCHASE` (and `track(TrackEvent.PURCHASE, Params, ...)`) — use `SDK.trackPurchase(PurchaseTrackingRequest, ...)` instead.

## Earlier releases

See the repository root and release tags for history prior to this file.
