# Fares and Surge Pricing

## How the fare is calculated
The fare for a ride is based on the travel distance between the pickup
location and the drop-off location.

- **Base fare formula:** `fare = distance × 10`
- The distance is measured along the actual route between pickup and
  drop-off (not a straight line).
- There is no separate booking fee or per-minute charge; the fare is
  distance-based.

**Example:** a 5 km ride costs `5 × 10 = 50`.

## Surge pricing
During periods of high demand, surge pricing may apply. When surge is
active, the fare is multiplied by a surge factor.

- **Surge factor:** `2×` the normal fare.
- **Surge formula:** `fare = distance × 10 × 2`

**Example:** the same 5 km ride during surge costs `5 × 10 × 2 = 100`.

Surge pricing is applied automatically based on demand at the time the
ride is requested. The fare shown when you request the ride is the fare
you pay.

## When is the fare confirmed?
The fare is calculated and shown when you request a ride. It does not
change after the ride is confirmed unless the route changes
significantly.
